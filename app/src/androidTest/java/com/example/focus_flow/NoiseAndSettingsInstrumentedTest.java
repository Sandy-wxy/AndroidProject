package com.example.focus_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.focus_flow.audio.NoisePlaybackController;
import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.data.local.model.NoiseMixRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NoiseAndSettingsInstrumentedTest {
    private Activity activity;

    @Before
    public void clearState() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NoisePlaybackController.get().stopAll();
        RepositoryProvider provider = RepositoryProvider.get(targetContext);
        SQLiteDatabase db = provider.database.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("noise_mix_items", null, null);
            db.delete("noise_mixes", null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        targetContext.getSharedPreferences("focus_flow_settings", Context.MODE_PRIVATE).edit().clear().commit();
        provider.noiseMixRepository.restorePresetMixesIfMissing();
    }

    @After
    public void tearDown() {
        NoisePlaybackController.get().stopAll();
        if (activity != null) {
            activity.finish();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
    }

    @Test
    public void rawNoiseResourcesCoverEveryNoiseType() {
        assertEquals(12, NoiseType.values().length);
        assertTrue(R.raw.noise_light_rain > 0);
        assertTrue(R.raw.noise_heavy_rain > 0);
        assertTrue(R.raw.noise_ocean_waves > 0);
        assertTrue(R.raw.noise_forest_birds > 0);
        assertTrue(R.raw.noise_cafe_ambience > 0);
        assertTrue(R.raw.noise_library_ambience > 0);
        assertTrue(R.raw.noise_fireplace > 0);
        assertTrue(R.raw.noise_wind > 0);
        assertTrue(R.raw.noise_stream > 0);
        assertTrue(R.raw.noise_white > 0);
        assertTrue(R.raw.noise_brown > 0);
        assertTrue(R.raw.noise_keyboard_typing > 0);
    }

    @Test
    public void playbackControllerStartsAndStopsPresetMix() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RepositoryProvider provider = RepositoryProvider.get(targetContext);
        NoiseMixRecord current = provider.noiseMixRepository.getCurrentMix();
        assertTrue(current.items.size() >= 2);
        NoisePlaybackController.get().applyMix(targetContext, current, 20);
        assertTrue(NoisePlaybackController.get().isPlaying());
        NoisePlaybackController.get().stopAll();
        assertFalse(NoisePlaybackController.get().isPlaying());
    }

    @Test
    public void noiseAndSettingsUiSaveCustomMixAndToggleAutoStop() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RepositoryProvider provider = RepositoryProvider.get(targetContext);
        Intent intent = new Intent(targetContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        int before = provider.noiseMixRepository.getAllMixes().size();
        onView(withId(R.id.nav_noise)).perform(click());
        onView(withText("白噪音控制台")).check(matches(isDisplayed()));
        onView(withId(R.id.noise_button_play_current)).perform(click());
        assertTrue(NoisePlaybackController.get().isPlaying());
        onView(withId(R.id.noise_button_save_custom)).perform(click());
        assertEquals(before + 1, provider.noiseMixRepository.getAllMixes().size());
        onView(withId(R.id.noise_button_stop)).perform(click());
        assertFalse(NoisePlaybackController.get().isPlaying());

        onView(withId(R.id.nav_home)).perform(click());
        onView(withId(R.id.home_button_settings)).perform(click());
        onView(withId(R.id.settings_auto_stop_switch)).check(matches(isDisplayed()));
        assertTrue(provider.settingsRepository.isAutoStopNoiseEnabled());
        onView(withId(R.id.settings_auto_stop_switch)).perform(click());
        assertFalse(provider.settingsRepository.isAutoStopNoiseEnabled());
    }
}
