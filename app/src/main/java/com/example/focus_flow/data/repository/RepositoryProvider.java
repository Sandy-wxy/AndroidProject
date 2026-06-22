package com.example.focus_flow.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.focus_flow.data.local.datasource.FocusBlockLocalDataSource;
import com.example.focus_flow.data.local.datasource.FocusSessionLocalDataSource;
import com.example.focus_flow.data.local.datasource.NoiseMixLocalDataSource;
import com.example.focus_flow.data.local.datasource.TaskLocalDataSource;
import com.example.focus_flow.data.local.sqlite.AppSQLiteOpenHelper;
import com.example.focus_flow.data.preferences.SettingsPreferences;

public class RepositoryProvider {
    private static RepositoryProvider instance;

    public final AppSQLiteOpenHelper database;
    public final TaskLocalDataSource taskLocalDataSource;
    public final FocusBlockLocalDataSource focusBlockLocalDataSource;
    public final FocusSessionLocalDataSource focusSessionLocalDataSource;
    public final NoiseMixLocalDataSource noiseMixLocalDataSource;
    public final SettingsPreferences settingsPreferences;
    public final TaskRepository taskRepository;
    public final FocusSessionRepository focusSessionRepository;
    public final NoiseMixRepository noiseMixRepository;
    public final SettingsRepository settingsRepository;

    private RepositoryProvider(Context context) {
        Context appContext = context.getApplicationContext();
        database = new AppSQLiteOpenHelper(appContext);
        taskLocalDataSource = new TaskLocalDataSource(database);
        focusBlockLocalDataSource = new FocusBlockLocalDataSource(database);
        focusSessionLocalDataSource = new FocusSessionLocalDataSource(database);
        noiseMixLocalDataSource = new NoiseMixLocalDataSource(database);
        settingsPreferences = new SettingsPreferences(appContext);
        taskRepository = new TaskRepository(database, taskLocalDataSource, focusBlockLocalDataSource);
        focusSessionRepository = new FocusSessionRepository(focusSessionLocalDataSource);
        noiseMixRepository = new NoiseMixRepository(noiseMixLocalDataSource, settingsPreferences);
        settingsRepository = new SettingsRepository(settingsPreferences);
        noiseMixRepository.restorePresetMixesIfMissing();
    }

    public static synchronized RepositoryProvider get(Context context) {
        if (instance == null) {
            instance = new RepositoryProvider(context);
        }
        return instance;
    }

    public void clearLearningData() {
        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("focus_sessions", null, null);
            db.delete("focus_blocks", null, null);
            db.delete("tasks", null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        taskRepository.refresh();
        focusSessionRepository.refresh();
    }

    public void refreshAfterCloudRestore() {
        taskRepository.refresh();
        focusSessionRepository.refresh();
        noiseMixRepository.restorePresetMixesIfMissing();
    }
}
