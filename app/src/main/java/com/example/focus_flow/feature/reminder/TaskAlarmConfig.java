package com.example.focus_flow.feature.reminder;

import java.util.Calendar;

public class TaskAlarmConfig {
    public static final String REPEAT_ONCE = "ONCE";
    public static final String REPEAT_DAILY = "DAILY";
    public static final String REPEAT_WEEKDAYS = "WEEKDAYS";
    public static final String REPEAT_WEEKLY = "WEEKLY";

    public long triggerAtMillis;
    public String repeatMode = REPEAT_ONCE;
    public String ringtone = "ALARM";
    public int ringDurationMinutes = 5;
    public int snoozeMinutes = 5;
    public int snoozeCount = 3;

    public long nextRepeatAfter(long afterMillis) {
        if (REPEAT_ONCE.equals(repeatMode)) {
            return -1L;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Math.max(triggerAtMillis, afterMillis));
        if (REPEAT_DAILY.equals(repeatMode)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            return calendar.getTimeInMillis();
        }
        if (REPEAT_WEEKLY.equals(repeatMode)) {
            calendar.add(Calendar.DAY_OF_MONTH, 7);
            return calendar.getTimeInMillis();
        }
        do {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);
        return calendar.getTimeInMillis();
    }
}
