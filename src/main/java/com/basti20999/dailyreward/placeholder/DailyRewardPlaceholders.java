package com.basti20999.dailyreward.placeholder;

import com.basti20999.dailyreward.DailyReward;
import com.basti20999.dailyreward.PlayerData;
import com.basti20999.dailyreward.config.PluginConfig;
import com.basti20999.dailyreward.util.CooldownUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DailyRewardPlaceholders extends PlaceholderExpansion {

    private final DailyReward plugin;

    public DailyRewardPlaceholders(DailyReward plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dailyreward";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Basti20999";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        PlayerData data = plugin.getPlayerDataOrNull(player.getUniqueId());
        PluginConfig cfg = plugin.cfg();

        return switch (params.toLowerCase()) {
            case "streak"      -> data == null ? "0" : String.valueOf(data.streak());
            case "total"       -> data == null ? "0" : String.valueOf(data.totalClaims());
            case "ready"       -> String.valueOf(
                    data == null || CooldownUtil.isReady(data.lastClaimed(), cfg.cooldownHours));
            case "cooldown"    -> {
                if (data == null) yield "Ready!";
                long remaining = CooldownUtil.remainingMillis(data.lastClaimed(), cfg.cooldownHours);
                yield CooldownUtil.format(remaining);
            }
            case "next_day"    -> {
                if (data == null) yield "1";
                int next = data.streak() + 1;
                yield String.valueOf(((next - 1) % 7) + 1);
            }
            case "last_claim"  -> data == null ? "0" : String.valueOf(data.lastClaimed());
            default -> null;
        };
    }
}
