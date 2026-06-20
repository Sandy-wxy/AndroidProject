package com.example.focus_flow.core.model;

public enum EndReason {
    NONE, TIMER_FINISHED, USER_STOPPED_EARLY, APP_INTERRUPTED;

    public static EndReason fromString(String value) {
        try {
            return value == null ? NONE : EndReason.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }
}
