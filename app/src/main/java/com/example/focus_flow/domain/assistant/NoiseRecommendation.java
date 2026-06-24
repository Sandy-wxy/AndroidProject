package com.example.focus_flow.domain.assistant;

import com.example.focus_flow.core.model.NoiseType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoiseRecommendation {
    public final String id;
    public final String title;
    public final String reason;
    public final AssistantSuggestion.Source source;
    public final List<NoiseSetting> settings;

    public NoiseRecommendation(String id, String title, String reason,
                               AssistantSuggestion.Source source, List<NoiseSetting> settings) {
        this.id = id == null ? "" : id.trim();
        this.title = title == null ? "" : title.trim();
        this.reason = reason == null ? "" : reason.trim();
        this.source = source == null ? AssistantSuggestion.Source.LOCAL : source;
        this.settings = Collections.unmodifiableList(new ArrayList<>(
                settings == null ? Collections.emptyList() : settings));
    }

    public boolean contains(NoiseType type) {
        for (NoiseSetting setting : settings) {
            if (setting.enabled && setting.type == type && setting.volumePercent > 0) {
                return true;
            }
        }
        return false;
    }

    public String settingSignature() {
        StringBuilder builder = new StringBuilder();
        for (NoiseSetting setting : settings) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(setting.type.name()).append(':').append(setting.volumePercent);
        }
        return builder.toString();
    }

    public AssistantSuggestion toSuggestion() {
        return new AssistantSuggestion(id, AssistantSuggestion.Category.NOISE, title, reason, source, settings);
    }
}
