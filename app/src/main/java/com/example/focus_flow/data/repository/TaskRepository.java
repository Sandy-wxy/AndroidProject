package com.example.focus_flow.data.repository;

import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.focus_flow.data.local.sqlite.AppSQLiteOpenHelper;
import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.data.local.datasource.FocusBlockLocalDataSource;
import com.example.focus_flow.data.local.datasource.TaskLocalDataSource;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.model.TaskRecord;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    private final AppSQLiteOpenHelper database;
    private final TaskLocalDataSource taskLocalDataSource;
    private final FocusBlockLocalDataSource focusBlockLocalDataSource;
    private final MutableLiveData<List<TaskRecord>> todayTasks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<TaskRecord>> allVisibleTasks = new MutableLiveData<>(new ArrayList<>());

    public TaskRepository(AppSQLiteOpenHelper database,
                          TaskLocalDataSource taskLocalDataSource,
                          FocusBlockLocalDataSource focusBlockLocalDataSource) {
        this.database = database;
        this.taskLocalDataSource = taskLocalDataSource;
        this.focusBlockLocalDataSource = focusBlockLocalDataSource;
    }

    public LiveData<List<TaskRecord>> observeTodayTasks() {
        return todayTasks;
    }

    public LiveData<List<TaskRecord>> observeAllVisibleTasks() {
        return allVisibleTasks;
    }

    public long insertTask(TaskRecord task, List<FocusBlockRecord> blocks) {
        long now = System.currentTimeMillis();
        task.createdAt = task.createdAt == 0 ? now : task.createdAt;
        task.updatedAt = now;
        android.database.sqlite.SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        long taskId;
        try {
            taskId = taskLocalDataSource.insertTask(task);
            for (FocusBlockRecord block : blocks) {
                block.taskId = taskId;
                block.createdAt = now;
                block.updatedAt = now;
            }
            focusBlockLocalDataSource.insertBlocks(blocks);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        refresh();
        return taskId;
    }

    public void updateTask(TaskRecord task) {
        taskLocalDataSource.updateTask(task);
        refresh();
    }

    public void replacePendingBlocks(long taskId, List<FocusBlockRecord> blocks) {
        long now = System.currentTimeMillis();
        android.database.sqlite.SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            focusBlockLocalDataSource.deletePendingBlocksByTaskId(taskId);
            for (FocusBlockRecord block : blocks) {
                block.taskId = taskId;
                block.createdAt = now;
                block.updatedAt = now;
            }
            focusBlockLocalDataSource.insertBlocks(blocks);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        refresh();
    }

    public void softDeleteTask(long taskId) {
        long now = System.currentTimeMillis();
        focusBlockLocalDataSource.cancelBlocksByTaskId(taskId);
        taskLocalDataSource.softDeleteTask(taskId, now);
        refresh();
    }

    public void markTaskCompleted(long taskId) {
        taskLocalDataSource.markTaskCompleted(taskId, System.currentTimeMillis());
        refresh();
    }

    public void updateTaskStatus(long taskId, TaskStatus status) {
        taskLocalDataSource.updateTaskStatus(taskId, status);
        refresh();
    }

    public TaskRecord getTaskById(long taskId) {
        return taskLocalDataSource.getTaskById(taskId);
    }

    public List<TaskRecord> getCompletedTasks() {
        return taskLocalDataSource.getCompletedTasks();
    }

    public List<TaskRecord> getCompletedTasksBetween(long startMillis, long endMillis) {
        return taskLocalDataSource.getCompletedTasksBetween(startMillis, endMillis);
    }

    public List<TaskRecord> getTasksForDate(String plannedDate) {
        return taskLocalDataSource.getTasksForDate(plannedDate);
    }

    public List<FocusBlockRecord> getBlocksByTaskId(long taskId) {
        return focusBlockLocalDataSource.getBlocksByTaskId(taskId);
    }

    public FocusBlockRecord getNextPendingBlock(long taskId) {
        return focusBlockLocalDataSource.getNextPendingBlock(taskId);
    }

    public void refresh() {
        publish(todayTasks, taskLocalDataSource.getTodayTasks(DateTimeUtils.todayDateString()));
        publish(allVisibleTasks, taskLocalDataSource.getAllVisibleTasks());
    }

    private <T> void publish(MutableLiveData<T> liveData, T value) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            liveData.setValue(value);
        } else {
            liveData.postValue(value);
        }
    }
}
