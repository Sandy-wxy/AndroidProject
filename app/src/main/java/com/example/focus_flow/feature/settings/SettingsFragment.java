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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;

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
import com.example.focus_flow.feature.sync.CloudSyncManager;
import com.example.focus_flow.feature.sync.CloudSyncPreferences;
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
        addCloudSyncCard();
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
        SwitchMaterial forestTab = new SwitchMaterial(requireContext());
        forestTab.setId(R.id.settings_forest_tab_switch);
        forestTab.setText("显示“种树”Tab");
        forestTab.setTextColor(requireContext().getColor(R.color.text_primary));
        forestTab.setChecked(provider.settingsRepository.isForestTabEnabled());
        body.addView(forestTab);
        body.addView(TaskUi.text(requireContext(),
                "关闭后底部导航将隐藏种树栏目，已获得的树木不会丢失。",
                12, requireContext().getColor(R.color.text_weak),
                android.graphics.Typeface.NORMAL));
        MaterialButton apply = TaskUi.button(requireContext(), "应用主题", true);
        body.addView(apply);
        content.addView(card);

        apply.setOnClickListener(v -> {
            ThemeMode mode = themeFromPosition(theme.getSelectedItemPosition());
            provider.settingsRepository.setThemeMode(mode);
            ThemeApplier.apply(mode);
            Toast.makeText(requireContext(), "主题已应用", Toast.LENGTH_SHORT).show();
        });
        forestTab.setOnCheckedChangeListener((buttonView, enabled) -> {
            provider.settingsRepository.setForestTabEnabled(enabled);
            Toast.makeText(requireContext(),
                    enabled ? "已显示种树 Tab" : "已隐藏种树 Tab",
                    Toast.LENGTH_SHORT).show();
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
        body.addView(TaskUi.text(requireContext(), "任务、番茄钟和统计默认保存在本机 SQLite 中，可按需同步到云端。", 14,
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

    private void addCloudSyncCard() {
        CloudSyncPreferences cloudPreferences = new CloudSyncPreferences(requireContext());
        CloudSyncManager cloudSync = new CloudSyncManager(requireContext());

        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 18);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "云备份与多端同步", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(),
                "支持 HTTPS/WebDAV。多台设备填写相同配置后，可同步任务、专注记录、森林和个人资料。",
                13, requireContext().getColor(R.color.text_secondary),
                android.graphics.Typeface.NORMAL));

        EditText url = cloudInput("云端文件地址（https://…/tomato-focus.json）",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setId(R.id.settings_cloud_url);
        url.setText(cloudPreferences.url());
        EditText username = cloudInput("云端账号", InputType.TYPE_CLASS_TEXT);
        username.setId(R.id.settings_cloud_username);
        username.setText(cloudPreferences.username());
        EditText password = cloudInput("应用密码",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setId(R.id.settings_cloud_password);
        password.setTransformationMethod(
                android.text.method.PasswordTransformationMethod.getInstance());
        password.setText(cloudPreferences.password());
        body.addView(url);
        body.addView(username);
        body.addView(password);

        SwitchMaterial autoSync = new SwitchMaterial(requireContext());
        autoSync.setId(R.id.settings_cloud_auto_sync);
        autoSync.setText("启动 App 时自动同步");
        autoSync.setTextColor(requireContext().getColor(R.color.text_primary));
        autoSync.setChecked(cloudPreferences.autoSync());
        body.addView(autoSync);

        TextView status = TaskUi.text(requireContext(),
                cloudPreferences.lastSyncAt() == 0
                        ? "尚未同步" : "最近同步：" + new java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                .format(new java.util.Date(cloudPreferences.lastSyncAt())),
                12, requireContext().getColor(R.color.text_weak),
                android.graphics.Typeface.NORMAL);
        body.addView(status);

        MaterialButton save = TaskUi.button(requireContext(), "保存云端配置", false);
        save.setId(R.id.settings_cloud_save);
        MaterialButton sync = TaskUi.button(requireContext(), "立即同步", true);
        sync.setId(R.id.settings_cloud_sync);
        MaterialButton backup = TaskUi.button(requireContext(), "仅备份本机到云端", false);
        backup.setId(R.id.settings_cloud_backup);
        MaterialButton restore = TaskUi.button(requireContext(), "从云端覆盖恢复", false);
        restore.setId(R.id.settings_cloud_restore);
        body.addView(save);
        body.addView(sync);
        body.addView(backup);
        body.addView(restore);
        body.addView(TaskUi.text(requireContext(),
                "同步采用较新数据优先；“覆盖恢复”会替换本机学习数据。账号和密码使用系统密钥加密保存。",
                12, requireContext().getColor(R.color.text_weak),
                android.graphics.Typeface.NORMAL));
        content.addView(card);

        save.setOnClickListener(v -> {
            String urlText = url.getText().toString().trim();
            String userText = username.getText().toString().trim();
            String passwordText = password.getText().toString();
            if (!urlText.startsWith("https://")) {
                url.setError("仅支持 HTTPS 地址");
                return;
            }
            if (userText.isEmpty()) {
                username.setError("请输入云端账号");
                return;
            }
            if (passwordText.isEmpty()) {
                password.setError("请输入应用密码");
                return;
            }
            try {
                cloudPreferences.save(urlText, userText, passwordText, autoSync.isChecked());
                Toast.makeText(requireContext(), "云端配置已安全保存", Toast.LENGTH_SHORT).show();
            } catch (IllegalStateException error) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        CloudSyncManager.Callback callback = new CloudSyncManager.Callback() {
            @Override
            public void onSuccess(String message) {
                setCloudControlsEnabled(true, save, sync, backup, restore);
                status.setText(message + " · "
                        + new java.text.SimpleDateFormat("MM-dd HH:mm",
                        java.util.Locale.getDefault()).format(new java.util.Date()));
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                setCloudControlsEnabled(true, save, sync, backup, restore);
                status.setText("同步失败：" + message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        };
        sync.setOnClickListener(v -> {
            setCloudControlsEnabled(false, save, sync, backup, restore);
            status.setText("正在比较云端与本机数据…");
            cloudSync.sync(callback);
        });
        backup.setOnClickListener(v -> {
            setCloudControlsEnabled(false, save, sync, backup, restore);
            status.setText("正在上传云端备份…");
            cloudSync.backup(callback);
        });
        restore.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("从云端覆盖恢复")
                .setMessage("本机任务、专注记录和森林数据将被云端备份替换。确定继续吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认恢复", (dialog, which) -> {
                    setCloudControlsEnabled(false, save, sync, backup, restore);
                    status.setText("正在下载并恢复云端数据…");
                    cloudSync.restore(callback);
                })
                .show());
    }

    private EditText cloudInput(String hint, int inputType) {
        EditText input = new EditText(requireContext());
        input.setHint(hint);
        input.setInputType(inputType);
        input.setSingleLine(true);
        input.setTextColor(requireContext().getColor(R.color.text_primary));
        input.setHintTextColor(requireContext().getColor(R.color.text_weak));
        return input;
    }

    private void setCloudControlsEnabled(boolean enabled, MaterialButton... buttons) {
        for (MaterialButton button : buttons) {
            button.setEnabled(enabled);
        }
    }

    private void addAboutCard() {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 18);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "番茄Focus", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(), "专注、任务、森林与可选云同步一体化的学习工具。", 14,
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
