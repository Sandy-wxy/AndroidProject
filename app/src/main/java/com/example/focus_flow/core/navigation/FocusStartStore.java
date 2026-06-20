package com.example.focus_flow.core.navigation;

import android.content.Context;
import android.content.SharedPreferences;

public class FocusStartStore {
    private static final String PREFS = "focus_flow_focus_start";
    private static final String KEY_PENDING_TASK_ID = "pending_task_id";

    private final SharedPreferences preferences;

    public FocusStartStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void requestStart(long taskId) {
        preferences.edit().putLong(KEY_PENDING_TASK_ID, taskId).apply();
    }

    public long consumePendingTaskId() {
        long taskId = preferences.getLong(KEY_PENDING_TASK_ID, -1L);
        if (taskId > 0) {
            preferences.edit().remove(KEY_PENDING_TASK_ID).apply();
        }
        return taskId;
    }
}
