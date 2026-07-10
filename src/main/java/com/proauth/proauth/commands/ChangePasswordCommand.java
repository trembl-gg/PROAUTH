package com.proauth.proauth.commands;

import com.proauth.proauth.ProAuth;
import com.proauth.proauth.utils.HashUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChangePasswordCommand implements CommandExecutor {
    private final ProAuth plugin;

    public ChangePasswordCommand(ProAuth plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            this.plugin.getCommandLogger().logCommand(player, command.getName(), args);
            if (!this.plugin.getSessionManager().isAuthenticated(player)) {
                player.sendMessage(this.plugin.getLocalizationManager().getMessage("changepassword.not-authenticated"));
                return true;
            } else if (args.length != 3) {
                player.sendMessage(this.plugin.getLocalizationManager().getMessage("changepassword.usage"));
                return true;
            } else {
                String oldPassword = args[0];
                String newPassword = args[1];
                String repeatPassword = args[2];
                if (!newPassword.equals(repeatPassword)) {
                    player.sendMessage(this.plugin.getLocalizationManager().getMessage("changepassword.passwords-not-match"));
                    return true;
                } else {
                    int minLength = this.plugin.getConfigManager().getMinPasswordLength();
                    if (newPassword.length() < minLength) {
                        player.sendMessage(this.plugin.getLocalizationManager().getMessage("changepassword.password-too-short", new String[]{String.valueOf(minLength)}));
                        return true;
                    } else if (oldPassword.equals(newPassword)) {
                        player.sendMessage(this.plugin.getLocalizationManager().getMessage("changepassword.same-as-old"));
                        return true;
                    } else {
                        String storedHash = this.plugin.getDatabaseManager().getPasswordHash(player.getName());
                        if (storedHash != null && HashUtils.verifyPassword(oldPassword, storedHash)) {
                            String salt = HashUtils.generateSalt();
                            String newHashedPassword = HashUtils.hashPassword(newPassword, salt);
                            if (this.plugin.getDatabaseManager().updatePassword(player.getName(), newHashedPassword)) {
                                player.sendMessage(this.plugin.getLocalizationManager().getMessage("changepassword.success"));
                                if (this.plugin.getTwoFAManager().is2FAEnabled(player)) {
                                    this.sendTelegramNotification(player);
                                }

                                this.plugin.getSessionManager().logoutPlayer(player);
                                player.sendMessage(this.plugin.getLocalizationManager().getMessage("changepassword.logout-notice"));
                            } else {
                                player.sendMessage(this.plugin.getLocalizationManager().getMessage("changepassword.error"));
                            }

                            return true;
                        } else {
                            player.sendMessage(this.plugin.getLocalizationManager().getMessage("changepassword.invalid-old-password"));
                            return true;
                        }
                    }
                }
            }
        } else {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("changepassword.only-players"));
            return true;
        }
    }

    private void sendTelegramNotification(Player player) {
        try {
            String chatId = this.plugin.getDatabaseManager().getTelegramChatId(player.getName());
            if (chatId != null && this.plugin.getTwoFAManager().isTelegramBotAvailable()) {
                String var5 = this.plugin.getLocalizationManager().getMessage("2fa.notifi");
                String message = var5 + player.getName() + this.plugin.getLocalizationManager().getMessage("2fa.notifi2") + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + "\n\ud83c\udf10 IP: " + player.getAddress().getAddress().getHostAddress() + this.plugin.getLocalizationManager().getMessage("2fa.notifi3");
                this.plugin.getTwoFAManager().getTelegramBot().sendMessage(chatId, message);
            }
        } catch (Exception e) {
            Logger var10000 = this.plugin.getLogger();
            String var10001 = this.plugin.getLocalizationManager().getMessage("2fa.notifi-error");
            var10000.warning(var10001 + e.getMessage());
        }

    }
}
