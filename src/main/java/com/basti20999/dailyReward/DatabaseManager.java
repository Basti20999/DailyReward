package com.basti20999.dailyReward;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.UUID;

/**
 * Manages the persistent storage of player reward timestamps.
 * Uses MySQL when configured, otherwise falls back to SQLite.
 */
public class DatabaseManager {

    private final DailyReward plugin;
    private final boolean mysql;
    private Connection connection;

    public DatabaseManager(DailyReward plugin) throws SQLException {
        this.plugin = plugin;
        this.mysql = "mysql".equalsIgnoreCase(
                plugin.getConfig().getString("database.type", "sqlite"));
        connect();
        createTable();
    }

    private void connect() throws SQLException {
        if (mysql) {
            connectMySQL();
        } else {
            connectSQLite();
        }
    }

    private void connectSQLite() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found.", e);
        }
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        File dbFile = new File(dataFolder, "rewards.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        plugin.getLogger().info("Connected to SQLite database.");
    }

    private void connectMySQL() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found.", e);
        }
        String host     = plugin.getConfig().getString("database.mysql.host", "localhost");
        int    port     = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "dailyreward");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");
        boolean ssl     = plugin.getConfig().getBoolean("database.mysql.use_ssl", false);

        String url = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%b&allowPublicKeyRetrieval=true",
                host, port, database, ssl);
        connection = DriverManager.getConnection(url, username, password);
        plugin.getLogger().info("Connected to MySQL database.");
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS daily_rewards (" +
                "  uuid         VARCHAR(36) NOT NULL PRIMARY KEY," +
                "  last_claimed BIGINT      NOT NULL" +
                ")"
            );
        }
    }

    /**
     * Loads all stored reward times into a map. Called once on startup.
     */
    public HashMap<UUID, Long> loadAll() throws SQLException {
        HashMap<UUID, Long> map = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, last_claimed FROM daily_rewards")) {
            while (rs.next()) {
                try {
                    map.put(UUID.fromString(rs.getString("uuid")), rs.getLong("last_claimed"));
                } catch (IllegalArgumentException ignored) {
                    // skip rows with malformed UUIDs
                }
            }
        }
        return map;
    }

    /**
     * Inserts or updates the reward timestamp for the given player.
     */
    public void save(UUID uuid, long timestamp) throws SQLException {
        String sql = mysql
            ? "INSERT INTO daily_rewards (uuid, last_claimed) VALUES (?, ?)" +
              " ON DUPLICATE KEY UPDATE last_claimed = VALUES(last_claimed)"
            : "INSERT OR REPLACE INTO daily_rewards (uuid, last_claimed) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, timestamp);
            stmt.executeUpdate();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close database connection: " + e.getMessage());
        }
    }
}
