package com.proauth.proauth.commands;

import com.proauth.proauth.ProAuth;
import com.proauth.proauth.utils.HashUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class RegisterCommand implements CommandExecutor {
    private final ProAuth plugin;

    public RegisterCommand(ProAuth plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            this.plugin.getCommandLogger().logCommand(player, command.getName(), args);
            if (this.plugin.getSessionManager().isAuthenticated(player)) {
                this.sendActionBar(player, this.plugin.getMessageManager().getMessage("register.already-authenticated"));
                return true;
            } else if (this.plugin.getDatabaseManager().isUserRegistered(player.getName())) {
                this.sendActionBar(player, this.plugin.getMessageManager().getMessage("register.already-registered"));
                return true;
            } else if (args.length != 2) {
                this.sendActionBar(player, this.plugin.getMessageManager().getMessage("register.usage"));
                player.sendMessage(this.plugin.getMessageManager().getMessage("register.usage"));
                return true;
            } else {
                String password = args[0];
                String repeatPassword = args[1];
                if (!password.equals(repeatPassword)) {
                    this.sendActionBar(player, this.plugin.getMessageManager().getMessage("register.passwords-not-match"));
                    player.sendMessage(this.plugin.getMessageManager().getMessage("register.passwords-not-match"));
                    return true;
                } else {
                    int minLength = this.plugin.getConfigManager().getMinPasswordLength();
                    if (password.length() < minLength) {
                        this.sendActionBar(player, this.plugin.getMessageManager().getMessage("register.password-too-short", new String[]{String.valueOf(minLength)}));
                        player.sendMessage(this.plugin.getMessageManager().getMessage("register.password-too-short", new String[]{String.valueOf(minLength)}));
                        return true;
                    } else {
                        String salt = HashUtils.generateSalt();
                        String hashedPassword = HashUtils.hashPassword(password, salt);
                        String ip = player.getAddress().getAddress().getHostAddress();
                        if (this.plugin.getDatabaseManager().registerPlayer(player.getUniqueId(), player.getName(), hashedPassword, ip)) {
                            this.plugin.getSessionManager().authenticatePlayer(player);
                            this.plugin.getDatabaseManager().updateLoginInfo(player.getName(), ip);
                            this.plugin.getTelemetryManager().recordRegistration();
                            this.sendActionBar(player, this.plugin.getMessageManager().getMessage("register.success"));
                            player.sendMessage(this.plugin.getMessageManager().getMessage("register.success"));
                            player.sendMessage(this.plugin.getMessageManager().getMessage("register.tip"));
                            this.removeProtectionEffects(player);
                        } else {
                            this.sendActionBar(player, this.plugin.getMessageManager().getMessage("register.error"));
                            player.sendMessage(this.plugin.getMessageManager().getMessage("register.error"));
                        }

                        return true;
                    }
                }
            }
        } else {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Эта команда только для игроков!");
            return true;
        }
    }

    private void sendActionBar(Player player, String message) {
        if (this.plugin.getConfigManager().isActionBarEnabled()) {
            try {
                player.sendActionBar(Component.text(message));
            } catch (Exception var4) {
                player.sendMessage(message);
            }
        } else {
            player.sendMessage(message);
        }

    }

    private void removeProtectionEffects(Player player) {
        if (this.plugin.getConfigManager().isProtectionEffectsEnabled()) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
        }

    }
}