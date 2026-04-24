package com.basti20999.dailyreward.gui;

import com.basti20999.dailyreward.DailyReward;
import com.basti20999.dailyreward.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiRefresher {

    private final DailyReward plugin;
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    public GuiRefresher(DailyReward plugin) {
        this.plugin = plugin;
    }

    public void start(Player player, Inventory inventory) {
        stop(player.getUniqueId());
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stop(player.getUniqueId());
                return;
            }
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top != inventory || !(top.getHolder() instanceof DailyRewardHolder)) {
                stop(player.getUniqueId());
                return;
            }
            PlayerData data = plugin.getPlayerData(player.getUniqueId());
            GuiBuilder.refresh(inventory, plugin.cfg(), data);
        }, 20L, 20L);
        tasks.put(player.getUniqueId(), task);
    }

    public void stop(UUID uuid) {
        BukkitTask task = tasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void stopAll() {
        for (BukkitTask task : tasks.values()) task.cancel();
        tasks.clear();
    }
}
