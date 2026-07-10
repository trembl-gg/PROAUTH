package com.proauth.proauth;

import com.proauth.proauth.commands.CommandManager;
import com.proauth.proauth.database.DatabaseManager;
import com.proauth.proauth.events.PlayerListener;
import com.proauth.proauth.utils.ConfigManager;
import com.proauth.proauth.utils.LocalizationManager;
import com.proauth.proauth.utils.LuckPermsManager;
import com.proauth.proauth.utils.MessageManager;
import com.proauth.proauth.utils.ProAuthCommandLogger;
import com.proauth.proauth.utils.SessionManager;
import com.proauth.proauth.utils.TelemetryManager;
import com.proauth.proauth.utils.TwoFAManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import com.proauth.proauth.logging.CommandLogFilter;


public class ProAuth extends JavaPlugin {
    private static ProAuth instance;
    private ConfigManager configManager;
    private LocalizationManager localizationManager;
    private MessageManager messageManager;
    private ProAuthCommandLogger commandLogger;
    private DatabaseManager databaseManager;
    private SessionManager sessionManager;
    private TwoFAManager twoFAManager;
    private TelemetryManager telemetryManager;
    private LuckPermsManager luckPermsManager;
    private CommandManager commandManager;
    private CommandLogFilter commandLogFilter;

    public ProAuth() {
    }

    public void onEnable() {
        instance = this;

        Logger rootLogger = (Logger) LogManager.getRootLogger();

        commandLogFilter = new CommandLogFilter();
        rootLogger.addFilter(commandLogFilter);
        this.showStartupLogo();

        try {
            this.configManager = new ConfigManager(this);
            this.configManager.loadConfig();
            this.localizationManager = new LocalizationManager(this);
            this.messageManager = new MessageManager(this);
            this.commandLogger = new ProAuthCommandLogger(this);
            this.databaseManager = new DatabaseManager(this);
            this.sessionManager = new SessionManager(this);
            this.commandManager = new CommandManager(this);
            this.databaseManager.initializeDatabase();
            this.twoFAManager = new TwoFAManager(this);
            this.telemetryManager = new TelemetryManager(this);
            this.luckPermsManager = new LuckPermsManager(this);
            this.commandManager.registerCommands();
            this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            this.getLogger().info("ProAuth succses loaded!");
            this.getLogger().info("Version: " + this.getDescription().getVersion());
        } catch (Exception e) {
            this.getLogger().severe("❌ Critical error while loading ProAuth!");
            this.getLogger().severe("❌ " + e.getMessage());
            e.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
        }

    }

    private void showStartupLogo() {
        this.getLogger().info("╔══════════════════════════════════════╗");
        this.getLogger().info("║              ProAuth                 ║");
        this.getLogger().info("║     Advanced Minecraft Security      ║");
        this.getLogger().info("║                                      ║");
        this.getLogger().info("╚══════════════════════════════════════╝");
        this.getLogger().info("Version: " + this.getDescription().getVersion());
        this.getLogger().info("Author: " + String.valueOf(this.getDescription().getAuthors()));
    }

    public void onDisable() {

        for(Player player : Bukkit.getOnlinePlayers()) {
            this.sessionManager.logoutPlayer(player);

            if (this.luckPermsManager != null && this.luckPermsManager.isEnabled()) {
                this.luckPermsManager.setPlayerUnauthorized(player);
            }
        }

        if (this.twoFAManager != null) {
            this.twoFAManager.shutdown();
        }

        if (this.databaseManager != null) {
            this.databaseManager.closeConnection();
        }

        this.getLogger().info("§cProAuth disabled!");
    }

    public static ProAuth getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public LocalizationManager getLocalizationManager() {
        return this.localizationManager;
    }

    public MessageManager getMessageManager() {
        return this.messageManager;
    }

    public ProAuthCommandLogger getCommandLogger() {
        return this.commandLogger;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public SessionManager getSessionManager() {
        return this.sessionManager;
    }

    public TwoFAManager getTwoFAManager() {
        return this.twoFAManager;
    }

    public TelemetryManager getTelemetryManager() {
        return this.telemetryManager;
    }

    public LuckPermsManager getLuckPermsManager() {
        return this.luckPermsManager;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }
}
