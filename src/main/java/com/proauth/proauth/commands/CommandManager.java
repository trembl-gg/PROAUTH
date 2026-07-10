package com.proauth.proauth.commands;

import com.proauth.proauth.ProAuth;

public class CommandManager {
    private final ProAuth plugin;
    private VanishLoginCommand vanishLoginCommand;

    public CommandManager(ProAuth plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        ProAuthTabCompleter tabCompleter = new ProAuthTabCompleter(this.plugin);
        this.plugin.getCommand("register").setExecutor(new RegisterCommand(this.plugin));
        this.plugin.getCommand("register").setTabCompleter(tabCompleter);
        this.plugin.getCommand("login").setExecutor(new LoginCommand(this.plugin));
        this.plugin.getCommand("login").setTabCompleter(tabCompleter);
        this.plugin.getCommand("proauth").setExecutor(new ProAuthCommand(this.plugin));
        this.plugin.getCommand("proauth").setTabCompleter(tabCompleter);
        this.plugin.getCommand("logout").setExecutor(new LogoutCommand(this.plugin));
        this.plugin.getCommand("logout").setTabCompleter(tabCompleter);
        this.plugin.getCommand("2fa").setExecutor(new TwoFACommand(this.plugin));
        this.plugin.getCommand("2fa").setTabCompleter(tabCompleter);
        this.plugin.getCommand("changepassword").setExecutor(new ChangePasswordCommand(this.plugin));
        this.plugin.getCommand("changepassword").setTabCompleter(tabCompleter);
        this.plugin.getCommand("vanishlogin").setExecutor(new VanishLoginCommand(this.plugin));
        this.plugin.getCommand("vanishlogin").setTabCompleter(tabCompleter);
        this.plugin.getLogger().info(this.plugin.getLocalizationManager().getMessage("console.command-manager-success"));
    }

    public VanishLoginCommand getVanishLoginCommand() {
        return (VanishLoginCommand)this.plugin.getCommand("vanishlogin").getExecutor();
    }
}
