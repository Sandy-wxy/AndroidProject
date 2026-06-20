package com.example.focus_flow.data.local.model;

import com.example.focus_flow.core.model.ColorTag;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.core.model.TaskStatus;

public class TaskRecord {
    public long id;
    public String title;
    public String subject;
    public String targetOutcome;
    public String description;
    public TaskDifficulty difficulty;
    public TaskPriority priority;
    public int estimatedTotalMinutes;
    public String plannedDate;
    public Long deadlineAt;
    public ColorTag colorTag;
    public boolean autoSplitEnabled;
    public TaskStatus status;
    public boolean isDeleted;
    public long createdAt;
    public long updatedAt;
    public Long completedAt;
    public Long deletedAt;

    public TaskRecord() {
        description = "";
        difficulty = TaskDifficulty.NORMAL;
        priority = TaskPriority.NORMAL;
        colorTag = ColorTag.CYAN;
        autoSplitEnabled = true;
        status = TaskStatus.PENDING;
    }
}
