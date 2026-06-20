package com.example.focus_flow.domain;

import com.example.focus_flow.core.model.DistractionReason;
import com.example.focus_flow.core.model.EndReason;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.RecommendationConfidence;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.core.model.TimeSegment;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.domain.rules.Advice;
import com.example.focus_flow.domain.rules.AdviceEngine;
import com.example.focus_flow.domain.rules.EncouragementCategory;
import com.example.focus_flow.domain.rules.EncouragementEngine;
import com.example.focus_flow.domain.rules.FocusRecommendationEngine;
import com.example.focus_flow.domain.rules.ProgressEngine;
import com.example.focus_flow.domain.rules.Recommendation;
import com.example.focus_flow.domain.rules.TaskSplitEngine;
import com.example.focus_flow.domain.stats.RecentStats;
import com.example.focus_flow.domain.stats.StatsCalculator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RuleEngineTest {
    private static final long NOW = 1_800_000_000_000L;

    @Test
    public void recommendationShortensForHighAbandonAndLowQuality() {
        FocusRecommendationEngine engine = new FocusRecommendationEngine();
        TaskRecord task = task(TaskDifficulty.HARD, TaskPriority.NORMAL, 180);
        RecentStats stats = new RecentStats(6, 0.50, 0.40, 2.3, 3.5,
                0, 20, 1, TimeSegment.MORNING, 0, 0.50, 2.3, 2, 30);

        Recommendation recommendation = engine.recommend(task, stats, 0, NOW);

        assertEquals(10, recommendation.focusMinutes);
        assertEquals(RecommendationConfidence.MEDIUM, recommendation.confidence);
        assertTrue(recommendation.reasonCodes.contains("R03_HIGH_ABANDON"));
        assertTrue(recommendation.reasonCodes.contains("R05_LOW_QUALITY"));
        assertTrue(recommendation.reasonCodes.contains("R06_MANY_PAUSES"));
        assertTrue(recommendation.reasonCodes.contains("R18_LAST_SESSION_BAD"));
    }

    @Test
    public void recommendationExtendsForStableUrgentTask() {
        FocusRecommendationEngine engine = new FocusRecommendationEngine();
        TaskRecord task = task(TaskDifficulty.NORMAL, TaskPriority.URGENT, 180);
        task.deadlineAt = NOW + 20L * 60L * 60L * 1000L;
        RecentStats stats = new RecentStats(12, 0.90, 0.05, 4.4, 0.5,
                4, 40, 2, TimeSegment.MORNING, 4, 0.90, 4.5, 5, 20);

        Recommendation recommendation = engine.recommend(task, stats, 0, NOW);

        assertEquals(60, recommendation.focusMinutes);
        assertEquals(RecommendationConfidence.HIGH, recommendation.confidence);
        assertTrue(recommendation.reasonCodes.contains("R01_HIGH_STABILITY"));
        assertTrue(recommendation.reasonCodes.contains("R11_PRIORITY_URGENT"));
        assertTrue(recommendation.reasonCodes.contains("R13_DEADLINE_24H"));
    }

    @Test
    public void splitTaskRespectsSequenceAndMaxBlockLength() {
        TaskRecord task = task(TaskDifficulty.HARD, TaskPriority.NORMAL, 125);
        Recommendation recommendation = new Recommendation(40, 8, 20, 3,
                RecommendationConfidence.MEDIUM, java.util.Collections.singletonList("BASE_HARD"));

        List<FocusBlockRecord> blocks = new TaskSplitEngine().splitTask(task, recommendation, 0);

        assertFalse(blocks.isEmpty());
        int total = 0;
        for (int i = 0; i < blocks.size(); i++) {
            assertEquals(i + 1, blocks.get(i).sequenceIndex);
            assertTrue(blocks.get(i).plannedFocusMinutes >= 10);
            assertTrue(blocks.get(i).plannedFocusMinutes <= 60);
            total += blocks.get(i).plannedFocusMinutes;
        }
        assertEquals(125, total);
    }

    @Test
    public void progressEngineAppliesQualityAndEarlyStopMultipliers() {
        ProgressEngine engine = new ProgressEngine();
        FocusSessionRecord completedLow = session(FocusSessionStatus.COMPLETED, 1500, 1.0, 2);
        FocusSessionRecord abandoned = session(FocusSessionStatus.ABANDONED, 1200, 0.50, 3);
        FocusSessionRecord interrupted = session(FocusSessionStatus.INTERRUPTED, 600, 0.20, null);

        assertEquals(16.25, engine.effectiveMinutesForSession(completedLow), 0.001);
        assertEquals(8.0, engine.effectiveMinutesForSession(abandoned), 0.001);
        assertEquals(5.0, engine.effectiveMinutesForSession(interrupted), 0.001);
    }

    @Test
    public void adviceEngineKeepsHighAbandonInsteadOfMedium() {
        AdviceEngine engine = new AdviceEngine();
        RecentStats stats = new RecentStats(6, 0.50, 0.40, 3.0, 1.0,
                0, 30, 1, TimeSegment.MORNING, 0, 0.50, 3.0, null, null);

        List<Advice> advices = engine.generate(stats, new ArrayList<>(), new ArrayList<>(), NOW, true);

        assertEquals("ADVICE_HIGH_ABANDON", advices.get(0).adviceId);
        for (Advice advice : advices) {
            assertFalse("ADVICE_MEDIUM_ABANDON".equals(advice.adviceId));
        }
    }

    @Test
    public void encouragementCategoryFollowsPriority() {
        EncouragementEngine engine = new EncouragementEngine();
        TaskRecord task = task(TaskDifficulty.HARD, TaskPriority.NORMAL, 60);
        RecentStats stats = RecentStats.defaults(TimeSegment.MORNING);
        FocusSessionRecord completedLow = session(FocusSessionStatus.COMPLETED, 1500, 1.0, 2);
        completedLow.endReason = EndReason.TIMER_FINISHED;

        assertEquals(EncouragementCategory.LOW_QUALITY_COMPLETE,
                engine.determineCategory(completedLow, task, stats, false));

        FocusSessionRecord abandoned = session(FocusSessionStatus.ABANDONED, 1200, 0.72, 4);
        assertEquals(EncouragementCategory.ABANDONED_NEAR_END,
                engine.determineCategory(abandoned, task, stats, false));
    }

    @Test
    public void statsCalculatorUsesDefaultsWhenEmpty() {
        RecentStats stats = new StatsCalculator().calculateRecentStats(new ArrayList<>(), NOW);
        assertEquals(0, stats.recentTotal);
        assertEquals(0.70, stats.completionRate, 0.001);
        assertEquals(3.5, stats.averageQuality, 0.001);
    }

    private TaskRecord task(TaskDifficulty difficulty, TaskPriority priority, int estimatedMinutes) {
        TaskRecord task = new TaskRecord();
        task.id = 1;
        task.title = "不参与推荐";
        task.description = "不参与推荐";
        task.targetOutcome = "不参与推荐";
        task.subject = "math";
        task.difficulty = difficulty;
        task.priority = priority;
        task.estimatedTotalMinutes = estimatedMinutes;
        task.plannedDate = "2026-05-26";
        task.status = TaskStatus.PENDING;
        return task;
    }

    private FocusSessionRecord session(FocusSessionStatus status, int seconds, double ratio, Integer quality) {
        FocusSessionRecord session = new FocusSessionRecord();
        session.status = status;
        session.actualFocusSeconds = seconds;
        session.progressRatio = ratio;
        session.qualityScore = quality;
        session.startedAt = NOW;
        session.subjectSnapshot = "math";
        session.taskTitleSnapshot = "snapshot";
        return session;
    }
}
