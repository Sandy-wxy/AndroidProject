package com.example.focus_flow.core.model;

public enum TaskPriority {
    LOW, NORMAL, HIGH, URGENT;

    public static TaskPriority fromString(String value) {
        try {
            return value == null ? NORMAL : TaskPriority.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return NORMAL;
        }
    }

    public int rank() {
        switch (this) {
            case URGENT:
                return 4;
            case HIGH:
                return 3;
            case NORMAL:
                return 2;
            case LOW:
            default:
                return 1;
        }
    }
}
