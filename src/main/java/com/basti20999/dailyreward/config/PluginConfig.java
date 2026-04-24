package com.basti20999.dailyreward.config;

import com.basti20999.dailyreward.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

public final class PluginConfig {

    public record StreakBonus(int threshold, double moneyMultiplier, int extraKeys, int bonusGems) {}

    public record DayReward(int day, int moneyMin, int moneyMax, String keyType,
                            int keyAmount, int gems, Material icon, String displayName,
                            List<String> lore) {}

    public record SoundSpec(String key, float volume, float pitch) {}

    // GUI
    public final String guiTitle;
    public final int guiSize;
    public final int rewardSlot;
    public final Material rewardMaterial;
    public final String rewardNameReady;
    public final String rewardNameCooldown;
    public final List<String> rewardLore;
    public final Material borderMaterial;
    public final String borderName;
    public final List<Integer> borderSlots;
    public final List<Integer> weeklyIndicatorSlots;
    public final boolean showCountdownInLore;
    public final boolean showStreakInLore;

    // Cooldown
    public final long cooldownHours;
    public final double streakGraceMultiplier;

    // Flat rewards
    public final int moneyMin;
    public final int moneyMax;
    public final String keyType;
    public final int keyAmount;
    public final int gems;

    // Commands
    public final String moneyGiveCmd;
    public final String keysGiveCmd;
    public final String gemsGiveCmd;

    // Sounds
    public final SoundSpec openSound;
    public final SoundSpec successSound;
    public final SoundSpec errorSound;

    // Streak
    public final boolean streakEnabled;
    public final TreeMap<Integer, StreakBonus> streakBonuses;

    // Weekly rewards
    public final boolean weeklyEnabled;
    public final Map<Integer, DayReward> weeklyRotation;
    public final Material pastDayMaterial;
    public final Material currentDayMaterial;
    public final Material futureDayMaterial;

    // Messages
    public final String msgOnlyPlayers;
    public final String msgSuccess;
    public final String msgSuccessStreak;
    public final String msgOnCooldown;
    public final String msgReset;
    public final String msgGiven;
    public final String msgReloaded;
    public final String msgNoPermission;
    public final String msgUnknownSubcommand;
    public final String msgUsage;
    public final String msgStatsHeader;
    public final String msgStatsLine;
    public final String msgTopHeader;
    public final String msgTopEntry;
    public final String msgPlayerNotFound;
    public final String msgNoData;

