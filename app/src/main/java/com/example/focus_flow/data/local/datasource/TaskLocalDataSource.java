package com.example.focus_flow.data.local.datasource;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.local.sqlite.AppSQLiteOpenHelper;
import com.example.focus_flow.data.mapper.DbMappers;

import java.util.ArrayList;
import java.util.List;

public class TaskLocalDataSource {
    private final AppSQLiteOpenHelper helper;

    public TaskLocalDataSource(AppSQLiteOpenHelper helper) {
        this.helper = helper;
    }

    public long insertTask(TaskRecord task) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insertOrThrow("tasks", null, DbMappers.taskValues(task));
        task.id = id;
        return id;
    }

    public void updateTask(TaskRecord task) {
        task.updatedAt = System.currentTimeMillis();
        helper.getWritableDatabase().update("tasks", DbMappers.taskValues(task), "id=?", args(task.id));
    }

    public void softDeleteTask(long taskId, long deletedAt) {
        TaskRecord task = getTaskById(taskId);
        if (task == null) {
            return;
        }
        task.isDeleted = true;
        task.deletedAt = deletedAt;
        task.status = TaskStatus.ARCHIVED;
        task.updatedAt = deletedAt;
        updateTask(task);
    }

    public TaskRecord getTaskById(long taskId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("tasks", null, "id=?", args(taskId), null, null, null)) {
            return cursor.moveToFirst() ? DbMappers.taskFrom(cursor) : null;
        }
    }

    public List<TaskRecord> getTodayTasks(String plannedDate) {
        return queryTasks("plannedDate=? AND isDeleted=0", new String[]{plannedDate},
                "status='IN_PROGRESS' DESC, deadlineAt IS NULL ASC, deadlineAt ASC, createdAt ASC");
    }

    public List<TaskRecord> getTasksForDate(String plannedDate) {
        return getTodayTasks(plannedDate);
    }

    public List<TaskRecord> getActiveTasks() {
        return queryTasks("isDeleted=0 AND status!='COMPLETED' AND status!='ARCHIVED'", null,
                "plannedDate ASC, createdAt ASC");
    }

    public List<TaskRecord> getCompletedTasks() {
        return queryTasks("isDeleted=0 AND status='COMPLETED'", null, "completedAt DESC");
    }

    public List<TaskRecord> getCompletedTasksBetween(long startMillis, long endMillis) {
        return queryTasks(
                "isDeleted=0 AND status='COMPLETED' AND completedAt>=? AND completedAt<=?",
                new String[]{String.valueOf(startMillis), String.valueOf(endMillis)},
                "completedAt DESC");
    }

    public List<TaskRecord> getAllVisibleTasks() {
        return queryTasks("isDeleted=0", null, "createdAt DESC");
    }

    public void markTaskCompleted(long taskId, long completedAt) {
        TaskRecord task = getTaskById(taskId);
        if (task == null) {
            return;
        }
        task.status = TaskStatus.COMPLETED;
        task.completedAt = completedAt;
        task.updatedAt = completedAt;
        updateTask(task);
    }

    public void updateTaskStatus(long taskId, TaskStatus status) {
        TaskRecord task = getTaskById(taskId);
        if (task == null) {
            return;
        }
        task.status = status;
        long now = System.currentTimeMillis();
        task.updatedAt = now;
        if (status == TaskStatus.COMPLETED && task.completedAt == null) {
            task.completedAt = now;
        }
        updateTask(task);
    }

    private List<TaskRecord> queryTasks(String selection, String[] selectionArgs, String orderBy) {
        List<TaskRecord> tasks = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("tasks", null, selection, selectionArgs, null, null, orderBy)) {
            while (cursor.moveToNext()) {
                tasks.add(DbMappers.taskFrom(cursor));
            }
        }
        return tasks;
    }

    private String[] args(long id) {
        return new String[]{String.valueOf(id)};
    }
}
