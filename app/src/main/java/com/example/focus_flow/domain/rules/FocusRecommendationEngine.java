package com.example.focus_flow.domain.rules;

import com.example.focus_flow.core.model.RecommendationConfidence;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.domain.stats.RecentStats;

import java.util.ArrayList;
import java.util.List;

public class FocusRecommendationEngine {
    private static final int[] ALLOWED_FOCUS = {10, 15, 20, 25, 30, 35, 40, 45, 50, 60};
    private static final int HOUR_MILLIS = 60 * 60 * 1000;

    public Recommendation recommend(TaskRecord task, RecentStats stats, double effectiveProgressMinutes, long nowMillis) {
        int focus = baseByDifficulty(task.difficulty);
        List<String> reasons = new ArrayList<>();
        reasons.add("BASE_" + task.difficulty.name());

        if (stats.recentTotal >= 5 && stats.completionRate >= 0.85
                && stats.averageQuality >= 4.0 && stats.abandonmentRate <= 0.10) {
            focus += 10;
            reasons.add("R01_HIGH_STABILITY");
        }
        if (stats.recentTotal >= 5 && stats.completionRate >= 0.70 && stats.averageQuality >= 3.5) {
            focus += 5;
            reasons.add("R02_STABLE");
        }
        if (stats.recentTotal >= 5 && stats.abandonmentRate >= 0.35) {
            focus -= 10;
            reasons.add("R03_HIGH_ABANDON");
        } else if (stats.recentTotal >= 5 && stats.abandonmentRate >= 0.20) {
            focus -= 5;
            reasons.add("R04_MEDIUM_ABANDON");
        }
        if (stats.recentTotal >= 3 && stats.averageQuality <= 2.5) {
            focus -= 10;
            reasons.add("R05_LOW_QUALITY");
        }
        if (stats.recentTotal >= 3 && stats.averagePauseCount >= 3.0) {
            focus -= 5;
            reasons.add("R06_MANY_PAUSES");
        }
        if (stats.consecutiveCompletedSessions >= 3) {
            focus += 5;
            reasons.add("R07_CONSECUTIVE_SUCCESS");
        }
        if (stats.timeSegmentSampleCount >= 3 && stats.timeSegmentSuccessRate >= 0.80
                && stats.timeSegmentQualityAvg >= 4.0) {
            focus += 5;
            reasons.add("R08_SEGMENT_STRONG");
        }
        if (stats.timeSegmentSampleCount >= 3 && stats.timeSegmentSuccessRate <= 0.45) {
            focus -= 5;
            reasons.add("R09_SEGMENT_WEAK");
        }
        if (task.priority == TaskPriority.HIGH) {
            focus += 5;
            reasons.add("R10_PRIORITY_HIGH");
        } else if (task.priority == TaskPriority.URGENT) {
            focus += 10;
            reasons.add("R11_PRIORITY_URGENT");
        } else if (task.priority == TaskPriority.LOW) {
            focus -= 5;
            reasons.add("R12_PRIORITY_LOW");
        }
        if (task.deadlineAt != null) {
            long millisToDeadline = task.deadlineAt - nowMillis;
            if (millisToDeadline > 6L * HOUR_MILLIS && millisToDeadline <= 24L * HOUR_MILLIS) {
                focus += 5;
                reasons.add("R13_DEADLINE_24H");
            } else if (millisToDeadline <= 6L * HOUR_MILLIS) {
                if (stats.completionRate >= 0.65) {
                    focus += 5;
                    reasons.add("R14_DEADLINE_6H_STABLE");
                } else {
                    focus -= 5;
                    reasons.add("R15_DEADLINE_6H_UNSTABLE");
                }
            }
        }
        if (stats.todayFocusMinutes >= 240) {
            focus -= 10;
            reasons.add("R17_TODAY_FATIGUE_HEAVY");
        } else if (stats.todayFocusMinutes >= 180) {
            focus -= 5;
            reasons.add("R16_TODAY_FATIGUE_LIGHT");
        }
        if (stats.lastSessionQuality != null && stats.lastSessionQuality <= 2
                && stats.minutesSinceLastSession != null && stats.minutesSinceLastSession <= 60) {
            focus -= 5;
            reasons.add("R18_LAST_SESSION_BAD");
        }

        int remaining = remainingMinutes(task, effectiveProgressMinutes);
        if (remaining <= 20 && remaining > 0) {
            focus = remaining;
            reasons.add("REMAINING_SMALL");
        }

        focus = roundToAllowed(clamp(focus, 10, 60));
        int shortBreak = calculateShortBreak(focus, stats);
        int longBreak = calculateLongBreak(focus, stats);
        int cycle = calculateLongBreakCycle(focus, task, stats);
        RecommendationConfidence confidence = stats.recentTotal >= 10
                ? RecommendationConfidence.HIGH
                : stats.recentTotal >= 3 ? RecommendationConfidence.MEDIUM : RecommendationConfidence.LOW;
        if (confidence == RecommendationConfidence.LOW) {
            reasons.add("LOW_HISTORY_DEFAULT");
        }
        return new Recommendation(focus, shortBreak, longBreak, cycle, confidence, reasons);
    }

