package com.example.focus_flow.data.local.model;

import com.example.focus_flow.core.model.FocusBlockStatus;
import com.example.focus_flow.core.model.RecommendationConfidence;

public class FocusBlockRecord {
    public long id;
    public long taskId;
    public int sequenceIndex;
    public int plannedFocusMinutes;
    public int plannedBreakMinutes;
    public FocusBlockStatus status;
    public RecommendationConfidence recommendationConfidence;
    public String recommendationReasons;
    public long createdAt;
    public long updatedAt;
    public Long completedAt;

    public FocusBlockRecord() {
        status = FocusBlockStatus.PENDING;
        recommendationConfidence = RecommendationConfidence.LOW;
        recommendationReasons = "";
    }
}
