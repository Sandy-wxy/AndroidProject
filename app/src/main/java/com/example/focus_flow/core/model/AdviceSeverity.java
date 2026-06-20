package com.example.focus_flow.core.model;

public enum AdviceSeverity {
    INFO, WARNING, POSITIVE;

    public static AdviceSeverity fromString(String value) {
        try {
            return value == null ? INFO : AdviceSeverity.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return INFO;
        }
    }
}
