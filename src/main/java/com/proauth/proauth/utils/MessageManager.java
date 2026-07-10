package com.proauth.proauth.utils;

import com.proauth.proauth.ProAuth;

public class MessageManager {

    private final ProAuth plugin;
    private final LocalizationManager localizationManager;

    public MessageManager(ProAuth plugin) {
        this.plugin = plugin;
        this.localizationManager = plugin.getLocalizationManager();
    }

    public void reloadLanguage() {
        this.localizationManager.reloadLanguage();
    }

    public String getMessage(String key) {
        return this.localizationManager.getMessage(key);
    }

    public String getMessage(String key, String... args) {
        return this.localizationManager.getMessage(key, args);
    }
}