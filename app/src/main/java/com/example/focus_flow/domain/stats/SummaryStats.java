package com.example.focus_flow.domain.stats;

public class SummaryStats {
    public final int totalFocusSeconds;
    public final int completedCount;
    public final int abandonedCount;
    public final int interruptedCount;
    public final double completionRate;
    public final double abandonmentRate;
    public final double averageQuality;
    public final double averagePauseCount;

    public SummaryStats(int totalFocusSeconds, int completedCount, int abandonedCount,
                        int interruptedCount, double completionRate, double abandonmentRate,
                        double averageQuality, double averagePauseCount) {
        this.totalFocusSeconds = totalFocusSeconds;
        this.completedCount = completedCount;
        this.abandonedCount = abandonedCount;
        this.interruptedCount = interruptedCount;
        this.completionRate = completionRate;
        this.abandonmentRate = abandonmentRate;
        this.averageQuality = averageQuality;
        this.averagePauseCount = averagePauseCount;
    }
}
