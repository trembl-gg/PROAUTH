package com.proauth.proauth.utils;

import com.proauth.proauth.ProAuth;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

public class ProAuthCommandLogger {
    private final ProAuth plugin;

    public ProAuthCommandLogger(ProAuth plugin) {
        this.plugin = plugin;
    }

    public void logCommand(Player player, String command, String[] args) {
        String hiddenCommand = this.hideCommandSensitiveData(command, args);
        Logger var10000 = this.plugin.getLogger();
        String var10001 = player.getName();
        var10000.info("[ProAuth] " + var10001 + " issued server command: " + hiddenCommand);
    }

    private String hideCommandSensitiveData(String command, String[] args) {
        String lowerCommand = command.toLowerCase();
        if ((lowerCommand.equals("register") || lowerCommand.equals("reg") || lowerCommand.equals("r")) && args.length >= 2) {
            return "/" + command + " [HIDDEN] [HIDDEN]";
        } else if ((lowerCommand.equals("login") || lowerCommand.equals("l") || lowerCommand.equals("auth")) && args.length >= 1) {
            return "/" + command + " [HIDDEN]";
        } else if ((lowerCommand.equals("changepassword") || lowerCommand.equals("changepass") || lowerCommand.equals("cpw") || lowerCommand.equals("passwd")) && args.length >= 3) {
            return "/" + command + " [HIDDEN] [HIDDEN] [HIDDEN]";
        } else if ((lowerCommand.equals("2fa") || lowerCommand.equals("telegram") || lowerCommand.equals("tg") || lowerCommand.equals("fa")) && args.length >= 1 && args[0].equalsIgnoreCase("code") && args.length >= 2) {
            return "/" + command + " code [HIDDEN]";
        } else {
            StringBuilder sb = new StringBuilder("/" + command);

            for(String arg : args) {
                sb.append(" ").append(arg);
            }

            return sb.toString();
        }
    }
}