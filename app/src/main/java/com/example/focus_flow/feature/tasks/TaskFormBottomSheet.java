package com.example.focus_flow.feature.tasks;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.core.common.StringUtils;
import com.example.focus_flow.core.model.ColorTag;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.domain.rules.FocusRecommendationEngine;
import com.example.focus_flow.domain.rules.ProgressEngine;
import com.example.focus_flow.domain.rules.Recommendation;
import com.example.focus_flow.domain.rules.TaskSplitEngine;
import com.example.focus_flow.domain.stats.RecentStats;
import com.example.focus_flow.domain.stats.StatsCalculator;
import com.example.focus_flow.feature.reminder.TaskReminderScheduler;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskFormBottomSheet extends BottomSheetDialogFragment {
    public interface Callback {
        void onSaved(boolean startNow, long taskId);
    }

    private final TaskRecord editingTask;
    private final Callback callback;
    private final String initialPlannedDate;
    private EditText titleInput;
    private EditText subjectInput;
    private EditText targetInput;
    private EditText descriptionInput;
    private EditText estimatedInput;
    private EditText plannedDateInput;
    private EditText deadlineInput;
    private EditText reminderInput;
    private Spinner difficultySpinner;
    private Spinner prioritySpinner;
    private Spinner colorSpinner;
    private SwitchMaterial autoSplitSwitch;

    public TaskFormBottomSheet(@Nullable TaskRecord editingTask, Callback callback) {
        this(editingTask, null, callback);
    }

    public TaskFormBottomSheet(@Nullable TaskRecord editingTask,
                               @Nullable String initialPlannedDate,
                               Callback callback) {
        this.editingTask = editingTask;
        this.initialPlannedDate = initialPlannedDate;
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout form = TaskUi.vertical(requireContext(), 22);
        scrollView.addView(form);
        form.addView(TaskUi.text(requireContext(), editingTask == null ? "新增学习任务" : "编辑学习任务",
                24, requireContext().getColor(com.example.focus_flow.R.color.text_primary), android.graphics.Typeface.BOLD));
        form.addView(TaskUi.spacer(requireContext(), 14));

        titleInput = input("任务名称 *", InputType.TYPE_CLASS_TEXT, 1);
        titleInput.setId(com.example.focus_flow.R.id.task_input_title);
        subjectInput = input("学习科目（默认：学习）", InputType.TYPE_CLASS_TEXT, 1);
        subjectInput.setId(com.example.focus_flow.R.id.task_input_subject);
        targetInput = input("任务目标（默认：完成本次学习任务）", InputType.TYPE_CLASS_TEXT, 2);
        targetInput.setId(com.example.focus_flow.R.id.task_input_target);
        descriptionInput = input("任务描述", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 3);
        descriptionInput.setId(com.example.focus_flow.R.id.task_input_description);
        estimatedInput = input("预计总时长（默认：25 分钟）", InputType.TYPE_CLASS_NUMBER, 1);
        estimatedInput.setId(com.example.focus_flow.R.id.task_input_estimated);
        plannedDateInput = input("计划日期（默认：今天）", InputType.TYPE_CLASS_TEXT, 1);
        plannedDateInput.setId(com.example.focus_flow.R.id.task_input_planned_date);
        deadlineInput = input("截止时间 yyyy-MM-dd HH:mm（可选）", InputType.TYPE_CLASS_TEXT, 1);
        deadlineInput.setId(com.example.focus_flow.R.id.task_input_deadline);
        reminderInput = input("闹钟提醒 yyyy-MM-dd HH:mm（可选）", InputType.TYPE_CLASS_TEXT, 1);
        reminderInput.setId(com.example.focus_flow.R.id.task_input_reminder);
        difficultySpinner = spinner(new String[]{"轻松", "普通", "困难", "高压"});
        difficultySpinner.setId(com.example.focus_flow.R.id.task_spinner_difficulty);
        prioritySpinner = spinner(new String[]{"低", "中", "高", "紧急"});
        prioritySpinner.setId(com.example.focus_flow.R.id.task_spinner_priority);
        colorSpinner = spinner(new String[]{"青色", "紫色", "蓝色", "绿色", "橙色", "粉色"});
        colorSpinner.setId(com.example.focus_flow.R.id.task_spinner_color);
        autoSplitSwitch = new SwitchMaterial(requireContext());
        autoSplitSwitch.setId(com.example.focus_flow.R.id.task_switch_auto_split);
        autoSplitSwitch.setText("自动拆分番茄钟");
        autoSplitSwitch.setTextColor(requireContext().getColor(com.example.focus_flow.R.color.text_primary));
        autoSplitSwitch.setChecked(true);

        addField(form, titleInput);
        form.addView(TaskUi.text(requireContext(), "其余项目已设置常用默认值，需要时再展开修改。",
                13, requireContext().getColor(com.example.focus_flow.R.color.text_secondary),
                android.graphics.Typeface.NORMAL));
        MaterialButton advancedToggle = TaskUi.button(requireContext(), "展开更多设置", false);
        form.addView(advancedToggle);

        LinearLayout advanced = TaskUi.vertical(requireContext(), 0);
        advanced.setVisibility(View.GONE);
        addField(advanced, subjectInput);
        addField(advanced, targetInput);
        addField(advanced, descriptionInput);
        addField(advanced, estimatedInput);
        addLabeledSpinner(advanced, "任务难度", difficultySpinner);
        addLabeledSpinner(advanced, "优先级", prioritySpinner);
        addField(advanced, plannedDateInput);
        addField(advanced, deadlineInput);
        addField(advanced, reminderInput);
        addLabeledSpinner(advanced, "颜色标签", colorSpinner);
        advanced.addView(autoSplitSwitch);
        form.addView(advanced);
        advancedToggle.setOnClickListener(v -> {
            boolean show = advanced.getVisibility() != View.VISIBLE;
            advanced.setVisibility(show ? View.VISIBLE : View.GONE);
            advancedToggle.setText(show ? "收起更多设置" : "展开更多设置");
        });
        form.addView(TaskUi.spacer(requireContext(), 18));

        LinearLayout buttons = TaskUi.horizontal(requireContext());
        MaterialButton save = TaskUi.button(requireContext(), editingTask == null ? "保存暂不开始" : "保存修改", true);
        save.setId(com.example.focus_flow.R.id.task_button_save);
        buttons.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (editingTask == null) {
            MaterialButton start = TaskUi.button(requireContext(), "保存并开始", true);
            start.setId(com.example.focus_flow.R.id.task_button_save_start);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            params.setMarginStart(TaskUi.dp(requireContext(), 10));
            buttons.addView(start, params);
            start.setOnClickListener(v -> saveTask(true));
        }
        save.setOnClickListener(v -> saveTask(false));
        form.addView(buttons);
        bindEditingTask();
        return scrollView;
    }

    private void bindEditingTask() {
        if (editingTask == null) {
            subjectInput.setText("学习");
            targetInput.setText("完成本次学习任务");
            estimatedInput.setText("25");
            plannedDateInput.setText(initialPlannedDate == null
                    ? DateTimeUtils.todayDateString()
                    : initialPlannedDate);
            return;
        }
        titleInput.setText(editingTask.title);
        subjectInput.setText(editingTask.subject);
        targetInput.setText(editingTask.targetOutcome);
        descriptionInput.setText(editingTask.description);
        estimatedInput.setText(String.valueOf(editingTask.estimatedTotalMinutes));
        plannedDateInput.setText(editingTask.plannedDate);
        if (editingTask.deadlineAt != null) {
            deadlineInput.setText(deadlineFormat().format(new java.util.Date(editingTask.deadlineAt)));
        }
        Long reminderAt = TaskReminderScheduler.getReminderAt(requireContext(), editingTask.id);
        if (reminderAt != null) {
            reminderInput.setText(deadlineFormat().format(new java.util.Date(reminderAt)));
        }
        difficultySpinner.setSelection(editingTask.difficulty.ordinal());
        prioritySpinner.setSelection(editingTask.priority.ordinal());
        colorSpinner.setSelection(editingTask.colorTag.ordinal());
        autoSplitSwitch.setChecked(editingTask.autoSplitEnabled);
    }

    private void saveTask(boolean startNow) {
        applyDefaults();
        if (!validate()) {
            return;
        }
        RepositoryProvider provider = RepositoryProvider.get(requireContext());
        TaskRecord task = editingTask == null ? new TaskRecord() : editingTask;
        task.title = StringUtils.trim(titleInput.getText().toString());
        task.subject = StringUtils.trim(subjectInput.getText().toString());
        task.targetOutcome = StringUtils.trim(targetInput.getText().toString());
        task.description = StringUtils.trim(descriptionInput.getText().toString());
        task.estimatedTotalMinutes = Integer.parseInt(StringUtils.trim(estimatedInput.getText().toString()));
        task.plannedDate = StringUtils.trim(plannedDateInput.getText().toString());
        task.deadlineAt = parseDeadline(false);
        task.difficulty = TaskDifficulty.values()[difficultySpinner.getSelectedItemPosition()];
        task.priority = TaskPriority.values()[prioritySpinner.getSelectedItemPosition()];
        task.colorTag = ColorTag.values()[colorSpinner.getSelectedItemPosition()];
        task.autoSplitEnabled = autoSplitSwitch.isChecked();
        if (task.status == null) {
            task.status = TaskStatus.PENDING;
        }

        List<FocusSessionRecord> taskSessions = editingTask == null
                ? java.util.Collections.emptyList()
                : provider.focusSessionRepository.getSessionsByTaskId(editingTask.id);
        double effective = new ProgressEngine().totalEffectiveMinutes(taskSessions);
        RecentStats stats = new StatsCalculator().calculateRecentStats(
                provider.focusSessionRepository.getRecentSessions(20), System.currentTimeMillis());
        Recommendation recommendation = new FocusRecommendationEngine().recommend(task, stats, effective, System.currentTimeMillis());
        List<FocusBlockRecord> blocks = new TaskSplitEngine().splitTask(task, recommendation, effective);

        long taskId;
        if (editingTask == null) {
            taskId = provider.taskRepository.insertTask(task, blocks);
        } else {
            provider.taskRepository.updateTask(task);
            provider.taskRepository.replacePendingBlocks(task.id, blocks);
            taskId = task.id;
        }
        Long reminderAt = parseReminder(false);
        if (reminderAt == null) {
            TaskReminderScheduler.cancel(requireContext(), taskId);
        } else {
            TaskReminderScheduler.schedule(requireContext(), taskId, task.title, reminderAt);
            requestNotificationPermissionIfNeeded();
        }
        callback.onSaved(startNow, taskId);
        dismiss();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4202);
        }
    }

    private void applyDefaults() {
        if (StringUtils.trim(subjectInput.getText().toString()).isEmpty()) {
            subjectInput.setText("学习");
        }
        if (StringUtils.trim(targetInput.getText().toString()).isEmpty()) {
            targetInput.setText("完成本次学习任务");
        }
        if (StringUtils.trim(estimatedInput.getText().toString()).isEmpty()) {
            estimatedInput.setText("25");
        }
        if (StringUtils.trim(plannedDateInput.getText().toString()).isEmpty()) {
            plannedDateInput.setText(DateTimeUtils.todayDateString());
        }
    }

    private boolean validate() {
        clearErrors();
        boolean ok = true;
        ok &= requireLength(titleInput, 1, 30, "任务名称长度需为 1-30 字");
        ok &= requireLength(subjectInput, 1, 20, "学习科目长度需为 1-20 字");
        ok &= requireLength(targetInput, 1, 80, "任务目标长度需为 1-80 字");
        ok &= requireLength(descriptionInput, 0, 200, "任务描述最多 200 字");
        String estimated = StringUtils.trim(estimatedInput.getText().toString());
        if (estimated.length() == 0) {
            estimatedInput.setError("请输入预计总时长");
            ok = false;
        } else {
            int value;
            try {
                value = Integer.parseInt(estimated);
            } catch (NumberFormatException ex) {
                value = -1;
            }
            if (value < 10 || value > 600) {
                estimatedInput.setError("范围必须是 10-600 分钟");
                ok = false;
            }
        }
        if (!StringUtils.trim(plannedDateInput.getText().toString()).matches("\\d{4}-\\d{2}-\\d{2}")) {
            plannedDateInput.setError("格式必须为 yyyy-MM-dd");
            ok = false;
        }
        Long deadline = parseDeadline(true);
        if (deadline != null && editingTask == null && deadline < System.currentTimeMillis() + 10 * 60 * 1000L) {
            deadlineInput.setError("新任务截止时间需至少晚于当前 10 分钟");
            ok = false;
        }
        String reminderText = StringUtils.trim(reminderInput.getText().toString());
        Long reminder = parseReminder(true);
        if (!reminderText.isEmpty() && reminder == null) {
            ok = false;
        } else if (reminder != null && reminder < System.currentTimeMillis() + 60 * 1000L) {
            reminderInput.setError("提醒时间需至少晚于当前 1 分钟");
            ok = false;
        }
        return ok;
    }

    private boolean requireLength(EditText input, int min, int max, String error) {
        String text = StringUtils.trim(input.getText().toString());
        if (text.length() < min || text.length() > max) {
            input.setError(error);
            return false;
        }
        return true;
    }

    private Long parseDeadline(boolean markError) {
        String text = StringUtils.trim(deadlineInput.getText().toString());
        if (text.length() == 0) {
            return null;
        }
        try {
            java.util.Date date = deadlineFormat().parse(text);
            return date == null ? null : date.getTime();
        } catch (ParseException ex) {
            if (markError) {
                deadlineInput.setError("格式必须为 yyyy-MM-dd HH:mm");
            }
            return null;
        }
    }

    private Long parseReminder(boolean markError) {
        String text = StringUtils.trim(reminderInput.getText().toString());
        if (text.length() == 0) {
            return null;
        }
        try {
            java.util.Date date = deadlineFormat().parse(text);
            return date == null ? null : date.getTime();
        } catch (ParseException ex) {
            if (markError) {
                reminderInput.setError("格式必须为 yyyy-MM-dd HH:mm");
            }
            return null;
        }
    }

    private SimpleDateFormat deadlineFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    private void clearErrors() {
        titleInput.setError(null);
        subjectInput.setError(null);
        targetInput.setError(null);
        descriptionInput.setError(null);
        estimatedInput.setError(null);
        plannedDateInput.setError(null);
        deadlineInput.setError(null);
        reminderInput.setError(null);
    }

    private EditText input(String hint, int inputType, int lines) {
        EditText input = new EditText(requireContext());
        input.setHint(hint);
        input.setInputType(inputType);
        input.setMinLines(lines);
        input.setTextColor(requireContext().getColor(com.example.focus_flow.R.color.text_primary));
        input.setHintTextColor(requireContext().getColor(com.example.focus_flow.R.color.text_weak));
        return input;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(requireContext());
        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, values));
        return spinner;
    }

    private void addField(LinearLayout form, EditText input) {
        form.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        form.addView(TaskUi.spacer(requireContext(), 8));
    }

    private void addLabeledSpinner(LinearLayout form, String label, Spinner spinner) {
        form.addView(TaskUi.text(requireContext(), label, 13,
                requireContext().getColor(com.example.focus_flow.R.color.text_secondary), android.graphics.Typeface.BOLD));
        form.addView(spinner);
        form.addView(TaskUi.spacer(requireContext(), 8));
    }
}
