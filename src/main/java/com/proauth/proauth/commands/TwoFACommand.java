package com.proauth.proauth.commands;

import com.proauth.proauth.ProAuth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class TwoFACommand implements CommandExecutor {
    private final ProAuth plugin;

    public TwoFACommand(ProAuth plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 0 && args[0].equalsIgnoreCase("code")) {
                this.plugin.getCommandLogger().logCommand(player, command.getName(), args);
                this.handleCode(player, args);
                return true;
            } else if (!this.plugin.getSessionManager().isAuthenticated(player)) {
                player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.not-authenticated"));
                return true;
            } else {
                if (args.length > 0) {
                    this.plugin.getCommandLogger().logCommand(player, command.getName(), args);
                }

                if (args.length == 0) {
                    this.sendHelp(player);
                    return true;
                } else {
                    switch (args[0].toLowerCase()) {
                        case "enable" -> this.handleEnable(player, args);
                        case "disable" -> this.handleDisable(player);
                        case "status" -> this.handleStatus(player);
                        case "help" -> this.sendHelp(player);
                        default -> player.sendMessage(this.plugin.getMessageManager().getMessage("general.unknown-command"));
                    }

                    return true;
                }
            }
        } else {
            sender.sendMessage(this.plugin.getMessageManager().getMessage("2fa.only-players"));
            return true;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.help-title"));
        player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.help-enable"));
        player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.help-disable"));
        player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.help-code"));
        player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.help-status"));
        player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.help-help"));
    }

    private void handleEnable(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.enable-usage"));
            String botUsername = this.plugin.getConfigManager().getTelegramBotUsername();
            player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.enable-instructions", new String[]{botUsername}));
            player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.enable-warning"));
        } else if (!this.plugin.getConfigManager().isTelegram2FAEnabled()) {
            player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.disabled-in-config"));
        } else {
            String chatId = args[1];
            if (this.plugin.getTwoFAManager().enable2FA(player, chatId)) {
                player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.enable-success"));
                player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.enable-notice"));
            }
        }

    }

    private void handleDisable(Player player) {
        if (this.plugin.getTwoFAManager().disable2FA(player)) {
            player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.disable-success"));
        } else {
            player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.disable-error"));
        }

    }

    private void handleCode(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.code-usage"));
            player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.code-info"));
            String ip = player.getAddress().getAddress().getHostAddress();
            int remainingAttempts = this.plugin.getTwoFAManager().getRemaining2FAAttempts(ip);
            player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.code-attempts", new String[]{String.valueOf(remainingAttempts)}));
        } else {
            String code = args[1];
            if (!this.plugin.getDatabaseManager().isUserRegistered(player.getName())) {
                player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.code-not-registered"));
            } else if (!this.plugin.getDatabaseManager().is2FAEnabled(player.getName())) {
                player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.code-not-enabled"));
            } else if (!this.plugin.getDatabaseManager().is2FAPending(player.getName())) {
                player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.code-not-pending"));
            } else if (this.plugin.getTwoFAManager().verify2FACode(player, code)) {
                player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.code-success"));
                this.plugin.getSessionManager().authenticatePlayer(player);
                this.plugin.getDatabaseManager().updateLoginInfo(player.getName(), player.getAddress().getAddress().getHostAddress());
                String ip = player.getAddress().getAddress().getHostAddress();
                this.plugin.getTwoFAManager().reset2FAAttempts(ip);
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                this.plugin.getTwoFAManager().sendLoginNotification(player);
            } else {
                String ip = player.getAddress().getAddress().getHostAddress();
                int remainingAttempts = this.plugin.getTwoFAManager().getRemaining2FAAttempts(ip);
                player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.code-invalid"));
                player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.code-attempts", new String[]{String.valueOf(remainingAttempts)}));
            }
        }

    }

    private void handleStatus(Player player) {
        boolean enabled = this.plugin.getTwoFAManager().is2FAEnabled(player);
        if (enabled) {
            player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.status-enabled"));
        } else {
            player.sendMessage(this.plugin.getMessageManager().getMessage("2fa.status-disabled"));
        }

    }
}