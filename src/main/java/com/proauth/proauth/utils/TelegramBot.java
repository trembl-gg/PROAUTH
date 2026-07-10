package com.proauth.proauth.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.proauth.proauth.ProAuth;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TelegramBot {
    private final ProAuth plugin;
    private final String botToken;
    private final String apiUrl;
    private String botUsername;

    public TelegramBot(ProAuth plugin) {
        this.plugin = plugin;
        this.botToken = plugin.getConfigManager().getTelegramToken();
        this.apiUrl = "https://api.telegram.org/bot" + this.botToken + "/";
        this.loadBotUsername();
    }

    private void loadBotUsername() {
        if (!this.botToken.equals("token") && !this.botToken.isEmpty()) {
            try {
                URL url = new URL(this.apiUrl + "getMe");
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();

                        String responseLine;
                        while((responseLine = br.readLine()) != null) {
                            response.append(responseLine);
                        }

                        JsonParser parser = new JsonParser();
                        JsonObject json = parser.parse(response.toString()).getAsJsonObject();
                        if (json.get("ok").getAsBoolean()) {
                            JsonObject botInfo = json.getAsJsonObject("result");
                            this.botUsername = botInfo.get("username").getAsString();
                            this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("telegram.bot-username-loaded", new String[]{this.botUsername}));
                        }
                    }
                } else {
                    this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.bot-username-error", new String[]{String.valueOf(responseCode)}));
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.bot-username-exception", new String[]{e.getMessage()}));
            }
        }

    }

    public String getBotUsername() {
        return this.botUsername != null ? "@" + this.botUsername : this.plugin.getMessageManager().getMessage("telegram.bot-username-unknown");
    }

    public boolean sendMessage(String chatId, String message) {
        return this.sendMessage(chatId, message, (String)null);
    }

    public boolean sendMessage(String chatId, String message, String keyboardJson) {
        if (!this.botToken.equals("token") && !this.botToken.isEmpty()) {
            try {
                URL url = new URL(this.apiUrl + "sendMessage");
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                String jsonInputString;
                if (keyboardJson != null) {
                    jsonInputString = String.format("{\"chat_id\": \"%s\", \"text\": \"%s\", \"parse_mode\": \"HTML\", \"reply_markup\": %s}", chatId, this.escapeJsonString(message), keyboardJson);
                } else {
                    jsonInputString = String.format("{\"chat_id\": \"%s\", \"text\": \"%s\", \"parse_mode\": \"HTML\"}", chatId, this.escapeJsonString(message));
                }

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    return true;
                } else {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errorResponse = new StringBuilder();

                        String line;
                        while((line = br.readLine()) != null) {
                            errorResponse.append(line);
                        }

                        this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.message-send-error", new String[]{String.valueOf(responseCode), errorResponse.toString()}));
                    }

                    return false;
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.message-send-exception", new String[]{e.getMessage()}));
                return false;
            }
        } else {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.bot-not-configured"));
            return false;
        }
    }

    private String escapeJsonString(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\b", "\\b").replace("\f", "\\f");
    }

    public boolean sendLoginNotification(String chatId, String username, String ip) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        String message = this.plugin.getMessageManager().getMessage("telegram.login-notification-title") + "\n\n" + this.plugin.getMessageManager().getMessage("telegram.login-notification-player", new String[]{username}) + "\n\ud83c\udf10 IP: <code>" + ip + "</code>\n" + this.plugin.getMessageManager().getMessage("telegram.login-notification-time", new String[]{time}) + "\n\n" + this.plugin.getMessageManager().getMessage("telegram.login-notification-warning");
        String keyboardJson = "{\"inline_keyboard\": [[{\"text\": \"\ud83d\udeab It wasn't me\",\"callback_data\": \"not_me:" + username + "\"}]]}";
        boolean sent = this.sendMessage(chatId, message, keyboardJson);
        if (sent) {
            this.plugin.getLogger().info(this.plugin.getMessageManager().getMessage("telegram.login-notification-sent", new String[]{username}));
        } else {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.login-notification-failed", new String[]{username}));
        }

        return sent;
    }

    public boolean sendBlockedNotification(String chatId, String username) {
        String message = this.plugin.getMessageManager().getMessage("telegram.manageaccount") + username + this.plugin.getMessageManager().getMessage("telegram.manageaccount2");
        return this.sendMessage(chatId, message);
    }

    public boolean sendPasswordChangeRequest(String chatId, String username) {
        String message = this.plugin.getMessageManager().getMessage("telegram.changepass") + username + this.plugin.getMessageManager().getMessage("telegram.changepass2");
        return this.sendMessage(chatId, message);
    }

    public boolean testConnection() {
        try {
            String var10002 = this.apiUrl;
            URL url = new URL(var10002 + "getMe");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int responseCode = conn.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            this.plugin.getLogger().warning(this.plugin.getMessageManager().getMessage("telegram.connection-error", new String[]{e.getMessage()}));
            return false;
        }
    }

    public void shutdown() {
    }
}