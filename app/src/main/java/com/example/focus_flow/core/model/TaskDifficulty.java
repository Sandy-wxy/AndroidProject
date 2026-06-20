package com.example.focus_flow.core.model;

public enum TaskDifficulty {
    EASY, NORMAL, HARD, EXTREME;

    public static TaskDifficulty fromString(String value) {
        try {
            return value == null ? NORMAL : TaskDifficulty.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return NORMAL;
        }
    }
}
