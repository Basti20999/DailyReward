package com.basti20999.dailyreward;

import com.basti20999.dailyreward.config.PluginConfig;
import com.basti20999.dailyreward.reward.RewardCalculator;
import com.basti20999.dailyreward.reward.RewardResult;
import com.basti20999.dailyreward.util.SchedulerUtil;
import com.basti20999.dailyreward.util.SoundUtil;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RewardService {

    private final DailyReward plugin;
    private final Set<UUID> claiming = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public RewardService(DailyReward plugin) {
        this.plugin = plugin;
    }

    public enum ClaimOutcome { SUCCESS, ON_COOLDOWN, ALREADY_CLAIMING }

    public ClaimOutcome attemptClaim(Player player) {
        PluginConfig cfg = plugin.cfg();
        UUID uuid = player.getUniqueId();
        if (!claiming.add(uuid)) return ClaimOutcome.ALREADY_CLAIMING;
        try {
            PlayerData data = plugin.getPlayerData(uuid);
            long now = System.currentTimeMillis();
            long cooldownMs = cfg.cooldownHours * 60L * 60L * 1000L;
            if (data.hasEverClaimed() && now - data.lastClaimed() < cooldownMs) {
                return ClaimOutcome.ON_COOLDOWN;
            }
            grantReward(player, data, now, cfg, true);
            return ClaimOutcome.SUCCESS;
        } finally {
            claiming.remove(uuid);
        }
    }

    public void forceGive(Player player) {
        PluginConfig cfg = plugin.cfg();
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        grantReward(player, data, System.currentTimeMillis(), cfg, false);
    }

    private void grantReward(Player player, PlayerData data, long now, PluginConfig cfg, boolean playSound) {
        int newStreak = RewardCalculator.computeNextStreak(cfg, data.lastClaimed(), data.streak());
        RewardResult result = RewardCalculator.calculate(cfg, newStreak);

        data.setLastClaimed(now);
        data.setStreak(newStreak);
        data.setTotalClaims(data.totalClaims() + 1);

        dispatch(cfg.moneyGiveCmd, player.getName(), String.valueOf(result.money()), result.keyType());
        dispatch(cfg.keysGiveCmd, player.getName(), String.valueOf(result.keyAmount()), result.keyType());
        dispatch(cfg.gemsGiveCmd, player.getName(), String.valueOf(result.gems()), result.keyType());

        UUID uuid = player.getUniqueId();
        PlayerData snapshot = new PlayerData(data.lastClaimed(), data.streak(), data.totalClaims());
        SchedulerUtil.async(plugin, () -> {
            try {
                plugin.getDatabase().save(uuid, snapshot);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to persist reward for " + player.getName() + ": " + e.getMessage());
            }
        });

        String msg = cfg.msgSuccessStreak
                .replace("{streak}", String.valueOf(newStreak))
                .replace("{day}", String.valueOf(result.dayInRotation()));
        player.sendMessage(ColorUtil.translate(msg));

        if (playSound) {
            SoundUtil.play(player, cfg.successSound.key(),
                    cfg.successSound.volume(), cfg.successSound.pitch(), plugin.getLogger());
        }
    }

    private void dispatch(String template, String player, String amount, String type) {
        if (template == null || template.isBlank()) return;
        String cmd = template
                .replace("{player}", player)
                .replace("{amount}", amount)
                .replace("{type}", type);
        SchedulerUtil.dispatchConsoleCommand(plugin, cmd);
    }

    public void resetPlayer(UUID uuid) {
        PlayerData data = plugin.getPlayerDataOrNull(uuid);
        if (data != null) {
            data.setLastClaimed(0L);
            data.setStreak(0);
        }
        SchedulerUtil.async(plugin, () -> {
            try {
                plugin.getDatabase().delete(uuid);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to reset DB entry for " + uuid + ": " + e.getMessage());
            }
        });
    }
}
