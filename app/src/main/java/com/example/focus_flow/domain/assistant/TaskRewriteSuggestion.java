package com.example.focus_flow.domain.assistant;

import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;

public class TaskRewriteSuggestion {
    public final String title;
    public final String subject;
    public final String targetOutcome;
    public final String description;
    public final int estimatedMinutes;
    public final TaskDifficulty difficulty;
    public final TaskPriority priority;
    public final AssistantSuggestion.Source source;

    public TaskRewriteSuggestion(String title, String subject, String targetOutcome,
                                 String description, int estimatedMinutes,
                                 TaskDifficulty difficulty, TaskPriority priority,
                                 AssistantSuggestion.Source source) {
        this.title = safe(title);
        this.subject = safe(subject);
        this.targetOutcome = safe(targetOutcome);
        this.description = safe(description);
        this.estimatedMinutes = Math.max(10, Math.min(600, estimatedMinutes));
        this.difficulty = difficulty == null ? TaskDifficulty.NORMAL : difficulty;
        this.priority = priority == null ? TaskPriority.NORMAL : priority;
        this.source = source == null ? AssistantSuggestion.Source.LOCAL : source;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