    public int roundToAllowed(int value) {
        int best = ALLOWED_FOCUS[0];
        int bestDistance = Math.abs(value - best);
        for (int allowed : ALLOWED_FOCUS) {
            int distance = Math.abs(value - allowed);
            if (distance < bestDistance || (distance == bestDistance && allowed < best)) {
                best = allowed;
                bestDistance = distance;
            }
        }
        return best;
    }

    public String reasonText(String code) {
        switch (code) {
            case "BASE_EASY":
                return "任务难度较低，推荐标准短专注。";
            case "BASE_NORMAL":
                return "任务难度普通，推荐中等专注时长。";
            case "BASE_HARD":
                return "任务难度较高，推荐更完整的沉浸时间。";
            case "BASE_EXTREME":
                return "任务压力较大，推荐可控的深度专注块。";
            case "R01_HIGH_STABILITY":
                return "你近期完成率和专注质量都很稳定。";
            case "R03_HIGH_ABANDON":
                return "你近期中途结束较多，本次推荐降低时长。";
            case "R05_LOW_QUALITY":
                return "你近期主观专注质量偏低，先从更轻量的节奏开始。";
            case "R08_SEGMENT_STRONG":
                return "当前时间段是你的高效时段。";
            case "R16_TODAY_FATIGUE_LIGHT":
                return "今天已经学习较久，适当缩短单次专注。";
            case "R17_TODAY_FATIGUE_HEAVY":
                return "今天学习负荷较高，本次采用保护性时长。";
            case "LOW_HISTORY_DEFAULT":
                return "历史数据较少，使用默认节奏。";
            default:
                return "";
        }
    }

    private int baseByDifficulty(TaskDifficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 25;
            case HARD:
                return 40;
            case EXTREME:
                return 35;
            case NORMAL:
            default:
                return 30;
        }
    }

    private int calculateShortBreak(int focusMinutes, RecentStats stats) {
        int base;
        if (focusMinutes <= 20) {
            base = 3;
        } else if (focusMinutes <= 30) {
            base = 5;
        } else if (focusMinutes <= 45) {
            base = 8;
        } else {
            base = 10;
        }
        if (stats.averageQuality <= 2.5 || stats.todayFocusMinutes >= 180) {
            base += 2;
        }
        return clamp(base, 3, 12);
    }

    private int calculateLongBreak(int focusMinutes, RecentStats stats) {
        int base;
        if (focusMinutes <= 30) {
            base = 15;
        } else if (focusMinutes <= 45) {
            base = 20;
        } else {
            base = 25;
        }
        if (stats.todayFocusMinutes >= 240) {
            base += 5;
        }
        return clamp(base, 15, 30);
    }

    private int calculateLongBreakCycle(int focusMinutes, TaskRecord task, RecentStats stats) {
        if (stats.todayFocusMinutes >= 240 || focusMinutes >= 50
                || task.difficulty == TaskDifficulty.HARD || task.difficulty == TaskDifficulty.EXTREME) {
            return 3;
        }
        if (focusMinutes <= 30 && task.difficulty == TaskDifficulty.EASY && stats.completionRate >= 0.8) {
            return 5;
        }
        return 4;
    }

    private int remainingMinutes(TaskRecord task, double effectiveProgressMinutes) {
        return Math.max(0, (int) Math.ceil(task.estimatedTotalMinutes - effectiveProgressMinutes));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
