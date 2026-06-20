package com.example.focus_flow.data.repository;

import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.data.local.datasource.FocusSessionLocalDataSource;
import com.example.focus_flow.data.local.model.FocusSessionRecord;

import java.util.ArrayList;
import java.util.List;

public class FocusSessionRepository {
    private final FocusSessionLocalDataSource localDataSource;
    private final MutableLiveData<FocusSessionRecord> runningSession = new MutableLiveData<>();
    private final MutableLiveData<List<FocusSessionRecord>> todaySessions = new MutableLiveData<>(new ArrayList<>());

    public FocusSessionRepository(FocusSessionLocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
    }

    public LiveData<FocusSessionRecord> observeRunningSession() {
        return runningSession;
    }

    public LiveData<List<FocusSessionRecord>> observeTodaySessions() {
        return todaySessions;
    }

    public long insertSession(FocusSessionRecord session) {
        long id = localDataSource.insertSession(session);
        refresh();
        return id;
    }

    public void updateSession(FocusSessionRecord session) {
        localDataSource.updateSession(session);
        refresh();
    }

    public FocusSessionRecord getSessionById(long sessionId) {
        return localDataSource.getSessionById(sessionId);
    }

    public FocusSessionRecord getRunningSession() {
        return localDataSource.getRunningSession();
    }

    public FocusSessionRecord getLatestUnratedTerminalSession() {
        return localDataSource.getLatestUnratedTerminalSession();
    }

    public List<FocusSessionRecord> getRecentSessions(int limit) {
        return localDataSource.getRecentSessions(limit);
    }

    public List<FocusSessionRecord> getSessionsBetween(long startMillis, long endMillis) {
        return localDataSource.getSessionsBetween(startMillis, endMillis);
    }

    public List<FocusSessionRecord> getSessionsByTaskId(long taskId) {
        return localDataSource.getSessionsByTaskId(taskId);
    }

    public void refresh() {
        long now = System.currentTimeMillis();
        publish(runningSession, localDataSource.getRunningSession());
        publish(todaySessions, localDataSource.getTodaySessions(DateTimeUtils.startOfDayMillis(now), DateTimeUtils.endOfDayMillis(now)));
    }

    private <T> void publish(MutableLiveData<T> liveData, T value) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            liveData.setValue(value);
        } else {
            liveData.postValue(value);
        }
    }
}
