package com.basti20999.dailyreward;

import com.basti20999.dailyreward.config.PluginConfig;
import com.basti20999.dailyreward.gui.DailyRewardHolder;
import com.basti20999.dailyreward.util.SchedulerUtil;
import com.basti20999.dailyreward.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;
import java.util.UUID;

public final class RewardListener implements Listener {

    private final DailyReward plugin;

    public RewardListener(DailyReward plugin) {
        this.plugin = plugin;
    }

    /**
     * Refresh player data from the database on join.
     * This ensures that claims made on another server (multi-server MySQL setup)
     * are visible immediately on this server.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        SchedulerUtil.async(plugin, () -> {
            try {
                PlayerData fresh = plugin.getDatabase().loadPlayer(uuid);
                if (fresh == null) return;
                PlayerData existing = plugin.getPlayerDataOrNull(uuid);
                if (existing != null) {
                    existing.setLastClaimed(fresh.lastClaimed());
                    existing.setStreak(fresh.streak());
                    existing.setTotalClaims(fresh.totalClaims());
                } else {
                    plugin.getAllPlayerData().put(uuid, fresh);
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed to refresh data for " + uuid + ": " + ex.getMessage());
            }
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DailyRewardHolder)) return;
        e.setCancelled(true);

        PluginConfig cfg = plugin.cfg();
        if (e.getSlot() != cfg.rewardSlot || !(e.getWhoClicked() instanceof Player p)) return;

        RewardService.ClaimOutcome outcome = plugin.getRewardService().attemptClaim(p);
        switch (outcome) {
            case SUCCESS -> p.closeInventory();
            case ON_COOLDOWN -> {
                PlayerData data = plugin.getPlayerData(p.getUniqueId());
                long remaining = com.basti20999.dailyreward.util.CooldownUtil
                        .remainingMillis(data.lastClaimed(), cfg.cooldownHours);
                String msg = cfg.msgOnCooldown.replace("{remaining}",
                        com.basti20999.dailyreward.util.CooldownUtil.format(remaining));
                p.sendMessage(ColorUtil.translate(msg));
                SoundUtil.play(p, cfg.errorSound.key(), cfg.errorSound.volume(),
                        cfg.errorSound.pitch(), plugin.getLogger());
                p.closeInventory();
            }
            case ALREADY_CLAIMING -> {
                // concurrent click - ignore silently
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof DailyRewardHolder) {
            plugin.getGuiRefresher().stop(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getGuiRefresher().stop(e.getPlayer().getUniqueId());
    }
}
