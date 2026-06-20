package com.example.focus_flow.data.local.datasource;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.sqlite.AppSQLiteOpenHelper;
import com.example.focus_flow.data.mapper.DbMappers;

import java.util.ArrayList;
import java.util.List;

public class FocusSessionLocalDataSource {
    private final AppSQLiteOpenHelper helper;

    public FocusSessionLocalDataSource(AppSQLiteOpenHelper helper) {
        this.helper = helper;
    }

    public long insertSession(FocusSessionRecord session) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insertOrThrow("focus_sessions", null, DbMappers.sessionValues(session));
        session.id = id;
        return id;
    }

    public void updateSession(FocusSessionRecord session) {
        helper.getWritableDatabase().update("focus_sessions", DbMappers.sessionValues(session), "id=?",
                new String[]{String.valueOf(session.id)});
    }

    public FocusSessionRecord getSessionById(long sessionId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("focus_sessions", null, "id=?",
                new String[]{String.valueOf(sessionId)}, null, null, null)) {
            return cursor.moveToFirst() ? DbMappers.sessionFrom(cursor) : null;
        }
    }

    public FocusSessionRecord getRunningSession() {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("focus_sessions", null, "status=?",
                new String[]{FocusSessionStatus.RUNNING.name()}, null, null, "startedAt DESC", "1")) {
            return cursor.moveToFirst() ? DbMappers.sessionFrom(cursor) : null;
        }
    }

    public FocusSessionRecord getLatestUnratedTerminalSession() {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("focus_sessions", null,
                "status IN ('COMPLETED','ABANDONED','INTERRUPTED') AND qualityScore IS NULL",
                null, null, null, "COALESCE(endedAt, startedAt) DESC", "1")) {
            return cursor.moveToFirst() ? DbMappers.sessionFrom(cursor) : null;
        }
    }

    public List<FocusSessionRecord> getRecentSessions(int limit) {
        return query("status IN ('COMPLETED','ABANDONED','INTERRUPTED')", null,
                "startedAt DESC", String.valueOf(limit));
    }

    public List<FocusSessionRecord> getSessionsByTaskId(long taskId) {
        return query("taskId=?", new String[]{String.valueOf(taskId)}, "startedAt DESC", null);
    }

    public List<FocusSessionRecord> getSessionsBetween(long startMillis, long endMillis) {
        return query("startedAt BETWEEN ? AND ?", new String[]{String.valueOf(startMillis), String.valueOf(endMillis)},
                "startedAt ASC", null);
    }

    public List<FocusSessionRecord> getTodaySessions(long startMillis, long endMillis) {
        return getSessionsBetween(startMillis, endMillis);
    }

    public List<FocusSessionRecord> getWeeklySessions(long weekStartMillis, long weekEndMillis) {
        return getSessionsBetween(weekStartMillis, weekEndMillis);
    }

    private List<FocusSessionRecord> query(String selection, String[] selectionArgs, String orderBy, String limit) {
        List<FocusSessionRecord> sessions = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("focus_sessions", null, selection, selectionArgs, null, null, orderBy, limit)) {
            while (cursor.moveToNext()) {
                sessions.add(DbMappers.sessionFrom(cursor));
            }
        }
        return sessions;
    }
}
