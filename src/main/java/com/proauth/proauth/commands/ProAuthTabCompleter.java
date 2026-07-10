package com.proauth.proauth.commands;

import com.proauth.proauth.ProAuth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class ProAuthTabCompleter implements TabCompleter {
    private final ProAuth plugin;

    public ProAuthTabCompleter(ProAuth plugin) {
        this.plugin = plugin;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList();
        } else {
            String commandName = command.getName().toLowerCase();
            if (!commandName.equals("register") && !commandName.equals("reg")) {
                if (!commandName.equals("login") && !commandName.equals("l")) {
                    if (commandName.equals("logout")) {
                        return new ArrayList();
                    } else if (commandName.equals("changepassword")) {
                        if (args.length == 1) {
                            return this.filterByPrefix(Arrays.asList("[старый пароль]"), args[0]);
                        } else if (args.length == 2) {
                            return this.filterByPrefix(Arrays.asList("[новый пароль]"), args[1]);
                        } else {
                            return (List<String>)(args.length == 3 ? this.filterByPrefix(Arrays.asList("[повтор нового пароля]"), args[2]) : new ArrayList());
                        }
                    } else if (commandName.equals("2fa")) {
                        if (args.length == 1) {
                            List<String> completions = Arrays.asList("enable", "disable", "status", "help", "code");
                            return this.filterByPrefix(completions, args[0]);
                        } else {
                            if (args.length == 2) {
                                String sub = args[0].toLowerCase();
                                if (sub.equals("code")) {
                                    return this.filterByPrefix(Arrays.asList("<code>"), args[1]);
                                }

                                if (sub.equals("enable")) {
                                    return this.filterByPrefix(Arrays.asList("<telegram_chat_id>"), args[1]);
                                }
                            }

                            return new ArrayList();
                        }
                    } else if (commandName.equals("vanishlogin")) {
                        return new ArrayList();
                    } else if (commandName.equals("proauth")) {
                        if (args.length == 1) {
                            List<String> completions = Arrays.asList("unregister", "session", "lastlogin", "lastip", "del2fa", "unban", "stats", "reload", "help", "resetpassword", "resetip", "resetall", "version");
                            return this.filterByPrefix(completions, args[0]);
                        } else {
                            if (args.length == 2) {
                                String subcommand = args[0].toLowerCase();
                                if (subcommand.equals("resetpassword") || subcommand.equals("resetip") || subcommand.equals("resetall") || subcommand.equals("unregister") || subcommand.equals("del2fa") || subcommand.equals("unban") || subcommand.equals("lastlogin") || subcommand.equals("lastip")) {
                                    List<String> playerNames = new ArrayList();

                                    for(Player p : this.plugin.getServer().getOnlinePlayers()) {
                                        if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                                            playerNames.add(p.getName());
                                        }
                                    }

                                    return playerNames;
                                }
                            }

                            return new ArrayList();
                        }
                    } else {
                        return new ArrayList();
                    }
                } else {
                    return (List<String>)(args.length == 1 ? this.filterByPrefix(Arrays.asList("[пароль]"), args[0]) : new ArrayList());
                }
            } else {
                return (List<String>)(args.length == 1 ? this.filterByPrefix(Arrays.asList("[пароль]", "[минимум 4 символа]"), args[0]) : new ArrayList());
            }
        }
    }

    private List<String> filterByPrefix(List<String> options, String prefix) {
        if (prefix == null) {
            prefix = "";
        }

        String lower = prefix.toLowerCase();
        List<String> out = new ArrayList();

        for(String s : options) {
            if (s.toLowerCase().startsWith(lower)) {
                out.add(s);
            }
        }

        return out;
    }
}