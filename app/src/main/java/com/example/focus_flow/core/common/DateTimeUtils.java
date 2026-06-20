package com.example.focus_flow.core.common;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateTimeUtils {
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        }
    };

    private DateTimeUtils() {
    }

    public static String todayDateString() {
        return formatDate(System.currentTimeMillis());
    }

    public static String formatDate(long epochMillis) {
        return DATE_FORMAT.get().format(new Date(epochMillis));
    }

    public static long startOfDayMillis(long epochMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long endOfDayMillis(long epochMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startOfDayMillis(epochMillis));
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTimeInMillis() - 1;
    }

    public static long startOfWeekMillis(long epochMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startOfDayMillis(epochMillis));
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        return calendar.getTimeInMillis();
    }

    public static long startOfMonthMillis(long epochMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startOfDayMillis(epochMillis));
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTimeInMillis();
    }

    public static String formatDurationShort(int totalSeconds) {
        int minutes = Math.max(0, totalSeconds) / 60;
        int seconds = Math.max(0, totalSeconds) % 60;
        if (minutes >= 60) {
            int hours = minutes / 60;
            int restMinutes = minutes % 60;
            return hours + "小时" + restMinutes + "分";
        }
        if (seconds == 0) {
            return minutes + "分钟";
        }
        return minutes + "分" + seconds + "秒";
    }
}
