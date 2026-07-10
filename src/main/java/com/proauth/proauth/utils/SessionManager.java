package com.proauth.proauth.utils;

import com.proauth.proauth.ProAuth;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SessionManager {
    private final ProAuth plugin;
    private final Set<UUID> authenticatedPlayers;
    private final Set<UUID> activeSessions;

    public SessionManager(ProAuth plugin) {
        this.plugin = plugin;
        this.authenticatedPlayers = new HashSet();
        this.activeSessions = new HashSet();
    }

    public void authenticatePlayer(Player player) {
        this.authenticatedPlayers.add(player.getUniqueId());

        String playerIP = player.getAddress().getAddress().getHostAddress();
        this.plugin.getDatabaseManager().updateLastIP(player.getName(), playerIP);
        this.plugin.getTelemetryManager().recordAuthentication();

        LuckPermsManager luckPerms = this.plugin.getLuckPermsManager();
        if (luckPerms != null && luckPerms.isEnabled()) {
            luckPerms.restorePlayerOriginalGroup(player);
        }

        // После авторизации игрок должен быть уязвим
        player.setInvulnerable(false);

        if (this.plugin.getConfigManager().isSessionEnabled()) {
            this.activeSessions.add(player.getUniqueId());

            if (this.plugin.getConfigManager().getSessionDuration() > 0) {
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    if (player.isOnline() && this.activeSessions.contains(player.getUniqueId())) {
                        this.logoutPlayer(player);
                        player.sendMessage(this.plugin.getLocalizationManager().getMessage("session-manager.session-expired"));
                    }
                }, (long) (this.plugin.getConfigManager().getSessionDuration() * 60) * 20L);
            }
        }
    }

    public void authenticatePlayerViaSession(Player player) {
        this.authenticatedPlayers.add(player.getUniqueId());

        LuckPermsManager luckPerms = this.plugin.getLuckPermsManager();
        if (luckPerms != null && luckPerms.isEnabled()) {
            luckPerms.markSessionLogin(player);
        }

        String playerIP = player.getAddress().getAddress().getHostAddress();
        this.plugin.getDatabaseManager().updateLastIP(player.getName(), playerIP);
        this.plugin.getTelemetryManager().recordAuthentication();

        // После входа по сессии игрок также должен быть уязвим
        player.setInvulnerable(false);

        if (this.plugin.getConfigManager().isSessionEnabled()) {
            this.activeSessions.add(player.getUniqueId());

            if (this.plugin.getConfigManager().getSessionDuration() > 0) {
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    if (player.isOnline() && this.activeSessions.contains(player.getUniqueId())) {
                        this.logoutPlayer(player);
                        player.sendMessage(this.plugin.getLocalizationManager().getMessage("session-manager.session-expired"));
                    }
                }, (long) (this.plugin.getConfigManager().getSessionDuration() * 60) * 20L);
            }
        }
    }

    public void authenticate(Player player) {
        this.authenticatePlayer(player);
    }

    public void logoutPlayer(Player player) {
        this.authenticatedPlayers.remove(player.getUniqueId());
        this.activeSessions.remove(player.getUniqueId());

        // Неавторизованный игрок должен быть защищён
        player.setInvulnerable(true);

        LuckPermsManager luckPerms = this.plugin.getLuckPermsManager();
        if (luckPerms != null && luckPerms.isEnabled()) {
            luckPerms.setPlayerUnauthorized(player);
        }

        try {
            if (this.plugin.getDatabaseManager() != null) {
                this.plugin.getDatabaseManager().updateLastIP(player.getName(), (String) null);
            }
        } catch (Exception e) {
            Logger logger = this.plugin.getLogger();
            logger.fine("Error clearing last IP for " + player.getName() + ": " + e.getMessage());
        }

        this.plugin.getDatabaseManager().updateLastOnline(player.getName());
    }

    public void logoutPlayerByName(String username) {
        Player player = Bukkit.getPlayerExact(username);
        if (player != null) {
            this.logoutPlayer(player);
        } else {
            this.authenticatedPlayers.removeIf((uuid) -> {
                Player p = Bukkit.getPlayer(uuid);
                return p != null && p.getName().equalsIgnoreCase(username);
            });
            this.activeSessions.removeIf((uuid) -> {
                Player p = Bukkit.getPlayer(uuid);
                return p != null && p.getName().equalsIgnoreCase(username);
            });
            this.plugin.getDatabaseManager().updateLastOnline(username);

            try {
                if (this.plugin.getDatabaseManager() != null) {
                    this.plugin.getDatabaseManager().updateLastIP(username, (String)null);
                }
            } catch (Exception e) {
                this.plugin.getLogger().fine("Error clearing last IP for user " + username + ": " + e.getMessage());
            }
        }

    }

    public boolean isAuthenticated(Player player) {
        return this.authenticatedPlayers.contains(player.getUniqueId());
    }

    public boolean hasActiveSession(Player player) {
        return this.activeSessions.contains(player.getUniqueId());
    }

    public void clearSessions() {
        this.authenticatedPlayers.clear();
        this.activeSessions.clear();
    }

    public int getAuthenticatedCount() {
        return this.authenticatedPlayers.size();
    }

    public int getActiveSessionsCount() {
        return this.activeSessions.size();
    }
}