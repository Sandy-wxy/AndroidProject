package com.example.focus_flow.domain.rules;

import com.example.focus_flow.core.model.AdviceSeverity;

public class Advice {
    public final String adviceId;
    public final String title;
    public final String content;
    public final int priority;
    public final AdviceSeverity severity;
    public final Long relatedTaskId;
    public final String actionText;

    public Advice(String adviceId, String title, String content, int priority,
                  AdviceSeverity severity, Long relatedTaskId, String actionText) {
        this.adviceId = adviceId;
        this.title = title;
        this.content = content;
        this.priority = priority;
        this.severity = severity;
        this.relatedTaskId = relatedTaskId;
        this.actionText = actionText;
    }
}
