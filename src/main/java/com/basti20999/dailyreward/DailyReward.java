package com.basti20999.dailyreward;

import com.basti20999.dailyreward.command.DailyCommand;
import com.basti20999.dailyreward.command.DailyTabCompleter;
import com.basti20999.dailyreward.config.PluginConfig;
import com.basti20999.dailyreward.gui.GuiRefresher;
import com.basti20999.dailyreward.placeholder.DailyRewardPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DailyReward extends JavaPlugin {

    private static DailyReward instance;

    private DatabaseManager database;
    private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private PluginConfig cfg;
    private RewardService rewardService;
    private GuiRefresher guiRefresher;
    private DailyRewardPlaceholders papiExpansion;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        cfg = new PluginConfig(this);

        try {
            database = new DatabaseManager(this);
            playerData.putAll(database.loadAll());
            getLogger().info("Loaded " + playerData.size() + " player entries from database.");
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        rewardService = new RewardService(this);
        guiRefresher = new GuiRefresher(this);

        PluginCommand cmd = getCommand("daily");
        if (cmd == null) {
            getLogger().severe("'daily' command not defined in plugin.yml; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        cmd.setExecutor(new DailyCommand(this));
        cmd.setTabCompleter(new DailyTabCompleter());

        getServer().getPluginManager().registerEvents(new RewardListener(this), this);

        registerPlaceholders();
        getLogger().info("DailyReward v" + getDescription().getVersion()
                + " enabled (Folia=" + com.basti20999.dailyreward.util.SchedulerUtil.isFolia() + ").");
    }

    @Override
    public void onDisable() {
        if (guiRefresher != null) guiRefresher.stopAll();
        if (papiExpansion != null) {
            try {
                papiExpansion.unregister();
            } catch (Throwable ignored) {
            }
        }
        if (database != null) database.close();
        getLogger().info("DailyReward disabled.");
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return;
        try {
            papiExpansion = new DailyRewardPlaceholders(this);
            if (papiExpansion.register()) {
                getLogger().info("Registered PlaceholderAPI expansion.");
            }
        } catch (Throwable t) {
            getLogger().warning("Failed to register PlaceholderAPI expansion: " + t.getMessage());
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        cfg = new PluginConfig(this);
        if (guiRefresher != null) guiRefresher.stopAll();
    }

    public static DailyReward getInstance() {
        return instance;
    }

    public PluginConfig cfg() {
        return cfg;
    }

    public DatabaseManager getDatabase() {
        return database;
    }

    public RewardService getRewardService() {
        return rewardService;
    }

    public GuiRefresher getGuiRefresher() {
        return guiRefresher;
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, k -> PlayerData.empty());
    }

    public PlayerData getPlayerDataOrNull(UUID uuid) {
        return playerData.get(uuid);
    }

    public Map<UUID, PlayerData> getAllPlayerData() {
        return playerData;
    }
}
