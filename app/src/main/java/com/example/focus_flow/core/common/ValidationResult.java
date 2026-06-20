package com.example.focus_flow.core.common;

public class ValidationResult {
    public final boolean valid;
    public final String message;

    private ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, "");
    }

    public static ValidationResult error(String message) {
        return new ValidationResult(false, message);
    }
}
