package com.example.focus_flow.data.mapper;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.focus_flow.core.model.ColorTag;
import com.example.focus_flow.core.model.DistractionReason;
import com.example.focus_flow.core.model.EndReason;
import com.example.focus_flow.core.model.FocusBlockStatus;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.core.model.RecommendationConfidence;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.NoiseMixItemRecord;
import com.example.focus_flow.data.local.model.NoiseMixRecord;
import com.example.focus_flow.data.local.model.TaskRecord;

public final class DbMappers {
    private DbMappers() {
    }

    public static ContentValues taskValues(TaskRecord task) {
        ContentValues values = new ContentValues();
        values.put("title", task.title);
        values.put("subject", task.subject);
        values.put("targetOutcome", task.targetOutcome);
        values.put("description", task.description == null ? "" : task.description);
        values.put("difficulty", task.difficulty.name());
        values.put("priority", task.priority.name());
        values.put("estimatedTotalMinutes", task.estimatedTotalMinutes);
        values.put("plannedDate", task.plannedDate);
        putNullableLong(values, "deadlineAt", task.deadlineAt);
        values.put("colorTag", task.colorTag.name());
        values.put("autoSplitEnabled", task.autoSplitEnabled ? 1 : 0);
        values.put("status", task.status.name());
        values.put("isDeleted", task.isDeleted ? 1 : 0);
        values.put("createdAt", task.createdAt);
        values.put("updatedAt", task.updatedAt);
        putNullableLong(values, "completedAt", task.completedAt);
        putNullableLong(values, "deletedAt", task.deletedAt);
        return values;
    }

