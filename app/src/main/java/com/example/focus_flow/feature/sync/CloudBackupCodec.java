package com.example.focus_flow.feature.sync;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.focus_flow.data.local.sqlite.AppSQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public final class CloudBackupCodec {
    private static final String[] TABLES = {
            "tasks", "focus_blocks", "focus_sessions", "noise_mixes", "noise_mix_items"
    };

    private CloudBackupCodec() {
    }

    public static JSONObject exportSnapshot(Context context, AppSQLiteOpenHelper helper)
            throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", 1);
        root.put("app", "番茄Focus");
        root.put("exportedAt", System.currentTimeMillis());
        JSONObject database = new JSONObject();
        SQLiteDatabase db = helper.getReadableDatabase();
        for (String table : TABLES) {
            database.put(table, exportTable(db, table));
        }
        root.put("database", database);
        root.put("account", exportPreferences(context, "focus_flow_account",
                new String[]{"logged_in", "name", "email"}));
        root.put("settings", exportPreferences(context, "focus_flow_settings", null));
        return root;
    }

    public static void importSnapshot(Context context, AppSQLiteOpenHelper helper, JSONObject root)
            throws Exception {
        if (root.optInt("format", 0) != 1 || !root.has("database")) {
            throw new IllegalArgumentException("云端备份格式不受支持");
        }
        JSONObject database = root.getJSONObject("database");
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("noise_mix_items", null, null);
            db.delete("noise_mixes", null, null);
            db.delete("focus_sessions", null, null);
            db.delete("focus_blocks", null, null);
            db.delete("tasks", null, null);
            for (String table : TABLES) {
                importTable(db, table, database.optJSONArray(table));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        importPreferences(context, "focus_flow_account", root.optJSONObject("account"));
        importPreferences(context, "focus_flow_settings", root.optJSONObject("settings"));
    }

    private static JSONArray exportTable(SQLiteDatabase db, String table) throws Exception {
        JSONArray rows = new JSONArray();
        try (Cursor cursor = db.query(table, null, null, null, null, null, "id ASC")) {
            while (cursor.moveToNext()) {
                JSONObject row = new JSONObject();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    String name = cursor.getColumnName(i);
                    if (cursor.isNull(i)) {
                        row.put(name, JSONObject.NULL);
                    } else if (cursor.getType(i) == Cursor.FIELD_TYPE_INTEGER) {
                        row.put(name, cursor.getLong(i));
                    } else if (cursor.getType(i) == Cursor.FIELD_TYPE_FLOAT) {
                        row.put(name, cursor.getDouble(i));
                    } else {
                        row.put(name, cursor.getString(i));
                    }
                }
                rows.put(row);
            }
        }
        return rows;
    }

    private static void importTable(SQLiteDatabase db, String table, JSONArray rows)
            throws Exception {
        if (rows == null) return;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            ContentValues values = new ContentValues();
            JSONArray names = row.names();
            if (names == null) continue;
            for (int j = 0; j < names.length(); j++) {
                String name = names.getString(j);
                Object value = row.get(name);
                if (value == JSONObject.NULL) values.putNull(name);
                else if (value instanceof Integer) values.put(name, (Integer) value);
                else if (value instanceof Long) values.put(name, (Long) value);
                else if (value instanceof Double) values.put(name, (Double) value);
                else values.put(name, String.valueOf(value));
            }
            if (db.insertOrThrow(table, null, values) < 0) {
                throw new IllegalStateException("恢复 " + table + " 失败");
            }
        }
    }

    private static JSONObject exportPreferences(Context context, String name, String[] allowList)
            throws Exception {
        JSONObject result = new JSONObject();
        Map<String, ?> values = context.getSharedPreferences(name, Context.MODE_PRIVATE).getAll();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (allowList != null && !contains(allowList, entry.getKey())) continue;
            Object value = entry.getValue();
            if (value instanceof Boolean || value instanceof Number || value instanceof String) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    private static void importPreferences(Context context, String name, JSONObject values)
            throws Exception {
        if (values == null) return;
        SharedPreferences.Editor editor = context
                .getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear();
        JSONArray names = values.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                String key = names.getString(i);
                Object value = values.get(key);
                if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
                else if (value instanceof Integer) editor.putInt(key, (Integer) value);
                else if (value instanceof Long) editor.putLong(key, (Long) value);
                else if (value instanceof Number) editor.putFloat(key, ((Number) value).floatValue());
                else editor.putString(key, String.valueOf(value));
            }
        }
        editor.apply();
    }

    private static boolean contains(String[] values, String target) {
        for (String value : values) {
            if (value.equals(target)) return true;
        }
        return false;
    }
}
