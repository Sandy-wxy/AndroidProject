package com.example.focus_flow.service.focus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.focus_flow.MainActivity;
import com.example.focus_flow.R;
import com.example.focus_flow.feature.focus.FocusTimerController;
import com.example.focus_flow.feature.focus.FocusTimerSnapshot;

public class FocusTimerService extends Service {
    public static final String ACTION_START = "com.example.focus_flow.action.FOCUS_START";
    public static final String ACTION_PAUSE = "com.example.focus_flow.action.FOCUS_PAUSE";
    public static final String ACTION_RESUME = "com.example.focus_flow.action.FOCUS_RESUME";
    public static final String ACTION_STOP_SERVICE = "com.example.focus_flow.action.FOCUS_STOP_SERVICE";
    private static final String CHANNEL_ID = "focus_timer";
    private static final int NOTIFICATION_ID = 2401;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private FocusTimerController controller;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateForegroundNotification();
            handler.postDelayed(this, 1000L);
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, FocusTimerService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        controller = new FocusTimerController(this);
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_PAUSE.equals(action)) {
            controller.pause();
        } else if (ACTION_RESUME.equals(action)) {
            controller.resume();
        } else if (ACTION_STOP_SERVICE.equals(action)) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        FocusTimerSnapshot snapshot = controller.getSnapshot();
        if (snapshot == null) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, buildNotification(snapshot));
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateForegroundNotification() {
        if (controller.completeIfTimeReached() != null) {
            notifyFinished();
            stopForeground(false);
            stopSelf();
            return;
        }
        FocusTimerSnapshot snapshot = controller.getSnapshot();
        if (snapshot == null) {
            stopForeground(true);
            stopSelf();
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, buildNotification(snapshot));
    }

    private Notification buildNotification(FocusTimerSnapshot snapshot) {
        PendingIntent openApp = PendingIntent.getActivity(
                this,
                1,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean paused = snapshot.paused;
        PendingIntent toggleIntent = PendingIntent.getService(
                this,
                paused ? 3 : 2,
                new Intent(this, FocusTimerService.class).setAction(paused ? ACTION_RESUME : ACTION_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Action toggleAction = new NotificationCompat.Action(
                R.drawable.ic_nav_focus, paused ? "继续" : "暂停", toggleIntent);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_focus)
                .setContentTitle(snapshot.session.taskTitleSnapshot)
                .setContentText((paused ? "已暂停 · " : "专注中 · ") + formatClock(snapshot.remainingSeconds))
                .setOngoing(!paused)
                .setOnlyAlertOnce(true)
                .setContentIntent(openApp)
                .addAction(toggleAction)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .extend(new NotificationCompat.WearableExtender().addAction(toggleAction))
                .build();
    }

    private void notifyFinished() {
        PendingIntent openApp = PendingIntent.getActivity(
                this,
                4,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_focus)
                .setContentTitle("专注完成")
                .setContentText("回到番茄Focus查看本次专注总结。")
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "专注计时",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("显示正在进行的本地专注倒计时。");
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private String formatClock(int totalSeconds) {
        int minutes = Math.max(0, totalSeconds) / 60;
        int seconds = Math.max(0, totalSeconds) % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
