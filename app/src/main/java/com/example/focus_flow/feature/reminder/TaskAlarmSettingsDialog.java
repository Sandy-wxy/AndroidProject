package com.example.focus_flow.feature.reminder;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.focus_flow.R;
import com.example.focus_flow.feature.tasks.TaskUi;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TaskAlarmSettingsDialog extends DialogFragment {
    public interface Callback {
        void onConfigured(@Nullable TaskAlarmConfig config);
    }

    private final TaskAlarmConfig initialConfig;
    private final Callback callback;

    public TaskAlarmSettingsDialog(@Nullable TaskAlarmConfig initialConfig, Callback callback) {
        this.initialConfig = initialConfig;
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LinearLayout root = TaskUi.vertical(requireContext(), 20);
        root.addView(TaskUi.text(requireContext(), "新建闹钟", 25,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        root.addView(TaskUi.text(requireContext(), "选择提醒日期与时间，并设置重复、铃声和再响规则。",
                13, requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        root.addView(TaskUi.spacer(requireContext(), 12));

        Calendar selected = Calendar.getInstance();
        if (initialConfig != null && initialConfig.triggerAtMillis > 0) {
            selected.setTimeInMillis(initialConfig.triggerAtMillis);
        } else {
            selected.add(Calendar.HOUR_OF_DAY, 1);
            selected.set(Calendar.MINUTE, 0);
        }

        MaterialButton dateButton = TaskUi.button(requireContext(), "", false);
        updateDateLabel(dateButton, selected);
        dateButton.setOnClickListener(v -> new DatePickerDialog(requireContext(),
                (picker, year, month, day) -> {
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, day);
                    updateDateLabel(dateButton, selected);
                },
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)).show());
        root.addView(dateButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, TaskUi.dp(requireContext(), 52)));
        root.addView(TaskUi.spacer(requireContext(), 8));

        LinearLayout timeRow = TaskUi.horizontal(requireContext());
        timeRow.setGravity(android.view.Gravity.CENTER);
        NumberPicker hour = numberPicker(0, 23, selected.get(Calendar.HOUR_OF_DAY));
        NumberPicker minute = numberPicker(0, 59, selected.get(Calendar.MINUTE));
        minute.setFormatter(value -> String.format(java.util.Locale.getDefault(), "%02d", value));
        hour.setFormatter(value -> String.format(java.util.Locale.getDefault(), "%02d", value));
        TextView separator = TaskUi.text(requireContext(), ":", 28,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD);
        separator.setGravity(android.view.Gravity.CENTER);
        timeRow.addView(hour, new LinearLayout.LayoutParams(
                TaskUi.dp(requireContext(), 110), TaskUi.dp(requireContext(), 150)));
        timeRow.addView(separator, new LinearLayout.LayoutParams(
                TaskUi.dp(requireContext(), 36), TaskUi.dp(requireContext(), 150)));
        timeRow.addView(minute, new LinearLayout.LayoutParams(
                TaskUi.dp(requireContext(), 110), TaskUi.dp(requireContext(), 150)));
        root.addView(timeRow);

        Spinner repeat = settingSpinner(root, "重复",
                new String[]{"只响一次", "每天", "工作日", "每周"},
                repeatPosition(initialConfig == null ? null : initialConfig.repeatMode));
        Spinner ringtone = settingSpinner(root, "闹钟铃声",
                new String[]{"系统闹钟", "系统通知音", "系统来电铃声"},
                ringtonePosition(initialConfig == null ? null : initialConfig.ringtone));
        Spinner duration = settingSpinner(root, "响铃时长",
                new String[]{"1 分钟", "3 分钟", "5 分钟", "10 分钟"},
                durationPosition(initialConfig == null ? 5 : initialConfig.ringDurationMinutes));
        Spinner snooze = settingSpinner(root, "再响间隔",
                new String[]{"5 分钟，3 次", "10 分钟，3 次", "15 分钟，2 次", "不再提醒"},
                snoozePosition(initialConfig));

        MaterialButton remove = TaskUi.button(requireContext(), "关闭该任务闹钟", false);
        root.addView(TaskUi.spacer(requireContext(), 10));
        root.addView(remove);

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.addView(root);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                Calendar result = Calendar.getInstance();
                result.set(selected.get(Calendar.YEAR), selected.get(Calendar.MONTH),
                        selected.get(Calendar.DAY_OF_MONTH),
                        hour.getValue(), minute.getValue(), 0);
                result.set(Calendar.MILLISECOND, 0);
                if (result.getTimeInMillis() < System.currentTimeMillis() + 60_000L) {
                    android.widget.Toast.makeText(requireContext(),
                            "闹钟时间需至少晚于当前 1 分钟", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                TaskAlarmConfig config = new TaskAlarmConfig();
                config.triggerAtMillis = result.getTimeInMillis();
                config.repeatMode = repeatValue(repeat.getSelectedItemPosition());
                config.ringtone = ringtoneValue(ringtone.getSelectedItemPosition());
                config.ringDurationMinutes = durationValue(duration.getSelectedItemPosition());
                applySnooze(config, snooze.getSelectedItemPosition());
                callback.onConfigured(config);
                dialog.dismiss();
            });
            remove.setOnClickListener(v -> {
                callback.onConfigured(null);
                dialog.dismiss();
            });
        });
        return dialog;
    }

    private void updateDateLabel(MaterialButton button, Calendar date) {
        String formatted = new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.getDefault())
                .format(date.getTime());
        button.setText("提醒日期  " + formatted + "  ›");
    }

    private NumberPicker numberPicker(int min, int max, int value) {
        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(value);
        picker.setWrapSelectorWheel(true);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        return picker;
    }

    private Spinner settingSpinner(LinearLayout root, String label, String[] values, int position) {
        root.addView(TaskUi.text(requireContext(), label, 14,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        Spinner spinner = new Spinner(requireContext());
        spinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, values));
        spinner.setSelection(position);
        root.addView(spinner);
        root.addView(TaskUi.spacer(requireContext(), 8));
        return spinner;
    }

    private int repeatPosition(String value) {
        if (TaskAlarmConfig.REPEAT_DAILY.equals(value)) return 1;
        if (TaskAlarmConfig.REPEAT_WEEKDAYS.equals(value)) return 2;
        if (TaskAlarmConfig.REPEAT_WEEKLY.equals(value)) return 3;
        return 0;
    }

    private String repeatValue(int position) {
        if (position == 1) return TaskAlarmConfig.REPEAT_DAILY;
        if (position == 2) return TaskAlarmConfig.REPEAT_WEEKDAYS;
        if (position == 3) return TaskAlarmConfig.REPEAT_WEEKLY;
        return TaskAlarmConfig.REPEAT_ONCE;
    }

    private int ringtonePosition(String value) {
        if ("NOTIFICATION".equals(value)) return 1;
        if ("RINGTONE".equals(value)) return 2;
        return 0;
    }

    private String ringtoneValue(int position) {
        if (position == 1) return "NOTIFICATION";
        if (position == 2) return "RINGTONE";
        return "ALARM";
    }

    private int durationPosition(int value) {
        if (value == 1) return 0;
        if (value == 3) return 1;
        if (value == 10) return 3;
        return 2;
    }

    private int durationValue(int position) {
        return new int[]{1, 3, 5, 10}[Math.max(0, Math.min(3, position))];
    }

    private int snoozePosition(TaskAlarmConfig config) {
        if (config == null) return 0;
        if (config.snoozeCount == 0) return 3;
        if (config.snoozeMinutes == 10) return 1;
        if (config.snoozeMinutes == 15) return 2;
        return 0;
    }

    private void applySnooze(TaskAlarmConfig config, int position) {
        if (position == 1) {
            config.snoozeMinutes = 10;
            config.snoozeCount = 3;
        } else if (position == 2) {
            config.snoozeMinutes = 15;
            config.snoozeCount = 2;
        } else if (position == 3) {
            config.snoozeMinutes = 0;
            config.snoozeCount = 0;
        } else {
            config.snoozeMinutes = 5;
            config.snoozeCount = 3;
        }
    }
}
