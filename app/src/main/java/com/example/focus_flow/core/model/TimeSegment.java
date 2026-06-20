package com.example.focus_flow.core.model;

import java.util.Calendar;

public enum TimeSegment {
    MORNING, NOON, AFTERNOON, EVENING, NIGHT;

    public static TimeSegment fromMillis(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) {
            return MORNING;
        }
        if (hour >= 11 && hour < 14) {
            return NOON;
        }
        if (hour >= 14 && hour < 18) {
            return AFTERNOON;
        }
        if (hour >= 18 && hour < 22) {
            return EVENING;
        }
        return NIGHT;
    }
}