    public PluginConfig(JavaPlugin plugin) {
        FileConfiguration c = plugin.getConfig();
        Logger log = plugin.getLogger();

        // GUI
        this.guiTitle = ColorUtil.translate(c.getString("gui.title", "&8Daily Reward"));
        int rawSize = c.getInt("gui.size", 45);
        this.guiSize = clampInventorySize(rawSize, log);
        int rawRewardSlot = c.getInt("gui.reward_slot", 22);
        this.rewardSlot = clampSlot(rawRewardSlot, guiSize, "gui.reward_slot", 22, log);
        this.rewardMaterial = parseMaterial(c.getString("gui.reward_material"), Material.CHEST, log);
        this.rewardNameReady = c.getString("gui.reward_name_ready", "&a&lCLAIM REWARD!");
        this.rewardNameCooldown = c.getString("gui.reward_name_cooldown", "&c&lALREADY CLAIMED");
        this.rewardLore = c.getStringList("gui.reward_lore");
        this.borderMaterial = parseMaterial(c.getString("gui.border_material"),
                Material.BLACK_STAINED_GLASS_PANE, log);
        this.borderName = c.getString("gui.border_name", " ");
        this.borderSlots = sanitizeSlots(c.getIntegerList("gui.border_slots"), guiSize);
        this.weeklyIndicatorSlots = sanitizeSlots(c.getIntegerList("gui.weekly_indicator_slots"), guiSize);
        this.showCountdownInLore = c.getBoolean("gui.show_countdown_in_lore", true);
        this.showStreakInLore = c.getBoolean("gui.show_streak_in_lore", true);

        // Cooldown
        long rawCd = c.getLong("cooldown_hours", 24);
        if (rawCd < 0) {
            log.warning("cooldown_hours is negative, using 24.");
            rawCd = 24;
        }
        this.cooldownHours = rawCd;
        this.streakGraceMultiplier = Math.max(1.0, c.getDouble("streak.grace_multiplier", 2.0));

        // Flat rewards
        int mmin = c.getInt("money.min", 100);
        int mmax = c.getInt("money.max", 1000);
        if (mmax < mmin) {
            log.warning("money.max (" + mmax + ") < money.min (" + mmin + "), swapping.");
            int tmp = mmin;
            mmin = mmax;
            mmax = tmp;
        }
        this.moneyMin = mmin;
        this.moneyMax = mmax;
        this.keyType = c.getString("keys.type", "rare");
        this.keyAmount = Math.max(0, c.getInt("keys.amount", 1));
        this.gems = Math.max(0, c.getInt("gems", 250));

        // Commands
        this.moneyGiveCmd = c.getString("commands.money_give", "");
        this.keysGiveCmd = c.getString("commands.keys_give", "");
        this.gemsGiveCmd = c.getString("commands.gems_give", "");

        // Sounds
        this.openSound = readSound(c, "sounds.open", "block.note_block.pling", 1.0f, 1.5f);
        this.successSound = readSound(c, "sounds.success", "entity.player.levelup", 1.0f, 1.0f);
        this.errorSound = readSound(c, "sounds.error", "entity.villager.no", 1.0f, 1.0f);

        // Streak
        this.streakEnabled = c.getBoolean("streak.enabled", true);
        this.streakBonuses = readStreakBonuses(c.getConfigurationSection("streak.bonus_at"));

        // Weekly rewards
        this.weeklyEnabled = c.getBoolean("weekly_rewards.enabled", true);
        this.weeklyRotation = readWeeklyRotation(
                c.getConfigurationSection("weekly_rewards.rotation"), log);
        this.pastDayMaterial = parseMaterial(
                c.getString("weekly_rewards.indicators.past"),
                Material.GRAY_STAINED_GLASS_PANE, log);
        this.currentDayMaterial = parseMaterial(
                c.getString("weekly_rewards.indicators.current"),
                Material.LIME_STAINED_GLASS_PANE, log);
        this.futureDayMaterial = parseMaterial(
                c.getString("weekly_rewards.indicators.future"),
                Material.LIGHT_GRAY_STAINED_GLASS_PANE, log);

        // Messages
        this.msgOnlyPlayers = c.getString("messages.general.only_players",
                "&cOnly players can use this command.");
        this.msgSuccess = c.getString("messages.success",
                "&aDailyReward &7» &fYou claimed your daily reward!");
        this.msgSuccessStreak = c.getString("messages.success_streak",
                "&aDailyReward &7» &fYou claimed your daily reward! &7(Streak: &e{streak}&7)");
        this.msgOnCooldown = c.getString("messages.on_cooldown",
                "&cDailyReward &7» &fYou have already claimed today! Come back in &e{remaining}&f.");
        this.msgReset = c.getString("messages.admin.reset",
                "&aDailyReward &7» &fReset cooldown for &e{player}&f.");
        this.msgGiven = c.getString("messages.admin.given",
                "&aDailyReward &7» &fGave a reward to &e{player}&f.");
        this.msgReloaded = c.getString("messages.admin.reloaded",
                "&aDailyReward &7» &fConfig reloaded.");
        this.msgNoPermission = c.getString("messages.no_permission",
                "&cYou don't have permission to do that.");
        this.msgUnknownSubcommand = c.getString("messages.unknown_subcommand",
                "&cUnknown subcommand. Try &e/daily &cor &e/daily stats&c.");
        this.msgUsage = c.getString("messages.usage",
                "&7Usage: &e{usage}");
        this.msgStatsHeader = c.getString("messages.stats.header",
                "&6=== Stats for &e{player} &6===");
        this.msgStatsLine = c.getString("messages.stats.line",
                "&7• &f{label}&7: &e{value}");
        this.msgTopHeader = c.getString("messages.top.header",
                "&6=== &eTop &6Daily Reward Streaks ===");
        this.msgTopEntry = c.getString("messages.top.entry",
                "&7#{rank} &f{player} &7— &eStreak: {streak} &7| &aTotal: {total}");
        this.msgPlayerNotFound = c.getString("messages.player_not_found",
                "&cPlayer &e{player} &cwas not found.");
        this.msgNoData = c.getString("messages.no_data",
                "&7No data available.");
    }

