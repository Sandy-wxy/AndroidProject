package com.example.focus_flow.data.repository;

import com.example.focus_flow.core.model.ThemeMode;
import com.example.focus_flow.data.preferences.SettingsPreferences;

public class SettingsRepository {
    private final SettingsPreferences preferences;

    public SettingsRepository(SettingsPreferences preferences) {
        this.preferences = preferences;
    }

    public ThemeMode getThemeMode() {
        return preferences.getThemeMode();
    }

    public void setThemeMode(ThemeMode mode) {
        preferences.setThemeMode(mode);
    }

    public boolean isNotificationEnabled() {
        return preferences.isNotificationEnabled();
    }

    public void setNotificationEnabled(boolean enabled) {
        preferences.setNotificationEnabled(enabled);
    }

    public boolean isAutoStopNoiseEnabled() {
        return preferences.isAutoStopNoiseEnabled();
    }

    public void setAutoStopNoiseEnabled(boolean enabled) {
        preferences.setAutoStopNoiseEnabled(enabled);
    }

    public int getMasterVolume() {
        return preferences.getMasterVolume();
    }

    public void setMasterVolume(int volume) {
        preferences.setMasterVolume(volume);
    }
}
