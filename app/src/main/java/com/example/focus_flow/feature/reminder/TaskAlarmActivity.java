package com.example.focus_flow.feature.reminder;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.focus_flow.MainActivity;
import com.example.focus_flow.R;
import com.example.focus_flow.core.navigation.FocusStartStore;
import com.example.focus_flow.feature.tasks.TaskUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class TaskAlarmActivity extends AppCompatActivity {
    private long taskId;
    private String taskTitle;
    private Ringtone ringtone;
    private TaskAlarmConfig config;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoStop = this::stopRinging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        bindIntent(getIntent());
        render();
        startRinging();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        bindIntent(intent);
        render();
        startRinging();
    }

    private void bindIntent(Intent intent) {
        taskId = intent.getLongExtra(TaskReminderReceiver.EXTRA_TASK_ID, -1L);
        taskTitle = intent.getStringExtra(TaskReminderReceiver.EXTRA_TASK_TITLE);
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = "学习任务";
        }
        config = TaskReminderScheduler.getConfig(this, taskId);
        if (config == null) config = new TaskAlarmConfig();
    }

    private void render() {
        com.example.focus_flow.core.ui.AuroraFrameLayout root =
                new com.example.focus_flow.core.ui.AuroraFrameLayout(this);
        root.setPadding(TaskUi.dp(this, 24), TaskUi.dp(this, 72),
                TaskUi.dp(this, 24), TaskUi.dp(this, 36));
        MaterialCardView card = TaskUi.glassCard(this);
        LinearLayout body = TaskUi.vertical(this, 24);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(body);
        body.addView(TaskUi.text(this, "任务时间到了", 28,
                getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.spacer(this, 12));
        body.addView(TaskUi.text(this, taskTitle, 22,
                getColor(R.color.focus_cyan), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(this, "是否进入任务？", 16,
                getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        body.addView(TaskUi.spacer(this, 22));

        MaterialButton yes = TaskUi.button(this, "是，进入专注", true);
        yes.setId(R.id.alarm_button_start);
        String laterText = config.snoozeCount <= 0
                ? "等会（未启用再响）"
                : "等会（" + config.snoozeMinutes + "分钟后提醒）";
        MaterialButton later = TaskUi.button(this, laterText, false);
        later.setId(R.id.alarm_button_snooze);
        later.setEnabled(config.snoozeCount > 0);
        MaterialButton close = TaskUi.button(this, "关闭", false);
        close.setId(R.id.alarm_button_close);
        body.addView(yes, matchWidth());
        body.addView(TaskUi.spacer(this, 8));
        body.addView(later, matchWidth());
        body.addView(TaskUi.spacer(this, 8));
        body.addView(close, matchWidth());
        root.addView(card, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        setContentView(root);

        yes.setOnClickListener(v -> enterFocus());
        later.setOnClickListener(v -> snooze());
        close.setOnClickListener(v -> closeAlarm());
    }

    private void enterFocus() {
        stopRinging();
        TaskReminderReceiver.cancelNotification(this, taskId);
        new FocusStartStore(this).requestStart(taskId);
        Intent main = new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_FOCUS, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(main);
        finish();
    }

    private void snooze() {
        stopRinging();
        TaskReminderReceiver.cancelNotification(this, taskId);
        TaskReminderScheduler.snooze(this, taskId, taskTitle);
        finish();
    }

    private void closeAlarm() {
        stopRinging();
        TaskReminderReceiver.cancelNotification(this, taskId);
        TaskReminderScheduler.cancel(this, taskId);
        finish();
    }

    private void startRinging() {
        stopRinging();
        int type = RingtoneManager.TYPE_ALARM;
        if ("NOTIFICATION".equals(config.ringtone)) type = RingtoneManager.TYPE_NOTIFICATION;
        else if ("RINGTONE".equals(config.ringtone)) type = RingtoneManager.TYPE_RINGTONE;
        android.net.Uri alarmUri = RingtoneManager.getDefaultUri(type);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(this, alarmUri);
        if (ringtone != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.setLooping(true);
            }
            ringtone.play();
        }
        handler.removeCallbacks(autoStop);
        handler.postDelayed(autoStop, Math.max(1, config.ringDurationMinutes) * 60_000L);
    }

    private void stopRinging() {
        handler.removeCallbacks(autoStop);
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        ringtone = null;
    }

    @Override
    protected void onDestroy() {
        stopRinging();
        super.onDestroy();
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
