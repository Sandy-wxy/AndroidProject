package com.example.focus_flow.feature.focus;

import android.content.Context;

import com.example.focus_flow.core.model.EndReason;
import com.example.focus_flow.core.model.FocusBlockStatus;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.NoiseMixRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.domain.rules.ProgressEngine;
import com.example.focus_flow.feature.reminder.TaskReminderScheduler;
import com.example.focus_flow.feature.widget.FocusQuickWidgetProvider;

import java.util.List;

public class FocusTimerController {
    private final Context context;
    private final RepositoryProvider provider;
    private final FocusTimerStateStore stateStore;
    private final ProgressEngine progressEngine = new ProgressEngine();

    public FocusTimerController(Context context) {
        this.context = context.getApplicationContext();
        provider = RepositoryProvider.get(this.context);
        stateStore = new FocusTimerStateStore(this.context);
    }

    public FocusSessionRecord startForTask(long taskId) {
        return startForTask(taskId, -1);
    }

    public FocusSessionRecord startForTask(long taskId, int requestedMinutes) {
        FocusSessionRecord running = provider.focusSessionRepository.getRunningSession();
        if (running != null) {
            return running;
        }
        TaskRecord task = provider.taskRepository.getTaskById(taskId);
        if (task == null || task.isDeleted || task.status == TaskStatus.ARCHIVED || task.status == TaskStatus.COMPLETED) {
            return null;
        }
        FocusBlockRecord block = provider.taskRepository.getNextPendingBlock(taskId);
        long now = System.currentTimeMillis();

        FocusSessionRecord session = new FocusSessionRecord();
        session.taskId = task.id;
        session.blockId = block == null ? null : block.id;
        session.taskTitleSnapshot = task.title;
        session.subjectSnapshot = task.subject;
        session.difficultySnapshot = task.difficulty;
        session.prioritySnapshot = task.priority;
        session.plannedFocusMinutes = block == null ? Math.max(10, Math.min(60, task.estimatedTotalMinutes)) : block.plannedFocusMinutes;
        if (requestedMinutes >= 10) {
            session.plannedFocusMinutes = Math.max(10, Math.min(60, requestedMinutes));
            session.blockId = null;
        }
        session.plannedBreakMinutes = block == null ? 5 : block.plannedBreakMinutes;
        session.startedAt = now;
        session.createdAt = now;
        session.status = FocusSessionStatus.RUNNING;
        NoiseMixRecord mix = provider.noiseMixRepository.getCurrentMix();
        session.noiseMixNameSnapshot = mix == null ? "" : mix.name;

        provider.focusSessionRepository.insertSession(session);
        if (block != null && requestedMinutes < 10) {
            provider.focusBlockLocalDataSource.updateBlockStatus(block.id, FocusBlockStatus.RUNNING, null);
        }
        provider.taskRepository.updateTaskStatus(task.id, TaskStatus.IN_PROGRESS);
        stateStore.clearPause();
        provider.focusSessionRepository.refresh();
        provider.taskRepository.refresh();
        FocusQuickWidgetProvider.refreshAll(context);
        return session;
    }

    public FocusTimerSnapshot getSnapshot() {
        FocusSessionRecord session = provider.focusSessionRepository.getRunningSession();
        if (session == null) {
            stateStore.clearPause();
            return null;
        }
        return buildSnapshot(session, System.currentTimeMillis());
    }

    public FocusSessionRecord completeIfTimeReached() {
        FocusTimerSnapshot snapshot = getSnapshot();
        if (snapshot == null || snapshot.paused || snapshot.remainingSeconds > 0) {
            return null;
        }
        return finishSession(snapshot.session, EndReason.TIMER_FINISHED, FocusSessionStatus.COMPLETED);
    }

    public void pause() {
        FocusSessionRecord session = provider.focusSessionRepository.getRunningSession();
        if (session == null || stateStore.isPaused(session.id)) {
            return;
        }
        session.pauseCount += 1;
        provider.focusSessionRepository.updateSession(session);
        stateStore.markPaused(session.id, System.currentTimeMillis());
    }

    public void resume() {
        FocusSessionRecord session = provider.focusSessionRepository.getRunningSession();
        if (session == null || !stateStore.isPaused(session.id)) {
            return;
        }
        long pauseStartedAt = stateStore.getPauseStartedAt(session.id);
        int extraPausedSeconds = Math.max(0, (int) ((System.currentTimeMillis() - pauseStartedAt) / 1000L));
        session.pausedSeconds += extraPausedSeconds;
        provider.focusSessionRepository.updateSession(session);
        stateStore.clearPause();
    }

    public FocusSessionRecord finishEarly() {
        FocusTimerSnapshot snapshot = getSnapshot();
        if (snapshot == null) {
            return null;
        }
        return finishSession(snapshot.session, EndReason.USER_STOPPED_EARLY, FocusSessionStatus.ABANDONED);
    }

