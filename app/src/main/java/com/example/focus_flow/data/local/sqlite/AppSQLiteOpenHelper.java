package com.example.focus_flow.data.local.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppSQLiteOpenHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "focus_flow.db";
    public static final int DATABASE_VERSION = 1;

    public AppSQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "subject TEXT NOT NULL," +
                "targetOutcome TEXT NOT NULL," +
                "description TEXT NOT NULL DEFAULT ''," +
                "difficulty TEXT NOT NULL," +
                "priority TEXT NOT NULL," +
                "estimatedTotalMinutes INTEGER NOT NULL CHECK(estimatedTotalMinutes BETWEEN 10 AND 600)," +
                "plannedDate TEXT NOT NULL," +
                "deadlineAt INTEGER," +
                "colorTag TEXT NOT NULL," +
                "autoSplitEnabled INTEGER NOT NULL DEFAULT 1," +
                "status TEXT NOT NULL," +
                "isDeleted INTEGER NOT NULL DEFAULT 0," +
                "createdAt INTEGER NOT NULL," +
                "updatedAt INTEGER NOT NULL," +
                "completedAt INTEGER," +
                "deletedAt INTEGER" +
                ")");
        db.execSQL("CREATE INDEX idx_tasks_today ON tasks(plannedDate, isDeleted, status)");
        db.execSQL("CREATE INDEX idx_tasks_deadline ON tasks(deadlineAt)");
        db.execSQL("CREATE INDEX idx_tasks_created ON tasks(createdAt)");

        db.execSQL("CREATE TABLE focus_blocks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "taskId INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE," +
                "sequenceIndex INTEGER NOT NULL," +
                "plannedFocusMinutes INTEGER NOT NULL CHECK(plannedFocusMinutes BETWEEN 10 AND 60)," +
                "plannedBreakMinutes INTEGER NOT NULL CHECK(plannedBreakMinutes BETWEEN 3 AND 30)," +
                "status TEXT NOT NULL," +
                "recommendationConfidence TEXT NOT NULL," +
                "recommendationReasons TEXT NOT NULL DEFAULT ''," +
                "createdAt INTEGER NOT NULL," +
                "updatedAt INTEGER NOT NULL," +
                "completedAt INTEGER" +
                ")");
        db.execSQL("CREATE INDEX idx_focus_blocks_task_status ON focus_blocks(taskId, status, sequenceIndex)");

        db.execSQL("CREATE TABLE focus_sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "taskId INTEGER REFERENCES tasks(id) ON DELETE SET NULL," +
                "blockId INTEGER REFERENCES focus_blocks(id) ON DELETE SET NULL," +
                "taskTitleSnapshot TEXT NOT NULL," +
                "subjectSnapshot TEXT NOT NULL," +
                "difficultySnapshot TEXT NOT NULL," +
                "prioritySnapshot TEXT NOT NULL," +
                "plannedFocusMinutes INTEGER NOT NULL," +
                "plannedBreakMinutes INTEGER NOT NULL," +
                "startedAt INTEGER NOT NULL," +
                "endedAt INTEGER," +
                "actualFocusSeconds INTEGER NOT NULL DEFAULT 0," +
                "pausedSeconds INTEGER NOT NULL DEFAULT 0," +
                "pauseCount INTEGER NOT NULL DEFAULT 0," +
                "status TEXT NOT NULL," +
                "endReason TEXT NOT NULL," +
                "progressRatio REAL NOT NULL CHECK(progressRatio BETWEEN 0 AND 1)," +
                "qualityScore INTEGER CHECK(qualityScore BETWEEN 1 AND 5)," +
                "distractionReason TEXT," +
                "reflectionNote TEXT NOT NULL DEFAULT ''," +
                "effectiveProgressMinutes REAL NOT NULL DEFAULT 0," +
                "noiseMixNameSnapshot TEXT NOT NULL DEFAULT ''," +
                "createdAt INTEGER NOT NULL" +
                ")");
        db.execSQL("CREATE INDEX idx_focus_sessions_status_started ON focus_sessions(status, startedAt)");
        db.execSQL("CREATE INDEX idx_focus_sessions_task_started ON focus_sessions(taskId, startedAt)");
        db.execSQL("CREATE INDEX idx_focus_sessions_subject_started ON focus_sessions(subjectSnapshot, startedAt)");
        db.execSQL("CREATE INDEX idx_focus_sessions_started ON focus_sessions(startedAt)");

        db.execSQL("CREATE TABLE noise_mixes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE," +
                "isPreset INTEGER NOT NULL DEFAULT 0," +
                "createdAt INTEGER NOT NULL," +
                "updatedAt INTEGER NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE noise_mix_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "mixId INTEGER NOT NULL REFERENCES noise_mixes(id) ON DELETE CASCADE," +
                "noiseType TEXT NOT NULL," +
                "volumePercent INTEGER NOT NULL CHECK(volumePercent BETWEEN 0 AND 100)," +
                "enabled INTEGER NOT NULL DEFAULT 1" +
                ")");
        db.execSQL("CREATE INDEX idx_noise_mix_items_mix_type ON noise_mix_items(mixId, noiseType)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS noise_mix_items");
        db.execSQL("DROP TABLE IF EXISTS noise_mixes");
        db.execSQL("DROP TABLE IF EXISTS focus_sessions");
        db.execSQL("DROP TABLE IF EXISTS focus_blocks");
        db.execSQL("DROP TABLE IF EXISTS tasks");
        onCreate(db);
    }
}
