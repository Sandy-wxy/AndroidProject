package com.example.focus_flow.core.model;

public enum ColorTag {
    CYAN, PURPLE, BLUE, GREEN, ORANGE, PINK;

    public static ColorTag fromString(String value) {
        try {
            return value == null ? CYAN : ColorTag.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return CYAN;
        }
    }
}
