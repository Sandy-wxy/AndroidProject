package com.example.focus_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.core.model.ColorTag;
import com.example.focus_flow.core.model.FocusBlockStatus;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.RecommendationConfidence;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.feature.focus.FocusTimerController;
import com.example.focus_flow.feature.focus.FocusTimerSnapshot;
import com.example.focus_flow.service.focus.FocusTimerService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FocusTimerInstrumentedTest {
    private Activity activity;

    @Before
    public void clearState() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        targetContext.stopService(new Intent(targetContext, FocusTimerService.class));
        RepositoryProvider provider = RepositoryProvider.get(targetContext);
        SQLiteDatabase db = provider.database.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("noise_mix_items", null, null);
            db.delete("noise_mixes", null, null);
            db.delete("focus_sessions", null, null);
            db.delete("focus_blocks", null, null);
            db.delete("tasks", null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        targetContext.getSharedPreferences("focus_flow_settings", Context.MODE_PRIVATE).edit().clear().commit();
        targetContext.getSharedPreferences("focus_flow_focus_start", Context.MODE_PRIVATE).edit().clear().commit();
        targetContext.getSharedPreferences("focus_flow_timer_state", Context.MODE_PRIVATE).edit().clear().commit();
        provider.noiseMixRepository.restorePresetMixesIfMissing();
        provider.taskRepository.refresh();
        provider.focusSessionRepository.refresh();
    }

    @After
    public void tearDown() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        targetContext.stopService(new Intent(targetContext, FocusTimerService.class));
        if (activity != null) {
            activity.finish();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
    }

    @Test
    public void saveAndStartPauseResumeFinishRateAndSummarize() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(targetContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        onView(withId(R.id.home_button_add_task)).perform(click());
        onView(withId(R.id.task_input_title)).perform(replaceText("Physics Sprint"), closeSoftKeyboard());
        onView(withId(R.id.task_input_subject)).perform(replaceText("Physics"), closeSoftKeyboard());
        onView(withId(R.id.task_input_target)).perform(replaceText("Review formulas"), closeSoftKeyboard());
        onView(withId(R.id.task_input_estimated)).perform(replaceText("30"), closeSoftKeyboard());
        onView(withId(R.id.task_button_save_start)).perform(scrollTo(), click());

        onView(withText("专注舱")).check(matches(isDisplayed()));
        onView(withText("Physics · Physics Sprint")).check(matches(isDisplayed()));
        onView(withId(R.id.focus_button_pause_resume)).check(matches(withText("暂停")));

        onView(withId(R.id.focus_button_pause_resume)).perform(click());
        onView(withId(R.id.focus_button_pause_resume)).check(matches(withText("继续")));
        onView(withId(R.id.focus_button_pause_resume)).perform(click());
        onView(withId(R.id.focus_button_pause_resume)).check(matches(withText("暂停")));

        onView(withId(R.id.focus_button_finish_early)).perform(scrollTo(), click());
        SystemClock.sleep(300);
        onView(withId(android.R.id.button1)).perform(click());
        onView(withText("记录这次专注")).check(matches(isDisplayed()));
        onView(withId(R.id.focus_rating_4)).perform(click());
        onView(withId(R.id.focus_button_save_rating)).perform(scrollTo(), click());
        onView(withText("专注总结")).check(matches(isDisplayed()));
        onView(withText("Physics Sprint")).check(matches(isDisplayed()));

        RepositoryProvider provider = RepositoryProvider.get(targetContext);
        FocusSessionRecord saved = provider.focusSessionRepository.getRecentSessions(5).get(0);
        assertEquals("Physics Sprint", saved.taskTitleSnapshot);
        assertEquals(FocusSessionStatus.ABANDONED, saved.status);
        assertEquals(Integer.valueOf(4), saved.qualityScore);
        assertEquals(1, saved.pauseCount);
        assertNotNull(saved.taskId);
        assertEquals(FocusBlockStatus.SKIPPED, provider.taskRepository.getBlocksByTaskId(saved.taskId).get(0).status);
    }

    @Test
    public void runningSessionCompletesFromPersistedTimestamp() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RepositoryProvider provider = RepositoryProvider.get(targetContext);
        long now = System.currentTimeMillis();
        TaskRecord task = new TaskRecord();
        task.title = "Timestamp Recovery";
        task.subject = "Math";
        task.targetOutcome = "Finish one short block";
        task.description = "";
        task.difficulty = TaskDifficulty.NORMAL;
        task.priority = TaskPriority.NORMAL;
        task.estimatedTotalMinutes = 10;
        task.plannedDate = "2026-05-26";
        task.colorTag = ColorTag.CYAN;
        task.autoSplitEnabled = true;
        task.status = TaskStatus.PENDING;
        task.createdAt = now;
        task.updatedAt = now;

        FocusBlockRecord block = new FocusBlockRecord();
        block.sequenceIndex = 1;
        block.plannedFocusMinutes = 10;
        block.plannedBreakMinutes = 3;
        block.status = FocusBlockStatus.PENDING;
        block.recommendationConfidence = RecommendationConfidence.LOW;
        block.recommendationReasons = "test";
        block.createdAt = now;
        block.updatedAt = now;
        long taskId = provider.taskRepository.insertTask(task, Collections.singletonList(block));

        FocusTimerController controller = new FocusTimerController(targetContext);
        FocusSessionRecord running = controller.startForTask(taskId);
        assertNotNull(running);
        running.startedAt = System.currentTimeMillis() - 610_000L;
        provider.focusSessionRepository.updateSession(running);

        FocusSessionRecord completed = controller.completeIfTimeReached();
        assertNotNull(completed);
        assertEquals(FocusSessionStatus.COMPLETED, completed.status);
        assertEquals(1.0, completed.progressRatio, 0.001);
    }

    @Test
    public void pausedSessionRebuildKeepsPausedTimeOutOfActiveSeconds() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RepositoryProvider provider = RepositoryProvider.get(targetContext);
        long now = System.currentTimeMillis();
        TaskRecord task = new TaskRecord();
        task.title = "Pause Recovery";
        task.subject = "Math";
        task.targetOutcome = "Check timer";
        task.estimatedTotalMinutes = 10;
        task.plannedDate = "2026-05-26";
        task.status = TaskStatus.PENDING;
        task.createdAt = now;
        task.updatedAt = now;

        FocusBlockRecord block = new FocusBlockRecord();
        block.sequenceIndex = 1;
        block.plannedFocusMinutes = 10;
        block.plannedBreakMinutes = 3;
        block.status = FocusBlockStatus.PENDING;
        block.recommendationConfidence = RecommendationConfidence.LOW;
        block.createdAt = now;
        block.updatedAt = now;
        long taskId = provider.taskRepository.insertTask(task, Collections.singletonList(block));

        FocusTimerController controller = new FocusTimerController(targetContext);
        FocusSessionRecord running = controller.startForTask(taskId);
        assertNotNull(running);
        running.startedAt = System.currentTimeMillis() - 120_000L;
        provider.focusSessionRepository.updateSession(running);
        controller.pause();

        long pauseStartedAt = System.currentTimeMillis() - 60_000L;
        targetContext.getSharedPreferences("focus_flow_timer_state", Context.MODE_PRIVATE)
                .edit()
                .putLong("paused_session_id", running.id)
                .putLong("pause_started_at", pauseStartedAt)
                .commit();

        FocusTimerSnapshot rebuilt = new FocusTimerController(targetContext).getSnapshot();
        assertNotNull(rebuilt);
        assertTrue(rebuilt.paused);
        assertTrue(rebuilt.activeSeconds >= 55 && rebuilt.activeSeconds <= 70);
    }
}
