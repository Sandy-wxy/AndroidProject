package com.example.focus_flow.feature.focus;

import com.example.focus_flow.data.local.model.FocusSessionRecord;

public class FocusTimerSnapshot {
    public final FocusSessionRecord session;
    public final boolean paused;
    public final int activeSeconds;
    public final int remainingSeconds;
    public final float progress;

    public FocusTimerSnapshot(FocusSessionRecord session,
                              boolean paused,
                              int activeSeconds,
                              int remainingSeconds,
                              float progress) {
        this.session = session;
        this.paused = paused;
        this.activeSeconds = activeSeconds;
        this.remainingSeconds = remainingSeconds;
        this.progress = progress;
    }
}
