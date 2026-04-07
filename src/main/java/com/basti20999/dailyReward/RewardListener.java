package com.basti20999.dailyReward;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

public class RewardListener implements Listener {

    private final DailyReward plugin;

    public RewardListener(DailyReward plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String guiTitle = ColorUtil.translate(plugin.getConfig().getString("gui.title", "&8Daily Reward"));
        if (!e.getView().getTitle().equals(guiTitle)) return;

        e.setCancelled(true);

        int rewardSlot = plugin.getConfig().getInt("gui.reward_slot", 13);
        if (e.getSlot() != rewardSlot || !(e.getWhoClicked() instanceof Player p)) return;

        long now = System.currentTimeMillis();
        Long last = plugin.getRewardTimes().get(p.getUniqueId());
        long cooldownMillis = plugin.getConfig().getLong("cooldown_hours", 24) * 60L * 60L * 1000L;

        if (last != null && now - last < cooldownMillis) {
            p.sendMessage(ColorUtil.translate(plugin.getConfig().getString(
                    "messages.on_cooldown", "&cYou have already claimed your daily reward!")));
            playSound(p, plugin.getConfig().getString("sounds.error.sound", "ENTITY_VILLAGER_NO"),
                    (float) plugin.getConfig().getDouble("sounds.error.volume", 1.0),
                    (float) plugin.getConfig().getDouble("sounds.error.pitch", 1.0));
            p.closeInventory();
            return;
        }

        int min = plugin.getConfig().getInt("money.min", 100);
        int max = plugin.getConfig().getInt("money.max", 1000);
        int money = ThreadLocalRandom.current().nextInt(min, max + 1);

        String keyType = plugin.getConfig().getString("keys.type", "rare");
        int keyAmount = plugin.getConfig().getInt("keys.amount", 1);
        int gems = plugin.getConfig().getInt("gems", 250);

        dispatchCommand(plugin.getConfig().getString("commands.money_give", ""),
                p.getName(), String.valueOf(money), keyType);
        dispatchCommand(plugin.getConfig().getString("commands.keys_give", ""),
                p.getName(), String.valueOf(keyAmount), keyType);
        dispatchCommand(plugin.getConfig().getString("commands.gems_give", ""),
                p.getName(), String.valueOf(gems), keyType);

        plugin.getRewardTimes().put(p.getUniqueId(), now);
        try {
            plugin.getDatabase().save(p.getUniqueId(), now);
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to save reward for " + p.getName() + ": " + ex.getMessage());
        }

        p.sendMessage(ColorUtil.translate(plugin.getConfig().getString(
                "messages.success", "&aDaily reward claimed!")));
        playSound(p, plugin.getConfig().getString("sounds.success.sound", "ENTITY_PLAYER_LEVELUP"),
                (float) plugin.getConfig().getDouble("sounds.success.volume", 1.0),
                (float) plugin.getConfig().getDouble("sounds.success.pitch", 1.0));
        p.closeInventory();
    }

    private void dispatchCommand(String template, String player, String amount, String type) {
        if (template == null || template.isBlank()) return;
        String cmd = template
                .replace("{player}", player)
                .replace("{amount}", amount)
                .replace("{type}", type);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    private void playSound(Player player, String soundName, float volume, float pitch) {
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase()));
        if (sound == null) {
            plugin.getLogger().warning("Unknown sound '" + soundName + "', skipping.");
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