    public static TaskRecord taskFrom(Cursor cursor) {
        TaskRecord task = new TaskRecord();
        task.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        task.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        task.subject = cursor.getString(cursor.getColumnIndexOrThrow("subject"));
        task.targetOutcome = cursor.getString(cursor.getColumnIndexOrThrow("targetOutcome"));
        task.description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        task.difficulty = TaskDifficulty.fromString(cursor.getString(cursor.getColumnIndexOrThrow("difficulty")));
        task.priority = TaskPriority.fromString(cursor.getString(cursor.getColumnIndexOrThrow("priority")));
        task.estimatedTotalMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("estimatedTotalMinutes"));
        task.plannedDate = cursor.getString(cursor.getColumnIndexOrThrow("plannedDate"));
        task.deadlineAt = nullableLong(cursor, "deadlineAt");
        task.colorTag = ColorTag.fromString(cursor.getString(cursor.getColumnIndexOrThrow("colorTag")));
        task.autoSplitEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("autoSplitEnabled")) == 1;
        task.status = TaskStatus.fromString(cursor.getString(cursor.getColumnIndexOrThrow("status")));
        task.isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow("isDeleted")) == 1;
        task.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
        task.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"));
        task.completedAt = nullableLong(cursor, "completedAt");
        task.deletedAt = nullableLong(cursor, "deletedAt");
        return task;
    }

    public static ContentValues blockValues(FocusBlockRecord block) {
        ContentValues values = new ContentValues();
        values.put("taskId", block.taskId);
        values.put("sequenceIndex", block.sequenceIndex);
        values.put("plannedFocusMinutes", block.plannedFocusMinutes);
        values.put("plannedBreakMinutes", block.plannedBreakMinutes);
        values.put("status", block.status.name());
        values.put("recommendationConfidence", block.recommendationConfidence.name());
        values.put("recommendationReasons", block.recommendationReasons == null ? "" : block.recommendationReasons);
        values.put("createdAt", block.createdAt);
        values.put("updatedAt", block.updatedAt);
        putNullableLong(values, "completedAt", block.completedAt);
        return values;
    }

    public static FocusBlockRecord blockFrom(Cursor cursor) {
        FocusBlockRecord block = new FocusBlockRecord();
        block.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        block.taskId = cursor.getLong(cursor.getColumnIndexOrThrow("taskId"));
        block.sequenceIndex = cursor.getInt(cursor.getColumnIndexOrThrow("sequenceIndex"));
        block.plannedFocusMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("plannedFocusMinutes"));
        block.plannedBreakMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("plannedBreakMinutes"));
        block.status = FocusBlockStatus.fromString(cursor.getString(cursor.getColumnIndexOrThrow("status")));
        block.recommendationConfidence = RecommendationConfidence.fromString(cursor.getString(cursor.getColumnIndexOrThrow("recommendationConfidence")));
        block.recommendationReasons = cursor.getString(cursor.getColumnIndexOrThrow("recommendationReasons"));
        block.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
        block.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"));
        block.completedAt = nullableLong(cursor, "completedAt");
        return block;
    }

    public static ContentValues sessionValues(FocusSessionRecord session) {
        ContentValues values = new ContentValues();
        putNullableLong(values, "taskId", session.taskId);
        putNullableLong(values, "blockId", session.blockId);
        values.put("taskTitleSnapshot", session.taskTitleSnapshot);
        values.put("subjectSnapshot", session.subjectSnapshot);
        values.put("difficultySnapshot", session.difficultySnapshot.name());
        values.put("prioritySnapshot", session.prioritySnapshot.name());
        values.put("plannedFocusMinutes", session.plannedFocusMinutes);
        values.put("plannedBreakMinutes", session.plannedBreakMinutes);
        values.put("startedAt", session.startedAt);
        putNullableLong(values, "endedAt", session.endedAt);
        values.put("actualFocusSeconds", session.actualFocusSeconds);
        values.put("pausedSeconds", session.pausedSeconds);
        values.put("pauseCount", session.pauseCount);
        values.put("status", session.status.name());
        values.put("endReason", session.endReason.name());
        values.put("progressRatio", session.progressRatio);
        if (session.qualityScore == null) {
            values.putNull("qualityScore");
        } else {
            values.put("qualityScore", session.qualityScore);
        }
        values.put("distractionReason", session.distractionReason == null ? DistractionReason.NONE.name() : session.distractionReason.name());
        values.put("reflectionNote", session.reflectionNote == null ? "" : session.reflectionNote);
        values.put("effectiveProgressMinutes", session.effectiveProgressMinutes);
        values.put("noiseMixNameSnapshot", session.noiseMixNameSnapshot == null ? "" : session.noiseMixNameSnapshot);
        values.put("createdAt", session.createdAt);
        return values;
    }

    public static FocusSessionRecord sessionFrom(Cursor cursor) {
        FocusSessionRecord session = new FocusSessionRecord();
        session.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        session.taskId = nullableLong(cursor, "taskId");
        session.blockId = nullableLong(cursor, "blockId");
        session.taskTitleSnapshot = cursor.getString(cursor.getColumnIndexOrThrow("taskTitleSnapshot"));
        session.subjectSnapshot = cursor.getString(cursor.getColumnIndexOrThrow("subjectSnapshot"));
        session.difficultySnapshot = TaskDifficulty.fromString(cursor.getString(cursor.getColumnIndexOrThrow("difficultySnapshot")));
        session.prioritySnapshot = TaskPriority.fromString(cursor.getString(cursor.getColumnIndexOrThrow("prioritySnapshot")));
        session.plannedFocusMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("plannedFocusMinutes"));
        session.plannedBreakMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("plannedBreakMinutes"));
        session.startedAt = cursor.getLong(cursor.getColumnIndexOrThrow("startedAt"));
        session.endedAt = nullableLong(cursor, "endedAt");
        session.actualFocusSeconds = cursor.getInt(cursor.getColumnIndexOrThrow("actualFocusSeconds"));
        session.pausedSeconds = cursor.getInt(cursor.getColumnIndexOrThrow("pausedSeconds"));
        session.pauseCount = cursor.getInt(cursor.getColumnIndexOrThrow("pauseCount"));
        session.status = FocusSessionStatus.fromString(cursor.getString(cursor.getColumnIndexOrThrow("status")));
        session.endReason = EndReason.fromString(cursor.getString(cursor.getColumnIndexOrThrow("endReason")));
        session.progressRatio = cursor.getDouble(cursor.getColumnIndexOrThrow("progressRatio"));
        if (!cursor.isNull(cursor.getColumnIndexOrThrow("qualityScore"))) {
            session.qualityScore = cursor.getInt(cursor.getColumnIndexOrThrow("qualityScore"));
        }
        session.distractionReason = DistractionReason.fromString(cursor.getString(cursor.getColumnIndexOrThrow("distractionReason")));
        session.reflectionNote = cursor.getString(cursor.getColumnIndexOrThrow("reflectionNote"));
        session.effectiveProgressMinutes = cursor.getDouble(cursor.getColumnIndexOrThrow("effectiveProgressMinutes"));
        session.noiseMixNameSnapshot = cursor.getString(cursor.getColumnIndexOrThrow("noiseMixNameSnapshot"));
        session.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
        return session;
    }

    public static ContentValues mixValues(NoiseMixRecord mix) {
        ContentValues values = new ContentValues();
        values.put("name", mix.name);
        values.put("isPreset", mix.isPreset ? 1 : 0);
        values.put("createdAt", mix.createdAt);
        values.put("updatedAt", mix.updatedAt);
        return values;
    }

    public static NoiseMixRecord mixFrom(Cursor cursor) {
        NoiseMixRecord mix = new NoiseMixRecord();
        mix.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        mix.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        mix.isPreset = cursor.getInt(cursor.getColumnIndexOrThrow("isPreset")) == 1;
        mix.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
        mix.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"));
        return mix;
    }

    public static ContentValues mixItemValues(NoiseMixItemRecord item) {
        ContentValues values = new ContentValues();
        values.put("mixId", item.mixId);
        values.put("noiseType", item.noiseType.name());
        values.put("volumePercent", item.volumePercent);
        values.put("enabled", item.enabled ? 1 : 0);
        return values;
    }

    public static NoiseMixItemRecord mixItemFrom(Cursor cursor) {
        NoiseMixItemRecord item = new NoiseMixItemRecord();
        item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        item.mixId = cursor.getLong(cursor.getColumnIndexOrThrow("mixId"));
        item.noiseType = NoiseType.fromString(cursor.getString(cursor.getColumnIndexOrThrow("noiseType")));
        item.volumePercent = cursor.getInt(cursor.getColumnIndexOrThrow("volumePercent"));
        item.enabled = cursor.getInt(cursor.getColumnIndexOrThrow("enabled")) == 1;
        return item;
    }

    private static Long nullableLong(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getLong(index);
    }

    private static void putNullableLong(ContentValues values, String key, Long value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }
}
