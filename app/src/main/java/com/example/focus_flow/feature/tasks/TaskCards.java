package com.example.focus_flow.feature.tasks;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.focus_flow.R;
import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.domain.rules.ProgressEngine;
import com.example.focus_flow.feature.reminder.TaskReminderScheduler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskCards {
    public interface Actions {
        void onStart(TaskRecord task);
        void onEdit(TaskRecord task);
        void onDelete(TaskRecord task);
        void onComplete(TaskRecord task);
    }

    private final Context context;
    private final RepositoryProvider provider;
    private final ProgressEngine progressEngine = new ProgressEngine();
    private final SimpleDateFormat deadlineFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    public TaskCards(Context context) {
        this.context = context;
        provider = RepositoryProvider.get(context);
    }

    public MaterialCardView taskCard(TaskRecord task, Actions actions) {
        return taskCard(task, actions, null, null);
    }

    public MaterialCardView taskCard(TaskRecord task, Actions actions,
                                     String quickActionLabel, Runnable quickAction) {
        List<FocusSessionRecord> sessions = provider.focusSessionRepository.getSessionsByTaskId(task.id);
        int progress = progressEngine.progressPercent(task, sessions);
        double effective = progressEngine.totalEffectiveMinutes(sessions);
        FocusBlockRecord next = provider.taskRepository.getNextPendingBlock(task.id);

        MaterialCardView card = TaskUi.glassCard(context);
        int tagColor = colorForTask(task);
        card.setStrokeColor(tagColor);
        card.setStrokeWidth(TaskUi.dp(context, 2));
        LinearLayout body = TaskUi.vertical(context, 18);
        card.addView(body);

        LinearLayout badges = TaskUi.horizontal(context);
        badges.addView(badge(task.subject, tagColor));
        badges.addView(badge(priorityText(task)));
        if (task.deadlineAt != null) {
            badges.addView(badge("截止 " + deadlineFormat.format(new java.util.Date(task.deadlineAt))));
        }
        Long reminderAt = TaskReminderScheduler.getReminderAt(context, task.id);
        if (reminderAt != null) {
            badges.addView(badge("闹钟 " + deadlineFormat.format(new java.util.Date(reminderAt)),
                    context.getColor(R.color.focus_orange)));
        }
        body.addView(badges);

        body.addView(TaskUi.spacer(context, 8));
        body.addView(TaskUi.text(context, task.title, 19, context.getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(context, task.targetOutcome, 14, context.getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        body.addView(TaskUi.spacer(context, 10));

        ProgressBar bar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(progress);
        bar.setProgressTintList(ColorStateList.valueOf(tagColor));
        body.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, TaskUi.dp(context, 10)));

        body.addView(TaskUi.spacer(context, 8));
        int remaining = Math.max(0, (int) Math.ceil(task.estimatedTotalMinutes - effective));
        body.addView(TaskUi.text(context,
                "已有效专注 " + Math.round(effective) + " 分钟 / 预计 " + task.estimatedTotalMinutes + " 分钟，剩余约 " + remaining + " 分钟",
                13, context.getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        String recommend = next == null ? "暂无待完成番茄钟" : "下一个番茄钟：" + next.plannedFocusMinutes + " 分钟";
        body.addView(TaskUi.text(context, recommend, 13, tagColor, android.graphics.Typeface.BOLD));

        LinearLayout actionsRow = TaskUi.horizontal(context);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);
        MaterialButton start = TaskUi.button(context, "开始", true);
        MaterialButton edit = TaskUi.button(context, "编辑", false);
        MaterialButton more = TaskUi.button(context, "更多", false);
        actionsRow.addView(start, weighted());
        actionsRow.addView(edit, weightedWithStartMargin());
        actionsRow.addView(more, weightedWithStartMargin());
        body.addView(TaskUi.spacer(context, 10));
        body.addView(actionsRow);
        if (quickActionLabel != null && quickAction != null) {
            MaterialButton quick = TaskUi.button(context, quickActionLabel, false);
            quick.setEnabled(task.status != TaskStatus.ARCHIVED);
            quick.setOnClickListener(v -> quickAction.run());
            body.addView(TaskUi.spacer(context, 8));
            body.addView(quick, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        start.setEnabled(task.status != TaskStatus.COMPLETED && task.status != TaskStatus.ARCHIVED);
        start.setOnClickListener(v -> actions.onStart(task));
        edit.setOnClickListener(v -> actions.onEdit(task));
        more.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(context, more);
            menu.getMenu().add("标记完成");
            menu.getMenu().add("删除");
            menu.setOnMenuItemClickListener(item -> {
                if ("删除".contentEquals(item.getTitle())) {
                    actions.onDelete(task);
                } else {
                    actions.onComplete(task);
                }
                return true;
            });
            menu.show();
        });
        return card;
    }

    public MaterialCardView metricCard(String title, String value) {
        MaterialCardView card = TaskUi.glassCard(context);
        LinearLayout body = TaskUi.vertical(context, 14);
        card.addView(body);
        body.addView(TaskUi.text(context, value, 22, context.getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(context, title, 12, context.getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        return card;
    }

    private TextView badge(String text) {
        return badge(text, context.getColor(R.color.focus_cyan));
    }

    private TextView badge(String text, int color) {
        TextView badge = TaskUi.text(context, text, 12, context.getColor(R.color.text_primary), android.graphics.Typeface.BOLD);
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        background.setCornerRadius(TaskUi.dp(context, 99));
        background.setColor(withAlpha(color, 36));
        background.setStroke(TaskUi.dp(context, 1), withAlpha(color, 110));
        badge.setBackground(background);
        badge.setPadding(TaskUi.dp(context, 10), TaskUi.dp(context, 5), TaskUi.dp(context, 10), TaskUi.dp(context, 5));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(TaskUi.dp(context, 8));
        badge.setLayoutParams(params);
        return badge;
    }

    private int colorForTask(TaskRecord task) {
        if (task.colorTag == null) {
            return context.getColor(R.color.focus_cyan);
        }
        switch (task.colorTag) {
            case PURPLE:
                return context.getColor(R.color.focus_purple);
            case BLUE:
                return context.getColor(R.color.focus_blue);
            case GREEN:
                return context.getColor(R.color.focus_green);
            case ORANGE:
                return context.getColor(R.color.focus_orange);
            case PINK:
                return context.getColor(R.color.focus_pink);
            case CYAN:
            default:
                return context.getColor(R.color.focus_cyan);
        }
    }

    private int withAlpha(int color, int alpha) {
        return android.graphics.Color.argb(alpha,
                android.graphics.Color.red(color),
                android.graphics.Color.green(color),
                android.graphics.Color.blue(color));
    }

    private String priorityText(TaskRecord task) {
        switch (task.priority) {
            case URGENT:
                return "紧急";
            case HIGH:
                return "高优先级";
            case LOW:
                return "低优先级";
            case NORMAL:
            default:
                return "中优先级";
        }
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }

    private LinearLayout.LayoutParams weightedWithStartMargin() {
        LinearLayout.LayoutParams params = weighted();
        params.setMarginStart(TaskUi.dp(context, 8));
        return params;
    }
}
