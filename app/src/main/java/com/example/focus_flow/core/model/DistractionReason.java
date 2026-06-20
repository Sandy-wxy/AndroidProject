package com.example.focus_flow.core.model;

public enum DistractionReason {
    NONE,
    PUSH_MESSAGE,
    FATIGUE_SLEEPY,
    TASK_TOO_HARD,
    TASK_UNCLEAR,
    ENVIRONMENT_NOISE,
    DEVICE_DISTRACTION,
    PHYSICAL_NEED,
    EMOTIONAL_STRESS,
    OTHER;

    public static DistractionReason fromString(String value) {
        try {
            return value == null ? NONE : DistractionReason.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }
}
