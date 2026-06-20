package com.example.focus_flow.feature.noise;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.focus_flow.R;
import com.example.focus_flow.audio.NoisePlaybackController;
import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.data.local.model.NoiseMixItemRecord;
import com.example.focus_flow.data.local.model.NoiseMixRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.feature.tasks.TaskUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class NoiseFragment extends Fragment {
    private LinearLayout content;
    private RepositoryProvider provider;
    private final Map<NoiseType, Integer> volumes = new EnumMap<>(NoiseType.class);
    private final Map<NoiseType, Boolean> enabled = new EnumMap<>(NoiseType.class);
    private int customSaveCount = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        provider = RepositoryProvider.get(requireContext());
        provider.noiseMixRepository.restorePresetMixesIfMissing();
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        content = TaskUi.vertical(requireContext(), 22);
        scrollView.addView(content);
        loadCurrentLevels();
        render();
        return scrollView;
    }

    @Override
    public void onPause() {
        if (provider != null && provider.settingsRepository.isAutoStopNoiseEnabled()) {
            NoisePlaybackController.get().stopAll();
        }
        super.onPause();
    }

    private void render() {
        content.removeAllViews();
        content.addView(TaskUi.text(requireContext(), "白噪音控制台", 30,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        content.addView(TaskUi.text(requireContext(), "本地多音源混音，离线循环播放。", 14,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        content.addView(TaskUi.spacer(requireContext(), 16));
        addMasterControls();
        addPresetMixes();
        addSoundConsole();
    }

    private void addMasterControls() {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 18);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "播放控制", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        LinearLayout buttons = TaskUi.horizontal(requireContext());
        MaterialButton play = TaskUi.button(requireContext(), "播放当前混音", true);
        play.setId(R.id.noise_button_play_current);
        MaterialButton stop = TaskUi.button(requireContext(), "停止", false);
        stop.setId(R.id.noise_button_stop);
        buttons.addView(play, weighted());
        buttons.addView(stop, weightedWithMargin());
        body.addView(buttons);

        body.addView(TaskUi.text(requireContext(), "总音量 " + provider.settingsRepository.getMasterVolume() + "%", 14,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.BOLD));
        SeekBar master = new SeekBar(requireContext());
        master.setId(R.id.noise_master_volume);
        master.setMax(100);
        master.setProgress(provider.settingsRepository.getMasterVolume());
        body.addView(master);
        MaterialButton save = TaskUi.button(requireContext(), "保存为自定义混音", false);
        save.setId(R.id.noise_button_save_custom);
        body.addView(save);
        content.addView(card);

        play.setOnClickListener(v -> applyCurrentLevels());
        stop.setOnClickListener(v -> NoisePlaybackController.get().stopAll());
        master.setOnSeekBarChangeListener(new SimpleSeekBarListener(progress -> {
            provider.settingsRepository.setMasterVolume(progress);
            if (NoisePlaybackController.get().isPlaying()) {
                applyCurrentLevels();
            }
        }));
        save.setOnClickListener(v -> saveCustomMix());
    }

    private void addPresetMixes() {
        content.addView(TaskUi.spacer(requireContext(), 8));
        content.addView(TaskUi.text(requireContext(), "预设混音", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        List<NoiseMixRecord> mixes = provider.noiseMixRepository.getAllMixes();
        for (NoiseMixRecord mix : mixes) {
            if (mix.isPreset) {
                content.addView(mixCard(mix));
            }
        }
    }

    private MaterialCardView mixCard(NoiseMixRecord mix) {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 16);
        card.addView(body);
        LinearLayout row = TaskUi.horizontal(requireContext());
        LinearLayout texts = TaskUi.vertical(requireContext(), 0);
        texts.addView(TaskUi.text(requireContext(), mix.name, 16,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        texts.addView(TaskUi.text(requireContext(), describeMix(mix), 13,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        MaterialButton apply = TaskUi.button(requireContext(), "应用", true);
        row.addView(apply, new LinearLayout.LayoutParams(TaskUi.dp(requireContext(), 112), ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(row);
        apply.setOnClickListener(v -> {
            provider.noiseMixRepository.applyMix(mix.id);
            applyMixToLocalState(mix);
            NoisePlaybackController.get().applyMix(requireContext(), mix, provider.settingsRepository.getMasterVolume());
            Toast.makeText(requireContext(), "已应用 " + mix.name, Toast.LENGTH_SHORT).show();
            render();
        });
        return card;
    }

    private void addSoundConsole() {
        content.addView(TaskUi.spacer(requireContext(), 8));
        content.addView(TaskUi.text(requireContext(), "12 种声音", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        for (NoiseType type : NoiseType.values()) {
            content.addView(soundCard(type));
        }
    }

    private MaterialCardView soundCard(NoiseType type) {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 14);
        card.addView(body);
        LinearLayout row = TaskUi.horizontal(requireContext());
        CheckBox checkBox = new CheckBox(requireContext());
        checkBox.setChecked(Boolean.TRUE.equals(enabled.get(type)));
        row.addView(checkBox);
        TextView name = TaskUi.text(requireContext(), type.displayName, 16,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView percent = TaskUi.text(requireContext(), volumeFor(type) + "%", 13,
                requireContext().getColor(R.color.focus_cyan), android.graphics.Typeface.BOLD);
        percent.setGravity(Gravity.END);
        row.addView(percent, new LinearLayout.LayoutParams(TaskUi.dp(requireContext(), 70), ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(row);

        SeekBar volume = new SeekBar(requireContext());
        volume.setMax(100);
        volume.setProgress(volumeFor(type));
        body.addView(volume);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enabled.put(type, isChecked);
            if (NoisePlaybackController.get().isPlaying()) {
                applyCurrentLevels();
            }
        });
        volume.setOnSeekBarChangeListener(new SimpleSeekBarListener(progress -> {
            volumes.put(type, progress);
            percent.setText(progress + "%");
            if (progress > 0) {
                enabled.put(type, true);
                checkBox.setChecked(true);
            }
            if (NoisePlaybackController.get().isPlaying()) {
                applyCurrentLevels();
            }
        }));
        return card;
    }

    private void loadCurrentLevels() {
        for (NoiseType type : NoiseType.values()) {
            volumes.put(type, 0);
            enabled.put(type, false);
        }
        NoiseMixRecord current = provider.noiseMixRepository.getCurrentMix();
        if (current != null) {
            applyMixToLocalState(current);
        }
    }

    private void applyMixToLocalState(NoiseMixRecord mix) {
        for (NoiseType type : NoiseType.values()) {
            volumes.put(type, 0);
            enabled.put(type, false);
        }
        for (NoiseMixItemRecord item : mix.items) {
            volumes.put(item.noiseType, item.volumePercent);
            enabled.put(item.noiseType, item.enabled);
        }
    }

    private void applyCurrentLevels() {
        NoisePlaybackController.get().applyLevels(requireContext(), volumes, enabled,
                provider.settingsRepository.getMasterVolume());
    }

    private void saveCustomMix() {
        NoiseMixRecord mix = new NoiseMixRecord();
        mix.name = "自定义混音 " + customSaveCount++;
        for (NoiseType type : NoiseType.values()) {
            int volume = volumeFor(type);
            if (Boolean.TRUE.equals(enabled.get(type)) && volume > 0) {
                NoiseMixItemRecord item = new NoiseMixItemRecord();
                item.noiseType = type;
                item.volumePercent = volume;
                item.enabled = true;
                mix.items.add(item);
            }
        }
        if (mix.items.isEmpty()) {
            Toast.makeText(requireContext(), "至少开启一个声音", Toast.LENGTH_SHORT).show();
            return;
        }
        long id = provider.noiseMixRepository.saveCustomMix(mix);
        provider.noiseMixRepository.applyMix(id);
        Toast.makeText(requireContext(), "自定义混音已保存", Toast.LENGTH_SHORT).show();
        render();
    }

    private int volumeFor(NoiseType type) {
        Integer volume = volumes.get(type);
        return volume == null ? 0 : volume;
    }

    private String describeMix(NoiseMixRecord mix) {
        StringBuilder builder = new StringBuilder();
        for (NoiseMixItemRecord item : mix.items) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(item.noiseType.displayName).append(' ').append(item.volumePercent).append('%');
        }
        return builder.toString();
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }

    private LinearLayout.LayoutParams weightedWithMargin() {
        LinearLayout.LayoutParams params = weighted();
        params.setMarginStart(TaskUi.dp(requireContext(), 10));
        return params;
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
