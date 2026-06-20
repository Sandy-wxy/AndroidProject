package com.example.focus_flow.feature.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;

import com.example.focus_flow.R;
import com.example.focus_flow.audio.NoisePlaybackController;
import com.example.focus_flow.core.model.ThemeMode;
import com.example.focus_flow.core.ui.ThemeApplier;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.feature.tasks.TaskUi;
import com.example.focus_flow.feature.reminder.TaskReminderScheduler;
import com.example.focus_flow.service.focus.FocusTimerService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {
    private RepositoryProvider provider;
    private LinearLayout content;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        provider = RepositoryProvider.get(requireContext());
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        content = TaskUi.vertical(requireContext(), 22);
        scrollView.addView(content);
        render();
        return scrollView;
    }

    private void render() {
        content.removeAllViews();
        addHeader();
        content.addView(TaskUi.spacer(requireContext(), 16));
        addAppearanceCard();
        addNoiseCard();
        addDataCard();
        addAboutCard();
    }

    private void addHeader() {
        LinearLayout row = TaskUi.horizontal(requireContext());
        AppCompatImageButton back = TaskUi.iconButton(requireContext(), R.drawable.ic_arrow_back);
        back.setId(R.id.settings_button_back);
        back.setContentDescription("返回");
        back.setOnClickListener(v -> requireActivity().finish());
        row.addView(back, new LinearLayout.LayoutParams(
                TaskUi.dp(requireContext(), 52), TaskUi.dp(requireContext(), 52)));

        LinearLayout titleBlock = TaskUi.vertical(requireContext(), 0);
        titleBlock.addView(TaskUi.text(requireContext(), "设置", 30,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        titleBlock.addView(TaskUi.text(requireContext(), "主题、通知、声音偏好和本地数据。", 14,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        titleParams.setMarginStart(TaskUi.dp(requireContext(), 14));
        row.addView(titleBlock, titleParams);
        content.addView(row);
    }

    private void addAppearanceCard() {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 18);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "外观", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        Spinner theme = new Spinner(requireContext());
        theme.setId(R.id.settings_theme_spinner);
        theme.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                new String[]{"跟随系统", "浅色", "深色"}));
        theme.setSelection(positionForTheme(provider.settingsRepository.getThemeMode()));
        body.addView(theme);
        MaterialButton apply = TaskUi.button(requireContext(), "应用主题", true);
        body.addView(apply);
        content.addView(card);

        apply.setOnClickListener(v -> {
            ThemeMode mode = themeFromPosition(theme.getSelectedItemPosition());
            provider.settingsRepository.setThemeMode(mode);
            ThemeApplier.apply(mode);
            Toast.makeText(requireContext(), "主题已应用", Toast.LENGTH_SHORT).show();
        });
    }

    private void addNoiseCard() {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 18);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "专注与声音", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        SwitchMaterial notification = new SwitchMaterial(requireContext());
        notification.setId(R.id.settings_notification_switch);
        notification.setText("启用专注通知");
        notification.setTextColor(requireContext().getColor(R.color.text_primary));
        notification.setChecked(provider.settingsRepository.isNotificationEnabled());
        body.addView(notification);
        SwitchMaterial autoStop = new SwitchMaterial(requireContext());
        autoStop.setId(R.id.settings_auto_stop_switch);
        autoStop.setText("离开白噪音页自动停止");
        autoStop.setTextColor(requireContext().getColor(R.color.text_primary));
        autoStop.setChecked(provider.settingsRepository.isAutoStopNoiseEnabled());
        body.addView(autoStop);
        body.addView(TaskUi.text(requireContext(), "默认总音量 " + provider.settingsRepository.getMasterVolume() + "%", 14,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.BOLD));
        SeekBar volume = new SeekBar(requireContext());
        volume.setId(R.id.settings_master_volume);
        volume.setMax(100);
        volume.setProgress(provider.settingsRepository.getMasterVolume());
        body.addView(volume);
        content.addView(card);

        notification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            provider.settingsRepository.setNotificationEnabled(isChecked);
            if (isChecked) {
                requestNotificationPermissionIfNeeded();
            }
        });
        autoStop.setOnCheckedChangeListener((buttonView, isChecked) ->
                provider.settingsRepository.setAutoStopNoiseEnabled(isChecked));
        volume.setOnSeekBarChangeListener(new SimpleSeekBarListener(progress ->
                provider.settingsRepository.setMasterVolume(progress)));
    }

    private void addDataCard() {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 18);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "本地数据", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(), "任务、番茄钟和统计只保存在本机 SQLite 中。", 14,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        MaterialButton clear = TaskUi.button(requireContext(), "清空任务和专注记录", false);
        clear.setId(R.id.settings_clear_data_button);
        body.addView(clear);
        content.addView(card);

        clear.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("清空本地学习数据")
                .setMessage("任务、拆分番茄钟和专注记录会被清空，白噪音预设和设置会保留。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认清空", (dialog, which) -> {
                    NoisePlaybackController.get().stopAll();
                    requireContext().stopService(new Intent(requireContext(), FocusTimerService.class));
                    provider.clearLearningData();
                    TaskReminderScheduler.cancelAll(requireContext());
                    requireContext().getSharedPreferences("focus_flow_focus_start", Context.MODE_PRIVATE).edit().clear().apply();
                    requireContext().getSharedPreferences("focus_flow_timer_state", Context.MODE_PRIVATE).edit().clear().apply();
                    Toast.makeText(requireContext(), "已清空学习数据", Toast.LENGTH_SHORT).show();
                })
                .show());
    }

    private void addAboutCard() {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 18);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "Focus_Flow", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(), "本地规则驱动的离线专注工具。账户与学习记录仅保存在本机，不进行云同步。", 14,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        content.addView(card);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 3201);
        }
    }

    private int positionForTheme(ThemeMode mode) {
        if (mode == ThemeMode.LIGHT) {
            return 1;
        }
        if (mode == ThemeMode.DARK) {
            return 2;
        }
        return 0;
    }

    private ThemeMode themeFromPosition(int position) {
        if (position == 1) {
            return ThemeMode.LIGHT;
        }
        if (position == 2) {
            return ThemeMode.DARK;
        }
        return ThemeMode.FOLLOW_SYSTEM;
    }

    private static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        interface Callback {
            void onProgress(int progress);
        }

        private final Callback callback;

        SimpleSeekBarListener(Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                callback.onProgress(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
