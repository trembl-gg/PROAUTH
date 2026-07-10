package com.proauth.proauth.commands;

import com.proauth.proauth.ProAuth;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishLoginCommand implements CommandExecutor {
    private final ProAuth plugin;
    private final Set<UUID> vanishPlayers;

    public VanishLoginCommand(ProAuth plugin) {
        this.plugin = plugin;
        this.vanishPlayers = new HashSet();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("proauth.vanish")) {
                sender.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.permission-denied"));
                return true;
            } else {
                if (this.vanishPlayers.contains(player.getUniqueId())) {
                    this.vanishPlayers.remove(player.getUniqueId());
                    if (this.plugin.getDatabaseManager() != null) {
                        this.plugin.getDatabaseManager().setVanishLogin(player.getName(), false);
                    }

                    if (this.plugin.getLuckPermsManager() != null && this.plugin.getLuckPermsManager().isEnabled()) {
                        this.plugin.getLuckPermsManager().setVanishJoinPermission(player, true);
                    }

                    player.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.vanish-disabled"));
                } else {
                    this.vanishPlayers.add(player.getUniqueId());
                    if (this.plugin.getDatabaseManager() != null) {
                        this.plugin.getDatabaseManager().setVanishLogin(player.getName(), true);
                    }

                    if (this.plugin.getLuckPermsManager() != null && this.plugin.getLuckPermsManager().isEnabled()) {
                        this.plugin.getLuckPermsManager().setVanishJoinPermission(player, false);
                    }

                    player.sendMessage(this.plugin.getLocalizationManager().getMessage("proauth.vanish-enabled"));
                }

                return true;
            }
        } else {
            sender.sendMessage(this.plugin.getLocalizationManager().getMessage("general.authentication-required"));
            return true;
        }
    }

    public boolean isVanishEnabled(Player player) {
        return this.vanishPlayers.contains(player.getUniqueId());
    }

    public Set<UUID> getVanishPlayers() {
        return new HashSet(this.vanishPlayers);
    }
}