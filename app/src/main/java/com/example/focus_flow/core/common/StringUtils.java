package com.example.focus_flow.core.common;

public final class StringUtils {
    private StringUtils() {
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isBlank(String value) {
        return trim(value).length() == 0;
    }

    public static String joinCsv(Iterable<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
