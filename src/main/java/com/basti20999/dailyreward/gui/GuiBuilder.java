package com.basti20999.dailyreward.gui;

import com.basti20999.dailyreward.ColorUtil;
import com.basti20999.dailyreward.DailyReward;
import com.basti20999.dailyreward.PlayerData;
import com.basti20999.dailyreward.config.PluginConfig;
import com.basti20999.dailyreward.util.CooldownUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class GuiBuilder {

    private GuiBuilder() {}

    public static Inventory build(DailyReward plugin, Player player) {
        PluginConfig cfg = plugin.cfg();
        DailyRewardHolder holder = new DailyRewardHolder();
        Inventory inv = Bukkit.createInventory(holder, cfg.guiSize, cfg.guiTitle);
        holder.bind(inv);

        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        populate(inv, cfg, data);
        return inv;
    }

    /**
     * Refresh only the volatile parts of the GUI — the reward slot (for the live countdown)
     * and weekly day indicators (so the "TODAY" highlight follows the streak).
     * Static decoration is left untouched to avoid client-side flicker.
     */
    public static void refresh(Inventory inv, PluginConfig cfg, PlayerData data) {
        updateIndicators(inv, cfg, data);
        updateRewardItem(inv, cfg, data);
    }

    private static void populate(Inventory inv, PluginConfig cfg, PlayerData data) {
        ItemStack border = createItem(cfg.borderMaterial, cfg.borderName, null);
        for (int slot : cfg.borderSlots) inv.setItem(slot, border);
        updateIndicators(inv, cfg, data);
        updateRewardItem(inv, cfg, data);
    }

    private static void updateIndicators(Inventory inv, PluginConfig cfg, PlayerData data) {
        if (!cfg.weeklyEnabled || cfg.weeklyRotation.isEmpty() || cfg.weeklyIndicatorSlots.isEmpty()) {
            return;
        }
        int currentDay = currentWeeklyDay(data, cfg);
        int count = Math.min(7, cfg.weeklyIndicatorSlots.size());
        for (int i = 0; i < count; i++) {
            int day = i + 1;
            int slot = cfg.weeklyIndicatorSlots.get(i);
            inv.setItem(slot, buildDayIndicator(cfg, day, currentDay));
        }
    }

    private static void updateRewardItem(Inventory inv, PluginConfig cfg, PlayerData data) {
        long remaining = CooldownUtil.remainingMillis(data.lastClaimed(), cfg.cooldownHours);
        boolean ready = remaining == 0L;
        String name = ready ? cfg.rewardNameReady : cfg.rewardNameCooldown;
        List<String> lore = buildRewardLore(cfg, data, remaining, ready);
        inv.setItem(cfg.rewardSlot, createItem(cfg.rewardMaterial, name, lore));
    }

    private static int currentWeeklyDay(PlayerData data, PluginConfig cfg) {
        int streak = data.streak();
        int nextStreak = computeNextStreak(data, cfg);
        return ((nextStreak - 1) % 7) + 1;
    }

    private static int computeNextStreak(PlayerData data, PluginConfig cfg) {
        if (!data.hasEverClaimed()) return 1;
        long now = System.currentTimeMillis();
        long diff = now - data.lastClaimed();
        long grace = (long) (CooldownUtil.cooldownMillis(cfg.cooldownHours) * cfg.streakGraceMultiplier);
        if (diff <= grace) return data.streak() + 1;
        return 1;
    }

    private static ItemStack buildDayIndicator(PluginConfig cfg, int day, int currentDay) {
        PluginConfig.DayReward reward = cfg.weeklyRotation.get(day);
        Material icon;
        String displayName;
        List<String> lore = new ArrayList<>();

        if (day < currentDay) icon = cfg.pastDayMaterial;
        else if (day == currentDay) icon = reward != null ? reward.icon() : cfg.currentDayMaterial;
        else icon = cfg.futureDayMaterial;

        if (reward != null) {
            displayName = reward.displayName();
            lore.addAll(reward.lore());
        } else {
            displayName = "&7Day " + day;
        }
        if (day == currentDay) {
            lore.add("");
            lore.add("&e&lTODAY");
        }
        return createItem(icon, displayName, lore);
    }

    private static List<String> buildRewardLore(PluginConfig cfg, PlayerData data,
                                                 long remaining, boolean ready) {
        List<String> result = new ArrayList<>(cfg.rewardLore.size() + 4);
        result.addAll(cfg.rewardLore);

        if (cfg.showStreakInLore) {
            result.add("");
            result.add("&7Current streak: &e" + data.streak() + " &7day(s)");
            result.add("&7Total claims: &e" + data.totalClaims());
        }
        if (cfg.showCountdownInLore) {
            if (ready) {
                result.add("");
                result.add("&a&lClick to claim!");
            } else {
                result.add("");
                result.add("&7Next claim in: &c" + CooldownUtil.format(remaining));
            }
        }
        return result;
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(ColorUtil.translate(name));
            if (lore != null) {
                List<String> translated = new ArrayList<>(lore.size());
                for (String line : lore) translated.add(ColorUtil.translate(line));
                meta.setLore(translated);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
