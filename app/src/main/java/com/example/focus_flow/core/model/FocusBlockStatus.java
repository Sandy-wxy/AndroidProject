package com.example.focus_flow.core.model;

public enum FocusBlockStatus {
    PENDING, RUNNING, COMPLETED, SKIPPED, CANCELLED;

    public static FocusBlockStatus fromString(String value) {
        try {
            return value == null ? PENDING : FocusBlockStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return PENDING;
        }
    }
}
