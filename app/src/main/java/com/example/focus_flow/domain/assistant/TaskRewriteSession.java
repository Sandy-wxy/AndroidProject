package com.example.focus_flow.domain.assistant;

import com.example.focus_flow.core.model.TimeSegment;

import java.util.Collections;
import java.util.List;

public class TaskRewriteSession {
    private final TaskRewriteEngine engine;
    private String lastRequestedText;

    public TaskRewriteSession(TaskRewriteEngine engine) {
        this.engine = engine == null ? new TaskRewriteEngine() : engine;
    }

    public List<TaskRewriteSuggestion> requestOnce(String roughTask) {
        return requestOnce(roughTask, TimeSegment.fromMillis(System.currentTimeMillis()));
    }

    public List<TaskRewriteSuggestion> requestOnce(String roughTask, TimeSegment segment) {
        String text = normalize(roughTask);
        if (text.length() < 2 || text.equals(lastRequestedText)) {
            return Collections.emptyList();
        }
        lastRequestedText = text;
        return engine.localRewrites(text, segment);
    }

    public boolean hasRequested() {
        return lastRequestedText != null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}