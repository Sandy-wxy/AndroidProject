package com.example.focus_flow.feature.tasks;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import com.example.focus_flow.core.model.TimeSegment;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.domain.assistant.AiPromptBuilder;
import com.example.focus_flow.domain.assistant.AiResponseParser;
import com.example.focus_flow.domain.assistant.TaskRewriteEngine;
import com.example.focus_flow.domain.assistant.TaskRewriteSession;
import com.example.focus_flow.domain.assistant.TaskRewriteSuggestion;
import com.example.focus_flow.domain.rules.FocusRecommendationEngine;
import com.example.focus_flow.domain.rules.ProgressEngine;
import com.example.focus_flow.domain.rules.Recommendation;
import com.example.focus_flow.domain.rules.TaskSplitEngine;
import com.example.focus_flow.domain.stats.RecentStats;
import com.example.focus_flow.domain.stats.StatsCalculator;
import com.example.focus_flow.feature.assistant.AiProxyClient;
import com.example.focus_flow.feature.assistant.AiUiTransitions;
import com.example.focus_flow.feature.reminder.TaskReminderScheduler;
import com.example.focus_flow.feature.reminder.TaskAlarmConfig;
import com.example.focus_flow.feature.reminder.TaskAlarmSettingsDialog;
import com.example.focus_flow.feature.widget.FocusQuickWidgetProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskFormBottomSheet extends BottomSheetDialogFragment {
    private static final long REWRITE_DEBOUNCE_MS = 2_000L;
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
    private MaterialButton reminderButton;
    private TaskAlarmConfig alarmConfig;
    private Spinner difficultySpinner;
    private Spinner prioritySpinner;
    private Spinner colorSpinner;
    private SwitchMaterial autoSplitSwitch;
    private LinearLayout rewriteContainer;
    private final TaskRewriteSession rewriteSession = new TaskRewriteSession(new TaskRewriteEngine());
    private final AiResponseParser aiResponseParser = new AiResponseParser();
    private final AiProxyClient aiProxyClient = new AiProxyClient();
    private final Handler rewriteHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingRewriteRunnable;
    private String pendingRewriteText;
    private String lastApiRewriteText;
    private boolean applyingRewrite;
    private LinearLayout advancedSection;
    private MaterialButton advancedToggleButton;

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

    @Override
    public void onDestroyView() {
        cancelPendingRewrite();
        super.onDestroyView();
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
        installDatePickers();
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
        rewriteContainer = TaskUi.vertical(requireContext(), 10);
        rewriteContainer.setId(com.example.focus_flow.R.id.task_rewrite_container);
        rewriteContainer.setVisibility(View.GONE);
        form.addView(rewriteContainer);
        installRewriteWatcher();
        form.addView(TaskUi.text(requireContext(), "其余项目已设置常用默认值，需要时再展开修改。",
                13, requireContext().getColor(com.example.focus_flow.R.color.text_secondary),
                android.graphics.Typeface.NORMAL));
        MaterialButton advancedToggle = TaskUi.button(requireContext(), "展开更多设置", false);
        advancedToggleButton = advancedToggle;
        advancedToggle.setId(com.example.focus_flow.R.id.task_button_advanced_toggle);
        form.addView(advancedToggle);

        LinearLayout advanced = TaskUi.vertical(requireContext(), 0);
        advancedSection = advanced;
        advanced.setVisibility(View.GONE);
        addField(advanced, subjectInput);
        addField(advanced, targetInput);
        addField(advanced, descriptionInput);
        addField(advanced, estimatedInput);
        addLabeledSpinner(advanced, "任务难度", difficultySpinner);
        addLabeledSpinner(advanced, "优先级", prioritySpinner);
        addField(advanced, plannedDateInput);
        addField(advanced, deadlineInput);
        reminderButton = TaskUi.button(requireContext(), "闹钟提醒：未设置", false);
        reminderButton.setId(com.example.focus_flow.R.id.task_button_alarm_settings);
        reminderButton.setOnClickListener(v -> showAlarmSettings());
        advanced.addView(reminderButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        advanced.addView(TaskUi.spacer(requireContext(), 8));
        addLabeledSpinner(advanced, "颜色标签", colorSpinner);
        advanced.addView(autoSplitSwitch);
        form.addView(advanced);
        advancedToggle.setOnClickListener(v -> {
            triggerPendingRewriteNow();
            setAdvancedVisible(advanced.getVisibility() != View.VISIBLE);
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

    private void setAdvancedVisible(boolean show) {
        if (advancedSection == null || advancedToggleButton == null) {
            return;
        }
        advancedSection.setVisibility(show ? View.VISIBLE : View.GONE);
        advancedToggleButton.setText(show ? "收起更多设置" : "展开更多设置");
    }
    private void installRewriteWatcher() {
        if (editingTask != null) {
            return;
        }
        titleInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                triggerPendingRewriteNow();
            }
        });
        titleInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (applyingRewrite) {
                    return;
                }
                scheduleRewrite(StringUtils.trim(s == null ? "" : s.toString()));
            }
        });
    }

    private void scheduleRewrite(String rough) {
        cancelPendingRewrite();
        if (rough.length() < 3) {
            return;
        }
        pendingRewriteText = rough;
        pendingRewriteRunnable = () -> runRewriteSuggestions(rough);
        rewriteHandler.postDelayed(pendingRewriteRunnable, REWRITE_DEBOUNCE_MS);
    }

    private void triggerPendingRewriteNow() {
        String rough = pendingRewriteText;
        if (rough == null || rough.length() < 3) {
            return;
        }
        cancelPendingRewrite();
        runRewriteSuggestions(rough);
    }

    private void cancelPendingRewrite() {
        if (pendingRewriteRunnable != null) {
            rewriteHandler.removeCallbacks(pendingRewriteRunnable);
        }
        pendingRewriteRunnable = null;
        pendingRewriteText = null;
    }

    private void runRewriteSuggestions(String rough) {
        if (!isAdded() || titleInput == null) {
            return;
        }
        String current = StringUtils.trim(titleInput.getText().toString());
        if (!rough.equals(current) || rough.length() < 3) {
            return;
        }
        List<TaskRewriteSuggestion> local = rewriteSession.requestOnce(
                rough, TimeSegment.fromMillis(System.currentTimeMillis()));
        if (!local.isEmpty()) {
            showRewriteSuggestions(local);
        }
        if (!rough.equals(lastApiRewriteText)) {
            lastApiRewriteText = rough;
            requestApiRewriteSuggestions(rough);
        }
    }
    private void requestApiRewriteSuggestions(String rough) {
        AiPromptBuilder.TaskRewriteContext context = new AiPromptBuilder.TaskRewriteContext();
        context.roughTask = rough;
        context.currentSegment = TimeSegment.fromMillis(System.currentTimeMillis());
        String prompt = new AiPromptBuilder().buildTaskRewritePrompt(context);
        aiProxyClient.chat(prompt, new AiProxyClient.Callback() {
            @Override
            public void onSuccess(String responseBody) {
                if (!isAdded()) {
                    return;
                }
                if (titleInput == null || !rough.equals(StringUtils.trim(titleInput.getText().toString()))) {
                    return;
                }
                List<TaskRewriteSuggestion> api = aiResponseParser.parseTaskRewrites(responseBody);
                if (!api.isEmpty()) {
                    showRewriteSuggestions(api);
                }
            }

            @Override
            public void onError(Exception error) {
                // Local rewrite suggestions stay visible when the proxy is slow or unavailable.
            }
        });
    }

    private void showRewriteSuggestions(List<TaskRewriteSuggestion> suggestions) {
        if (rewriteContainer == null) {
            return;
        }
        boolean animate = rewriteContainer.getVisibility() == View.VISIBLE
                && rewriteContainer.getChildCount() > 0;
        AiUiTransitions.crossFadeChildren(rewriteContainer,
                () -> renderRewriteSuggestions(suggestions), animate);
    }

    private void renderRewriteSuggestions(List<TaskRewriteSuggestion> suggestions) {
        rewriteContainer.removeAllViews();
        if (suggestions == null || suggestions.isEmpty()) {
            rewriteContainer.setVisibility(View.GONE);
            return;
        }
        rewriteContainer.setVisibility(View.VISIBLE);
        rewriteContainer.addView(TaskUi.text(requireContext(), "智能改写建议", 14,
                requireContext().getColor(com.example.focus_flow.R.color.text_secondary),
                android.graphics.Typeface.BOLD));
        List<TaskRewriteSuggestion> visible = new ArrayList<>(suggestions);
        for (int i = 0; i < Math.min(3, visible.size()); i++) {
            TaskRewriteSuggestion suggestion = visible.get(i);
            MaterialButton button = TaskUi.button(requireContext(),
                    suggestion.title + " · " + suggestion.estimatedMinutes + "min", false);
            if (i == 0) button.setId(com.example.focus_flow.R.id.task_rewrite_suggestion_1);
            if (i == 1) button.setId(com.example.focus_flow.R.id.task_rewrite_suggestion_2);
            if (i == 2) button.setId(com.example.focus_flow.R.id.task_rewrite_suggestion_3);
            button.setAllCaps(false);
            button.setOnClickListener(v -> applyRewriteSuggestion(suggestion));
            rewriteContainer.addView(button, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void applyRewriteSuggestion(TaskRewriteSuggestion suggestion) {
        cancelPendingRewrite();
        applyingRewrite = true;
        titleInput.setText(suggestion.title);
        subjectInput.setText(suggestion.subject);
        targetInput.setText(suggestion.targetOutcome);
        descriptionInput.setText(suggestion.description);
        estimatedInput.setText(String.valueOf(suggestion.estimatedMinutes));
        difficultySpinner.setSelection(suggestion.difficulty.ordinal());
        prioritySpinner.setSelection(suggestion.priority.ordinal());
        applyingRewrite = false;
        setAdvancedVisible(true);
        if (rewriteContainer != null) {
            rewriteContainer.setVisibility(View.GONE);
        }
    }

    private void bindEditingTask() {
        if (editingTask == null) {
            subjectInput.setText("学习");
            targetInput.setText("完成本次学习任务");
            estimatedInput.setText("25");
            plannedDateInput.setText(initialPlannedDate == null
                    ? DateTimeUtils.todayDateString()
                    : initialPlannedDate);
            updateAlarmButton();
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
        alarmConfig = TaskReminderScheduler.getConfig(requireContext(), editingTask.id);
        updateAlarmButton();
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
        if (alarmConfig == null) {
            TaskReminderScheduler.cancel(requireContext(), taskId);
        } else {
            TaskReminderScheduler.schedule(requireContext(), taskId, task.title, alarmConfig);
            requestNotificationPermissionIfNeeded();
        }
        FocusQuickWidgetProvider.refreshAll(requireContext());
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

    private void installDatePickers() {
        configurePickerInput(plannedDateInput);
        configurePickerInput(deadlineInput);
        plannedDateInput.setOnClickListener(v -> showPlannedDatePicker());
        deadlineInput.setOnClickListener(v -> showDeadlineDatePicker());
    }

    private void configurePickerInput(EditText input) {
        input.setInputType(InputType.TYPE_NULL);
        input.setFocusable(false);
        input.setFocusableInTouchMode(false);
        input.setCursorVisible(false);
        input.setClickable(true);
    }

    private void showPlannedDatePicker() {
        Calendar calendar = calendarFromDateText(StringUtils.trim(plannedDateInput.getText().toString()));
        new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            calendar.set(year, month, day, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            plannedDateInput.setText(DateTimeUtils.formatDate(calendar.getTimeInMillis()));
            plannedDateInput.setError(null);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showDeadlineDatePicker() {
        Calendar calendar = calendarFromDeadlineText();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            showDeadlineTimePicker(calendar);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.setButton(android.content.DialogInterface.BUTTON_NEUTRAL,
                "\u6e05\u9664", (ignored, which) -> {
                    deadlineInput.setText("");
                    deadlineInput.setError(null);
                    updateAlarmButton();
                });
        dialog.show();
    }

    private void showDeadlineTimePicker(Calendar calendar) {
        new TimePickerDialog(requireContext(), (picker, hour, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            deadlineInput.setText(deadlineFormat().format(calendar.getTime()));
            deadlineInput.setError(null);
            updateAlarmButton();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private Calendar calendarFromDeadlineText() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        String deadline = StringUtils.trim(deadlineInput.getText().toString());
        if (!deadline.isEmpty()) {
            try {
                Date date = deadlineFormat().parse(deadline);
                if (date != null) {
                    calendar.setTime(date);
                    return calendar;
                }
            } catch (ParseException ignored) {
                // Fall back to planned date plus the next convenient hour.
            }
        }
        String planned = StringUtils.trim(plannedDateInput.getText().toString());
        try {
            Date date = dateFormat().parse(planned);
            if (date != null) {
                Calendar plannedCalendar = Calendar.getInstance();
                plannedCalendar.setTime(date);
                calendar.set(Calendar.YEAR, plannedCalendar.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, plannedCalendar.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, plannedCalendar.get(Calendar.DAY_OF_MONTH));
            }
        } catch (ParseException ignored) {
            // Keep the default fallback when planned date is invalid.
        }
        return calendar;
    }

    private Calendar calendarFromDateText(String text) {
        Calendar calendar = Calendar.getInstance();
        if (text.isEmpty()) {
            return calendar;
        }
        try {
            Date date = dateFormat().parse(text);
            if (date != null) {
                calendar.setTime(date);
            }
        } catch (ParseException ignored) {
            // Invalid manual values still open the picker on today.
        }
        return calendar;
    }

    private SimpleDateFormat dateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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
        if (alarmConfig != null && alarmConfig.triggerAtMillis < System.currentTimeMillis() + 60 * 1000L) {
            android.widget.Toast.makeText(requireContext(),
                    "闹钟时间需至少晚于当前 1 分钟", android.widget.Toast.LENGTH_SHORT).show();
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
    }

    private void showAlarmSettings() {
        new TaskAlarmSettingsDialog(alarmConfig, config -> {
            alarmConfig = config;
            updateAlarmButton();
        }).show(getParentFragmentManager(), "task_alarm_settings");
    }

    private void updateAlarmButton() {
        if (reminderButton == null) return;
        if (alarmConfig == null) {
            reminderButton.setText("闹钟提醒：未设置");
            return;
        }
        String repeat;
        if (TaskAlarmConfig.REPEAT_DAILY.equals(alarmConfig.repeatMode)) repeat = "每天";
        else if (TaskAlarmConfig.REPEAT_WEEKDAYS.equals(alarmConfig.repeatMode)) repeat = "工作日";
        else if (TaskAlarmConfig.REPEAT_WEEKLY.equals(alarmConfig.repeatMode)) repeat = "每周";
        else repeat = "只响一次";
        reminderButton.setText("闹钟  "
                + deadlineFormat().format(new java.util.Date(alarmConfig.triggerAtMillis))
                + "  ·  " + repeat + "  ›");
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
