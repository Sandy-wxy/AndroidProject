package com.example.focus_flow.core.model;

public enum TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, OVERDUE, ARCHIVED, COMPLETED_PENDING_CONFIRM;

    public static TaskStatus fromString(String value) {
        try {
            return value == null ? PENDING : TaskStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return PENDING;
        }
    }
}
