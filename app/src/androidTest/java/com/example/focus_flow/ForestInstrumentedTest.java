package com.example.focus_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.focus_flow.core.model.DistractionReason;
import com.example.focus_flow.core.model.EndReason;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ForestInstrumentedTest {
    private Activity activity;

    @Before
    public void clearState() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
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
        seedForest(provider);
        provider.focusSessionRepository.refresh();
    }

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
    }

    @Test
    public void forestEvolutionLevelIsVisible() {
        activity = TestActivityLauncher.launchMainActivity();

        onView(withId(R.id.nav_forest)).perform(click());
        onView(withId(R.id.forest_evolution_level)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    private void seedForest(RepositoryProvider provider) {
        long now = System.currentTimeMillis();
        insert(provider, "math", 30, now);
        insert(provider, "english", 45, now - 24L * 60L * 60L * 1000L);
        insert(provider, "programming", 90, now - 2L * 24L * 60L * 60L * 1000L);
        insert(provider, "history", 25, now - 3L * 24L * 60L * 60L * 1000L);
    }

    private void insert(RepositoryProvider provider, String subject, int minutes, long startedAt) {
        FocusSessionRecord session = new FocusSessionRecord();
        session.taskTitleSnapshot = subject + " task";
        session.subjectSnapshot = subject;
        session.difficultySnapshot = TaskDifficulty.NORMAL;
        session.prioritySnapshot = TaskPriority.NORMAL;
        session.plannedFocusMinutes = minutes;
        session.plannedBreakMinutes = 5;
        session.startedAt = startedAt;
        session.endedAt = startedAt + minutes * 60L * 1000L;
        session.actualFocusSeconds = minutes * 60;
        session.status = FocusSessionStatus.COMPLETED;
        session.endReason = EndReason.TIMER_FINISHED;
        session.progressRatio = 1.0;
        session.qualityScore = 5;
        session.distractionReason = DistractionReason.NONE;
        session.effectiveProgressMinutes = minutes;
        session.createdAt = startedAt;
        provider.focusSessionRepository.insertSession(session);
    }
}
