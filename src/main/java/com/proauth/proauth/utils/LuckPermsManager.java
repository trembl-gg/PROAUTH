package com.proauth.proauth.utils;

import com.proauth.proauth.ProAuth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsManager {
    private final ProAuth plugin;
    private final ConfigManager configManager;
    private Object api;
    private boolean enabled = false;
    private String unauthorizedGroup = "unloggined";
    private String vanishloginGroup = "vanishlogin";
    private Map<UUID, String> playerOriginalGroups = new HashMap();
    private Map<UUID, Boolean> playerSessionLogin = new HashMap();

    public LuckPermsManager(ProAuth plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.initializeLuckPerms();
    }

    private void initializeLuckPerms() {
        try {
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(luckPermsClass);
            if (provider != null) {
                this.api = provider.getProvider();
                this.loadConfig();
                this.enabled = true;
                this.plugin.getLogger().info("✅ LuckPerms integration enabled");
            } else {
                this.enabled = false;
                this.plugin.getLogger().fine("⚠️ LuckPerms not found - group assignment disabled");
            }
        } catch (ClassNotFoundException var3) {
            this.enabled = false;
            this.plugin.getLogger().fine("⚠️ LuckPerms not installed - group assignment disabled");
        } catch (Exception e) {
            this.enabled = false;
            this.plugin.getLogger().warning("⚠️ Error initializing LuckPerms: " + e.getMessage());
        }

    }

    private void loadConfig() {
        this.unauthorizedGroup = this.configManager.getLuckPermsUnauthorizedGroup();
        this.vanishloginGroup = this.configManager.getLuckPermsVanishLoginGroup();
        this.plugin.getLogger().info("\ud83d\udccb LuckPerms groups - Unauthorized: '" + this.unauthorizedGroup + "', VanishLogin: '" + this.vanishloginGroup + "'");
        if (this.enabled) {
            this.initializeUnauthorizedGroups();
        }

    }

    private void initializeUnauthorizedGroups() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                String createUnauthedCmd = "lp creategroup " + this.unauthorizedGroup;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), createUnauthedCmd);
                this.plugin.getLogger().info("[ProAuth] Executed command: " + createUnauthedCmd);
                String setUnauthedPermCmd = "lp group " + this.unauthorizedGroup + " permission set flectonepulse.module.message.chat false";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), setUnauthedPermCmd);
                this.plugin.getLogger().info("[ProAuth] Executed command: " + setUnauthedPermCmd);
                String setUnauthedWeight = "lp group " + this.unauthorizedGroup + " permission set weight.100000";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), setUnauthedWeight);
                this.plugin.getLogger().info("[ProAuth] Executed command: " + setUnauthedWeight);
                String createVanishCmd = "lp creategroup " + this.vanishloginGroup;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), createVanishCmd);
                this.plugin.getLogger().info("[ProAuth] Executed command: " + createVanishCmd);
                String setVanishChatPermCmd = "lp group " + this.vanishloginGroup + " permission set flectonepulse.module.message.chat false";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), setVanishChatPermCmd);
                this.plugin.getLogger().info("[ProAuth] Executed command: " + setVanishChatPermCmd);
                String setVanishJoinPermCmd = "lp group " + this.vanishloginGroup + " permission set flectonepulse.module.message.join false";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), setVanishJoinPermCmd);
                this.plugin.getLogger().info("[ProAuth] Executed command: " + setVanishJoinPermCmd);
                String setVanishWeight = "lp group " + this.vanishloginGroup + " permission set weight.100000";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), setVanishWeight);
                this.plugin.getLogger().info("[ProAuth] Executed command: " + setVanishWeight);
                this.plugin.getLogger().info("✅ Initialized unauthorized groups '" + this.unauthorizedGroup + "' and '" + this.vanishloginGroup + "'");
            } catch (Exception e) {
                this.plugin.getLogger().warning("Error initializing unauthorized groups: " + e.getMessage());
            }

        });
    }

    public void storePlayerOriginalGroup(Player player) {
        if (this.enabled && this.api != null) {
            try {
                Object userManager = this.api.getClass().getMethod("getUserManager").invoke(this.api);
                Object userCompletableFuture = userManager.getClass().getMethod("loadUser", UUID.class).invoke(userManager, player.getUniqueId());
                Object user = userCompletableFuture.getClass().getMethod("join").invoke(userCompletableFuture);
                if (user == null) {
                    return;
                }

                Object dataSet = user.getClass().getMethod("data").invoke(user);
                Object queryOptions = Class.forName("net.luckperms.api.query.QueryOptions").getMethod("defaultContextualOptions").invoke((Object)null);
                Object nodeStream = dataSet.getClass().getMethod("stream").invoke(dataSet);
                String originalGroup = this.getFirstParentGroup(nodeStream);
                if (originalGroup != null) {
                    this.playerOriginalGroups.put(player.getUniqueId(), originalGroup);
                    Logger var10 = this.plugin.getLogger();
                    String var11 = player.getName();
                    var10.fine("Stored original group for " + var11 + ": " + originalGroup);
                } else {
                    this.playerOriginalGroups.put(player.getUniqueId(), "default");
                }
            } catch (Exception e) {
                Logger var10000 = this.plugin.getLogger();
                String var10001 = player.getName();
                var10000.fine("Could not store original group for " + var10001 + ": " + e.getMessage());
            }

        }
    }

    public void markSessionLogin(Player player) {
        this.playerSessionLogin.put(player.getUniqueId(), true);
    }

    public boolean isSessionLogin(Player player) {
        Boolean isSession = (Boolean)this.playerSessionLogin.remove(player.getUniqueId());
        return isSession != null && isSession;
    }

    private String getFirstParentGroup(Object nodeStream) {
        try {
            for(Object node : (List)nodeStream.getClass().getMethod("collect", Collector.class).invoke(nodeStream, Collectors.toList())) {
                Boolean isGroupNode = (Boolean)node.getClass().getMethod("isGroupNode").invoke(node);
                if (isGroupNode) {
                    String groupName = (String)node.getClass().getMethod("getGroupName").invoke(node);
                    if (!groupName.equals(this.unauthorizedGroup)) {
                        return groupName;
                    }
                }
            }
        } catch (Exception e) {
            this.plugin.getLogger().fine("Error parsing groups: " + e.getMessage());
        }

        return null;
    }

    public String getPlayerPrimaryGroup(Player player) {
        if (this.enabled && this.api != null) {
            try {
                Object userManager = this.api.getClass().getMethod("getUserManager").invoke(this.api);
                Object userCompletableFuture = userManager.getClass().getMethod("loadUser", UUID.class).invoke(userManager, player.getUniqueId());
                Object user = userCompletableFuture.getClass().getMethod("join").invoke(userCompletableFuture);
                if (user == null) {
                    return null;
                } else {
                    Object dataSet = user.getClass().getMethod("data").invoke(user);
                    Object nodeStream = dataSet.getClass().getMethod("stream").invoke(dataSet);
                    String group = this.getFirstParentGroup(nodeStream);
                    return group;
                }
            } catch (Exception e) {
                Logger var10000 = this.plugin.getLogger();
                String var10001 = player.getName();
                var10000.fine("Error fetching primary group for " + var10001 + ": " + e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public void restorePlayerOriginalGroup(Player player) {
        if (this.enabled) {
            try {
                this.removePlayerFromGroup(player, this.unauthorizedGroup);
                this.removePlayerFromGroup(player, this.vanishloginGroup);
            } catch (Exception e) {
                this.plugin.getLogger().fine("Error removing temp groups: " + e.getMessage());
            }

            try {
                this.setPlayerPermission(player, "flectonepulse.module.message.chat", "true");
            } catch (Exception e) {
                Logger var10000 = this.plugin.getLogger();
                String var10001 = player.getName();
                var10000.fine("Error setting chat permission true for " + var10001 + ": " + e.getMessage());
            }

            String originalGroup = (String)this.playerOriginalGroups.remove(player.getUniqueId());
            if (originalGroup == null) {
                originalGroup = this.plugin.getDatabaseManager() != null ? this.plugin.getDatabaseManager().getPlayerOriginalGroup(player.getName()) : "default";
            }

            if (originalGroup != null && !originalGroup.equals("default") && !originalGroup.equals(this.unauthorizedGroup) && !originalGroup.equals(this.vanishloginGroup)) {
                this.addPlayerToGroup(player, originalGroup);
            }

        }
    }

    public void setPlayerUnauthorized(Player player) {
        if (this.enabled) {
            String currentGroup = this.getPlayerPrimaryGroup(player);
            if (currentGroup != null && !currentGroup.equals(this.unauthorizedGroup) && !currentGroup.equals(this.vanishloginGroup)) {
                if (this.plugin.getDatabaseManager() != null) {
                    this.plugin.getDatabaseManager().savePlayerOriginalGroup(player.getName(), currentGroup);
                }

                this.playerOriginalGroups.put(player.getUniqueId(), currentGroup);
            }

            String groupToAssign = this.unauthorizedGroup;
            boolean useVanishlogin = this.plugin.getDatabaseManager() != null && this.plugin.getDatabaseManager().isVanishLoginEnabled(player.getName());
            if (useVanishlogin) {
                groupToAssign = this.vanishloginGroup;
            }

            this.addPlayerToGroup(player, groupToAssign);
            this.setPlayerPermission(player, "flectonepulse.module.message.chat", "false");
            if (groupToAssign.equals(this.vanishloginGroup)) {
                this.setPlayerPermission(player, "flectonepulse.module.message.join", "false");
            }

        }
    }

    private void addPlayerToGroup(Player player, String groupName) {
        if (this.enabled) {
            try {
                String var5 = player.getName();
                String cmd = "lp user " + var5 + " parent add " + groupName;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                Logger var6 = this.plugin.getLogger();
                String var7 = player.getName();
                var6.info("[ProAuth] Added group for " + var7 + ": " + groupName);
                this.plugin.getLogger().fine("[ProAuth] Executed command: " + cmd);
            } catch (Exception e) {
                Logger var10000 = this.plugin.getLogger();
                String var10001 = player.getName();
                var10000.warning("Error adding player " + var10001 + " to group " + groupName + ": " + e.getMessage());
            }

        }
    }

    private void removePlayerFromGroup(Player player, String groupName) {
        if (this.enabled) {
            try {
                String var5 = player.getName();
                String cmd = "lp user " + var5 + " parent remove " + groupName;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                Logger var6 = this.plugin.getLogger();
                String var7 = player.getName();
                var6.info("[ProAuth] Removed group for " + var7 + ": " + groupName);
                this.plugin.getLogger().fine("[ProAuth] Executed command: " + cmd);
            } catch (Exception e) {
                Logger var10000 = this.plugin.getLogger();
                String var10001 = player.getName();
                var10000.warning("Error removing player " + var10001 + " from group " + groupName + ": " + e.getMessage());
            }

        }
    }

    private void setPlayerPermission(Player player, String permission, String value) {
        if (this.enabled) {
            try {
                String cmd = "lp user " + player.getName() + " permission set " + permission + " " + value;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                this.plugin.getLogger().fine("[ProAuth] Executed command: " + cmd);
            } catch (Exception e) {
                Logger var10000 = this.plugin.getLogger();
                String var10001 = player.getName();
                var10000.warning("Error setting permission for " + var10001 + ": " + e.getMessage());
            }

        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public String getUnauthorizedGroup() {
        return this.unauthorizedGroup;
    }

    public String getVanishLoginGroup() {
        return this.vanishloginGroup;
    }

    public void applyVanishLogin(Player player) {
        if (this.enabled) {
            this.addPlayerToGroup(player, this.vanishloginGroup);
            this.setPlayerPermission(player, "flectonepulse.module.message.chat", "false");
            this.setPlayerPermission(player, "flectonepulse.module.message.join", "false");
        }
    }

    public void removeVanishLogin(Player player) {
        if (this.enabled) {
            this.removePlayerFromGroup(player, this.vanishloginGroup);
            this.setPlayerPermission(player, "flectonepulse.module.message.chat", "true");
            this.setPlayerPermission(player, "flectonepulse.module.message.join", "true");
        }
    }

    public void setVanishJoinPermission(Player player, boolean enabled) {
        if (this.enabled) {
            this.setPlayerPermission(player, "flectonepulse.module.message.join", enabled ? "true" : "false");
        }
    }
}