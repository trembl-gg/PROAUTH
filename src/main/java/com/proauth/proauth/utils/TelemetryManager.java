package com.proauth.proauth.utils;

import com.proauth.proauth.ProAuth;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.Bukkit;

public class TelemetryManager {
    private final ProAuth plugin;
    private final AtomicLong registrationCount = new AtomicLong(0L);
    private final AtomicLong authenticationCount = new AtomicLong(0L);
    private boolean enabled = false;
    private String serverId = null;
    private static final String TELEMETRY_SERVER = "http://213.108.170.58:2060";

    public TelemetryManager(ProAuth plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfigManager().isTelemetryEnabled();
        String serverName = Bukkit.getServer().getName();
        if (serverName == null || serverName.isEmpty()) {
            String var10000 = UUID.randomUUID().toString();
            serverName = "ProAuth-" + var10000.substring(0, 8);
        }

        this.serverId = serverName;
        if (this.enabled) {
            this.startTelemetryScheduler();
        }

    }

    public void recordRegistration() {
        if (this.enabled) {
            this.registrationCount.incrementAndGet();
        }
    }

    public void recordAuthentication() {
        if (this.enabled) {
            this.authenticationCount.incrementAndGet();
        }
    }

    private void startTelemetryScheduler() {
        Bukkit.getScheduler().runTaskTimer(this.plugin, this::sendTelemetry, 0L, 72000L);
    }

    private void sendTelemetry() {
        if (this.enabled) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    long registrations = this.registrationCount.getAndSet(0L);
                    long authentications = this.authenticationCount.getAndSet(0L);
                    long registeredUsers = (long)this.plugin.getDatabaseManager().getRegisteredUsersCount();
                    String json = String.format("{\"server_id\":\"%s\",\"registrations\":%d,\"authentications\":%d,\"total_users\":%d}", this.serverId, registrations, authentications, registeredUsers);
                    this.sendToServer(json);
                } catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to send telemetry: " + e.getMessage());
                }

            });
        }
    }

    private void sendToServer(String json) {
        try {
            URL url = new URL("http://213.108.170.58:2060/api/telemetry");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200 && responseCode != 201) {
                this.plugin.getLogger().warning("⚠ Telemetry server returned: " + responseCode);
            } else {
                this.plugin.getLogger().info("✅ Telemetry sent successfully");
            }

            connection.disconnect();
        } catch (Exception e) {
            this.plugin.getLogger().warning("❌ Telemetry error: " + e.getMessage());
        }

    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}