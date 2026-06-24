package com.example.focus_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.containsString;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.hamcrest.Matcher;

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
    public void bottomNavigationShowsOnlyFourPrimaryDestinations() {
        activity = TestActivityLauncher.launchMainActivity();

        onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_tasks)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_focus)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_forest)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_home)).check(navItemHasCompactIconLabel());
        onView(withId(R.id.nav_tasks)).check(navItemHasCompactIconLabel());
        onView(withId(R.id.nav_focus)).check(navItemHasCompactIconLabel());
        onView(withId(R.id.nav_forest)).check(navItemHasCompactIconLabel());
        onView(withId(R.id.nav_noise)).check(doesNotExist());
        onView(withId(R.id.nav_stats)).check(doesNotExist());
        onView(withId(R.id.nav_profile)).check(doesNotExist());
    }

    @Test
    public void profileOpenedFromHomeMenuHasBackButton() {
        activity = TestActivityLauncher.launchMainActivity();

        onView(withId(R.id.home_button_settings)).perform(click());
        onView(withText("我的")).perform(click());
        onView(withId(R.id.profile_button_back_home)).check(matches(isDisplayed()));
        onView(withId(R.id.profile_button_back_home)).check(matches(withContentDescription("返回")));
        onView(withId(R.id.profile_button_back_home)).check(matches(isAssignableFrom(androidx.appcompat.widget.AppCompatImageButton.class)));
        onView(withId(R.id.profile_button_back_home)).perform(click());
        onView(withId(R.id.home_button_add_task)).check(matches(isDisplayed()));
    }

    @Test
    public void studyReportOpenedFromHomeMenuHasBackButton() {
        activity = TestActivityLauncher.launchMainActivity();

        onView(withId(R.id.home_button_settings)).perform(click());
        onView(withText("学习报告")).perform(click());
        onView(withId(R.id.stats_button_back_home)).check(matches(isDisplayed()));
        onView(withId(R.id.stats_button_back_home)).check(matches(withContentDescription("返回")));
        onView(withId(R.id.stats_button_back_home)).check(matches(isAssignableFrom(androidx.appcompat.widget.AppCompatImageButton.class)));
        onView(withId(R.id.stats_button_back_home)).perform(click());
        onView(withId(R.id.home_button_add_task)).check(matches(isDisplayed()));
    }

    @Test
    public void addTaskFromHomeThenTaskAppearsOnHomeAndTasksPage() {
        activity = TestActivityLauncher.launchMainActivity();

        onView(withId(R.id.home_button_add_task)).perform(click());
        onView(withId(R.id.task_input_title)).perform(replaceText("Algebra Review"), closeSoftKeyboard());
        onView(withId(R.id.task_button_advanced_toggle)).perform(scrollTo(), click());
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

    @Test
    public void taskDateFieldsUsePickersAndFillFormattedValues() {
        activity = TestActivityLauncher.launchMainActivity();

        onView(withId(R.id.home_button_add_task)).perform(click());
        onView(withId(R.id.task_button_advanced_toggle)).perform(scrollTo(), click());

        onView(withId(R.id.task_input_planned_date)).perform(scrollTo(), click());
        onView(isAssignableFrom(DatePicker.class)).perform(setDate(2031, 4, 8));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.task_input_planned_date))
                .perform(scrollTo())
                .check(matches(withText("2031-04-08")));

        onView(withId(R.id.task_input_deadline)).perform(scrollTo(), click());
        onView(isAssignableFrom(DatePicker.class)).perform(setDate(2031, 4, 9));
        onView(withId(android.R.id.button1)).perform(click());
        onView(isAssignableFrom(TimePicker.class)).perform(setTime(14, 35));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.task_input_deadline))
                .perform(scrollTo())
                .check(matches(withText("2031-04-09 14:35")));
    }

    @Test
    public void homeDatePickerJumpsDirectlyToSelectedDate() {
        activity = TestActivityLauncher.launchMainActivity();

        onView(withText("\u9009\u62e9\u65e5\u671f")).perform(click());
        onView(isAssignableFrom(DatePicker.class)).perform(setDate(2030, 5, 20));
        onView(withId(android.R.id.button1)).perform(click());

        onView(withText(containsString("2030\u5e745\u670820\u65e5"))).check(matches(isDisplayed()));
    }
    @Test
    public void exposedCompleteButtonMarksTaskCompleted() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RepositoryProvider provider = RepositoryProvider.get(targetContext);
        activity = TestActivityLauncher.launchMainActivity();

        onView(withId(R.id.home_button_add_task)).perform(click());
        onView(withId(R.id.task_input_title)).perform(replaceText("Complete Button Task"), closeSoftKeyboard());
        onView(withId(R.id.task_button_save)).perform(scrollTo(), click());
        onView(withId(R.id.task_button_complete)).perform(scrollTo(), click());

        provider.taskRepository.refresh();
        java.util.List<TaskRecord> tasks = provider.taskRepository.observeAllVisibleTasks().getValue();
        assertFalse(tasks == null || tasks.isEmpty());
        assertEquals(TaskStatus.COMPLETED, provider.taskRepository.getTaskById(tasks.get(0).id).status);
    }

    @Test
    public void roughTaskInputShowsRewriteSuggestionsWhenTitleLosesFocus() {
        activity = TestActivityLauncher.launchMainActivity();

        onView(withId(R.id.home_button_add_task)).perform(click());
        onView(withId(R.id.task_input_title)).perform(click(), replaceText("chapter 4 notes"));
        onView(withId(R.id.task_rewrite_suggestion_1)).check(doesNotExist());
        onView(withId(R.id.task_button_advanced_toggle)).perform(click());
        onView(withId(R.id.task_rewrite_suggestion_1)).perform(scrollTo()).check(matches(isDisplayed()));
    }
    @Test
    public void roughTaskInputWaitsForTypingPauseBeforeRewriteSuggestions() {
        activity = TestActivityLauncher.launchMainActivity();

        onView(withId(R.id.home_button_add_task)).perform(click());
        onView(withId(R.id.task_input_title)).perform(replaceText("chapter 3 exercises"));
        onView(withId(R.id.task_rewrite_suggestion_1)).check(doesNotExist());
        SystemClock.sleep(2200);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        onView(withId(R.id.task_rewrite_suggestion_1)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.task_rewrite_suggestion_1)).perform(scrollTo(), click());
        onView(withId(R.id.task_button_advanced_toggle)).check(matches(withText("收起更多设置")));
        onView(withId(R.id.task_input_subject)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.task_input_target)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.task_input_estimated)).check(matches(withText("60")));
    }
    private static ViewAssertion navItemHasCompactIconLabel() {
        return (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            assertTrue("Nav item should be a container", view instanceof ViewGroup);
            ViewGroup group = (ViewGroup) view;
            assertEquals("Nav item should contain icon and label", 2, group.getChildCount());
            View icon = group.getChildAt(0);
            View labelView = group.getChildAt(1);
            assertTrue("First child should be an icon", icon instanceof ImageView);
            assertTrue("Second child should be text", labelView instanceof TextView);
            TextView label = (TextView) labelView;
            int expectedGap = dp(view, 3);
            int actualGap = label.getTop() - icon.getBottom();
            assertEquals("Icon and label gap should be 3dp", expectedGap, actualGap);
            float expectedTextSize = sp(view, 13);
            assertTrue("Bottom nav label should be about 13sp",
                    Math.abs(label.getTextSize() - expectedTextSize) <= 1.5f);
        };
    }

    private static ViewAction setDate(int year, int month, int day) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(DatePicker.class);
            }

            @Override
            public String getDescription() {
                return "set date on DatePicker";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((DatePicker) view).updateDate(year, month - 1, day);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    private static ViewAction setTime(int hour, int minute) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TimePicker.class);
            }

            @Override
            public String getDescription() {
                return "set time on TimePicker";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TimePicker picker = (TimePicker) view;
                picker.setHour(hour);
                picker.setMinute(minute);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private static float sp(View view, int value) {
        return value * view.getResources().getDisplayMetrics().scaledDensity;
    }
}
