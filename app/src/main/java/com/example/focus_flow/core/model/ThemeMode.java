package com.example.focus_flow.core.model;

public enum ThemeMode {
    FOLLOW_SYSTEM, LIGHT, DARK;

    public static ThemeMode fromString(String value) {
        try {
            return value == null ? FOLLOW_SYSTEM : ThemeMode.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return FOLLOW_SYSTEM;
        }
    }
}
