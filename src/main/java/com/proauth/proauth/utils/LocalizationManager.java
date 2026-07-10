package com.proauth.proauth.utils;

import com.proauth.proauth.ProAuth;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class LocalizationManager {
    private final ProAuth plugin;
    private final Map<String, YamlConfiguration> messages = new HashMap();
    private String currentLanguage;

    public LocalizationManager(ProAuth plugin) {
        this.plugin = plugin;
        this.currentLanguage = plugin.getConfigManager().getLanguage().toLowerCase();
        this.loadMessages();
    }

    private void loadMessages() {
        String[] languages = new String[]{"en", "ru"};

        for(String lang : languages) {
            String resourceName = "messages_" + lang + ".yml";
            YamlConfiguration config = new YamlConfiguration();

            try {
                InputStream inputStream = this.plugin.getResource(resourceName);
                if (inputStream != null) {
                    config.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    this.messages.put(lang, config);
                    this.plugin.getLogger().info("✓ Loaded language: " + lang);
                } else {
                    this.plugin.getLogger().warning("✗ Could not find resource: " + resourceName);
                }
            } catch (Exception e) {
                this.plugin.getLogger().severe("✗ Error loading localization: " + resourceName);
                e.printStackTrace();
            }
        }

    }

    public void reloadLanguage() {
        this.currentLanguage = this.plugin.getConfigManager().getLanguage().toLowerCase();
    }

    public String getMessage(String key) {
        return this.getMessage(key, new String[0]);
    }

    public String getMessage(String key, String... args) {
        String message = this.getMessageRaw(key);
        if (message != null && !message.isEmpty()) {
            for(int i = 0; i < args.length; ++i) {
                message = message.replace("{" + i + "}", args[i]);
            }

            return message;
        } else {
            return key;
        }
    }

    private String getMessageRaw(String key) {
        YamlConfiguration config = (YamlConfiguration)this.messages.get(this.currentLanguage);
        if (config == null) {
            config = (YamlConfiguration)this.messages.get("en");
        }

        if (config != null) {
            String message = config.getString(key);
            if (message != null && !message.isEmpty()) {
                return message;
            }
        }

        return null;
    }

    public boolean hasMessage(String key) {
        YamlConfiguration config = (YamlConfiguration)this.messages.get(this.currentLanguage);
        if (config == null) {
            config = (YamlConfiguration)this.messages.get("en");
        }

        return config != null && config.contains(key);
    }
}