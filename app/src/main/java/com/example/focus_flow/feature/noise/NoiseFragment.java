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
import androidx.navigation.fragment.NavHostFragment;

import com.example.focus_flow.R;
import com.example.focus_flow.audio.NoisePlaybackController;
import com.example.focus_flow.core.model.DistractionReason;
import com.example.focus_flow.core.model.TimeSegment;
import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.NoiseMixItemRecord;
import com.example.focus_flow.data.local.model.NoiseMixRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.domain.assistant.AiPromptBuilder;
import com.example.focus_flow.domain.assistant.AiResponseParser;
import com.example.focus_flow.domain.assistant.NoiseRecommendation;
import com.example.focus_flow.domain.assistant.NoiseRecommendationEngine;
import com.example.focus_flow.domain.assistant.NoiseSetting;
import com.example.focus_flow.feature.assistant.AiProxyClient;
import com.example.focus_flow.feature.assistant.AiUiTransitions;
import com.example.focus_flow.feature.tasks.TaskUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NoiseFragment extends Fragment {
    private LinearLayout content;
    private RepositoryProvider provider;
    private final Map<NoiseType, Integer> volumes = new EnumMap<>(NoiseType.class);
    private final Map<NoiseType, Boolean> enabled = new EnumMap<>(NoiseType.class);
    private final AiResponseParser aiResponseParser = new AiResponseParser();
    private final AiProxyClient aiProxyClient = new AiProxyClient();
    private LinearLayout smartSuggestionContainer;
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
        addHeader();
        addMasterControls();
        addSmartSuggestions();
        addPresetMixes();
        addSoundConsole();
    }

    private void addHeader() {
        content.addView(TaskUi.backHeader(requireContext(), R.id.noise_button_back_home,
                "白噪音控制台", "本地多音源混音，离线循环播放。", v -> navigateBack()));
        content.addView(TaskUi.spacer(requireContext(), 16));
    }

    private void navigateBack() {
        androidx.navigation.NavController navController = NavHostFragment.findNavController(this);
        if (!navController.navigateUp()) {
            navController.navigate(R.id.homeFragment);
        }
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

    private void addSmartSuggestions() {
        content.addView(TaskUi.spacer(requireContext(), 8));
        content.addView(TaskUi.text(requireContext(), "智能白噪音推荐", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        smartSuggestionContainer = TaskUi.vertical(requireContext(), 14);
        smartSuggestionContainer.setId(R.id.noise_smart_suggestions);
        content.addView(smartSuggestionContainer);

        NoiseRecommendationEngine.Context localContext = buildLocalNoiseContext();
        List<NoiseRecommendation> local = new NoiseRecommendationEngine().recommend(localContext);
        for (int i = 0; i < Math.min(3, local.size()); i++) {
            smartSuggestionContainer.addView(noiseRecommendationCard(local.get(i), i));
        }
        requestApiNoiseSuggestions(localContext);
    }

    private NoiseRecommendationEngine.Context buildLocalNoiseContext() {
        NoiseRecommendationEngine.Context context = new NoiseRecommendationEngine.Context();
        context.currentSegment = TimeSegment.fromMillis(System.currentTimeMillis());
        List<FocusSessionRecord> recent = provider.focusSessionRepository.getRecentSessions(20);
        if (!recent.isEmpty()) {
            FocusSessionRecord latest = recent.get(0);
            context.subject = latest.subjectSnapshot == null ? "" : latest.subjectSnapshot;
            context.difficulty = latest.difficultySnapshot;
        }
        context.topDistractionReason = topDistractionReason(recent);
        List<NoiseType> preferred = new ArrayList<>();
        for (NoiseType type : NoiseType.values()) {
            if (Boolean.TRUE.equals(enabled.get(type)) && volumeFor(type) > 0) {
                preferred.add(type);
            }
        }
        context.recentPreferredTypes = preferred;
        return context;
    }

    private void requestApiNoiseSuggestions(NoiseRecommendationEngine.Context localContext) {
        AiPromptBuilder.NoiseAdviceContext context = new AiPromptBuilder.NoiseAdviceContext();
        context.subject = localContext.subject;
        context.difficulty = localContext.difficulty;
        context.currentSegment = localContext.currentSegment;
        context.recentPreferredTypes = localContext.recentPreferredTypes;
        String prompt = new AiPromptBuilder().buildNoiseAdvicePrompt(context);
        aiProxyClient.chat(prompt, new AiProxyClient.Callback() {
            @Override
            public void onSuccess(String responseBody) {
                if (!isAdded() || smartSuggestionContainer == null) {
                    return;
                }
                List<NoiseRecommendation> api = aiResponseParser.parseNoiseRecommendations(responseBody);
                if (api.isEmpty()) {
                    return;
                }
                AiUiTransitions.crossFadeChildren(smartSuggestionContainer, () -> {
                    for (int i = 0; i < Math.min(2, api.size()); i++) {
                        smartSuggestionContainer.addView(noiseRecommendationCard(api.get(i), i + 3));
                    }
                }, smartSuggestionContainer.getChildCount() > 0);
            }

            @Override
            public void onError(Exception error) {
                // Keep local sound recommendations when the proxy times out or returns invalid data.
            }
        });
    }

    private MaterialCardView noiseRecommendationCard(NoiseRecommendation recommendation, int index) {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 14);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), recommendation.title, 16,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(), recommendation.reason + "\n" + describeSettings(recommendation),
                13, requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        MaterialButton apply = TaskUi.button(requireContext(), "应用推荐", true);
        if (index == 0) {
            apply.setId(R.id.noise_smart_apply_1);
        }
        apply.setOnClickListener(v -> applyNoiseRecommendation(recommendation));
        body.addView(apply);
        return card;
    }

    private void applyNoiseRecommendation(NoiseRecommendation recommendation) {
        for (NoiseType type : NoiseType.values()) {
            volumes.put(type, 0);
            enabled.put(type, false);
        }
        for (NoiseSetting setting : recommendation.settings) {
            volumes.put(setting.type, setting.volumePercent);
            enabled.put(setting.type, setting.enabled && setting.volumePercent > 0);
        }
        applyCurrentLevels();
        Toast.makeText(requireContext(), "已应用智能白噪音", Toast.LENGTH_SHORT).show();
        render();
    }

    private String describeSettings(NoiseRecommendation recommendation) {
        StringBuilder builder = new StringBuilder();
        for (NoiseSetting setting : recommendation.settings) {
            if (!setting.enabled || setting.volumePercent <= 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(setting.type.displayName).append(' ').append(setting.volumePercent).append('%');
        }
        return builder.toString();
    }

    private DistractionReason topDistractionReason(List<FocusSessionRecord> sessions) {
        Map<DistractionReason, Integer> counts = new EnumMap<>(DistractionReason.class);
        for (FocusSessionRecord session : sessions) {
            if (session.distractionReason == null || session.distractionReason == DistractionReason.NONE) {
                continue;
            }
            Integer value = counts.get(session.distractionReason);
            counts.put(session.distractionReason, value == null ? 1 : value + 1);
        }
        DistractionReason top = DistractionReason.NONE;
        int best = 0;
        for (Map.Entry<DistractionReason, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                top = entry.getKey();
            }
        }
        return top;
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
