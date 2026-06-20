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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.focus_flow.data.repository.RepositoryProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TaskFlowInstrumentedTest {
    private Activity activity;

    @Before
    public void clearState() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
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
        targetContext.getSharedPreferences("focus_flow_settings", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        targetContext.getSharedPreferences("focus_flow_focus_start", Context.MODE_PRIVATE).edit().clear().commit();
        targetContext.getSharedPreferences("focus_flow_timer_state", Context.MODE_PRIVATE).edit().clear().commit();
        provider.noiseMixRepository.restorePresetMixesIfMissing();
        provider.taskRepository.refresh();
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
    public void addTaskFromHomeThenTaskAppearsOnHomeAndTasksPage() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(targetContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        onView(withId(R.id.home_button_add_task)).perform(click());
        onView(withId(R.id.task_input_title)).perform(replaceText("Algebra Review"), closeSoftKeyboard());
        onView(withId(R.id.task_input_subject)).perform(replaceText("Math"), closeSoftKeyboard());
        onView(withId(R.id.task_input_target)).perform(replaceText("Finish worksheet"), closeSoftKeyboard());
        onView(withId(R.id.task_input_description)).perform(replaceText("Practice proofs"), closeSoftKeyboard());
        onView(withId(R.id.task_input_estimated)).perform(replaceText("50"), closeSoftKeyboard());
        onView(withId(R.id.task_button_save)).perform(scrollTo(), click());

        onView(withText("Algebra Review")).check(matches(isDisplayed()));
        onView(withText("下一个番茄钟：15 分钟")).check(matches(isDisplayed()));

        onView(withId(R.id.nav_tasks)).perform(click());
        onView(withText("Algebra Review")).check(matches(isDisplayed()));
        onView(withText("Finish worksheet")).check(matches(isDisplayed()));
    }
}
