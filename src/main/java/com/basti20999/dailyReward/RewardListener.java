package com.basti20999.dailyReward;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RewardListener implements Listener {

    private final DailyReward plugin;

    public RewardListener(DailyReward plugin) {
        this.plugin = plugin;
    }

    private String translateHexColorCodes(String message) {
        // Ersetze #RRGGBB durch das Minecraft Hex-Format §x§R§R§G§G§B§B
        Pattern hexPattern = Pattern.compile("#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group(1);
            char[] chars = color.toCharArray();
            String replacement = "§x§" + chars[0] + "§" + chars[1] + "§" + chars[2] + "§" + chars[3] + "§" + chars[4] + "§" + chars[5];
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);
        // Nun die standard & Codes parsen
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(translateHexColorCodes(plugin.getConfig().getString("gui.title")))) {
            e.setCancelled(true);

            int rewardSlot = plugin.getConfig().getInt("gui.reward_slot", 13);
            if (e.getSlot() == rewardSlot && e.getWhoClicked() instanceof Player p) {
                long now = System.currentTimeMillis();
                Long last = plugin.getRewardTimes().get(p.getUniqueId());
                int cooldown = plugin.getConfig().getInt("cooldown_hours", 12);

                if (last == null || now - last >= cooldown * 60 * 60 * 1000L) {
                    int min = plugin.getConfig().getInt("money.min", 10000);
                    int max = plugin.getConfig().getInt("money.max", 50000);
                    int money = new Random().nextInt(max - min + 1) + min;

                    String keyType = plugin.getConfig().getString("keys.type", "rare");
                    int keyAmount = plugin.getConfig().getInt("keys.amount", 1);
                    int gems = plugin.getConfig().getInt("gems", 250);

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getConfig().getString("commands.money_give").replace("{player}", p.getName()).replace("{amount}", String.valueOf(money)));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getConfig().getString("commands.keys_give").replace("{player}", p.getName()).replace("{type}", keyType).replace("{amount}", String.valueOf(keyAmount)));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getConfig().getString("commands.gems_give").replace("{player}", p.getName()).replace("{amount}", String.valueOf(gems)));

                    p.sendMessage(translateHexColorCodes(plugin.getConfig().getString("messages.success")));
                    p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.success.sound")), (float) plugin.getConfig().getDouble("sounds.success.volume"), (float) plugin.getConfig().getDouble("sounds.success.pitch"));

                    plugin.getRewardTimes().put(p.getUniqueId(), now);
                    p.closeInventory();
                } else {
                    p.sendMessage(translateHexColorCodes(plugin.getConfig().getString("messages.on_cooldown")));
                    p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.error.sound")), (float) plugin.getConfig().getDouble("sounds.error.volume"), (float) plugin.getConfig().getDouble("sounds.error.pitch"));
                    p.closeInventory();
                }
            }
        }
    }
}