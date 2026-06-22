package com.example.focus_flow.feature.reminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.focus_flow.R;
import com.example.focus_flow.MainActivity;
import com.example.focus_flow.core.navigation.FocusStartStore;

public class TaskReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_RING = "com.example.focus_flow.action.TASK_REMINDER";
    public static final String ACTION_SNOOZE = "com.example.focus_flow.action.TASK_REMINDER_SNOOZE";
    public static final String ACTION_CLOSE = "com.example.focus_flow.action.TASK_REMINDER_CLOSE";
    public static final String ACTION_START = "com.example.focus_flow.action.TASK_REMINDER_START";
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String CHANNEL_ID = "task_alarm_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L);
        if (taskId <= 0) {
            return;
        }
        String title = intent.getStringExtra(EXTRA_TASK_TITLE);
        if (title == null) title = TaskReminderScheduler.getTitle(context, taskId);
        if (ACTION_START.equals(intent.getAction())) {
            cancelNotification(context, taskId);
            new FocusStartStore(context).requestStart(taskId);
            Intent main = new Intent(context, MainActivity.class)
                    .putExtra(MainActivity.EXTRA_OPEN_FOCUS, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(main);
            return;
        }
        if (ACTION_SNOOZE.equals(intent.getAction())) {
            TaskReminderScheduler.snooze(context, taskId, title);
            cancelNotification(context, taskId);
            return;
        }
        if (ACTION_CLOSE.equals(intent.getAction())) {
            TaskReminderScheduler.cancel(context, taskId);
            cancelNotification(context, taskId);
            return;
        }
        if (title == null || title.trim().isEmpty()) {
            title = "学习任务";
        }
        TaskReminderScheduler.onAlarmTriggered(context, taskId, title);
        createChannel(context);

        Intent alarmIntent = new Intent(context, TaskAlarmActivity.class)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_TASK_TITLE, title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent fullScreen = PendingIntent.getActivity(
                context,
                (int) taskId,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent start = actionIntent(context, taskId, title, ACTION_START, 1);
        PendingIntent snooze = actionIntent(context, taskId, title, ACTION_SNOOZE, 2);
        PendingIntent close = actionIntent(context, taskId, title, ACTION_CLOSE, 3);
        NotificationCompat.Action startAction = new NotificationCompat.Action(
                R.drawable.ic_nav_focus, "开始", start);
        NotificationCompat.Action snoozeAction = new NotificationCompat.Action(
                R.drawable.ic_nav_focus, "稍后", snooze);
        NotificationCompat.Action closeAction = new NotificationCompat.Action(
                R.drawable.ic_nav_focus, "关闭", close);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_focus)
                .setContentTitle("任务提醒")
                .setContentText(title + " · 是否进入任务？")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(fullScreen)
                .setFullScreenIntent(fullScreen, true)
                .addAction(startAction)
                .addAction(snoozeAction)
                .addAction(closeAction)
                .extend(new NotificationCompat.WearableExtender()
                        .addAction(startAction)
                        .addAction(snoozeAction)
                        .addAction(closeAction))
                .build();
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId(taskId), notification);
        }
        try {
            context.startActivity(alarmIntent);
        } catch (RuntimeException ignored) {
            // Full-screen notification remains as the system-approved fallback.
        }
    }

    private static PendingIntent actionIntent(Context context, long taskId, String title,
                                              String action, int offset) {
        Intent intent = new Intent(context, TaskReminderReceiver.class)
                .setAction(action)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_TASK_TITLE, title);
        return PendingIntent.getBroadcast(context,
                (int) (taskId % 100000) * 10 + offset, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void cancelNotification(Context context, long taskId) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(notificationId(taskId));
        }
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "任务闹钟", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("到点提醒是否开始学习任务");
        channel.enableVibration(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), attributes);
        manager.createNotificationChannel(channel);
    }

    private static int notificationId(long taskId) {
        return 7000 + (int) Math.abs(taskId % 100000);
    }
}
