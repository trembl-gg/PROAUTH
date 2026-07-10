package com.proauth.proauth.utils;

import com.proauth.proauth.ProAuth;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.BanList.Type;
import org.bukkit.entity.Player;

public class TwoFAManager {
    private final ProAuth plugin;
    private final SecureRandom random;
    private final Map<UUID, String> pending2FACodes;
    private final Map<String, String> pendingLinks;
    private final Map<String, Boolean> accountLocks;
    private final Map<String, Integer> twoFAAttempts;
    private final Map<String, Long> ipBans;
    private TelegramBot telegramBot;
    private TelegramUpdateListener updateListener;

    public TwoFAManager(ProAuth plugin) {
        this.plugin = plugin;
        this.random = new SecureRandom();
        this.pending2FACodes = new HashMap();
        this.pendingLinks = new HashMap();
        this.accountLocks = new HashMap();
        this.twoFAAttempts = new HashMap();
        this.ipBans = new HashMap();
        plugin.getServer().getScheduler().runTaskLater(plugin, this::delayedTelegramInit, 20L);
    }

    private void delayedTelegramInit() {
        try {
            this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("twofa.bot-initializing"));
            this.initializeTelegramBot();
        } catch (Exception e) {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("twofa.bot-init-error", new String[]{e.getMessage()}));
        }

    }

    private void initializeTelegramBot() {
        if (this.plugin.getConfigManager() == null) {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("twofa.config-manager-not-loaded"));
        } else if (!this.plugin.getConfigManager().isTelegram2FAEnabled()) {
            this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("twofa.disabled-in-config"));
        } else {
            String token = this.plugin.getConfigManager().getTelegramToken();
            if (!token.equals("token") && !token.isEmpty()) {
                try {
                    this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("twofa.bot-loading"));
                    this.telegramBot = new TelegramBot(this.plugin);

                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException var3) {
                        Thread.currentThread().interrupt();
                    }

                    if (this.telegramBot.testConnection()) {
                        this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("twofa.init-success"));
                        this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("twofa.bot-connected", new String[]{this.telegramBot.getBotUsername()}));
                        this.updateListener = new TelegramUpdateListener(this.plugin);
                    } else {
                        this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("twofa.bot-connection-failed"));
                        this.telegramBot = null;
                    }
                } catch (Exception e) {
                    this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("twofa.init-error", new String[]{e.getMessage()}));
                    this.telegramBot = null;
                }
            } else {
                this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("twofa.token-not-configured"));
                this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("twofa.token-configure-path"));
            }
        }

    }

    public boolean verify2FACode(Player player, String inputCode) {
        String ip = player.getAddress().getAddress().getHostAddress();
        if (this.isIPBanned(ip)) {
            String var100011 = String.valueOf(ChatColor.RED);
            player.sendMessage(var100011 + this.plugin.getMessageManager().getMessage("twofa.security-warning-too-many"));
            String var8 = String.valueOf(ChatColor.YELLOW);
            player.sendMessage(var8 + this.plugin.getMessageManager().getMessage("twofa.ip-unblock-time", new String[]{String.valueOf(this.getRemainingBanTime(ip))}));
            return false;
        } else {
            String storedCode = (String)this.pending2FACodes.get(player.getUniqueId());
            if (storedCode == null) {
                storedCode = this.plugin.getDatabaseManager().get2FACode(player.getName());
            }

            if (storedCode != null && storedCode.equals(inputCode)) {
                this.twoFAAttempts.remove(ip);
                this.pending2FACodes.remove(player.getUniqueId());
                this.plugin.getDatabaseManager().set2FAPending(player.getName(), false, (String)null);
                return true;
            } else {
                int attempts = (Integer)this.twoFAAttempts.getOrDefault(ip, 0) + 1;
                this.twoFAAttempts.put(ip, attempts);
                int maxAttempts = this.plugin.getConfigManager().get2FAMaxAttempts();
                int remaining = maxAttempts - attempts;
                String var10001 = String.valueOf(ChatColor.RED);
                player.sendMessage(var10001 + this.plugin.getMessageManager().getMessage("twofa.invalid-code", new String[]{String.valueOf(remaining)}));
                if (attempts >= maxAttempts) {
                    this.handleMax2FAAttempts(player, ip);
                }

                return false;
            }
        }
    }

    private void handleMax2FAAttempts(Player player, String ip) {
        int banTime = this.plugin.getConfigManager().get2FABanTime();
        if (this.plugin.getConfigManager().is2FAIPBanEnabled()) {
            BanList banList = Bukkit.getBanList(Type.IP);
            banList.addBan(ip, this.plugin.getMessageManager().getMessage("twofa.ip-ban-reason"), new Date(System.currentTimeMillis() + (long)banTime * 1000L), "ProAuth 2FA System");
            this.ipBans.put(ip, System.currentTimeMillis() + (long)banTime * 1000L);
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("twofa.ip-ban-log", new String[]{ip, player.getName()}));
        }

        String var10001 = String.valueOf(ChatColor.RED);
        player.kickPlayer(var10001 + this.plugin.getMessageManager().getMessage("twofa.max-attempts-reached") + String.valueOf(ChatColor.YELLOW) + this.plugin.getMessageManager().getMessage("twofa.ip-blocked", new String[]{String.valueOf(banTime / 60)}) + String.valueOf(ChatColor.GRAY) + this.plugin.getMessageManager().getMessage("twofa.ip-blocked-reason"));
        this.twoFAAttempts.remove(ip);
        String chatId = this.plugin.getDatabaseManager().getTelegramChatId(player.getName());
        if (chatId != null && this.telegramBot != null) {
            String message = this.plugin.getMessageManager().getMessage("twofa.login-notification-security", new String[]{player.getName(), ip, String.valueOf(banTime / 60)});
            this.telegramBot.sendMessage(chatId, message);
        }

    }

    private boolean isIPBanned(String ip) {
        if (this.ipBans.containsKey(ip)) {
            long unbanTime = (Long)this.ipBans.get(ip);
            if (System.currentTimeMillis() > unbanTime) {
                this.ipBans.remove(ip);
                BanList banList = Bukkit.getBanList(Type.IP);
                if (banList.isBanned(ip)) {
                    banList.pardon(ip);
                }

                return false;
            } else {
                return true;
            }
        } else {
            BanList banList = Bukkit.getBanList(Type.IP);
            return banList.isBanned(ip);
        }
    }

    private int getRemainingBanTime(String ip) {
        if (this.ipBans.containsKey(ip)) {
            long remaining = (Long)this.ipBans.get(ip) - System.currentTimeMillis();
            return (int)Math.max(1L, remaining / 60000L);
        } else {
            return 0;
        }
    }

    public void reset2FAAttempts(String ip) {
        this.twoFAAttempts.remove(ip);
    }

    public int getRemaining2FAAttempts(String ip) {
        int attempts = (Integer)this.twoFAAttempts.getOrDefault(ip, 0);
        return this.plugin.getConfigManager().get2FAMaxAttempts() - attempts;
    }

    public String generate2FACode() {
        int code = 100000 + this.random.nextInt(900000);
        return String.valueOf(code);
    }

    public boolean send2FACode(Player player) {
        if (!this.plugin.getDatabaseManager().is2FAEnabled(player.getName())) {
            return false;
        } else {
            String chatId = this.plugin.getDatabaseManager().getTelegramChatId(player.getName());
            if (chatId == null) {
                String var6 = String.valueOf(ChatColor.RED);
                player.sendMessage(var6 + this.plugin.getMessageManager().getMessage("twofa.telegram-chat-id-not-found"));
                return false;
            } else if (this.isAccountLocked(player.getName())) {
                String var10001 = String.valueOf(ChatColor.RED);
                player.sendMessage(var10001 + this.plugin.getMessageManager().getMessage("twofa.account-temporarily-locked"));
                return false;
            } else {
                String code = this.generate2FACode();
                this.pending2FACodes.put(player.getUniqueId(), code);
                this.plugin.getDatabaseManager().set2FAPending(player.getName(), true, code);
                if (this.telegramBot != null) {
                    String var10000 = player.getName();
                    String message = this.plugin.getMessageManager().getMessage("twofa.init-login-code", new String[]{var10000, code, player.getAddress().getAddress().getHostAddress(), String.valueOf(this.plugin.getConfigManager().get2FAMaxAttempts()), code});
                    return this.telegramBot.sendMessage(chatId, message);
                } else {
                    return false;
                }
            }
        }
    }

    public void clearPendingCode(Player player) {
        this.pending2FACodes.remove(player.getUniqueId());
        this.plugin.getDatabaseManager().set2FAPending(player.getName(), false, (String)null);
    }

    public boolean is2FAEnabled(Player player) {
        return this.plugin.getDatabaseManager().is2FAEnabled(player.getName());
    }

    public boolean isTelegramBotAvailable() {
        return this.telegramBot != null;
    }

    public TelegramBot getTelegramBot() {
        return this.telegramBot;
    }

    public void sendLoginNotification(Player player) {
        if (this.plugin.getDatabaseManager().is2FAEnabled(player.getName())) {
            String chatId = this.plugin.getDatabaseManager().getTelegramChatId(player.getName());
            if (chatId != null && this.telegramBot != null) {
                String ip = "Неизвестно";
                if (player.getAddress() != null) {
                    ip = player.getAddress().getAddress().getHostAddress();
                }

                this.telegramBot.sendLoginNotification(chatId, player.getName(), ip);
            }
        }

    }

    public boolean remove2FA(String username) {
        try {
            String chatId = this.plugin.getDatabaseManager().getTelegramChatId(username);
            this.plugin.getDatabaseManager().set2FAEnabled(username, false);
            this.plugin.getDatabaseManager().setTelegramChatId(username, (String)null);
            this.plugin.getDatabaseManager().set2FAPending(username, false, (String)null);
            this.accountLocks.remove(username);
            this.pending2FACodes.entrySet().removeIf((entry) -> {
                Player player = Bukkit.getPlayer((UUID)entry.getKey());
                return player != null && player.getName().equalsIgnoreCase(username);
            });
            if (chatId != null && this.telegramBot != null) {
                this.telegramBot.sendMessage(chatId, this.plugin.getMessageManager().getMessage("twofa.telegram-2fa-disabled-admin", new String[]{username}));
            }

            Player player = Bukkit.getPlayerExact(username);
            if (player != null && player.isOnline()) {
                this.plugin.getSessionManager().logoutPlayer(player);
                String var10001 = String.valueOf(ChatColor.YELLOW);
                player.sendMessage(var10001 + this.plugin.getMessageManager().getMessage("twofa.player-2fa-disabled-notice"));
            }

            this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("twofa.2fa-disabled-log", new String[]{username}));
            return true;
        } catch (Exception e) {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("twofa.2fa-disable-error", new String[]{username, e.getMessage()}));
            return false;
        }
    }

    public boolean blockAccount(String username) {
        if (this.isAccountLocked(username)) {
            this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("twofa.account-already-locked", new String[]{username}));
            return true;
        } else {
            this.accountLocks.put(username, true);
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("twofa.account-locked-warning", new String[]{username}));
            Player player = Bukkit.getPlayerExact(username);
            if (player != null && player.isOnline()) {
                this.plugin.getSessionManager().logoutPlayer(player);
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    String var10001 = String.valueOf(ChatColor.RED);
                    player.kickPlayer(var10001 + this.plugin.getMessageManager().getMessage("twofa.account-locked-kick"));
                });
            } else {
                this.plugin.getSessionManager().logoutPlayerByName(username);
            }

            String chatId = this.plugin.getDatabaseManager().getTelegramChatId(username);
            if (chatId != null && this.telegramBot != null) {
                String message = this.plugin.getMessageManager().getMessage("twofa.account-blocked-telegram", new String[]{username});
                this.telegramBot.sendMessage(chatId, message);
            }

            return true;
        }
    }

    public boolean unblockAccount(String username) {
        if (!this.isAccountLocked(username)) {
            this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("twofa.account-not-unlocked", new String[]{username}));
            return true;
        } else {
            this.accountLocks.remove(username);
            this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("twofa.account-unlocked", new String[]{username}));
            String chatId = this.plugin.getDatabaseManager().getTelegramChatId(username);
            if (chatId != null && this.telegramBot != null) {
                this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("twofa.unblock-notification-sent", new String[]{username}));
            }

            return true;
        }
    }

    public boolean isAccountLocked(String username) {
        return this.accountLocks.containsKey(username) && (Boolean)this.accountLocks.get(username);
    }

    private boolean isChatIdAlreadyLinked(String chatId, String currentUsername) {
        String existingUser = this.plugin.getDatabaseManager().getUsernameByChatId(chatId);
        return existingUser != null && !existingUser.equals(currentUsername);
    }

    public boolean enable2FA(Player player, String telegramChatId) {
        try {
            if (!this.plugin.getConfigManager().isTelegram2FAEnabled()) {
                String var21 = String.valueOf(ChatColor.RED);
                player.sendMessage(var21 + this.plugin.getMessageManager().getMessage("twofa.config-2fa-disabled"));
                return false;
            } else if (this.telegramBot == null) {
                String var20 = String.valueOf(ChatColor.RED);
                player.sendMessage(var20 + this.plugin.getMessageManager().getMessage("twofa.bot-unavailable"));
                return false;
            } else if (this.isChatIdAlreadyLinked(telegramChatId, player.getName())) {
                String var19 = String.valueOf(ChatColor.RED);
                player.sendMessage(var19 + this.plugin.getMessageManager().getMessage("twofa.chat-id-already-linked"));
                return false;
            } else if (!this.pendingLinks.containsKey(telegramChatId)) {
                String botUsername = this.telegramBot.getBotUsername();
                String var10 = String.valueOf(ChatColor.YELLOW);
                player.sendMessage(var10 + "⚠️ " + String.valueOf(ChatColor.BOLD) + this.plugin.getMessageManager().getMessage("twofa.pending-link-exists"));
                String var16 = String.valueOf(ChatColor.YELLOW);
                player.sendMessage(var16 + this.plugin.getMessageManager().getMessage("twofa.confirm-command"));
                var10 = String.valueOf(ChatColor.GREEN);
                player.sendMessage(var10 + "/2fa enable " + telegramChatId);
                var10 = String.valueOf(ChatColor.RED);
                player.sendMessage(var10 + "❗ " + String.valueOf(ChatColor.BOLD) + this.plugin.getMessageManager().getMessage("twofa.important-never-share"));
                var16 = String.valueOf(ChatColor.RED);
                player.sendMessage(var16 + this.plugin.getMessageManager().getMessage("twofa.never-share-command"));
                var16 = String.valueOf(ChatColor.RED);
                player.sendMessage(var16 + this.plugin.getMessageManager().getMessage("twofa.legitimate-requires-confirmation"));
                String telegramMessage = this.plugin.getMessageManager().getMessage("twofa.pending-link-telegram", new String[]{player.getName(), botUsername, telegramChatId});
                this.telegramBot.sendMessage(telegramChatId, telegramMessage);
                this.pendingLinks.put(telegramChatId, player.getName());
                return false;
            } else if (((String)this.pendingLinks.get(telegramChatId)).equals(player.getName())) {
                this.plugin.getDatabaseManager().setTelegramChatId(player.getName(), telegramChatId);
                this.plugin.getDatabaseManager().set2FAEnabled(player.getName(), true);
                this.pendingLinks.remove(telegramChatId);
                String botUsername = this.telegramBot.getBotUsername();
                String var10000 = player.getName();
                String successMessage = this.plugin.getMessageManager().getMessage("twofa.account-linked-success", new String[]{var10000, botUsername});
                this.telegramBot.sendMessage(telegramChatId, successMessage);
                String var10001 = String.valueOf(ChatColor.GREEN);
                player.sendMessage(var10001 + this.plugin.getMessageManager().getMessage("twofa.2fa-enable-success", new String[]{botUsername}));
                Logger var8 = this.plugin.getLogger();
                var10001 = player.getName();
                var8.info(this.plugin.getMessageManager().getMessage("twofa.2fa-enable-log", new String[]{var10001, telegramChatId}));
                return true;
            } else {
                String var15 = String.valueOf(ChatColor.RED);
                player.sendMessage(var15 + this.plugin.getMessageManager().getMessage("twofa.linked-to-another"));
                return false;
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("twofa.2fa-enable-error", new String[]{e.getMessage()}));
            String var100011 = String.valueOf(ChatColor.RED);
            player.sendMessage(var100011 + this.plugin.getMessageManager().getMessage("twofa.2fa-enable-error-message"));
            return false;
        }
    }

    public boolean disable2FA(Player player) {
        try {
            String chatId = this.plugin.getDatabaseManager().getTelegramChatId(player.getName());
            this.plugin.getDatabaseManager().set2FAEnabled(player.getName(), false);
            this.plugin.getDatabaseManager().setTelegramChatId(player.getName(), (String)null);
            if (chatId != null && this.telegramBot != null) {
                this.telegramBot.sendMessage(chatId, this.plugin.getMessageManager().getMessage("twofa.disable-2fa-telegram", new String[]{player.getName()}));
            }

            String var4 = String.valueOf(ChatColor.GREEN);
            player.sendMessage(var4 + this.plugin.getMessageManager().getMessage("twofa.2fa-disabled"));
            return true;
        } catch (Exception e) {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("twofa.2fa-disable-error", new String[]{player.getName(), e.getMessage()}));
            String var10001 = String.valueOf(ChatColor.RED);
            player.sendMessage(var10001 + this.plugin.getMessageManager().getMessage("twofa.2fa-disable-error-message"));
            return false;
        }
    }

    public void shutdown() {
        if (this.updateListener != null) {
            this.updateListener.shutdown();
        }

        this.pending2FACodes.clear();
        this.pendingLinks.clear();
        this.twoFAAttempts.clear();
        this.ipBans.clear();
    }
}