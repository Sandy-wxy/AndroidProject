package com.example.focus_flow.domain.stats;

import com.example.focus_flow.core.model.TimeSegment;

public class TimeSegmentStats {
    public final TimeSegment segment;
    public final int sampleCount;
    public final double successRate;
    public final double averageQuality;

    public TimeSegmentStats(TimeSegment segment, int sampleCount, double successRate, double averageQuality) {
        this.segment = segment;
        this.sampleCount = sampleCount;
        this.successRate = successRate;
        this.averageQuality = averageQuality;
    }
}
