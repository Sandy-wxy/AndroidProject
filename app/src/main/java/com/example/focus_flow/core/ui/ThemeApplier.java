package com.example.focus_flow.core.ui;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.focus_flow.core.model.ThemeMode;

public final class ThemeApplier {
    private ThemeApplier() {
    }

    public static void apply(ThemeMode mode) {
        if (mode == ThemeMode.DARK) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (mode == ThemeMode.LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
