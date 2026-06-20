package com.example.focus_flow.core.model;

public enum RecommendationConfidence {
    LOW, MEDIUM, HIGH;

    public static RecommendationConfidence fromString(String value) {
        try {
            return value == null ? LOW : RecommendationConfidence.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return LOW;
        }
    }
}
