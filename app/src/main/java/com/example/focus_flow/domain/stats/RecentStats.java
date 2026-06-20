package com.example.focus_flow.domain.stats;

import com.example.focus_flow.core.model.TimeSegment;

public class RecentStats {
    public final int recentTotal;
    public final double completionRate;
    public final double abandonmentRate;
    public final double averageQuality;
    public final double averagePauseCount;
    public final int consecutiveCompletedSessions;
    public final int todayFocusMinutes;
    public final int todaySessionCount;
    public final TimeSegment currentTimeSegment;
    public final int timeSegmentSampleCount;
    public final double timeSegmentSuccessRate;
    public final double timeSegmentQualityAvg;
    public final Integer lastSessionQuality;
    public final Integer minutesSinceLastSession;

    public RecentStats(
            int recentTotal,
            double completionRate,
            double abandonmentRate,
            double averageQuality,
            double averagePauseCount,
            int consecutiveCompletedSessions,
            int todayFocusMinutes,
            int todaySessionCount,
            TimeSegment currentTimeSegment,
            int timeSegmentSampleCount,
            double timeSegmentSuccessRate,
            double timeSegmentQualityAvg,
            Integer lastSessionQuality,
            Integer minutesSinceLastSession) {
        this.recentTotal = recentTotal;
        this.completionRate = completionRate;
        this.abandonmentRate = abandonmentRate;
        this.averageQuality = averageQuality;
        this.averagePauseCount = averagePauseCount;
        this.consecutiveCompletedSessions = consecutiveCompletedSessions;
        this.todayFocusMinutes = todayFocusMinutes;
        this.todaySessionCount = todaySessionCount;
        this.currentTimeSegment = currentTimeSegment;
        this.timeSegmentSampleCount = timeSegmentSampleCount;
        this.timeSegmentSuccessRate = timeSegmentSuccessRate;
        this.timeSegmentQualityAvg = timeSegmentQualityAvg;
        this.lastSessionQuality = lastSessionQuality;
        this.minutesSinceLastSession = minutesSinceLastSession;
    }

    public static RecentStats defaults(TimeSegment segment) {
        return new RecentStats(0, 0.70, 0.15, 3.5, 0.8, 0, 0, 0,
                segment, 0, 0.70, 3.5, null, null);
    }
}
