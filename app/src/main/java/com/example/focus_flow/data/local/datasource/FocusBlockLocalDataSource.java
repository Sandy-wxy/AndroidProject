package com.example.focus_flow.data.local.datasource;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.focus_flow.core.model.FocusBlockStatus;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.sqlite.AppSQLiteOpenHelper;
import com.example.focus_flow.data.mapper.DbMappers;

import java.util.ArrayList;
import java.util.List;

public class FocusBlockLocalDataSource {
    private final AppSQLiteOpenHelper helper;

    public FocusBlockLocalDataSource(AppSQLiteOpenHelper helper) {
        this.helper = helper;
    }

    public void insertBlocks(List<FocusBlockRecord> blocks) {
        SQLiteDatabase db = helper.getWritableDatabase();
        boolean ownsTransaction = !db.inTransaction();
        if (ownsTransaction) {
            db.beginTransaction();
        }
        try {
            for (FocusBlockRecord block : blocks) {
                long id = db.insertOrThrow("focus_blocks", null, DbMappers.blockValues(block));
                block.id = id;
            }
            if (ownsTransaction) {
                db.setTransactionSuccessful();
            }
        } finally {
            if (ownsTransaction) {
                db.endTransaction();
            }
        }
    }

    public void deletePendingBlocksByTaskId(long taskId) {
        helper.getWritableDatabase().delete("focus_blocks", "taskId=? AND status=?",
                new String[]{String.valueOf(taskId), FocusBlockStatus.PENDING.name()});
    }

    public List<FocusBlockRecord> getBlocksByTaskId(long taskId) {
        List<FocusBlockRecord> blocks = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("focus_blocks", null, "taskId=?",
                new String[]{String.valueOf(taskId)}, null, null, "sequenceIndex ASC")) {
            while (cursor.moveToNext()) {
                blocks.add(DbMappers.blockFrom(cursor));
            }
        }
        return blocks;
    }

    public FocusBlockRecord getNextPendingBlock(long taskId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("focus_blocks", null, "taskId=? AND status=?",
                new String[]{String.valueOf(taskId), FocusBlockStatus.PENDING.name()}, null, null,
                "sequenceIndex ASC", "1")) {
            return cursor.moveToFirst() ? DbMappers.blockFrom(cursor) : null;
        }
    }

    public FocusBlockRecord getBlockById(long blockId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("focus_blocks", null, "id=?",
                new String[]{String.valueOf(blockId)}, null, null, null)) {
            return cursor.moveToFirst() ? DbMappers.blockFrom(cursor) : null;
        }
    }

    public void updateBlock(FocusBlockRecord block) {
        block.updatedAt = System.currentTimeMillis();
        helper.getWritableDatabase().update("focus_blocks", DbMappers.blockValues(block), "id=?",
                new String[]{String.valueOf(block.id)});
    }

    public void updateBlockStatus(long blockId, FocusBlockStatus status, Long completedAt) {
        FocusBlockRecord block = getBlockById(blockId);
        if (block == null) {
            return;
        }
        block.status = status;
        block.completedAt = completedAt;
        updateBlock(block);
    }

    public void cancelBlocksByTaskId(long taskId) {
        List<FocusBlockRecord> blocks = getBlocksByTaskId(taskId);
        for (FocusBlockRecord block : blocks) {
            if (block.status == FocusBlockStatus.PENDING) {
                block.status = FocusBlockStatus.CANCELLED;
                updateBlock(block);
            }
        }
    }
}
