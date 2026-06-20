package com.example.focus_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.core.model.DistractionReason;
import com.example.focus_flow.core.model.EndReason;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.service.focus.FocusTimerService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StatsInstrumentedTest {
    private Activity activity;

    @Before
    public void clearState() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        targetContext.stopService(new Intent(targetContext, FocusTimerService.class));
        targetContext.getSharedPreferences("focus_flow_focus_start", Context.MODE_PRIVATE).edit().clear().commit();
        targetContext.getSharedPreferences("focus_flow_timer_state", Context.MODE_PRIVATE).edit().clear().commit();

        RepositoryProvider provider = RepositoryProvider.get(targetContext);
        SQLiteDatabase db = provider.database.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("focus_sessions", null, null);
            db.delete("focus_blocks", null, null);
            db.delete("tasks", null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        provider.taskRepository.refresh();
        provider.focusSessionRepository.refresh();
        seedSessions(provider, System.currentTimeMillis());
    }

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
    }

    @Test
    public void statsDashboardRendersChartsAndPeriodSwitches() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(targetContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        onView(withId(R.id.nav_stats)).perform(click());
        onView(withText("专注统计")).check(matches(isDisplayed()));
        onView(withId(R.id.stats_filter_today)).check(matches(isDisplayed()));
        onView(withId(R.id.stats_filter_week)).check(matches(isDisplayed()));
        onView(withId(R.id.stats_filter_month)).check(matches(isDisplayed()));
        onView(withId(R.id.stats_trend_chart)).check(matches(isDisplayed()));
        onView(withId(R.id.stats_subject_chart)).check(matches(isDisplayed()));
        onView(withText("高效时段洞察")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("最常用白噪音")).perform(scrollTo()).check(matches(isDisplayed()));

        onView(withText("专注统计")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.stats_filter_today)).perform(click());
        onView(withText("总时长")).check(matches(isDisplayed()));
        onView(withId(R.id.stats_filter_month)).perform(click());
        onView(withText("最近 7 天趋势")).check(matches(isDisplayed()));
        onView(withId(R.id.stats_filter_week)).perform(click());
        onView(withText("智能建议")).perform(scrollTo()).check(matches(isDisplayed()));
    }

    private void seedSessions(RepositoryProvider provider, long now) {
        long todayStart = DateTimeUtils.startOfDayMillis(now);
        insert(provider, "数学复盘", "数学", todayStart + 9L * 60L * 60L * 1000L,
                FocusSessionStatus.COMPLETED, 25 * 60, 5, DistractionReason.NONE, "雨夜窗边");
        insert(provider, "英语阅读", "英语", todayStart - 1L * 24L * 60L * 60L * 1000L + 20L * 60L * 60L * 1000L,
                FocusSessionStatus.COMPLETED, 30 * 60, 4, DistractionReason.PUSH_MESSAGE, "图书馆自习");
        insert(provider, "物理错题", "物理", todayStart - 2L * 24L * 60L * 60L * 1000L + 15L * 60L * 60L * 1000L,
                FocusSessionStatus.ABANDONED, 12 * 60, 2, DistractionReason.TASK_TOO_HARD, "深海专注");
        insert(provider, "数学练习", "数学", todayStart - 3L * 24L * 60L * 60L * 1000L + 9L * 60L * 60L * 1000L,
                FocusSessionStatus.COMPLETED, 25 * 60, 5, DistractionReason.NONE, "雨夜窗边");
        insert(provider, "语文背诵", "语文", todayStart - 4L * 24L * 60L * 60L * 1000L + 7L * 60L * 60L * 1000L,
                FocusSessionStatus.COMPLETED, 20 * 60, 4, DistractionReason.ENVIRONMENT_NOISE, "森林晨雾");
        insert(provider, "化学实验题", "化学", todayStart - 5L * 24L * 60L * 60L * 1000L + 14L * 60L * 60L * 1000L,
                FocusSessionStatus.COMPLETED, 25 * 60, 5, DistractionReason.NONE, "图书馆自习");
        insert(provider, "英语听力", "英语", todayStart - 6L * 24L * 60L * 60L * 1000L + 21L * 60L * 60L * 1000L,
                FocusSessionStatus.INTERRUPTED, 10 * 60, 2, DistractionReason.FATIGUE_SLEEPY, "咖啡馆低语");
        provider.focusSessionRepository.refresh();
    }

    private void insert(RepositoryProvider provider, String title, String subject, long startedAt,
                        FocusSessionStatus status, int seconds, int quality,
                        DistractionReason reason, String noiseMix) {
        FocusSessionRecord session = new FocusSessionRecord();
        session.taskTitleSnapshot = title;
        session.subjectSnapshot = subject;
        session.difficultySnapshot = TaskDifficulty.NORMAL;
        session.prioritySnapshot = TaskPriority.NORMAL;
        session.plannedFocusMinutes = 25;
        session.plannedBreakMinutes = 5;
        session.startedAt = startedAt;
        session.endedAt = startedAt + seconds * 1000L;
        session.actualFocusSeconds = seconds;
        session.pausedSeconds = 30;
        session.pauseCount = reason == DistractionReason.NONE ? 0 : 1;
        session.status = status;
        session.endReason = status == FocusSessionStatus.COMPLETED ? EndReason.TIMER_FINISHED : EndReason.USER_STOPPED_EARLY;
        session.progressRatio = status == FocusSessionStatus.COMPLETED ? 1.0 : seconds / (25.0 * 60.0);
        session.qualityScore = quality;
        session.distractionReason = reason;
        session.reflectionNote = "instrumented stats seed";
        session.effectiveProgressMinutes = seconds / 60.0;
        session.noiseMixNameSnapshot = noiseMix;
        session.createdAt = startedAt;
        provider.focusSessionRepository.insertSession(session);
    }
}
