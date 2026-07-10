package com.proauth.proauth.database;

import com.proauth.proauth.ProAuth;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class DatabaseManager {
    private final ProAuth plugin;
    private Connection connection;
    private boolean isInitialized = false;

    public DatabaseManager(ProAuth plugin) {
        this.plugin = plugin;
    }

    public void initializeDatabase() {
        try {
            File dataFolder = new File(this.plugin.getDataFolder().getParentFile(), "ProAUTH");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
                this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("database.folder-created", new String[]{dataFolder.getAbsolutePath()}));
            }

            File databaseFile = new File(dataFolder, "users.db");
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            url = url + "?journal_mode=WAL&synchronous=NORMAL";
            this.connection = DriverManager.getConnection(url);

            try (Statement stmt = this.connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            String createTableSQL = "CREATE TABLE IF NOT EXISTS users (uuid TEXT PRIMARY KEY, username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, last_ip TEXT, last_login TEXT, twofa_enabled INTEGER DEFAULT 0, telegram_chat_id TEXT, twofa_pending INTEGER DEFAULT 0, twofa_code TEXT, registered_date TEXT, last_online TEXT, original_group TEXT, vanishlogin INTEGER DEFAULT 0)";

            try (Statement stmt = this.connection.createStatement()) {
                stmt.execute(createTableSQL);
                this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("database.table-created"));
            }

            try (Statement stmt = this.connection.createStatement()) {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_username ON users (username)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_telegram_chat_id ON users (telegram_chat_id)");

                try {
                    stmt.execute("ALTER TABLE users ADD COLUMN original_group TEXT");
                } catch (SQLException e) {
                    if (!e.getMessage().contains("duplicate column")) {
                        throw e;
                    }
                }

                try {
                    stmt.execute("ALTER TABLE users ADD COLUMN vanishlogin INTEGER DEFAULT 0");
                } catch (SQLException e) {
                    if (!e.getMessage().contains("duplicate column")) {
                        throw e;
                    }
                }
            }

            this.isInitialized = true;
            this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("database.initialized"));
        } catch (SQLException e) {
            this.plugin.getLogger().severe(this.plugin.getMessageManager().getMessage("database.init-error", new String[]{e.getMessage()}));
            e.printStackTrace();
        }

    }

    public void reinitializeDatabase() {
        this.isInitialized = false;
        this.closeConnection();
        this.initializeDatabase();
    }

    private Connection getConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            File dataFolder = new File(this.plugin.getDataFolder().getParentFile(), "ProAUTH");
            File databaseFile = new File(dataFolder, "users.db");
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath() + "?journal_mode=WAL&synchronous=NORMAL";
            this.connection = DriverManager.getConnection(url);

            try (Statement stmt = this.connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            this.isInitialized = true;
        }

        return this.connection;
    }

    private void checkInitialization() throws SQLException {
        if (!this.isInitialized) {
            throw new SQLException(this.plugin.getMessageManager().getMessage("database.not-initialized"));
        } else {
            this.getConnection();
        }
    }

    public boolean registerPlayer(UUID uuid, String username, String passwordHash, String ip) {
        try {
            this.checkInitialization();
            String sql = "INSERT INTO users (uuid, username, password_hash, last_ip, registered_date, last_online) VALUES (?, ?, ?, ?, ?, ?)";

            boolean var10;
            try (
                    Connection conn = this.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
            ) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, username);
                pstmt.setString(3, passwordHash);
                pstmt.setString(4, ip);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentTime = dateFormat.format(new Date());
                pstmt.setString(5, currentTime);
                pstmt.setString(6, currentTime);
                pstmt.executeUpdate();
                this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("database.player-registered", new String[]{username}));
                var10 = true;
            }

            return var10;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка регистрации пользователя " + username + ": " + e.getMessage());
            return false;
        }
    }

    public boolean isUserRegistered(String username) {
        try {
            this.checkInitialization();
            String sql = "SELECT username FROM users WHERE username = ?";

            boolean var6;
            try (
                    Connection conn = this.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
            ) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                var6 = rs.next();
            }

            return var6;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка проверки регистрации для " + username + ": " + e.getMessage());
            return false;
        }
    }

    public String getPasswordHash(String username) {
        try {
            this.checkInitialization();
            String sql = "SELECT password_hash FROM users WHERE username = ?";

            String var12;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        var12 = rs.getString("password_hash");
                        String var7 = var12;
                        return var7;
                    }

                    var12 = null;
                }
            }

            return var12;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка получения хеша пароля для " + username + ": " + e.getMessage());
            return null;
        }
    }

    public void updateLoginInfo(String username, String ip) {
        try {
            this.checkInitialization();
            String sql = "UPDATE users SET last_ip = ?, last_login = ?, last_online = ? WHERE username = ?";

            try (
                    Connection conn = this.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
            ) {
                pstmt.setString(1, ip);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentTime = dateFormat.format(new Date());
                pstmt.setString(2, currentTime);
                pstmt.setString(3, currentTime);
                pstmt.setString(4, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка обновления информации для " + username + ": " + e.getMessage());
        }

    }

    public String getLastLogin(String username) {
        try {
            this.checkInitialization();
            String sql = "SELECT last_login FROM users WHERE username = ?";

            String lastLogin;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        lastLogin = rs.getString("last_login");
                        String var7 = lastLogin != null ? lastLogin : "Никогда";
                        String var8 = var7;
                        return var8;
                    }

                    lastLogin = "Не найден";
                }
            }

            return lastLogin;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка получения последнего входа для " + username + ": " + e.getMessage());
            return "Ошибка";
        }
    }

    public String getLastIP(String username) {
        try {
            this.checkInitialization();
            String sql = "SELECT last_ip FROM users WHERE username = ?";

            String lastIP;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        lastIP = rs.getString("last_ip");
                        String var7 = lastIP != null ? lastIP : "Неизвестно";
                        String var8 = var7;
                        return var8;
                    }

                    lastIP = "Не найден";
                }
            }

            return lastIP;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка получения IP для " + username + ": " + e.getMessage());
            return "Ошибка";
        }
    }

    public boolean unregisterUser(String username) {
        try {
            this.checkInitialization();
            String sql = "DELETE FROM users WHERE username = ?";

            boolean var12;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        this.plugin.getSessionManager().logoutPlayerByName(username);
                        var12 = true;
                        boolean var7 = var12;
                        return var7;
                    }

                    var12 = false;
                }
            }

            return var12;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка удаления пользователя " + username + ": " + e.getMessage());
            return false;
        }
    }

    public void setTelegramChatId(String username, String chatId) {
        try {
            this.checkInitialization();
            String sql = "UPDATE users SET telegram_chat_id = ? WHERE username = ?";

            try (
                    Connection conn = this.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
            ) {
                pstmt.setString(1, chatId);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка установки Telegram ID для " + username + ": " + e.getMessage());
        }

    }

    public String getTelegramChatId(String username) {
        try {
            this.checkInitialization();
            String sql = "SELECT telegram_chat_id FROM users WHERE username = ?";

            String var12;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        var12 = rs.getString("telegram_chat_id");
                        String var7 = var12;
                        return var7;
                    }

                    var12 = null;
                }
            }

            return var12;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка получения Telegram ID для " + username + ": " + e.getMessage());
            return null;
        }
    }

    public void set2FAEnabled(String username, boolean enabled) {
        try {
            this.checkInitialization();
            String sql = "UPDATE users SET twofa_enabled = ? WHERE username = ?";

            try (
                    Connection conn = this.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
            ) {
                pstmt.setInt(1, enabled ? 1 : 0);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка установки 2FA для " + username + ": " + e.getMessage());
        }

    }

    public boolean is2FAEnabled(String username) {
        try {
            this.checkInitialization();
            String sql = "SELECT twofa_enabled FROM users WHERE username = ?";

            boolean var12;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        var12 = rs.getInt("twofa_enabled") == 1;
                        boolean var7 = var12;
                        return var7;
                    }

                    var12 = false;
                }
            }

            return var12;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка проверки 2FA для " + username + ": " + e.getMessage());
            return false;
        }
    }

    public void set2FAPending(String username, boolean pending, String code) {
        try {
            this.checkInitialization();
            String sql = "UPDATE users SET twofa_pending = ?, twofa_code = ? WHERE username = ?";

            try (
                    Connection conn = this.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
            ) {
                pstmt.setInt(1, pending ? 1 : 0);
                pstmt.setString(2, code);
                pstmt.setString(3, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка установки 2FA pending для " + username + ": " + e.getMessage());
        }

    }

    public String get2FACode(String username) {
        try {
            this.checkInitialization();
            String sql = "SELECT twofa_code FROM users WHERE username = ? AND twofa_pending = 1";

            String var12;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        var12 = rs.getString("twofa_code");
                        String var7 = var12;
                        return var7;
                    }

                    var12 = null;
                }
            }

            return var12;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка получения 2FA кода для " + username + ": " + e.getMessage());
            return null;
        }
    }

    public boolean is2FAPending(String username) {
        try {
            this.checkInitialization();
            String sql = "SELECT twofa_pending FROM users WHERE username = ?";

            boolean var12;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        var12 = rs.getInt("twofa_pending") == 1;
                        boolean var7 = var12;
                        return var7;
                    }

                    var12 = false;
                }
            }

            return var12;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка проверки 2FA pending для " + username + ": " + e.getMessage());
            return false;
        }
    }

    public boolean updatePassword(String username, String newHashedPassword) {
        try {
            this.checkInitialization();
            String sql = "UPDATE users SET password_hash = ? WHERE username = ?";

            boolean var7;
            try (
                    Connection conn = this.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
            ) {
                pstmt.setString(1, newHashedPassword);
                pstmt.setString(2, username);
                int affectedRows = pstmt.executeUpdate();
                var7 = affectedRows > 0;
            }

            return var7;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка обновления пароля для " + username + ": " + e.getMessage());
            return false;
        }
    }

    public String getUsernameByChatId(String chatId) {
        try {
            this.checkInitialization();
            String sql = "SELECT username FROM users WHERE telegram_chat_id = ?";

            String var12;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, chatId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        var12 = rs.getString("username");
                        String var7 = var12;
                        return var7;
                    }

                    var12 = null;
                }
            }

            return var12;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка поиска пользователя по chat_id " + chatId + ": " + e.getMessage());
            return null;
        }
    }

    public void updateLastOnline(String username) {
        try {
            this.checkInitialization();
            String sql = "UPDATE users SET last_online = ? WHERE username = ?";

            try (
                    Connection conn = this.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
            ) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentTime = dateFormat.format(new Date());
                pstmt.setString(1, currentTime);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка обновления last_online для " + username + ": " + e.getMessage());
        }

    }

    public void updateLastIP(String username, String ip) {
        try {
            this.checkInitialization();
            String sql = "UPDATE users SET last_ip = ? WHERE username = ?";

            try (
                    Connection conn = this.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
            ) {
                pstmt.setString(1, ip);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка обновления IP для " + username + ": " + e.getMessage());
        }

    }

    public int getRegisteredUsersCount() {
        try {
            String sql = "SELECT COUNT(*) FROM users";

            int var4;
            try (Statement stmt = this.connection.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (!rs.next()) {
                        return 0;
                    }

                    var4 = rs.getInt(1);
                }
            }

            return var4;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Ошибка при подсчёте зарегистрированных пользователей: " + e.getMessage());
            return 0;
        }
    }

    public void closeConnection() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
                this.isInitialized = false;
                this.plugin.getLogger().info("Соединение с базой данных закрыто");
            }
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Ошибка закрытия соединения с базой данных: " + e.getMessage());
        }

    }

    public boolean isInitialized() {
        return this.isInitialized;
    }

    public boolean isUserRegisteredByUUID(UUID uuid) {
        String sql = "SELECT COUNT(*) FROM users WHERE uuid = ?";

        try {
            boolean var5;
            try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    var5 = rs.next() && rs.getInt(1) > 0;
                }
            }

            return var5;
        } catch (SQLException e) {
            this.plugin.getLogger().severe(this.plugin.getMessageManager().getMessage("database.count-error", new String[]{e.getMessage()}));
            return false;
        }
    }

    public String getUsernameByUUID(UUID uuid) {
        String sql = "SELECT username FROM users WHERE uuid = ?";

        try {
            String var5;
            try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }

                    var5 = rs.getString("username");
                }
            }

            return var5;
        } catch (SQLException e) {
            this.plugin.getLogger().severe(this.plugin.getMessageManager().getMessage("database.username-search-error", new String[]{e.getMessage()}));
            return null;
        }
    }

    public UUID getUUIDByUsername(String username) {
        String sql = "SELECT uuid FROM users WHERE username = ?";

        try {
            UUID var5;
            try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }

                    var5 = UUID.fromString(rs.getString("uuid"));
                }
            }

            return var5;
        } catch (SQLException e) {
            this.plugin.getLogger().severe(this.plugin.getMessageManager().getMessage("database.username-search-error", new String[]{e.getMessage()}));
            return null;
        }
    }

    public void savePlayerOriginalGroup(String username, String originalGroup) {
        try {
            this.checkInitialization();
            String sql = "UPDATE users SET original_group = ? WHERE username = ?";

            try (
                    Connection conn = this.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
            ) {
                pstmt.setString(1, originalGroup);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Error saving original group for " + username + ": " + e.getMessage());
        }

    }

    public String getPlayerOriginalGroup(String username) {
        try {
            this.checkInitialization();
            String sql = "SELECT original_group FROM users WHERE username = ?";

            String var7;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    ResultSet rs = pstmt.executeQuery();
                    if (!rs.next()) {
                        return "default";
                    }

                    String group = rs.getString("original_group");
                    var7 = group != null ? group : "default";
                }
            }

            return var7;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Error getting original group for " + username + ": " + e.getMessage());
            return "default";
        }
    }

    public boolean isVanishLoginEnabled(String username) {
        try {
            this.checkInitialization();
            String sql = "SELECT vanishlogin FROM users WHERE username = ?";

            boolean var6;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    ResultSet rs = pstmt.executeQuery();
                    if (!rs.next()) {
                        return false;
                    }

                    var6 = rs.getInt("vanishlogin") == 1;
                }
            }

            return var6;
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Error getting vanishlogin for " + username + ": " + e.getMessage());
            return false;
        }
    }

    public void setVanishLogin(String username, boolean enabled) {
        try {
            this.checkInitialization();
            String sql = "UPDATE users SET vanishlogin = ? WHERE username = ?";

            try (
                    Connection conn = this.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
            ) {
                pstmt.setInt(1, enabled ? 1 : 0);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Error setting vanishlogin for " + username + ": " + e.getMessage());
        }

    }
}