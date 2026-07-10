package com.proauth.proauth.utils;

import com.proauth.proauth.ProAuth;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
    private final ProAuth plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(ProAuth plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        File proAuthFolder = new File(this.plugin.getDataFolder().getParentFile(), "ProAUTH");
        if (!proAuthFolder.exists()) {
            proAuthFolder.mkdirs();
            this.plugin.getLogger().info("Created folder ProAUTH: " + proAuthFolder.getAbsolutePath());
        }

        this.configFile = new File(proAuthFolder, "config.yml");
        if (!this.configFile.exists()) {
            this.plugin.getLogger().info("Creating config.yml in folder ProAUTH...");
            this.createDefaultConfig();
        } else {
            this.plugin.getLogger().info("Config.yml was found: " + this.configFile.getAbsolutePath());
        }

        this.config = YamlConfiguration.loadConfiguration(this.configFile);
        this.checkAndAddMissingOptions();
        if (this.config == null) {
            this.plugin.getLogger().severe("Can't load config.yml!");
        } else {
            this.plugin.getLogger().info("Configuration succesful loaded from: " + this.configFile.getAbsolutePath());
        }

    }

    private void createDefaultConfig() {
        try {
            this.config = new YamlConfiguration();
            this.setDefaultConfigValues();
            this.config.save(this.configFile);
            this.plugin.getLogger().info("Created default configuration file in: " + this.configFile.getAbsolutePath());
        } catch (IOException e) {
            this.plugin.getLogger().severe("Can't create config.yml: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private void setDefaultConfigValues() {
        this.config.set("language", "en");
        this.config.set("session.enabled", true);
        this.config.set("session.duration-minutes", 60);
        this.config.set("login.max-attempts", 3);
        this.config.set("login.ip-ban-enabled", true);
        this.config.set("login.ban-duration-seconds", 3600);
        this.config.set("security.min-password-length", 4);
        this.config.set("security.require-strong-password", false);
        this.config.set("security.uuid-authentication", true);
        this.config.set("telemetry.enabled", true);
        this.config.set("telegram.2fa-enabled", false);
        this.config.set("telegram.bot-token", "token");
        this.config.set("telegram.bot-username", "username not provided");
        this.config.set("messages.actionbar-enabled", true);
        this.config.set("messages.use-actionbar-for-login", true);
        this.config.set("protection.apply-effects", true);
        this.config.set("protection.block-movement", true);
        this.config.set("protection.block-chat", true);
        this.config.set("protection.block-commands", true);
        this.config.set("luckperms.unauthorized", "unloggined");
        this.config.set("luckperms.vanishlogin", "vanishlogin");
        this.config.set("protection.block-interactions", true);
    }

    private void checkAndAddMissingOptions() {
        boolean needsSave = false;
        if (!this.config.contains("language")) {
            this.config.set("language", "en");
            needsSave = true;
        }

        if (!this.config.contains("security.2fa-max-attempts")) {
            this.config.set("security.2fa-max-attempts", 5);
            needsSave = true;
        }

        if (!this.config.contains("security.2fa-ip-ban-enabled")) {
            this.config.set("security.2fa-ip-ban-enabled", true);
            needsSave = true;
        }

        if (!this.config.contains("security.2fa-ban-duration-seconds")) {
            this.config.set("security.2fa-ban-duration-seconds", 1800);
            needsSave = true;
        }

        if (!this.config.contains("security.uuid-authentication")) {
            this.config.set("security.uuid-authentication", true);
            needsSave = true;
        }

        if (!this.config.contains("telemetry.enabled")) {
            this.config.set("telemetry.enabled", true);
            needsSave = true;
        }

        if (!this.config.contains("session.enabled")) {
            this.config.set("session.enabled", true);
            needsSave = true;
        }

        if (!this.config.contains("session.duration-minutes")) {
            this.config.set("session.duration-minutes", 60);
            needsSave = true;
        }

        if (!this.config.contains("login.max-attempts")) {
            this.config.set("login.max-attempts", 3);
            needsSave = true;
        }

        if (!this.config.contains("login.ip-ban-enabled")) {
            this.config.set("login.ip-ban-enabled", true);
            needsSave = true;
        }

        if (!this.config.contains("login.ban-duration-seconds")) {
            this.config.set("login.ban-duration-seconds", 3600);
            needsSave = true;
        }

        if (!this.config.contains("security.min-password-length")) {
            this.config.set("security.min-password-length", 4);
            needsSave = true;
        }

        if (!this.config.contains("telegram.2fa-enabled")) {
            this.config.set("telegram.2fa-enabled", false);
            needsSave = true;
        }

        if (!this.config.contains("telegram.bot-token")) {
            this.config.set("telegram.bot-token", "token");
            needsSave = true;
        }

        if (!this.config.contains("telegram.bot-username")) {
            this.config.set("telegram.bot-username", "юзернейм не указан");
            needsSave = true;
        }

        if (!this.config.contains("messages.actionbar-enabled")) {
            this.config.set("messages.actionbar-enabled", true);
            needsSave = true;
        }

        if (!this.config.contains("protection.apply-effects")) {
            this.config.set("protection.apply-effects", true);
            needsSave = true;
        }

        if (!this.config.contains("luckperms.unauthorized")) {
            this.config.set("luckperms.unauthorized", "unloggined");
            needsSave = true;
        }

        if (!this.config.contains("luckperms.vanishlogin")) {
            this.config.set("luckperms.vanishlogin", "vanishlogin");
            needsSave = true;
        }

        if (needsSave) {
            try {
                this.config.save(this.configFile);
                this.plugin.getLogger().info("Added missing parameters to config.yml");
            } catch (IOException e) {
                this.plugin.getLogger().warning("Can't save updated config.yml: " + e.getMessage());
            }
        }

    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public int get2FAMaxAttempts() {
        return this.config.getInt("security.2fa-max-attempts", 5);
    }

    public boolean is2FAIPBanEnabled() {
        return this.config.getBoolean("security.2fa-ip-ban-enabled", true);
    }

    public int get2FABanTime() {
        return this.config.getInt("security.2fa-ban-duration-seconds", 1800);
    }

    public String getLanguage() {
        return this.config.getString("language", "en");
    }

    public boolean isSessionEnabled() {
        return this.config.getBoolean("session.enabled", true);
    }

    public int getSessionDuration() {
        return this.config.getInt("session.duration-minutes", 60);
    }

    public int getMaxAttempts() {
        return this.config.getInt("login.max-attempts", 3);
    }

    public boolean isIPBanEnabled() {
        return this.config.getBoolean("login.ip-ban-enabled", true);
    }

    public int getBanTime() {
        return this.config.getInt("login.ban-duration-seconds", 3600);
    }

    public int getMinPasswordLength() {
        return this.config.getInt("security.min-password-length", 4);
    }

    public boolean isTelegram2FAEnabled() {
        boolean enabled = this.config.getBoolean("telegram.2fa-enabled", false);
        String token = this.getTelegramToken();
        this.plugin.getLogger().info("\ud83d\udd27 Telegram 2FA Debug:");
        this.plugin.getLogger().info("\ud83d\udd27 2FA Enabled: " + enabled);
        this.plugin.getLogger().info("\ud83d\udd27 Token: " + (token.equals("token") ? "DEFAULT_TOKEN" : "SET"));
        this.plugin.getLogger().info("\ud83d\udd27 Bot Username: " + this.getTelegramBotUsername());
        return enabled && !token.equals("token") && !token.isEmpty();
    }

    public String getTelegramToken() {
        return this.config.getString("telegram.bot-token", "token").trim();
    }

    public String getTelegramBotUsername() {
        return this.config.getString("telegram.bot-username", "юзернейм не указан").trim();
    }

    public boolean isActionBarEnabled() {
        return this.config.getBoolean("messages.actionbar-enabled", true);
    }

    public boolean isProtectionEffectsEnabled() {
        return this.config.getBoolean("protection.apply-effects", true);
    }

    public boolean isBlockMovementEnabled() {
        return this.config.getBoolean("protection.block-movement", true);
    }

    public boolean isBlockChatEnabled() {
        return this.config.getBoolean("protection.block-chat", true);
    }

    public boolean isBlockCommandsEnabled() {
        return this.config.getBoolean("protection.block-commands", true);
    }

    public boolean isBlockInteractionsEnabled() {
        return this.config.getBoolean("protection.block-interactions", true);
    }

    public boolean isUUIDAuthenticationEnabled() {
        return this.config.getBoolean("security.uuid-authentication", true);
    }

    public boolean isUUIDPasswordRequired(UUID uuid) {
        return this.config.getBoolean("security.uuid-password-required." + uuid.toString(), false);
    }

    public void setUUIDPasswordRequired(UUID uuid, boolean required) {
        this.config.set("security.uuid-password-required." + uuid.toString(), required);

        try {
            this.config.save(this.configFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Error saving config: " + e.getMessage());
        }

    }

    public boolean isTelemetryEnabled() {
        return this.config.getBoolean("telemetry.enabled", true);
    }

    public void setTelemetryEnabled(boolean enabled) {
        this.config.set("telemetry.enabled", enabled);

        try {
            this.config.save(this.configFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Error saving config: " + e.getMessage());
        }

    }

    public String getLuckPermsUnauthorizedGroup() {
        return this.config.getString("luckperms.unauthorized", "unloggined");
    }

    public String getLuckPermsVanishLoginGroup() {
        return this.config.getString("luckperms.vanishlogin", "vanishlogin");
    }
}