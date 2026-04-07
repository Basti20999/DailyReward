package com.basti20999.dailyReward;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class DailyReward extends JavaPlugin {

    private static DailyReward instance;
    private final HashMap<UUID, Long> rewardTimes = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getCommand("daily").setExecutor(new RewardCommand(this));
        getServer().getPluginManager().registerEvents(new RewardListener(this), this);
        getLogger().info("DailyReward enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DailyReward disabled.");
    }

    public static DailyReward getInstance() {
        return instance;
    }

    public HashMap<UUID, Long> getRewardTimes() {
        return rewardTimes;
    }
}