    private static int clampInventorySize(int raw, Logger log) {
        int size = raw;
        if (size < 9) size = 9;
        if (size > 54) size = 54;
        if (size % 9 != 0) {
            size = Math.round(size / 9f) * 9;
            if (size < 9) size = 9;
        }
        if (size != raw) log.warning("gui.size " + raw + " invalid, using " + size + ".");
        return size;
    }

    private static int clampSlot(int raw, int invSize, String name, int fallback, Logger log) {
        if (raw < 0 || raw >= invSize) {
            log.warning(name + " (" + raw + ") out of range for size " + invSize + ", using " + fallback + ".");
            return Math.min(fallback, invSize - 1);
        }
        return raw;
    }

    private static List<Integer> sanitizeSlots(List<Integer> slots, int invSize) {
        if (slots == null) return Collections.emptyList();
        List<Integer> result = new ArrayList<>();
        for (int s : slots) {
            if (s >= 0 && s < invSize) result.add(s);
        }
        return Collections.unmodifiableList(result);
    }

    private static Material parseMaterial(String name, Material fallback, Logger log) {
        if (name == null) return fallback;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warning("Unknown material '" + name + "', using " + fallback.name() + ".");
            return fallback;
        }
    }

    private static SoundSpec readSound(FileConfiguration c, String path,
                                        String defKey, float defVol, float defPitch) {
        String key = c.getString(path + ".sound", defKey);
        float vol = (float) c.getDouble(path + ".volume", defVol);
        float pitch = (float) c.getDouble(path + ".pitch", defPitch);
        return new SoundSpec(key, vol, pitch);
    }

    private static TreeMap<Integer, StreakBonus> readStreakBonuses(ConfigurationSection section) {
        TreeMap<Integer, StreakBonus> map = new TreeMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            int threshold;
            try {
                threshold = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            double multiplier = section.getDouble(key + ".money_multiplier", 1.0);
            int extraKeys = section.getInt(key + ".extra_keys", 0);
            int bonusGems = section.getInt(key + ".bonus_gems", 0);
            map.put(threshold, new StreakBonus(threshold, multiplier, extraKeys, bonusGems));
        }
        return map;
    }

    private static Map<Integer, DayReward> readWeeklyRotation(ConfigurationSection section, Logger log) {
        Map<Integer, DayReward> map = new LinkedHashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            int day;
            try {
                day = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            if (day < 1 || day > 7) continue;

            List<Integer> moneyRange = section.getIntegerList(key + ".money");
            int mMin = moneyRange.size() > 0 ? moneyRange.get(0) : 100;
            int mMax = moneyRange.size() > 1 ? moneyRange.get(1) : mMin;
            if (mMax < mMin) mMax = mMin;

            String kType = section.getString(key + ".keys.type", "common");
            int kAmount = Math.max(0, section.getInt(key + ".keys.amount", 1));
            int dGems = Math.max(0, section.getInt(key + ".gems", 100));

            Material icon = parseMaterial(section.getString(key + ".icon"),
                    Material.SUNFLOWER, log);
            String name = section.getString(key + ".name", "&aDay " + day);
            List<String> lore = section.getStringList(key + ".lore");

            map.put(day, new DayReward(day, mMin, mMax, kType, kAmount, dGems, icon, name, lore));
        }
        return map;
    }
}
