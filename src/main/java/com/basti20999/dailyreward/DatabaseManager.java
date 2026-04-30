package com.basti20999.dailyreward;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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
        migrate();
    }

    private void connect() throws SQLException {
        if (mysql) connectMySQL();
        else connectSQLite();
    }

    private void connectSQLite() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found.", e);
        }
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new SQLException("Could not create plugin data folder.");
        }
        File dbFile = new File(dataFolder, "rewards.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        plugin.getLogger().info("Connected to SQLite database.");
    }

    private void connectMySQL() throws SQLException {
        try {
            Class.forName("com.basti20999.dailyreward.libs.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC driver not found.", e);
            }
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
                "  last_claimed BIGINT      NOT NULL," +
                "  streak       INT         NOT NULL DEFAULT 0," +
                "  total_claims INT         NOT NULL DEFAULT 0" +
                ")"
            );
        }
    }

    private void migrate() throws SQLException {
        if (!columnExists("streak")) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("ALTER TABLE daily_rewards ADD COLUMN streak INT NOT NULL DEFAULT 0");
                plugin.getLogger().info("Migrated database: added 'streak' column.");
            }
        }
        if (!columnExists("total_claims")) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("ALTER TABLE daily_rewards ADD COLUMN total_claims INT NOT NULL DEFAULT 0");
                plugin.getLogger().info("Migrated database: added 'total_claims' column.");
            }
        }
    }

    private boolean columnExists(String column) {
        String sql = mysql
                ? "SELECT * FROM daily_rewards LIMIT 0"
                : "SELECT * FROM daily_rewards LIMIT 0";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            var meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                if (column.equalsIgnoreCase(meta.getColumnName(i))) return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to inspect table schema: " + e.getMessage());
        }
        return false;
    }

    public PlayerData loadPlayer(UUID uuid) throws SQLException {
        String sql = "SELECT last_claimed, streak, total_claims FROM daily_rewards WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PlayerData(
                            rs.getLong("last_claimed"),
                            rs.getInt("streak"),
                            rs.getInt("total_claims"));
                }
            }
        }
        return null;
    }

    public Map<UUID, PlayerData> loadAll() throws SQLException {
        Map<UUID, PlayerData> map = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT uuid, last_claimed, streak, total_claims FROM daily_rewards")) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    long last = rs.getLong("last_claimed");
                    int streak = rs.getInt("streak");
                    int total = rs.getInt("total_claims");
                    map.put(uuid, new PlayerData(last, streak, total));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed UUID rows
                }
            }
        }
        return map;
    }

    public void save(UUID uuid, PlayerData data) throws SQLException {
        String sql = mysql
            ? "INSERT INTO daily_rewards (uuid, last_claimed, streak, total_claims) VALUES (?, ?, ?, ?)" +
              " ON DUPLICATE KEY UPDATE last_claimed = VALUES(last_claimed), streak = VALUES(streak), total_claims = VALUES(total_claims)"
            : "INSERT OR REPLACE INTO daily_rewards (uuid, last_claimed, streak, total_claims) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, data.lastClaimed());
            stmt.setInt(3, data.streak());
            stmt.setInt(4, data.totalClaims());
            stmt.executeUpdate();
        }
    }

    public void delete(UUID uuid) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM daily_rewards WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        }
    }

    public LinkedHashMap<UUID, PlayerData> topStreaks(int limit) throws SQLException {
        LinkedHashMap<UUID, PlayerData> result = new LinkedHashMap<>();
        String sql = "SELECT uuid, last_claimed, streak, total_claims FROM daily_rewards" +
                     " ORDER BY streak DESC, total_claims DESC LIMIT ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        result.put(uuid, new PlayerData(
                                rs.getLong("last_claimed"),
                                rs.getInt("streak"),
                                rs.getInt("total_claims")));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        return result;
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
