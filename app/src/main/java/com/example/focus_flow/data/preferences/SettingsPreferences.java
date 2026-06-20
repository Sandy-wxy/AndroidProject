package com.example.focus_flow.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.focus_flow.core.model.ThemeMode;

public class SettingsPreferences {
    private static final String PREFS = "focus_flow_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_AUTO_STOP_NOISE = "auto_stop_noise";
    private static final String KEY_CURRENT_NOISE_MIX_ID = "current_noise_mix_id";
    private static final String KEY_MASTER_VOLUME = "master_volume";

    private final SharedPreferences preferences;

    public SettingsPreferences(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public ThemeMode getThemeMode() {
        return ThemeMode.fromString(preferences.getString(KEY_THEME_MODE, ThemeMode.FOLLOW_SYSTEM.name()));
    }

    public void setThemeMode(ThemeMode mode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.name()).apply();
    }

    public boolean isNotificationEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    public void setNotificationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply();
    }

    public boolean isAutoStopNoiseEnabled() {
        return preferences.getBoolean(KEY_AUTO_STOP_NOISE, true);
    }

    public void setAutoStopNoiseEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_STOP_NOISE, enabled).apply();
    }

    public long getCurrentNoiseMixId() {
        return preferences.getLong(KEY_CURRENT_NOISE_MIX_ID, -1L);
    }

    public void setCurrentNoiseMixId(long mixId) {
        preferences.edit().putLong(KEY_CURRENT_NOISE_MIX_ID, mixId).apply();
    }

    public int getMasterVolume() {
        return preferences.getInt(KEY_MASTER_VOLUME, 80);
    }

    public void setMasterVolume(int volume) {
        preferences.edit().putInt(KEY_MASTER_VOLUME, Math.max(0, Math.min(100, volume))).apply();
    }
}
