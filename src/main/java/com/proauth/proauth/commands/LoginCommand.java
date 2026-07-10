package com.proauth.proauth.commands;

import com.proauth.proauth.ProAuth;
import com.proauth.proauth.utils.HashUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.ChatColor;
import org.bukkit.BanList.Type;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class LoginCommand implements CommandExecutor {
    private final ProAuth plugin;
    private final Map<UUID, Integer> loginAttempts;

    public LoginCommand(ProAuth plugin) {
        this.plugin = plugin;
        this.loginAttempts = new HashMap();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            this.plugin.getCommandLogger().logCommand(player, command.getName(), args);
            if (this.plugin.getSessionManager().isAuthenticated(player)) {
                player.sendMessage(this.plugin.getMessageManager().getMessage("login.already-authenticated"));
                return true;
            } else if (args.length != 1) {
                player.sendMessage(this.plugin.getMessageManager().getMessage("login.usage"));
                return true;
            } else if (!this.plugin.getDatabaseManager().isUserRegistered(player.getName())) {
                player.sendMessage(this.plugin.getMessageManager().getMessage("login.not-registered"));
                return true;
            } else {
                String password = args[0];
                String storedHash = this.plugin.getDatabaseManager().getPasswordHash(player.getName());
                if (storedHash != null && HashUtils.verifyPassword(password, storedHash)) {
                    if (this.plugin.getTwoFAManager().is2FAEnabled(player)) {
                        if (this.plugin.getDatabaseManager().is2FAPending(player.getName())) {
                            player.sendMessage(this.plugin.getMessageManager().getMessage("login.2fa-pending"));
                        } else if (this.plugin.getTwoFAManager().send2FACode(player)) {
                            player.sendMessage(this.plugin.getMessageManager().getMessage("login.2fa-sent"));
                        } else {
                            player.sendMessage(this.plugin.getMessageManager().getMessage("login.2fa-error"));
                        }
                    } else {
                        this.completeLogin(player);
                    }
                } else {
                    int attempts = (Integer)this.loginAttempts.getOrDefault(player.getUniqueId(), 0) + 1;
                    this.loginAttempts.put(player.getUniqueId(), attempts);
                    int maxAttempts = this.plugin.getConfigManager().getMaxAttempts();
                    int remaining = maxAttempts - attempts;
                    player.sendMessage(this.plugin.getMessageManager().getMessage("login.invalid-password", new String[]{String.valueOf(remaining)}));
                    if (attempts >= maxAttempts) {
                        this.handleMaxAttempts(player);
                    }
                }

                return true;
            }
        } else {
            sender.sendMessage(this.plugin.getMessageManager().getMessage("general.not-permission"));
            return true;
        }
    }

    private void completeLogin(Player player) {
        this.plugin.getSessionManager().authenticatePlayer(player);
        this.plugin.getDatabaseManager().updateLoginInfo(player.getName(), player.getAddress().getAddress().getHostAddress());
        this.plugin.getTelemetryManager().recordAuthentication();
        this.loginAttempts.remove(player.getUniqueId());
        String message = this.plugin.getMessageManager().getMessage("login.success");
        this.sendActionBar(player, message);
        player.sendMessage(message);
        this.removeProtectionEffects(player);
        this.plugin.getTwoFAManager().sendLoginNotification(player);
    }

    private void sendActionBar(Player player, String message) {
        if (this.plugin.getConfigManager().isActionBarEnabled()) {
            try {
                player.sendActionBar(Component.text(message));
            } catch (Exception var4) {
                player.sendMessage(message);
            }
        }

    }

    private void removeProtectionEffects(Player player) {
        if (this.plugin.getConfigManager().isProtectionEffectsEnabled()) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
        }

    }

    private void handleMaxAttempts(Player player) {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        String ip = player.getAddress().getAddress().getHostAddress();
        if (this.plugin.getConfigManager().isIPBanEnabled()) {
            this.plugin.getLogger().warning("IP " + ip + this.plugin.getMessageManager().getMessage("general.ip-limit") + playerName);
            BanList banList = this.plugin.getServer().getBanList(Type.IP);
            banList.addBan(ip, this.plugin.getMessageManager().getMessage("general.max-tries-to-login"), new Date(System.currentTimeMillis() + (long)this.plugin.getConfigManager().getBanTime() * 1000L), "ProAuth");
        }

        String var10000 = String.valueOf(ChatColor.RED);
        String kickMessage = var10000 + this.plugin.getMessageManager().getMessage("general.limit-to-login") + String.valueOf(ChatColor.YELLOW) + this.plugin.getMessageManager().getMessage("general.wait", new String[]{String.valueOf(this.plugin.getConfigManager().getBanTime() / 60)}) + this.plugin.getMessageManager().getMessage("general.minutes");
        player.kickPlayer(kickMessage);
        this.loginAttempts.remove(playerId);
    }

    public void completeLoginWith2FA(Player player) {
        this.completeLogin(player);
    }
}