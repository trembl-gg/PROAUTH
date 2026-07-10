package com.proauth.proauth.events;

import com.proauth.proauth.ProAuth;
import com.proauth.proauth.utils.LuckPermsManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerListener implements Listener {
    private final ProAuth plugin;
    private final Map<UUID, Long> chatCooldown = new HashMap();

    public PlayerListener(ProAuth plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean isSessionLogin = false;
        LuckPermsManager luckPerms = this.plugin.getLuckPermsManager();
        if (this.plugin.getConfigManager().isSessionEnabled() && this.plugin.getDatabaseManager() != null) {
            try {
                String lastIP = this.plugin.getDatabaseManager().getLastIP(player.getName());
                String currentIP = player.getAddress().getAddress().getHostAddress();
                if (lastIP != null && lastIP.equals(currentIP)) {
                    this.plugin.getSessionManager().authenticatePlayerViaSession(player);
                    isSessionLogin = true;
                    this.plugin.getLogger().info("[ProAuth] Session auto-authenticated for " + player.getName());
                }
            } catch (Exception e) {
                Logger var10000 = this.plugin.getLogger();
                String var10001 = player.getName();
                var10000.fine("Error checking session IP for " + var10001 + ": " + e.getMessage());
            }
        }

        if (!isSessionLogin && luckPerms != null && luckPerms.isEnabled()) {
            isSessionLogin = luckPerms.isSessionLogin(player);
        }

        if (!isSessionLogin) {
            if (luckPerms != null && luckPerms.isEnabled()) {
                luckPerms.storePlayerOriginalGroup(player);
            }

            if (luckPerms != null && luckPerms.isEnabled()) {
                luckPerms.setPlayerUnauthorized(player);
            }
        }

        if (this.plugin.getConfigManager().isProtectionEffectsEnabled()) {
            this.applyProtectionEffects(player);
        }

    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getConfigManager().isBlockMovementEnabled() && !this.plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!this.plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            long lastChat = (Long)this.chatCooldown.getOrDefault(player.getUniqueId(), 0L);
            if (now - lastChat > 3000L) {
                this.chatCooldown.put(player.getUniqueId(), now);
                if (!this.plugin.getDatabaseManager().isUserRegistered(player.getName())) {
                    player.sendMessage(this.plugin.getMessageManager().getMessage("general.registration-hint"));
                } else {
                    player.sendMessage(this.plugin.getMessageManager().getMessage("general.auth-hint"));
                }
            } else {
                player.sendMessage(this.plugin.getMessageManager().getMessage("general.spam-blocked"));
            }
        }

    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if ((message.equalsIgnoreCase("/reload") || message.toLowerCase().startsWith("/reload ")) && !player.hasPermission("proauth.bypass") && !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage(this.plugin.getMessageManager().getMessage("general.debug-blocked"));
        } else {
            if (this.plugin.getConfigManager().isBlockCommandsEnabled() && !this.plugin.getSessionManager().isAuthenticated(player)) {
                String lower = message.toLowerCase();
                if (!lower.startsWith("/login") && !lower.startsWith("/l") && !lower.startsWith("/register") && !lower.startsWith("/reg") && !lower.startsWith("/2fa") && !lower.startsWith("/logout") && !lower.startsWith("/vanishlogin") && !lower.startsWith("/vl")) {
                    event.setCancelled(true);
                    player.sendMessage(this.plugin.getLocalizationManager().getMessage("general.authentication-required"));
                }
            }

        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!this.plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
            player.sendMessage(this.plugin.getLocalizationManager().getMessage("general.interaction-blocked"));
        }

    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player)event.getPlayer();
            if (!this.plugin.getSessionManager().isAuthenticated(player)) {
                event.setCancelled(true);
                player.sendMessage(this.plugin.getLocalizationManager().getMessage("general.inventory-blocked"));
            }
        }

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        if (!this.plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
            player.sendMessage(this.plugin.getLocalizationManager().getMessage("general.inventory-blocked"));
        }

    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!this.plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
            player.sendMessage(this.plugin.getLocalizationManager().getMessage("general.inventory-blocked"));
        }

    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!this.plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("proauth.bypass") && !player.isOp()) {
        }

    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player)event.getEntity();
            if (!this.plugin.getSessionManager().isAuthenticated(player)) {
                event.setCancelled(true);
                return;
            }

            if (player.isInvulnerable()) {
                event.setCancelled(true);
            }
        }

        if (event instanceof EntityDamageByEntityEvent damageEvent) {
            if (damageEvent.getDamager() instanceof Player) {
                Player damager = (Player)damageEvent.getDamager();
                if (!this.plugin.getSessionManager().isAuthenticated(damager)) {
                    event.setCancelled(true);
                    if (damageEvent.getEntity() instanceof Player) {
                        damager.sendMessage(this.plugin.getLocalizationManager().getMessage("general.damage-blocked"));
                    }

                    return;
                }
            } else if (damageEvent.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile)damageEvent.getDamager();
                if (projectile.getShooter() instanceof Player) {
                    Player shooter = (Player)projectile.getShooter();
                    if (!this.plugin.getSessionManager().isAuthenticated(shooter)) {
                        event.setCancelled(true);
                        if (damageEvent.getEntity() instanceof Player) {
                            shooter.sendMessage(this.plugin.getLocalizationManager().getMessage("general.damage-blocked"));
                        }

                        return;
                    }
                }
            }
        }

    }

    private void applyProtectionEffects(Player player) {
        if (!this.plugin.getSessionManager().isAuthenticated(player)) {
        }

    }
}