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
    private static final String REPEAT_PREFIX = "repeat_";
    private static final String RINGTONE_PREFIX = "ringtone_";
    private static final String DURATION_PREFIX = "duration_";
    private static final String SNOOZE_MINUTES_PREFIX = "snooze_minutes_";
    private static final String SNOOZE_COUNT_PREFIX = "snooze_count_";
    private static final String SNOOZE_USED_PREFIX = "snooze_used_";

    private TaskReminderScheduler() {
    }

    public static void schedule(Context context, long taskId, String title, long triggerAtMillis) {
        TaskAlarmConfig config = new TaskAlarmConfig();
        config.triggerAtMillis = triggerAtMillis;
        schedule(context, taskId, title, config);
    }

    public static void schedule(Context context, long taskId, String title, TaskAlarmConfig config) {
        Context appContext = context.getApplicationContext();
        cancelAlarm(appContext, taskId);
        saveConfig(appContext, taskId, title, config, 0);
        setSystemAlarm(appContext, taskId, title, config.triggerAtMillis);
    }

    public static boolean snooze(Context context, long taskId, String title) {
        TaskAlarmConfig config = getConfig(context, taskId);
        if (config == null || config.snoozeCount <= 0) {
            return false;
        }
        int used = preferences(context).getInt(SNOOZE_USED_PREFIX + taskId, 0);
        if (used >= config.snoozeCount) {
            return false;
        }
        config.triggerAtMillis = System.currentTimeMillis() + config.snoozeMinutes * 60_000L;
        saveConfig(context, taskId, title, config, used + 1);
        setSystemAlarm(context.getApplicationContext(), taskId, title, config.triggerAtMillis);
        return true;
    }

    public static void onAlarmTriggered(Context context, long taskId, String title) {
        TaskAlarmConfig current = getConfig(context, taskId);
        if (current == null) {
            return;
        }
        long next = current.nextRepeatAfter(System.currentTimeMillis());
        if (next <= 0) {
            cancelAlarm(context.getApplicationContext(), taskId);
            return;
        }
        current.triggerAtMillis = next;
        saveConfig(context, taskId, title, current, 0);
        setSystemAlarm(context.getApplicationContext(), taskId, title, next);
    }

    public static void cancel(Context context, long taskId) {
        Context appContext = context.getApplicationContext();
        cancelAlarm(appContext, taskId);
        SharedPreferences.Editor editor = preferences(appContext).edit();
        for (String prefix : prefixes()) {
            editor.remove(prefix + taskId);
        }
        editor.commit();
    }

    public static void cancelAll(Context context) {
        SharedPreferences preferences = preferences(context);
        for (String key : preferences.getAll().keySet()) {
            if (!key.startsWith(TIME_PREFIX)) continue;
            try {
                cancelAlarm(context.getApplicationContext(),
                        Long.parseLong(key.substring(TIME_PREFIX.length())));
            } catch (NumberFormatException ignored) {
            }
        }
        preferences.edit().clear().commit();
    }

    public static Long getReminderAt(Context context, long taskId) {
        TaskAlarmConfig config = getConfig(context, taskId);
        return config == null ? null : config.triggerAtMillis;
    }

    public static TaskAlarmConfig getConfig(Context context, long taskId) {
        SharedPreferences preferences = preferences(context);
        if (!preferences.contains(TIME_PREFIX + taskId)) {
            return null;
        }
        TaskAlarmConfig config = new TaskAlarmConfig();
        config.triggerAtMillis = preferences.getLong(TIME_PREFIX + taskId, 0L);
        config.repeatMode = preferences.getString(REPEAT_PREFIX + taskId, TaskAlarmConfig.REPEAT_ONCE);
        config.ringtone = preferences.getString(RINGTONE_PREFIX + taskId, "ALARM");
        config.ringDurationMinutes = preferences.getInt(DURATION_PREFIX + taskId, 5);
        config.snoozeMinutes = preferences.getInt(SNOOZE_MINUTES_PREFIX + taskId, 5);
        config.snoozeCount = preferences.getInt(SNOOZE_COUNT_PREFIX + taskId, 3);
        return config;
    }

    public static String getTitle(Context context, long taskId) {
        return preferences(context).getString(TITLE_PREFIX + taskId, "学习任务");
    }

    public static void restoreAll(Context context) {
        SharedPreferences preferences = preferences(context);
        long now = System.currentTimeMillis();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (!entry.getKey().startsWith(TIME_PREFIX) || !(entry.getValue() instanceof Long)) continue;
            long taskId;
            try {
                taskId = Long.parseLong(entry.getKey().substring(TIME_PREFIX.length()));
            } catch (NumberFormatException ignored) {
                continue;
            }
            TaskAlarmConfig config = getConfig(context, taskId);
            if (config == null) continue;
            if (config.triggerAtMillis <= now) {
                long next = config.nextRepeatAfter(now);
                if (next <= 0) {
                    cancel(context, taskId);
                    continue;
                }
                config.triggerAtMillis = next;
                saveConfig(context, taskId, getTitle(context, taskId), config, 0);
            }
            setSystemAlarm(context.getApplicationContext(), taskId,
                    getTitle(context, taskId), config.triggerAtMillis);
        }
    }

    private static void saveConfig(Context context, long taskId, String title,
                                   TaskAlarmConfig config, int snoozeUsed) {
        preferences(context).edit()
                .putLong(TIME_PREFIX + taskId, config.triggerAtMillis)
                .putString(TITLE_PREFIX + taskId, title == null ? "学习任务" : title)
                .putString(REPEAT_PREFIX + taskId, config.repeatMode)
                .putString(RINGTONE_PREFIX + taskId, config.ringtone)
                .putInt(DURATION_PREFIX + taskId, config.ringDurationMinutes)
                .putInt(SNOOZE_MINUTES_PREFIX + taskId, config.snoozeMinutes)
                .putInt(SNOOZE_COUNT_PREFIX + taskId, config.snoozeCount)
                .putInt(SNOOZE_USED_PREFIX + taskId, snoozeUsed)
                .commit();
    }

    private static void setSystemAlarm(Context context, long taskId, String title, long triggerAtMillis) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        PendingIntent operation = reminderIntent(context, taskId, title);
        PendingIntent showIntent = PendingIntent.getActivity(
                context, requestCode(taskId) + 1,
                new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), operation);
    }

    private static void cancelAlarm(Context context, long taskId) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(reminderIntent(context, taskId, null));
    }

    private static PendingIntent reminderIntent(Context context, long taskId, String title) {
        Intent intent = new Intent(context, TaskReminderReceiver.class)
                .setAction(TaskReminderReceiver.ACTION_RING)
                .putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId);
        if (title != null) intent.putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, title);
        return PendingIntent.getBroadcast(context, requestCode(taskId), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int requestCode(long taskId) {
        return (int) (taskId ^ (taskId >>> 32));
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String[] prefixes() {
        return new String[]{TIME_PREFIX, TITLE_PREFIX, REPEAT_PREFIX, RINGTONE_PREFIX,
                DURATION_PREFIX, SNOOZE_MINUTES_PREFIX, SNOOZE_COUNT_PREFIX, SNOOZE_USED_PREFIX};
    }
}
