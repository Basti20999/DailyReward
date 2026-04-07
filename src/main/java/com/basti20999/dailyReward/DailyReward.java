package com.basti20999.dailyReward;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class DailyReward extends JavaPlugin {

    private static DailyReward instance;
    private DatabaseManager database;
    private final HashMap<UUID, Long> rewardTimes = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        try {
            database = new DatabaseManager(this);
            rewardTimes.putAll(database.loadAll());
            getLogger().info("Loaded " + rewardTimes.size() + " reward entries from database.");
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("daily").setExecutor(new RewardCommand(this));
        getServer().getPluginManager().registerEvents(new RewardListener(this), this);
        getLogger().info("DailyReward enabled.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("DailyReward disabled.");
    }

    public static DailyReward getInstance() {
        return instance;
    }

    public DatabaseManager getDatabase() {
        return database;
    }

    public HashMap<UUID, Long> getRewardTimes() {
        return rewardTimes;
    }
}
