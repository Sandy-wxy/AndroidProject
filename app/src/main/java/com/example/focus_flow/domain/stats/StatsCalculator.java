package com.example.focus_flow.domain.stats;

import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.core.model.DistractionReason;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.TimeSegment;
import com.example.focus_flow.data.local.model.FocusSessionRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsCalculator {
    public RecentStats calculateRecentStats(List<FocusSessionRecord> allSessions, long nowMillis) {
        List<FocusSessionRecord> recent = latestFinished(allSessions, 20, nowMillis - 14L * 24L * 60L * 60L * 1000L);
        SummaryStats summary = calculateSummary(recent);
        int consecutiveCompleted = 0;
        for (FocusSessionRecord session : recent) {
            if (session.status == FocusSessionStatus.COMPLETED) {
                consecutiveCompleted++;
            } else {
                break;
            }
        }
        int todayMinutes = totalSecondsBetween(allSessions, DateTimeUtils.startOfDayMillis(nowMillis),
                DateTimeUtils.endOfDayMillis(nowMillis)) / 60;
        int todayCount = countSessionsBetween(allSessions, DateTimeUtils.startOfDayMillis(nowMillis),
                DateTimeUtils.endOfDayMillis(nowMillis));
        TimeSegment segment = TimeSegment.fromMillis(nowMillis);
        TimeSegmentStats segmentStats = calculateTimeSegmentStats(allSessions, segment);
        Integer lastQuality = recent.isEmpty() ? null : recent.get(0).qualityScore;
        Integer minutesSinceLast = null;
        if (!recent.isEmpty()) {
            minutesSinceLast = (int) Math.max(0, (nowMillis - recent.get(0).startedAt) / 60000L);
        }
        return new RecentStats(
                recent.size(),
                recent.isEmpty() ? 0.70 : summary.completionRate,
                recent.isEmpty() ? 0.15 : summary.abandonmentRate,
                recent.isEmpty() ? 3.5 : summary.averageQuality,
                recent.isEmpty() ? 0.8 : summary.averagePauseCount,
                consecutiveCompleted,
                todayMinutes,
                todayCount,
                segment,
                segmentStats.sampleCount,
                segmentStats.sampleCount < 3 ? (recent.isEmpty() ? 0.70 : summary.completionRate) : segmentStats.successRate,
                segmentStats.sampleCount < 3 ? (recent.isEmpty() ? 3.5 : summary.averageQuality) : segmentStats.averageQuality,
                lastQuality,
                minutesSinceLast
        );
    }

    public SummaryStats calculateSummary(List<FocusSessionRecord> sessions) {
        int totalSeconds = 0;
        int completed = 0;
        int abandoned = 0;
        int interrupted = 0;
        int qualityCount = 0;
        int qualitySum = 0;
        int pauseSum = 0;
        for (FocusSessionRecord session : sessions) {
            totalSeconds += Math.max(0, session.actualFocusSeconds);
            pauseSum += Math.max(0, session.pauseCount);
            if (session.status == FocusSessionStatus.COMPLETED) {
                completed++;
            } else if (session.status == FocusSessionStatus.ABANDONED) {
                abandoned++;
            } else if (session.status == FocusSessionStatus.INTERRUPTED) {
                interrupted++;
            }
            if (session.qualityScore != null) {
                qualitySum += session.qualityScore;
                qualityCount++;
            }
        }
        int total = sessions.size();
        return new SummaryStats(
                totalSeconds,
                completed,
                abandoned,
                interrupted,
                total == 0 ? 0.70 : completed / (double) total,
                total == 0 ? 0.15 : abandoned / (double) total,
                qualityCount == 0 ? 3.5 : qualitySum / (double) qualityCount,
                total == 0 ? 0.8 : pauseSum / (double) total
        );
    }

    public TimeSegmentStats calculateTimeSegmentStats(List<FocusSessionRecord> sessions, TimeSegment segment) {
        int sampleCount = 0;
        int completed = 0;
        int qualitySum = 0;
        int qualityCount = 0;
        for (FocusSessionRecord session : sessions) {
            if (TimeSegment.fromMillis(session.startedAt) != segment) {
                continue;
            }
            sampleCount++;
            if (session.status == FocusSessionStatus.COMPLETED) {
                completed++;
            }
            if (session.qualityScore != null) {
                qualitySum += session.qualityScore;
                qualityCount++;
            }
        }
        double successRate = sampleCount == 0 ? 0.70 : completed / (double) sampleCount;
        double averageQuality = qualityCount == 0 ? 3.5 : qualitySum / (double) qualityCount;
        return new TimeSegmentStats(segment, sampleCount, successRate, averageQuality);
    }

    public Map<String, Integer> subjectSeconds(List<FocusSessionRecord> sessions) {
        Map<String, Integer> result = new HashMap<>();
        for (FocusSessionRecord session : sessions) {
            String subject = session.subjectSnapshot == null ? "" : session.subjectSnapshot;
            result.put(subject, result.containsKey(subject) ? result.get(subject) + session.actualFocusSeconds : session.actualFocusSeconds);
        }
        return result;
    }

    public Map<DistractionReason, Integer> distractionCounts(List<FocusSessionRecord> sessions) {
        Map<DistractionReason, Integer> result = new HashMap<>();
        for (FocusSessionRecord session : sessions) {
            DistractionReason reason = session.distractionReason == null ? DistractionReason.NONE : session.distractionReason;
            if (reason == DistractionReason.NONE) {
                continue;
            }
            result.put(reason, result.containsKey(reason) ? result.get(reason) + 1 : 1);
        }
        return result;
    }

    public int totalSecondsBetween(List<FocusSessionRecord> sessions, long startMillis, long endMillis) {
        int total = 0;
        for (FocusSessionRecord session : sessions) {
            if (session.startedAt >= startMillis && session.startedAt <= endMillis
                    && session.status != FocusSessionStatus.RUNNING) {
                total += Math.max(0, session.actualFocusSeconds);
            }
        }
        return total;
    }

    public int countSessionsBetween(List<FocusSessionRecord> sessions, long startMillis, long endMillis) {
        int total = 0;
        for (FocusSessionRecord session : sessions) {
            if (session.startedAt >= startMillis && session.startedAt <= endMillis
                    && session.status != FocusSessionStatus.RUNNING) {
                total++;
            }
        }
        return total;
    }

    private List<FocusSessionRecord> latestFinished(List<FocusSessionRecord> allSessions, int limit, long earliestMillis) {
        List<FocusSessionRecord> finished = new ArrayList<>();
        for (FocusSessionRecord session : allSessions) {
            if (session.status != FocusSessionStatus.RUNNING && session.startedAt >= earliestMillis) {
                finished.add(session);
            }
        }
        finished.sort((left, right) -> Long.compare(right.startedAt, left.startedAt));
        if (finished.size() > limit) {
            return new ArrayList<>(finished.subList(0, limit));
        }
        return finished;
    }
}
