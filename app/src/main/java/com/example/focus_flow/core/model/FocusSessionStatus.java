package com.example.focus_flow.core.model;

public enum FocusSessionStatus {
    RUNNING, COMPLETED, ABANDONED, INTERRUPTED;

    public static FocusSessionStatus fromString(String value) {
        try {
            return value == null ? INTERRUPTED : FocusSessionStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return INTERRUPTED;
        }
    }
}
