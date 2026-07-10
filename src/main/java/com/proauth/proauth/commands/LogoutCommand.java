package com.proauth.proauth.commands;

import com.proauth.proauth.ProAuth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LogoutCommand implements CommandExecutor {
    private final ProAuth plugin;

    public LogoutCommand(ProAuth plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!this.plugin.getSessionManager().isAuthenticated(player)) {
                player.sendMessage(this.plugin.getLocalizationManager().getMessage("logout.not-authenticated"));
                return true;
            } else {
                this.plugin.getSessionManager().logoutPlayer(player);
                player.sendMessage(this.plugin.getLocalizationManager().getMessage("logout.success"));
                return true;
            }
        } else {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("login.only-players"));
            return true;
        }
    }
}
