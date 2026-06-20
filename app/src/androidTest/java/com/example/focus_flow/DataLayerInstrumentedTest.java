package com.example.focus_flow;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.focus_flow.core.model.ColorTag;
import com.example.focus_flow.core.model.EndReason;
import com.example.focus_flow.core.model.FocusBlockStatus;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.core.model.RecommendationConfidence;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.data.local.datasource.FocusBlockLocalDataSource;
import com.example.focus_flow.data.local.datasource.FocusSessionLocalDataSource;
import com.example.focus_flow.data.local.datasource.NoiseMixLocalDataSource;
import com.example.focus_flow.data.local.datasource.TaskLocalDataSource;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.NoiseMixItemRecord;
import com.example.focus_flow.data.local.model.NoiseMixRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.local.sqlite.AppSQLiteOpenHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DataLayerInstrumentedTest {
    private Context dbContext;
    private AppSQLiteOpenHelper helper;
    private TaskLocalDataSource taskDataSource;
    private FocusBlockLocalDataSource blockDataSource;
    private FocusSessionLocalDataSource sessionDataSource;
    private NoiseMixLocalDataSource noiseMixDataSource;

    @Before
    public void setUp() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        dbContext = new IsolatedDatabaseContext(targetContext, "data-layer-" + System.nanoTime());
        dbContext.deleteDatabase(AppSQLiteOpenHelper.DATABASE_NAME);

        helper = new AppSQLiteOpenHelper(dbContext);
        taskDataSource = new TaskLocalDataSource(helper);
        blockDataSource = new FocusBlockLocalDataSource(helper);
        sessionDataSource = new FocusSessionLocalDataSource(helper);
        noiseMixDataSource = new NoiseMixLocalDataSource(helper);
    }

    @After
    public void tearDown() {
        if (helper != null) {
            helper.close();
        }
        if (dbContext != null) {
            dbContext.deleteDatabase(AppSQLiteOpenHelper.DATABASE_NAME);
        }
    }

    @Test
    public void sqliteOpenHelperCreatesExpectedTablesAndIndexes() {
        SQLiteDatabase db = helper.getReadableDatabase();

        assertTrue(hasSchemaObject(db, "table", "tasks"));
        assertTrue(hasSchemaObject(db, "table", "focus_blocks"));
        assertTrue(hasSchemaObject(db, "table", "focus_sessions"));
        assertTrue(hasSchemaObject(db, "table", "noise_mixes"));
        assertTrue(hasSchemaObject(db, "table", "noise_mix_items"));

        assertTrue(hasSchemaObject(db, "index", "idx_tasks_today"));
        assertTrue(hasSchemaObject(db, "index", "idx_focus_blocks_task_status"));
        assertTrue(hasSchemaObject(db, "index", "idx_focus_sessions_status_started"));
        assertTrue(hasSchemaObject(db, "index", "idx_noise_mix_items_mix_type"));

        assertTrue(tableHasColumns(db, "tasks", "title", "plannedDate", "status", "isDeleted", "deletedAt"));
        assertTrue(tableHasColumns(db, "focus_blocks", "taskId", "sequenceIndex", "status", "completedAt"));
        assertTrue(tableHasColumns(db, "focus_sessions", "status", "startedAt", "taskTitleSnapshot", "progressRatio"));
        assertTrue(tableHasColumns(db, "noise_mixes", "name", "isPreset"));
        assertTrue(tableHasColumns(db, "noise_mix_items", "mixId", "noiseType", "volumePercent", "enabled"));
    }

    @Test
    public void noiseMixRestorePresetMixesRestoresFivePresetsAndKeepsCustomMixes() {
        NoiseMixRecord custom = newMix("custom-mix", false, now(1));
        custom.items.add(newMixItem(NoiseType.WHITE_NOISE, 40));
        noiseMixDataSource.insertMixWithItems(custom);

        NoiseMixRecord stalePreset = newMix("stale-preset", true, now(2));
        stalePreset.items.add(newMixItem(NoiseType.HEAVY_RAIN, 55));
        noiseMixDataSource.insertMixWithItems(stalePreset);

        noiseMixDataSource.restorePresetMixesIfMissing();

        List<NoiseMixRecord> mixes = noiseMixDataSource.getAllMixes();
        List<NoiseMixRecord> presets = presetMixes(mixes);
        assertEquals(6, mixes.size());
        assertEquals(5, presets.size());
        assertNotNull(findMixByName(mixes, "custom-mix"));
        assertNull(findMixByName(mixes, "stale-preset"));

        Set<String> presetNames = new HashSet<>();
        Set<NoiseType> presetNoiseTypes = new HashSet<>();
        int presetItemCount = 0;
        for (NoiseMixRecord preset : presets) {
            assertTrue(preset.id > 0);
            assertTrue(preset.isPreset);
            assertNotNull(preset.name);
            assertFalse(preset.name.isEmpty());
            assertFalse(preset.items.isEmpty());
            assertTrue(presetNames.add(preset.name));
            for (NoiseMixItemRecord item : preset.items) {
                assertEquals(preset.id, item.mixId);
                assertTrue(item.id > 0);
                assertTrue(item.enabled);
                assertTrue(item.volumePercent >= 0 && item.volumePercent <= 100);
                presetNoiseTypes.add(item.noiseType);
                presetItemCount++;
            }
        }
        assertEquals(11, presetItemCount);
        assertTrue(presetNoiseTypes.contains(NoiseType.LIGHT_RAIN));
        assertTrue(presetNoiseTypes.contains(NoiseType.OCEAN_WAVES));
        assertTrue(presetNoiseTypes.contains(NoiseType.FOREST_BIRDS));
        assertTrue(presetNoiseTypes.contains(NoiseType.LIBRARY_AMBIENCE));
        assertTrue(presetNoiseTypes.contains(NoiseType.CAFE_AMBIENCE));

        noiseMixDataSource.restorePresetMixesIfMissing();
        assertEquals(5, presetMixes(noiseMixDataSource.getAllMixes()).size());
    }

    @Test
    public void taskInsertReadAndSoftDeleteRoundTrip() {
        TaskRecord task = newTask("task-round-trip", now(10));

        long taskId = taskDataSource.insertTask(task);
        TaskRecord saved = taskDataSource.getTaskById(taskId);

        assertNotNull(saved);
        assertEquals(taskId, saved.id);
        assertEquals("task-round-trip", saved.title);
        assertEquals("math", saved.subject);
        assertEquals("finish worksheet", saved.targetOutcome);
        assertEquals(TaskDifficulty.HARD, saved.difficulty);
        assertEquals(TaskPriority.HIGH, saved.priority);
        assertEquals(TaskStatus.PENDING, saved.status);
        assertFalse(saved.isDeleted);
        assertEquals(1, taskDataSource.getAllVisibleTasks().size());

        long deletedAt = now(20);
        taskDataSource.softDeleteTask(taskId, deletedAt);

        TaskRecord deleted = taskDataSource.getTaskById(taskId);
        assertNotNull(deleted);
        assertTrue(deleted.isDeleted);
        assertEquals(TaskStatus.ARCHIVED, deleted.status);
        assertEquals(Long.valueOf(deletedAt), deleted.deletedAt);
        assertTrue(taskDataSource.getAllVisibleTasks().isEmpty());
    }

    @Test
    public void focusBlockBatchInsertAndNextPendingUsesLowestPendingSequence() {
        long taskId = taskDataSource.insertTask(newTask("block-parent", now(30)));
        List<FocusBlockRecord> blocks = Arrays.asList(
                newBlock(taskId, 3, FocusBlockStatus.PENDING),
                newBlock(taskId, 1, FocusBlockStatus.COMPLETED),
                newBlock(taskId, 2, FocusBlockStatus.PENDING)
        );

        blockDataSource.insertBlocks(blocks);

        for (FocusBlockRecord block : blocks) {
            assertTrue(block.id > 0);
        }
        List<FocusBlockRecord> savedBlocks = blockDataSource.getBlocksByTaskId(taskId);
        assertEquals(3, savedBlocks.size());
        assertEquals(1, savedBlocks.get(0).sequenceIndex);
        assertEquals(2, savedBlocks.get(1).sequenceIndex);
        assertEquals(3, savedBlocks.get(2).sequenceIndex);

        FocusBlockRecord nextPending = blockDataSource.getNextPendingBlock(taskId);
        assertNotNull(nextPending);
        assertEquals(2, nextPending.sequenceIndex);
        assertEquals(FocusBlockStatus.PENDING, nextPending.status);
    }

    @Test
    public void focusSessionRunningQueryReturnsLatestRunningSessionOnly() {
        FocusSessionRecord olderRunning = newSession("older-running", now(40), FocusSessionStatus.RUNNING);
        FocusSessionRecord completedLater = newSession("completed-later", now(50), FocusSessionStatus.COMPLETED);
        completedLater.endedAt = now(55);
        completedLater.endReason = EndReason.TIMER_FINISHED;
        completedLater.progressRatio = 1.0;
        FocusSessionRecord latestRunning = newSession("latest-running", now(60), FocusSessionStatus.RUNNING);

        sessionDataSource.insertSession(olderRunning);
        sessionDataSource.insertSession(completedLater);
        sessionDataSource.insertSession(latestRunning);

        FocusSessionRecord running = sessionDataSource.getRunningSession();
        assertNotNull(running);
        assertEquals(latestRunning.id, running.id);
        assertEquals("latest-running", running.taskTitleSnapshot);
        assertEquals(FocusSessionStatus.RUNNING, running.status);
    }

    private boolean hasSchemaObject(SQLiteDatabase db, String type, String name) {
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type=? AND name=?",
                new String[]{type, name})) {
            return cursor.moveToFirst();
        }
    }

    private boolean tableHasColumns(SQLiteDatabase db, String tableName, String... expectedColumns) {
        Set<String> actualColumns = new HashSet<>();
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                actualColumns.add(cursor.getString(nameIndex));
            }
        }
        for (String expectedColumn : expectedColumns) {
            if (!actualColumns.contains(expectedColumn)) {
                return false;
            }
        }
        return true;
    }

    private List<NoiseMixRecord> presetMixes(List<NoiseMixRecord> mixes) {
        java.util.ArrayList<NoiseMixRecord> presets = new java.util.ArrayList<>();
        for (NoiseMixRecord mix : mixes) {
            if (mix.isPreset) {
                presets.add(mix);
            }
        }
        return presets;
    }

    private NoiseMixRecord findMixByName(List<NoiseMixRecord> mixes, String name) {
        for (NoiseMixRecord mix : mixes) {
            if (name.equals(mix.name)) {
                return mix;
            }
        }
        return null;
    }

    private NoiseMixRecord newMix(String name, boolean isPreset, long timestamp) {
        NoiseMixRecord mix = new NoiseMixRecord();
        mix.name = name;
        mix.isPreset = isPreset;
        mix.createdAt = timestamp;
        mix.updatedAt = timestamp;
        return mix;
    }

    private NoiseMixItemRecord newMixItem(NoiseType noiseType, int volumePercent) {
        NoiseMixItemRecord item = new NoiseMixItemRecord();
        item.noiseType = noiseType;
        item.volumePercent = volumePercent;
        item.enabled = true;
        return item;
    }

    private TaskRecord newTask(String title, long timestamp) {
        TaskRecord task = new TaskRecord();
        task.title = title;
        task.subject = "math";
        task.targetOutcome = "finish worksheet";
        task.description = "practice";
        task.difficulty = TaskDifficulty.HARD;
        task.priority = TaskPriority.HIGH;
        task.estimatedTotalMinutes = 45;
        task.plannedDate = "2026-05-26";
        task.deadlineAt = timestamp + 3_600_000L;
        task.colorTag = ColorTag.GREEN;
        task.autoSplitEnabled = true;
        task.status = TaskStatus.PENDING;
        task.isDeleted = false;
        task.createdAt = timestamp;
        task.updatedAt = timestamp;
        return task;
    }

    private FocusBlockRecord newBlock(long taskId, int sequenceIndex, FocusBlockStatus status) {
        FocusBlockRecord block = new FocusBlockRecord();
        block.taskId = taskId;
        block.sequenceIndex = sequenceIndex;
        block.plannedFocusMinutes = 25;
        block.plannedBreakMinutes = 5;
        block.status = status;
        block.recommendationConfidence = RecommendationConfidence.MEDIUM;
        block.recommendationReasons = "test";
        block.createdAt = now(70 + sequenceIndex);
        block.updatedAt = block.createdAt;
        if (status == FocusBlockStatus.COMPLETED) {
            block.completedAt = block.createdAt + 1_000L;
        }
        return block;
    }

    private FocusSessionRecord newSession(String title, long startedAt, FocusSessionStatus status) {
        FocusSessionRecord session = new FocusSessionRecord();
        session.taskTitleSnapshot = title;
        session.subjectSnapshot = "math";
        session.difficultySnapshot = TaskDifficulty.NORMAL;
        session.prioritySnapshot = TaskPriority.NORMAL;
        session.plannedFocusMinutes = 25;
        session.plannedBreakMinutes = 5;
        session.startedAt = startedAt;
        session.status = status;
        session.createdAt = startedAt;
        return session;
    }

    private long now(int offsetSeconds) {
        return 1_800_000_000_000L + (offsetSeconds * 1_000L);
    }

    private static class IsolatedDatabaseContext extends ContextWrapper {
        private final File databaseDir;

        IsolatedDatabaseContext(Context base, String dirName) {
            super(base);
            databaseDir = new File(base.getCacheDir(), "sqlite-tests" + File.separator + dirName);
        }

        @Override
        public File getDatabasePath(String name) {
            if (!databaseDir.exists() && !databaseDir.mkdirs()) {
                throw new IllegalStateException("Could not create database test directory");
            }
            return new File(databaseDir, name);
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(
                String name,
                int mode,
                SQLiteDatabase.CursorFactory factory) {
            return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(
                String name,
                int mode,
                SQLiteDatabase.CursorFactory factory,
                DatabaseErrorHandler errorHandler) {
            return SQLiteDatabase.openDatabase(
                    getDatabasePath(name).getPath(),
                    factory,
                    SQLiteDatabase.CREATE_IF_NECESSARY,
                    errorHandler);
        }

        @Override
        public boolean deleteDatabase(String name) {
            return SQLiteDatabase.deleteDatabase(getDatabasePath(name));
        }
    }
}
