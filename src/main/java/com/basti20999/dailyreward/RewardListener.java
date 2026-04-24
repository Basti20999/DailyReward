package com.basti20999.dailyreward;

import com.basti20999.dailyreward.config.PluginConfig;
import com.basti20999.dailyreward.gui.DailyRewardHolder;
import com.basti20999.dailyreward.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class RewardListener implements Listener {

    private final DailyReward plugin;

    public RewardListener(DailyReward plugin) {
        this.plugin = plugin;
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
