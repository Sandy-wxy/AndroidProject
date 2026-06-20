package com.example.focus_flow.feature.focus;

import android.content.Context;
import android.content.SharedPreferences;

class FocusTimerStateStore {
    private static final String PREFS = "focus_flow_timer_state";
    private static final String KEY_PAUSED_SESSION_ID = "paused_session_id";
    private static final String KEY_PAUSE_STARTED_AT = "pause_started_at";

    private final SharedPreferences preferences;

    FocusTimerStateStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    boolean isPaused(long sessionId) {
        return preferences.getLong(KEY_PAUSED_SESSION_ID, -1L) == sessionId
                && preferences.getLong(KEY_PAUSE_STARTED_AT, 0L) > 0;
    }

    long getPauseStartedAt(long sessionId) {
        return isPaused(sessionId) ? preferences.getLong(KEY_PAUSE_STARTED_AT, 0L) : 0L;
    }

    void markPaused(long sessionId, long pauseStartedAt) {
        preferences.edit()
                .putLong(KEY_PAUSED_SESSION_ID, sessionId)
                .putLong(KEY_PAUSE_STARTED_AT, pauseStartedAt)
                .apply();
    }

    void clearPause() {
        preferences.edit()
                .remove(KEY_PAUSED_SESSION_ID)
                .remove(KEY_PAUSE_STARTED_AT)
                .apply();
    }
}
