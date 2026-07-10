package com.proauth.proauth.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.proauth.proauth.ProAuth;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TelegramUpdateListener {
    private final ProAuth plugin;
    private final String botToken;
    private final String apiUrl;
    private ScheduledExecutorService scheduler;
    private int lastUpdateId = 0;
    private boolean isRunning = false;
    private int errorCount = 0;
    private static final int MAX_ERRORS = 5;

    public TelegramUpdateListener(ProAuth plugin) {
        this.plugin = plugin;
        this.botToken = plugin.getConfigManager().getTelegramToken();
        this.apiUrl = "https://api.telegram.org/bot" + this.botToken + "/";
        this.startListener();
    }

    private void startListener() {
        if (this.scheduler != null && !this.scheduler.isShutdown()) {
            this.scheduler.shutdown();
        }

        if (!this.botToken.equals("token") && !this.botToken.isEmpty()) {
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.isRunning = true;
            this.scheduler.scheduleAtFixedRate(() -> {
                if (this.isRunning) {
                    try {
                        this.checkForUpdates();
                    } catch (Exception var2) {
                        ++this.errorCount;
                        if (this.errorCount >= 5) {
                            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.listener-too-many-errors"));
                            this.isRunning = false;
                        }
                    }
                }

            }, 5L, 3L, TimeUnit.SECONDS);
            this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("telegram.listener-started"));
        } else {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.listener-not-configured"));
        }

    }

    private void checkForUpdates() {
        try {
            URL url = new URL(this.apiUrl + "getUpdates?offset=" + (this.lastUpdateId + 1) + "&timeout=10&limit=10");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                this.errorCount = 0;

                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();

                    String responseLine;
                    while((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }

                    JsonParser parser = new JsonParser();
                    JsonObject jsonResponse = parser.parse(response.toString()).getAsJsonObject();
                    if (jsonResponse.get("ok").getAsBoolean()) {
                        for(JsonElement element : jsonResponse.getAsJsonArray("result")) {
                            JsonObject update = element.getAsJsonObject();
                            int updateId = update.get("update_id").getAsInt();
                            this.lastUpdateId = updateId;
                            if (update.has("callback_query")) {
                                JsonObject callbackQuery = update.getAsJsonObject("callback_query");
                                String callbackData = callbackQuery.get("data").getAsString();
                                String callbackId = callbackQuery.get("id").getAsString();
                                JsonObject from = callbackQuery.getAsJsonObject("from");
                                String chatId = String.valueOf(from.get("id").getAsLong());
                                this.handleCallbackQuery(chatId, callbackData, callbackId);
                            }

                            if (update.has("message")) {
                                JsonObject message = update.getAsJsonObject("message");
                                JsonObject chat = message.get("chat").getAsJsonObject();
                                String chatId = String.valueOf(chat.get("id").getAsLong());
                                if (message.has("text")) {
                                    String text = message.get("text").getAsString();
                                    this.handleTextMessage(chatId, text);
                                }
                            }
                        }
                    }
                }
            } else if (responseCode == 409) {
                this.lastUpdateId = 0;
            }
        } catch (Exception e) {
            if (!e.getMessage().contains("timeout") && !e.getMessage().contains("Timeout")) {
                this.plugin.getLogger().warning("Ошибка проверки обновлений Telegram: " + e.getMessage());
            }
        }

    }

    private void handleTextMessage(String chatId, String text) {
        try {
            switch (text) {
                case "/start" -> this.handleStartCommand(chatId);
                case "/unblock" -> this.handleUnblockCommand(chatId);
                case "/help" -> this.handleHelpCommand(chatId);
                default -> this.handleUnknownCommand(chatId);
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.text-message-error", new String[]{e.getMessage()}));
        }

    }

    private void handleCallbackQuery(String chatId, String callbackData, String callbackId) {
        try {
            String[] parts = callbackData.split(":");
            String action = parts[0];
            String username = parts.length > 1 ? parts[1] : "";
            switch (action) {
                case "not_me":
                    if (!username.isEmpty()) {
                        this.plugin.getTwoFAManager().blockAccount(username);
                        this.sendMessage(chatId, this.plugin.getMessageManager().getMessage("telegram.account-blocked"));
                    }
                    break;
                case "block_account":
                    if (!username.isEmpty()) {
                        this.plugin.getTwoFAManager().blockAccount(username);
                        this.sendMessage(chatId, this.plugin.getMessageManager().getMessage("telegram.account-blocked-success"));
                    }
                    break;
                case "confirm_link":
                    if (!username.isEmpty()) {
                        this.sendMessage(chatId, this.plugin.getMessageManager().getMessage("telegram.confirm-link-info"));
                    }
                    break;
                case "cancel_link":
                    if (!username.isEmpty()) {
                        this.sendMessage(chatId, this.plugin.getMessageManager().getMessage("telegram.cancel-link-info"));
                    }
            }

            this.answerCallbackQuery(callbackId);
        } catch (Exception e) {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.callback-query-error", new String[]{e.getMessage()}));
            this.answerCallbackQuery(callbackId);
        }

    }

    private void handleUnblockCommand(String chatId) {
        try {
            String username = this.plugin.getDatabaseManager().getUsernameByChatId(chatId);
            if (username == null) {
                this.sendMessage(chatId, this.plugin.getMessageManager().getMessage("telegram.account-not-linked"));
                return;
            }

            if (!this.plugin.getTwoFAManager().isAccountLocked(username)) {
                this.sendMessage(chatId, this.plugin.getMessageManager().getMessage("telegram.account-not-blocked", new String[]{username}));
                return;
            }

            if (this.plugin.getTwoFAManager().unblockAccount(username)) {
                this.sendMessage(chatId, this.plugin.getMessageManager().getMessage("telegram.account-unblocked", new String[]{username}));
            } else {
                this.sendMessage(chatId, this.plugin.getMessageManager().getMessage("telegram.account-unblock-failed", new String[]{username}));
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.unblock-command-error", new String[]{e.getMessage()}));
        }

    }

    private void handleStartCommand(String chatId) {
        try {
            String username = this.plugin.getDatabaseManager().getUsernameByChatId(chatId);
            if (username != null) {
                boolean is2FAEnabled = this.plugin.getDatabaseManager().is2FAEnabled(username);
                if (is2FAEnabled) {
                    String var10000 = this.plugin.getTwoFAManager().isAccountLocked(username) ? "\ud83d\udeab" : "\ud83d\udd13";
                    String message = this.plugin.getTwoFAManager().isAccountLocked(username) ? this.plugin.getMessageManager().getMessage("telegram.twofa-enabled-blocked", new String[]{username}) : this.plugin.getMessageManager().getMessage("telegram.twofa-enabled-active", new String[]{username});
                    this.sendMessage(chatId, message);
                } else {
                    String message = this.plugin.getMessageManager().getMessage("telegram.twofa-disabled", new String[]{username});
                    this.sendMessage(chatId, message);
                }
            } else {
                String botUsername = "боту";
                if (this.plugin.getTwoFAManager().getTelegramBot() != null) {
                    botUsername = this.plugin.getTwoFAManager().getTelegramBot().getBotUsername();
                }

                String message = this.plugin.getMessageManager().getMessage("telegram.start-not-linked", new String[]{chatId, botUsername});
                this.sendMessage(chatId, message);
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.start-command-error", new String[]{e.getMessage()}));
        }

    }

    private void handleHelpCommand(String chatId) {
        String message = this.plugin.getMessageManager().getMessage("telegram.help-command");
        this.sendMessage(chatId, message);
    }

    private void handleUnknownCommand(String chatId) {
        String message = this.plugin.getMessageManager().getMessage("telegram.unknown-command");
        this.sendMessage(chatId, message);
    }

    private void sendMessage(String chatId, String message) {
        if (this.plugin.getTwoFAManager().getTelegramBot() != null) {
            this.plugin.getTwoFAManager().getTelegramBot().sendMessage(chatId, message);
        }

    }

    private void answerCallbackQuery(String callbackQueryId) {
        try {
            URL url = new URL(this.apiUrl + "answerCallbackQuery");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            String jsonInputString = "{\"callback_query_id\": \"" + callbackQueryId + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            conn.getResponseCode();
        } catch (Exception var10) {
        }

    }

    public void shutdown() {
        this.isRunning = false;
        if (this.scheduler != null) {
            this.scheduler.shutdown();

            try {
                if (!this.scheduler.awaitTermination(5L, TimeUnit.SECONDS)) {
                    this.scheduler.shutdownNow();
                }
            } catch (InterruptedException var2) {
                this.scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("telegram.listener-stopped"));
    }
}