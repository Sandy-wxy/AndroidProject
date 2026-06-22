package com.example.focus_flow.feature.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import com.example.focus_flow.MainActivity;
import com.example.focus_flow.R;
import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.core.navigation.FocusStartStore;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.feature.focus.FocusTimerController;
import com.example.focus_flow.feature.focus.FocusTimerSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FocusQuickWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_START_TASK =
            "com.example.focus_flow.action.WIDGET_START_TASK";
    private static final String EXTRA_TASK_ID = "task_id";

    private static final int[] ROW_IDS = {
            R.id.widget_task_row_1, R.id.widget_task_row_2, R.id.widget_task_row_3
    };
    private static final int[] TITLE_IDS = {
            R.id.widget_task_title_1, R.id.widget_task_title_2, R.id.widget_task_title_3
    };
    private static final int[] DETAIL_IDS = {
            R.id.widget_task_detail_1, R.id.widget_task_detail_2, R.id.widget_task_detail_3
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_START_TASK.equals(intent.getAction())) {
            long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L);
            if (taskId > 0) {
                new FocusStartStore(context).requestStart(taskId);
                openApp(context, true, false);
            }
        }
    }

    private static void update(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget_focus_quick);
        views.setOnClickPendingIntent(R.id.widget_title,
                openIntent(context, false, false, 801));
        views.setOnClickPendingIntent(R.id.widget_add,
                openIntent(context, false, true, 802));

        FocusTimerController controller = new FocusTimerController(context);
        if (controller.completeIfTimeReached() != null) {
            controller = new FocusTimerController(context);
        }
        FocusTimerSnapshot snapshot = controller.getSnapshot();
        if (snapshot != null) {
            showRunning(context, views, snapshot);
        } else {
            showTodayTasks(context, views);
        }
        manager.updateAppWidget(id, views);
    }

    private static void showRunning(Context context, RemoteViews views,
                                    FocusTimerSnapshot snapshot) {
        views.setTextViewText(R.id.widget_title, "专注进行中");
        views.setViewVisibility(R.id.widget_running, View.VISIBLE);
        views.setViewVisibility(R.id.widget_task_list, View.GONE);
        views.setTextViewText(R.id.widget_running_task,
                snapshot.session.taskTitleSnapshot);
        views.setTextViewText(R.id.widget_running_label,
                snapshot.paused ? "已暂停" : "正在专注");
        views.setTextViewText(R.id.widget_running_hint,
                "计划 " + snapshot.session.plannedFocusMinutes + " 分钟 · 点击进入专注舱");
        long base = SystemClock.elapsedRealtime() + snapshot.remainingSeconds * 1000L;
        views.setChronometer(R.id.widget_countdown, base, null, !snapshot.paused);
        views.setChronometerCountDown(R.id.widget_countdown, true);
        views.setOnClickPendingIntent(R.id.widget_running,
                openIntent(context, true, false, 803));
    }

    private static void showTodayTasks(Context context, RemoteViews views) {
        views.setTextViewText(R.id.widget_title, "今日任务");
        views.setViewVisibility(R.id.widget_running, View.GONE);
        views.setViewVisibility(R.id.widget_task_list, View.VISIBLE);

        List<TaskRecord> source = RepositoryProvider.get(context)
                .taskRepository.getTasksForDate(DateTimeUtils.todayDateString());
        List<TaskRecord> tasks = new ArrayList<>();
        for (TaskRecord task : source) {
            if (!task.isDeleted && task.status != TaskStatus.COMPLETED
                    && task.status != TaskStatus.ARCHIVED) {
                tasks.add(task);
            }
        }
        views.setViewVisibility(R.id.widget_empty,
                tasks.isEmpty() ? View.VISIBLE : View.GONE);
        views.setOnClickPendingIntent(R.id.widget_empty,
                openIntent(context, false, true, 804));

        for (int i = 0; i < ROW_IDS.length; i++) {
            if (i >= tasks.size()) {
                views.setViewVisibility(ROW_IDS[i], View.GONE);
                continue;
            }
            TaskRecord task = tasks.get(i);
            views.setViewVisibility(ROW_IDS[i], View.VISIBLE);
            views.setTextViewText(TITLE_IDS[i], task.title);
            String subject = task.subject == null || task.subject.trim().isEmpty()
                    ? "学习" : task.subject;
            views.setTextViewText(DETAIL_IDS[i],
                    subject + " · 预计 " + task.estimatedTotalMinutes + " 分钟 · 点击开始");
            views.setOnClickPendingIntent(ROW_IDS[i],
                    startTaskIntent(context, task.id, i));
        }
    }

    private static PendingIntent startTaskIntent(Context context, long taskId, int index) {
        Intent intent = new Intent(context, FocusQuickWidgetProvider.class)
                .setAction(ACTION_START_TASK)
                .putExtra(EXTRA_TASK_ID, taskId);
        return PendingIntent.getBroadcast(context,
                9000 + index + (int) (taskId % 1000), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent openIntent(Context context, boolean focus,
                                            boolean addTask, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_FOCUS, focus)
                .putExtra(MainActivity.EXTRA_ADD_TASK, addTask)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void openApp(Context context, boolean focus, boolean addTask) {
        Intent main = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_FOCUS, focus)
                .putExtra(MainActivity.EXTRA_ADD_TASK, addTask)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(main);
    }

    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, FocusQuickWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) update(context, manager, id);
    }
}