    public FocusSessionRecord saveRating(long sessionId, int qualityScore, com.example.focus_flow.core.model.DistractionReason reason, String note) {
        FocusSessionRecord session = provider.focusSessionRepository.getSessionById(sessionId);
        if (session == null) {
            return null;
        }
        session.qualityScore = Math.max(1, Math.min(5, qualityScore));
        session.distractionReason = reason == null ? com.example.focus_flow.core.model.DistractionReason.NONE : reason;
        session.reflectionNote = note == null ? "" : note.trim();
        session.effectiveProgressMinutes = progressEngine.effectiveMinutesForSession(session);
        provider.focusSessionRepository.updateSession(session);
        updateTaskCompletion(session);
        provider.focusSessionRepository.refresh();
        provider.taskRepository.refresh();
        return session;
    }

    public FocusSessionRecord completeWithoutReview(long sessionId) {
        FocusSessionRecord session = provider.focusSessionRepository.getSessionById(sessionId);
        if (session == null) {
            return null;
        }
        session.effectiveProgressMinutes = progressEngine.effectiveMinutesForSession(session);
        provider.focusSessionRepository.updateSession(session);
        updateTaskCompletion(session);
        provider.focusSessionRepository.refresh();
        provider.taskRepository.refresh();
        return session;
    }

    public FocusSessionRecord getLatestUnratedTerminalSession() {
        return provider.focusSessionRepository.getLatestUnratedTerminalSession();
    }

    public FocusSessionRecord getSessionById(long sessionId) {
        return provider.focusSessionRepository.getSessionById(sessionId);
    }

    private FocusSessionRecord finishSession(FocusSessionRecord session, EndReason reason, FocusSessionStatus status) {
        long now = System.currentTimeMillis();
        FocusTimerSnapshot snapshot = buildSnapshot(session, now);
        session.actualFocusSeconds = snapshot.activeSeconds;
        session.pausedSeconds = currentPausedSeconds(session, now);
        session.endedAt = now;
        session.status = status;
        session.endReason = reason;
        int plannedSeconds = Math.max(60, session.plannedFocusMinutes * 60);
        session.progressRatio = Math.max(0, Math.min(1.0, session.actualFocusSeconds / (double) plannedSeconds));
        provider.focusSessionRepository.updateSession(session);
        stateStore.clearPause();

        if (session.blockId != null) {
            FocusBlockStatus blockStatus = reason == EndReason.TIMER_FINISHED || session.progressRatio >= 0.70
                    ? FocusBlockStatus.COMPLETED
                    : FocusBlockStatus.SKIPPED;
            provider.focusBlockLocalDataSource.updateBlockStatus(session.blockId, blockStatus,
                    blockStatus == FocusBlockStatus.COMPLETED ? now : null);
        }
        provider.focusSessionRepository.refresh();
        provider.taskRepository.refresh();
        FocusQuickWidgetProvider.refreshAll(context);
        return session;
    }

    private void updateTaskCompletion(FocusSessionRecord session) {
        if (session.taskId == null) {
            return;
        }
        TaskRecord task = provider.taskRepository.getTaskById(session.taskId);
        if (task == null || task.status == TaskStatus.ARCHIVED) {
            return;
        }
        List<FocusSessionRecord> sessions = provider.focusSessionRepository.getSessionsByTaskId(session.taskId);
        int progressPercent = progressEngine.progressPercent(task, sessions);
        provider.taskRepository.updateTaskStatus(session.taskId,
                progressPercent >= 100 ? TaskStatus.COMPLETED : TaskStatus.IN_PROGRESS);
        if (progressPercent >= 100) {
            TaskReminderScheduler.cancel(context, session.taskId);
        }
    }

    private FocusTimerSnapshot buildSnapshot(FocusSessionRecord session, long now) {
        int plannedSeconds = Math.max(60, session.plannedFocusMinutes * 60);
        int activeSeconds = activeSeconds(session, now);
        int remainingSeconds = Math.max(0, plannedSeconds - activeSeconds);
        float progress = Math.min(1f, activeSeconds / (float) plannedSeconds);
        return new FocusTimerSnapshot(session, stateStore.isPaused(session.id), activeSeconds, remainingSeconds, progress);
    }

    private int activeSeconds(FocusSessionRecord session, long now) {
        long anchor = stateStore.isPaused(session.id) ? stateStore.getPauseStartedAt(session.id) : now;
        long elapsedSeconds = Math.max(0, (anchor - session.startedAt) / 1000L);
        return Math.max(0, (int) elapsedSeconds - session.pausedSeconds);
    }

    private int currentPausedSeconds(FocusSessionRecord session, long now) {
        if (!stateStore.isPaused(session.id)) {
            return session.pausedSeconds;
        }
        long pauseStartedAt = stateStore.getPauseStartedAt(session.id);
        return session.pausedSeconds + Math.max(0, (int) ((now - pauseStartedAt) / 1000L));
    }
}
