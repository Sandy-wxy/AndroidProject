package com.example.focus_flow.domain.assistant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AssistantSuggestion {
    public enum Category {
        STUDY_PLAN,
        NOISE,
        TASK_REWRITE,
        STATS
    }

    public enum Source {
        LOCAL,
        API
    }

    public final String id;
    public final Category category;
    public final String title;
    public final String content;
    public final Source source;
    public final List<NoiseSetting> noiseSettings;

    public AssistantSuggestion(String id, Category category, String title, String content,
                               Source source, List<NoiseSetting> noiseSettings) {
        this.id = safe(id);
        this.category = category == null ? Category.STUDY_PLAN : category;
        this.title = safe(title);
        this.content = safe(content);
        this.source = source == null ? Source.LOCAL : source;
        this.noiseSettings = Collections.unmodifiableList(new ArrayList<>(
                noiseSettings == null ? Collections.emptyList() : noiseSettings));
    }

    public static AssistantSuggestion local(String id, Category category, String title, String content) {
        return new AssistantSuggestion(id, category, title, content, Source.LOCAL, Collections.emptyList());
    }

    public static AssistantSuggestion api(String id, Category category, String title, String content) {
        return new AssistantSuggestion(id, category, title, content, Source.API, Collections.emptyList());
    }

    public AssistantSuggestion withNoiseSettings(List<NoiseSetting> settings) {
        return new AssistantSuggestion(id, category, title, content, source, settings);
    }

    String dedupeKey() {
        return normalize(title) + "|" + normalize(content);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value) {
        return safe(value).replaceAll("\\s+", " ").toLowerCase();
    }
}
