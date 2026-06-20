package com.example.focus_flow.domain.rules;

import com.example.focus_flow.core.model.AdviceSeverity;
import com.example.focus_flow.core.model.DistractionReason;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.domain.stats.RecentStats;
import com.example.focus_flow.domain.stats.StatsCalculator;
import com.example.focus_flow.domain.stats.SummaryStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AdviceEngine {
    public List<Advice> generate(RecentStats stats, List<TaskRecord> tasks,
                                 List<FocusSessionRecord> recentSessions, long nowMillis,
                                 boolean homeLimit) {
        List<Advice> advices = new ArrayList<>();
        addAbandonAdvice(stats, advices);
        if (stats.recentTotal >= 3 && stats.averageQuality <= 2.5) {
            advices.add(advice("ADVICE_LOW_QUALITY", "降低单次压力",
                    "近期专注质量偏低，建议降低单次时长，并选择白噪音遮蔽干扰。",
                    88, AdviceSeverity.WARNING, null, "使用短专注"));
        }
        if (stats.averagePauseCount >= 3.0) {
            advices.add(advice("ADVICE_MANY_PAUSES", "减少暂停准备",
                    "暂停次数较多，建议开始前关闭消息提醒，并把水、纸笔等准备好。",
                    80, AdviceSeverity.WARNING, null, "开始前检查"));
        }
        if (stats.timeSegmentSampleCount >= 3 && stats.timeSegmentSuccessRate >= 0.80
                && stats.timeSegmentQualityAvg >= 4.0) {
            advices.add(advice("ADVICE_STRONG_MOMENT", "现在是高效时段",
                    "现在是你的高效时段，适合处理高优先级或高难度任务。",
                    70, AdviceSeverity.POSITIVE, null, "开始重点任务"));
        }
        if (stats.timeSegmentSampleCount >= 3 && stats.timeSegmentSuccessRate <= 0.45) {
            advices.add(advice("ADVICE_WEAK_MOMENT", "先做轻量启动",
                    "当前时段历史完成率较低，建议先做一个 15-20 分钟轻量任务。",
                    65, AdviceSeverity.INFO, null, "开始短专注"));
        }
        TaskRecord urgent = findDeadlineUrgent(tasks, nowMillis);
        if (urgent != null) {
            advices.add(advice("ADVICE_DEADLINE_URGENT", "优先推进截止任务",
                    "有任务即将截止，建议优先推进截止最近的任务。",
                    95, AdviceSeverity.WARNING, urgent.id, "查看任务"));
        }
        int overloadPriority = stats.todayFocusMinutes >= 240 ? 98 : 85;
        if (stats.todayFocusMinutes >= 240 || recentTodayQualityLow(recentSessions, nowMillis)) {
            advices.add(advice("ADVICE_OVERLOAD", "安排一次恢复",
                    "今天学习负荷较高，建议安排一次较长休息，下一轮不要超过 25 分钟。",
                    overloadPriority, AdviceSeverity.WARNING, null, "休息一下"));
        }
        if (noPlanToday(tasks, stats)) {
            advices.add(advice("ADVICE_NO_PLAN_TODAY", "添加一个小任务",
                    "今天还没有学习计划，可以先添加一个 25 分钟的小任务。",
                    60, AdviceSeverity.INFO, null, "添加任务"));
        }
        if (subjectImbalance(tasks, recentSessions)) {
            advices.add(advice("ADVICE_SUBJECT_IMBALANCE", "检查科目分布",
                    "本周学习时间集中在一个科目上，可以检查是否有其他任务被忽略。",
                    55, AdviceSeverity.INFO, null, "查看任务"));
        }
        if (estimateTooLow(tasks, recentSessions)) {
            advices.add(advice("ADVICE_ESTIMATE_TOO_LOW", "预估时间略偏低",
                    "你的任务预计时长可能偏低，新增任务时建议多预留 20%-30%。",
                    70, AdviceSeverity.INFO, null, "调整预估"));
        }
        if (stats.consecutiveCompletedSessions >= 3 && stats.todaySessionCount == 0 && isAfterSixPm(nowMillis)) {
            advices.add(advice("ADVICE_STREAK_PROTECT", "保护连续学习",
                    "连续学习记录正在保持，今晚可以完成一个 15 分钟短专注保护节奏。",
                    78, AdviceSeverity.POSITIVE, null, "短专注"));
        }
        Map<DistractionReason, Integer> counts = distractionCounts(recentSessions, nowMillis);
        if (countFor(counts, DistractionReason.TASK_UNCLEAR) >= 2) {
            advices.add(advice("ADVICE_TASK_UNCLEAR", "把目标写清楚",
                    "多次因为目标不清晰而分心，建议新增任务时把目标写成可验证结果。",
                    82, AdviceSeverity.WARNING, null, "编辑目标"));
        }
        if (countFor(counts, DistractionReason.TASK_TOO_HARD) >= 2) {
            advices.add(advice("ADVICE_TASK_TOO_HARD", "把困难任务切小",
                    "任务难度造成中断，建议把困难任务拆成多个 20-30 分钟小块。",
                    82, AdviceSeverity.WARNING, null, "拆小任务"));
        }
        if (stats.recentTotal >= 5 && stats.completionRate >= 0.85 && stats.averageQuality >= 4.0) {
            advices.add(advice("ADVICE_POSITIVE_STABLE", "状态很稳定",
                    "近期完成率和质量都很好，可以尝试更长的深度专注。",
                    50, AdviceSeverity.POSITIVE, null, "挑战长专注"));
        }
        return filterAndSort(advices, homeLimit);
    }

    private void addAbandonAdvice(RecentStats stats, List<Advice> advices) {
        if (stats.recentTotal >= 5 && stats.abandonmentRate >= 0.35) {
            advices.add(advice("ADVICE_HIGH_ABANDON", "先缩短一轮",
                    "最近中途结束偏多，建议下一次使用 15-20 分钟短专注，并在开始前把目标写得更小。",
                    90, AdviceSeverity.WARNING, null, "开始短专注"));
        } else if (stats.recentTotal >= 5 && stats.abandonmentRate >= 0.20) {
            advices.add(advice("ADVICE_MEDIUM_ABANDON", "恢复完成节奏",
                    "近期有一定中断，建议优先完成一个较短番茄钟恢复节奏。",
                    75, AdviceSeverity.WARNING, null, "开始短专注"));
        }
    }

    private List<Advice> filterAndSort(List<Advice> advices, boolean homeLimit) {
        Collections.sort(advices, Comparator.comparingInt((Advice advice) -> advice.priority).reversed());
        List<Advice> filtered = new ArrayList<>();
        Map<AdviceSeverity, Integer> severityCount = new EnumMap<>(AdviceSeverity.class);
        for (Advice advice : advices) {
            int count = severityCount.containsKey(advice.severity) ? severityCount.get(advice.severity) : 0;
            if (count >= 2) {
                continue;
            }
            filtered.add(advice);
            severityCount.put(advice.severity, count + 1);
            if (homeLimit && filtered.size() >= 4) {
                break;
            }
        }
        return filtered;
    }

    private Advice advice(String id, String title, String content, int priority,
                          AdviceSeverity severity, Long taskId, String actionText) {
        return new Advice(id, title, content, priority, severity, taskId, actionText);
    }

    private TaskRecord findDeadlineUrgent(List<TaskRecord> tasks, long nowMillis) {
        TaskRecord best = null;
        for (TaskRecord task : tasks) {
            if (task.deadlineAt == null || task.status == TaskStatus.COMPLETED || task.isDeleted) {
                continue;
            }
            long remainMillis = task.deadlineAt - nowMillis;
            if (remainMillis <= 24L * 60L * 60L * 1000L && task.estimatedTotalMinutes >= 60) {
                if (best == null || task.deadlineAt < best.deadlineAt) {
                    best = task;
                }
            }
        }
        return best;
    }

    private boolean noPlanToday(List<TaskRecord> tasks, RecentStats stats) {
        if (stats.todaySessionCount > 0) {
            return false;
        }
        for (TaskRecord task : tasks) {
            if (!task.isDeleted && task.status != TaskStatus.COMPLETED && task.status != TaskStatus.ARCHIVED) {
                return false;
            }
        }
        return true;
    }

    private boolean recentTodayQualityLow(List<FocusSessionRecord> sessions, long nowMillis) {
        long start = com.example.focus_flow.core.common.DateTimeUtils.startOfDayMillis(nowMillis);
        List<FocusSessionRecord> today = new ArrayList<>();
        for (FocusSessionRecord session : sessions) {
            if (session.startedAt >= start && session.qualityScore != null) {
                today.add(session);
            }
        }
        if (today.size() < 3) {
            return false;
        }
        today.sort((left, right) -> Long.compare(right.startedAt, left.startedAt));
        int sum = 0;
        int count = Math.min(3, today.size());
        for (int i = 0; i < count; i++) {
            sum += today.get(i).qualityScore;
        }
        return sum / (double) count <= 2.5;
    }

    private boolean subjectImbalance(List<TaskRecord> tasks, List<FocusSessionRecord> sessions) {
        int weekSeconds = 0;
        int topSeconds = 0;
        java.util.Map<String, Integer> subjectSeconds = new StatsCalculator().subjectSeconds(sessions);
        for (Integer seconds : subjectSeconds.values()) {
            weekSeconds += seconds;
            topSeconds = Math.max(topSeconds, seconds);
        }
        if (weekSeconds <= 0 || topSeconds / (double) weekSeconds < 0.70) {
            return false;
        }
        for (TaskRecord task : tasks) {
            if (!task.isDeleted && task.status != TaskStatus.COMPLETED && task.status != TaskStatus.ARCHIVED) {
                return true;
            }
        }
        return false;
    }

    private boolean estimateTooLow(List<TaskRecord> tasks, List<FocusSessionRecord> sessions) {
        int matched = 0;
        for (TaskRecord task : tasks) {
            if (task.status != TaskStatus.COMPLETED || task.estimatedTotalMinutes <= 0) {
                continue;
            }
            double effective = 0;
            for (FocusSessionRecord session : sessions) {
                if (session.taskId != null && session.taskId == task.id) {
                    effective += session.effectiveProgressMinutes;
                }
            }
            if (effective / task.estimatedTotalMinutes >= 1.30) {
                matched++;
            }
        }
        return matched >= 2;
    }

    private boolean isAfterSixPm(long nowMillis) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        return calendar.get(java.util.Calendar.HOUR_OF_DAY) >= 18;
    }

    private Map<DistractionReason, Integer> distractionCounts(List<FocusSessionRecord> sessions, long nowMillis) {
        long start = nowMillis - 7L * 24L * 60L * 60L * 1000L;
        Map<DistractionReason, Integer> counts = new EnumMap<>(DistractionReason.class);
        for (FocusSessionRecord session : sessions) {
            if (session.startedAt < start || session.distractionReason == null || session.distractionReason == DistractionReason.NONE) {
                continue;
            }
            counts.put(session.distractionReason, countFor(counts, session.distractionReason) + 1);
        }
        return counts;
    }

    private int countFor(Map<DistractionReason, Integer> counts, DistractionReason reason) {
        return counts.containsKey(reason) ? counts.get(reason) : 0;
    }
}
