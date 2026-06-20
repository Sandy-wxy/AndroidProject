package com.example.focus_flow.feature.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.focus_flow.MainActivity;

import java.util.Map;

public final class TaskReminderScheduler {
    private static final String PREFS = "focus_flow_task_reminders";
    private static final String TIME_PREFIX = "time_";
    private static final String TITLE_PREFIX = "title_";

    private TaskReminderScheduler() {
    }

    public static void schedule(Context context, long taskId, String title, long triggerAtMillis) {
        Context appContext = context.getApplicationContext();
        cancelAlarm(appContext, taskId);
        preferences(appContext).edit()
                .putLong(TIME_PREFIX + taskId, triggerAtMillis)
                .putString(TITLE_PREFIX + taskId, title == null ? "学习任务" : title)
                .commit();

        AlarmManager manager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        PendingIntent operation = reminderIntent(appContext, taskId, title);
        PendingIntent showIntent = PendingIntent.getActivity(
                appContext,
                requestCode(taskId) + 1,
                new Intent(appContext, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), operation);
    }

    public static void snooze(Context context, long taskId, String title) {
        schedule(context, taskId, title, System.currentTimeMillis() + 5L * 60L * 1000L);
    }

    public static void cancel(Context context, long taskId) {
        Context appContext = context.getApplicationContext();
        cancelAlarm(appContext, taskId);
        preferences(appContext).edit()
                .remove(TIME_PREFIX + taskId)
                .remove(TITLE_PREFIX + taskId)
                .commit();
    }

    public static void cancelAll(Context context) {
        SharedPreferences preferences = preferences(context);
        for (String key : preferences.getAll().keySet()) {
            if (!key.startsWith(TIME_PREFIX)) {
                continue;
            }
            try {
                cancelAlarm(context.getApplicationContext(),
                        Long.parseLong(key.substring(TIME_PREFIX.length())));
            } catch (NumberFormatException ignored) {
            }
        }
        preferences.edit().clear().commit();
    }

    public static Long getReminderAt(Context context, long taskId) {
        SharedPreferences preferences = preferences(context);
        String key = TIME_PREFIX + taskId;
        return preferences.contains(key) ? preferences.getLong(key, 0L) : null;
    }

    public static void restoreAll(Context context) {
        SharedPreferences preferences = preferences(context);
        long now = System.currentTimeMillis();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (!entry.getKey().startsWith(TIME_PREFIX) || !(entry.getValue() instanceof Long)) {
                continue;
            }
            long taskId;
            try {
                taskId = Long.parseLong(entry.getKey().substring(TIME_PREFIX.length()));
            } catch (NumberFormatException ignored) {
                continue;
            }
            long triggerAt = (Long) entry.getValue();
            if (triggerAt <= now) {
                cancel(context, taskId);
                continue;
            }
            schedule(context, taskId,
                    preferences.getString(TITLE_PREFIX + taskId, "学习任务"), triggerAt);
        }
    }

    private static void cancelAlarm(Context context, long taskId) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) {
            manager.cancel(reminderIntent(context, taskId, null));
        }
    }

    private static PendingIntent reminderIntent(Context context, long taskId, String title) {
        Intent intent = new Intent(context, TaskReminderReceiver.class)
                .setAction(TaskReminderReceiver.ACTION_RING)
                .putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId);
        if (title != null) {
            intent.putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, title);
        }
        return PendingIntent.getBroadcast(
                context,
                requestCode(taskId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int requestCode(long taskId) {
        return (int) (taskId ^ (taskId >>> 32));
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
