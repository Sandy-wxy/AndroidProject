package com.example.focus_flow.data.local.model;

import com.example.focus_flow.core.model.DistractionReason;
import com.example.focus_flow.core.model.EndReason;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;

public class FocusSessionRecord {
    public long id;
    public Long taskId;
    public Long blockId;
    public String taskTitleSnapshot;
    public String subjectSnapshot;
    public TaskDifficulty difficultySnapshot;
    public TaskPriority prioritySnapshot;
    public int plannedFocusMinutes;
    public int plannedBreakMinutes;
    public long startedAt;
    public Long endedAt;
    public int actualFocusSeconds;
    public int pausedSeconds;
    public int pauseCount;
    public FocusSessionStatus status;
    public EndReason endReason;
    public double progressRatio;
    public Integer qualityScore;
    public DistractionReason distractionReason;
    public String reflectionNote;
    public double effectiveProgressMinutes;
    public String noiseMixNameSnapshot;
    public long createdAt;

    public FocusSessionRecord() {
        difficultySnapshot = TaskDifficulty.NORMAL;
        prioritySnapshot = TaskPriority.NORMAL;
        actualFocusSeconds = 0;
        pausedSeconds = 0;
        pauseCount = 0;
        status = FocusSessionStatus.RUNNING;
        endReason = EndReason.NONE;
        progressRatio = 0;
        distractionReason = DistractionReason.NONE;
        reflectionNote = "";
        noiseMixNameSnapshot = "";
    }
}
