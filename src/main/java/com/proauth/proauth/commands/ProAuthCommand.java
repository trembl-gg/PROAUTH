package com.proauth.proauth.commands;

import com.proauth.proauth.ProAuth;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.BanList.Type;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ProAuthCommand implements CommandExecutor {
    private final ProAuth plugin;

    public ProAuthCommand(ProAuth plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            this.sendHelp(sender);
            return true;
        } else {
            switch (args[0].toLowerCase()) {
                case "unregister":
                case "unreg":
                    this.handleUnregister(sender, args.length > 1 ? args[1] : "");
                    break;
                case "help":
                    this.sendHelp(sender);
                    break;
                case "session":
                    this.handleSession(sender, args.length > 1 ? args[1] : "");
                    break;
                case "lastlogin":
                case "lastlog":
                    this.handleLastLogin(sender, args.length > 1 ? args[1] : "");
                    break;
                case "lastip":
                    this.handleLastIP(sender, args.length > 1 ? args[1] : "");
                    break;
                case "del2fa":
                case "remove2fa":
                    this.handleRemove2FA(sender, args.length > 1 ? args[1] : "");
                    break;
                case "unban":
                    this.handleUnban(sender, args.length > 1 ? args[1] : "");
                    break;
                case "stats":
                case "statistics":
                    this.handleStats(sender);
                    break;
                case "reload":
                    this.handleReload(sender);
                    break;
                default:
                    sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-title"));
                    this.sendHelp(sender);
            }

            return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-title"));
        sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-unregister"));
        sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-session"));
        sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-lastlogin"));
        sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-lastip"));
        sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-del2fa"));
        sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-unban"));
        sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-stats"));
        sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-reload"));
        sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-help"));
    }

    private void handleUnregister(CommandSender sender, String username) {
        if (!sender.hasPermission("proauth.admin")) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.permission-denied"));
        } else if (username.isEmpty()) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-unregister"));
        } else if (!this.plugin.getDatabaseManager().isUserRegistered(username)) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.unregister-user-not-found", new String[]{username}));
        } else {
            if (this.plugin.getDatabaseManager().unregisterUser(username)) {
                sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.unregister-success", new String[]{username}));
                this.plugin.getSessionManager().logoutPlayerByName(username);
            }

        }
    }

    private void handleRemove2FA(CommandSender sender, String username) {
        if (!sender.hasPermission("proauth.admin")) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.permission-denied"));
        } else if (username.isEmpty()) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-del2fa"));
        } else if (!this.plugin.getDatabaseManager().isUserRegistered(username)) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.del2fa-not-found", new String[]{username}));
        } else {
            if (this.plugin.getTwoFAManager().remove2FA(username)) {
                sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.del2fa-success", new String[]{username}));
            }

        }
    }

    private void handleUnban(CommandSender sender, String username) {
        if (!sender.hasPermission("proauth.admin")) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.permission-denied"));
        } else {
            try {
                BanList nameBanList = Bukkit.getBanList(Type.NAME);
                if (nameBanList.isBanned(username)) {
                    nameBanList.pardon(username);
                    sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.unban-success", new String[]{username}));
                } else {
                    sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.unban-failed", new String[]{username}));
                }
            } catch (Exception var4) {
                sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.unban-failed", new String[]{username}));
            }

        }
    }

    private void handleSession(CommandSender sender, String mode) {
        if (!sender.hasPermission("proauth.admin")) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.permission-denied"));
        } else {
            switch (mode.toLowerCase()) {
                case "on":
                    this.plugin.getConfigManager().getConfig().set("session.enabled", true);
                    sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.session-enabled"));
                    break;
                case "off":
                    this.plugin.getConfigManager().getConfig().set("session.enabled", false);
                    sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.session-disabled"));
                    break;
                case "status":
                    int sessions = this.plugin.getSessionManager().getActiveSessionsCount();
                    int authenticated = this.plugin.getSessionManager().getAuthenticatedCount();
                    sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.session-status", new String[]{String.valueOf(sessions), String.valueOf(authenticated)}));
                    break;
                default:
                    sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.help-session"));
            }

        }
    }

    private void handleLastLogin(CommandSender sender, String username) {
        if (!sender.hasPermission("proauth.admin")) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.permission-denied"));
        } else if (username.isEmpty()) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.command-usage-lastlogin"));
        } else {
            String lastLogin = this.plugin.getDatabaseManager().getLastLogin(username);
            if (lastLogin != null && !lastLogin.isEmpty()) {
                sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.lastlogin-info", new String[]{username, lastLogin}));
            } else {
                sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.lastlogin-not-found", new String[]{username}));
            }

        }
    }

    private void handleLastIP(CommandSender sender, String username) {
        if (!sender.hasPermission("proauth.admin")) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.permission-denied"));
        } else if (username.isEmpty()) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.command-usage-lastip"));
        } else {
            String lastIP = this.plugin.getDatabaseManager().getLastIP(username);
            if (lastIP != null && !lastIP.isEmpty()) {
                sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.lastip-info", new String[]{username, lastIP}));
            } else {
                sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.lastip-not-found", new String[]{username}));
            }

        }
    }

    private void handleStats(CommandSender sender) {
        if (!sender.hasPermission("proauth.admin")) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.permission-denied"));
        } else {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.stats-title"));
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.stats-registered", new String[]{String.valueOf(this.plugin.getDatabaseManager().getRegisteredUsersCount())}));
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.stats-authenticated", new String[]{String.valueOf(this.plugin.getSessionManager().getAuthenticatedCount())}));
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.stats-sessions", new String[]{String.valueOf(this.plugin.getSessionManager().getActiveSessionsCount())}));
        }
    }

    private void handleReload(CommandSender sender) {
        try {
            this.plugin.getConfigManager().loadConfig();
            this.plugin.getLocalizationManager().reloadLanguage();
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.reload-success"));
        } catch (Exception var3) {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.reload-failed"));
        }

    }
}