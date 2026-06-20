package com.example.focus_flow.data.local.datasource;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.data.local.model.NoiseMixItemRecord;
import com.example.focus_flow.data.local.model.NoiseMixRecord;
import com.example.focus_flow.data.local.sqlite.AppSQLiteOpenHelper;
import com.example.focus_flow.data.mapper.DbMappers;

import java.util.ArrayList;
import java.util.List;

public class NoiseMixLocalDataSource {
    private final AppSQLiteOpenHelper helper;

    public NoiseMixLocalDataSource(AppSQLiteOpenHelper helper) {
        this.helper = helper;
    }

    public long insertMix(NoiseMixRecord mix) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insertOrThrow("noise_mixes", null, DbMappers.mixValues(mix));
        mix.id = id;
        return id;
    }

    public void insertItems(long mixId, List<NoiseMixItemRecord> items) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (NoiseMixItemRecord item : items) {
                item.mixId = mixId;
                long id = db.insertOrThrow("noise_mix_items", null, DbMappers.mixItemValues(item));
                item.id = id;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public NoiseMixRecord insertMixWithItems(NoiseMixRecord mix) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long mixId = db.insertOrThrow("noise_mixes", null, DbMappers.mixValues(mix));
            mix.id = mixId;
            for (NoiseMixItemRecord item : mix.items) {
                item.mixId = mixId;
                item.id = db.insertOrThrow("noise_mix_items", null, DbMappers.mixItemValues(item));
            }
            db.setTransactionSuccessful();
            return mix;
        } finally {
            db.endTransaction();
        }
    }

    public List<NoiseMixRecord> getAllMixes() {
        List<NoiseMixRecord> mixes = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("noise_mixes", null, null, null, null, null, "isPreset DESC, createdAt ASC")) {
            while (cursor.moveToNext()) {
                NoiseMixRecord mix = DbMappers.mixFrom(cursor);
                mix.items.addAll(getItemsForMix(mix.id));
                mixes.add(mix);
            }
        }
        return mixes;
    }

    public NoiseMixRecord getMixWithItems(long mixId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("noise_mixes", null, "id=?",
                new String[]{String.valueOf(mixId)}, null, null, null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            NoiseMixRecord mix = DbMappers.mixFrom(cursor);
            mix.items.addAll(getItemsForMix(mix.id));
            return mix;
        }
    }

    public void updateItemVolume(long itemId, int volumePercent, boolean enabled) {
        NoiseMixItemRecord item = getItemById(itemId);
        if (item == null) {
            return;
        }
        item.volumePercent = Math.max(0, Math.min(100, volumePercent));
        item.enabled = enabled;
        helper.getWritableDatabase().update("noise_mix_items", DbMappers.mixItemValues(item), "id=?",
                new String[]{String.valueOf(item.id)});
    }

    public void deleteCustomMix(long mixId) {
        NoiseMixRecord mix = getMixWithItems(mixId);
        if (mix == null || mix.isPreset) {
            return;
        }
        helper.getWritableDatabase().delete("noise_mixes", "id=?", new String[]{String.valueOf(mixId)});
    }

    public void restorePresetMixesIfMissing() {
        if (countPresetMixes() >= 5) {
            return;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("noise_mix_items", "mixId IN (SELECT id FROM noise_mixes WHERE isPreset=1)", null);
            db.delete("noise_mixes", "isPreset=1", null);
            insertPreset(db, "雨夜窗边", pair(NoiseType.LIGHT_RAIN, 75), pair(NoiseType.FIREPLACE, 25));
            insertPreset(db, "深海专注", pair(NoiseType.OCEAN_WAVES, 65), pair(NoiseType.BROWN_NOISE, 30));
            insertPreset(db, "森林晨雾", pair(NoiseType.FOREST_BIRDS, 60), pair(NoiseType.STREAM, 35), pair(NoiseType.WIND, 20));
            insertPreset(db, "图书馆自习", pair(NoiseType.LIBRARY_AMBIENCE, 70), pair(NoiseType.KEYBOARD_TYPING, 20));
            insertPreset(db, "咖啡馆低语", pair(NoiseType.CAFE_AMBIENCE, 75), pair(NoiseType.LIGHT_RAIN, 25));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertPreset(SQLiteDatabase db, String name, NoisePair... pairs) {
        NoiseMixRecord mix = new NoiseMixRecord();
        mix.name = name;
        mix.isPreset = true;
        mix.createdAt = System.currentTimeMillis();
        mix.updatedAt = mix.createdAt;
        for (NoisePair pair : pairs) {
            NoiseMixItemRecord item = new NoiseMixItemRecord();
            item.noiseType = pair.type;
            item.volumePercent = pair.volume;
            item.enabled = true;
            mix.items.add(item);
        }
        long mixId = db.insertOrThrow("noise_mixes", null, DbMappers.mixValues(mix));
        for (NoiseMixItemRecord item : mix.items) {
            item.mixId = mixId;
            db.insertOrThrow("noise_mix_items", null, DbMappers.mixItemValues(item));
        }
    }

    private long countPresetMixes() {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM noise_mixes WHERE isPreset=1", null)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
    }

    private List<NoiseMixItemRecord> getItemsForMix(long mixId) {
        List<NoiseMixItemRecord> items = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("noise_mix_items", null, "mixId=?",
                new String[]{String.valueOf(mixId)}, null, null, "id ASC")) {
            while (cursor.moveToNext()) {
                items.add(DbMappers.mixItemFrom(cursor));
            }
        }
        return items;
    }

    private NoiseMixItemRecord getItemById(long itemId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("noise_mix_items", null, "id=?",
                new String[]{String.valueOf(itemId)}, null, null, null)) {
            return cursor.moveToFirst() ? DbMappers.mixItemFrom(cursor) : null;
        }
    }

    private NoisePair pair(NoiseType type, int volume) {
        return new NoisePair(type, volume);
    }

    private static class NoisePair {
        final NoiseType type;
        final int volume;

        NoisePair(NoiseType type, int volume) {
            this.type = type;
            this.volume = volume;
        }
    }
}
