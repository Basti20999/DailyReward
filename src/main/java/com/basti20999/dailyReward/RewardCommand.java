package com.basti20999.dailyReward;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RewardCommand implements CommandExecutor {

    private final DailyReward plugin;

    public RewardCommand(DailyReward plugin) {
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

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(translateHexColorCodes(name));
            if (lore != null) {
                lore.replaceAll(this::translateHexColorCodes);
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(translateHexColorCodes(plugin.getConfig().getString("messages.general.only_players")));
            return true;
        }

        String title = translateHexColorCodes(plugin.getConfig().getString("gui.title"));
        int invSize = plugin.getConfig().getInt("gui.size", 27);
        Inventory inv = Bukkit.createInventory(null, invSize, title);

        long now = System.currentTimeMillis();
        Long last = plugin.getRewardTimes().get(p.getUniqueId());
        int cooldown = plugin.getConfig().getInt("cooldown_hours", 12);

        List<String> lore = plugin.getConfig().getStringList("gui.reward_lore");
        String rewardName = (last == null || now - last >= cooldown * 60 * 60 * 1000L)
                ? translateHexColorCodes(plugin.getConfig().getString("gui.reward_name_ready"))
                : translateHexColorCodes(plugin.getConfig().getString("gui.reward_name_cooldown"));

        Material rewardMaterial = Material.valueOf(plugin.getConfig().getString("gui.reward_material", "CHEST_MINECART"));
        ItemStack rewardItem = createItem(rewardMaterial, rewardName, lore);
        int rewardSlot = plugin.getConfig().getInt("gui.reward_slot", 13);
        inv.setItem(rewardSlot, rewardItem);

        Material borderMaterial = Material.valueOf(plugin.getConfig().getString("gui.border_material", "BLACK_STAINED_GLASS_PANE"));
        String borderName = translateHexColorCodes(plugin.getConfig().getString("gui.border_name", "§f"));
        ItemStack borderItem = createItem(borderMaterial, borderName, null);
        List<Integer> borderSlots = plugin.getConfig().getIntegerList("gui.border_slots");
        for (int slot : borderSlots) {
            inv.setItem(slot, borderItem);
        }

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.open.sound")), (float) plugin.getConfig().getDouble("sounds.open.volume"), (float) plugin.getConfig().getDouble("sounds.open.pitch"));
        return true;
    }
}