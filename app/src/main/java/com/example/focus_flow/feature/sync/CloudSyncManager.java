package com.example.focus_flow.feature.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.example.focus_flow.data.repository.RepositoryProvider;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudSyncManager {
    public interface Callback {
        void onSuccess(String message);
        void onError(String message);
    }

    private final Context context;
    private final RepositoryProvider provider;
    private final CloudSyncPreferences preferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public CloudSyncManager(Context context) {
        this.context = context.getApplicationContext();
        provider = RepositoryProvider.get(context);
        preferences = new CloudSyncPreferences(context);
    }

    public void backup(Callback callback) {
        run(callback, () -> {
            ensureReady();
            JSONObject snapshot = CloudBackupCodec.exportSnapshot(context, provider.database);
            put(snapshot.toString());
            long now = System.currentTimeMillis();
            preferences.setLastSyncAt(now);
            return "已备份到云端";
        });
    }

    public void restore(Callback callback) {
        run(callback, () -> {
            ensureReady();
            JSONObject remote = get();
            CloudBackupCodec.importSnapshot(context, provider.database, remote);
            provider.refreshAfterCloudRestore();
            preferences.setLastSyncAt(System.currentTimeMillis());
            return "云端数据已恢复到本机";
        });
    }

    public void sync(Callback callback) {
        run(callback, () -> {
            ensureReady();
            JSONObject remote;
            try {
                remote = get();
            } catch (MissingBackupException missing) {
                JSONObject local = CloudBackupCodec.exportSnapshot(context, provider.database);
                put(local.toString());
                preferences.setLastSyncAt(System.currentTimeMillis());
                return "云端为空，已上传本机数据";
            }

            long lastSync = preferences.lastSyncAt();
            long remoteChanged = remote.optLong("exportedAt", 0L);
            long localChanged = localModifiedAt();
            boolean localHasChanges = localChanged > lastSync;
            boolean remoteHasChanges = remoteChanged > lastSync;

            if (remoteHasChanges && (!localHasChanges || remoteChanged > localChanged)) {
                CloudBackupCodec.importSnapshot(context, provider.database, remote);
                provider.refreshAfterCloudRestore();
                preferences.setLastSyncAt(System.currentTimeMillis());
                return "已同步云端较新的数据";
            }
            if (localHasChanges || remoteChanged == 0L) {
                JSONObject local = CloudBackupCodec.exportSnapshot(context, provider.database);
                put(local.toString());
                preferences.setLastSyncAt(System.currentTimeMillis());
                return remoteHasChanges ? "检测到两端更改，已保留较新的本机数据" : "已同步本机最新数据";
            }
            preferences.setLastSyncAt(System.currentTimeMillis());
            return "云端与本机已是最新";
        });
    }

    private void ensureReady() {
        if (!preferences.isConfigured()) {
            throw new IllegalStateException("请先保存有效的 HTTPS 云端配置");
        }
        if (provider.focusSessionRepository.getRunningSession() != null) {
            throw new IllegalStateException("专注进行中，结束后再同步");
        }
    }

    private JSONObject get() throws Exception {
        HttpURLConnection connection = open("GET");
        int code = connection.getResponseCode();
        if (code == HttpURLConnection.HTTP_NOT_FOUND) {
            connection.disconnect();
            throw new MissingBackupException();
        }
        if (code < 200 || code >= 300) {
            throw httpError(connection, code);
        }
        try (InputStream input = connection.getInputStream()) {
            return new JSONObject(new String(readAll(input), StandardCharsets.UTF_8));
        } finally {
            connection.disconnect();
        }
    }

    private void put(String json) throws Exception {
        HttpURLConnection connection = open("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw httpError(connection, code);
        }
        connection.disconnect();
    }

    private HttpURLConnection open(String method) throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(preferences.url()).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(20_000);
        String auth = preferences.username() + ":" + preferences.password();
        connection.setRequestProperty("Authorization", "Basic "
                + Base64.encodeToString(auth.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private IllegalStateException httpError(HttpURLConnection connection, int code) {
        String detail = "";
        try (InputStream error = connection.getErrorStream()) {
            if (error != null) {
                detail = new String(readAll(error), StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {
        } finally {
            connection.disconnect();
        }
        if (detail.length() > 100) detail = detail.substring(0, 100);
        return new IllegalStateException("云端请求失败（HTTP " + code + "）"
                + (detail.isEmpty() ? "" : "：" + detail));
    }

    private long localModifiedAt() {
        SQLiteDatabase db = provider.database.getReadableDatabase();
        String sql = "SELECT MAX(value) FROM ("
                + "SELECT MAX(updatedAt) value FROM tasks UNION ALL "
                + "SELECT MAX(updatedAt) value FROM focus_blocks UNION ALL "
                + "SELECT MAX(COALESCE(endedAt, createdAt)) value FROM focus_sessions UNION ALL "
                + "SELECT MAX(updatedAt) value FROM noise_mixes)";
        try (Cursor cursor = db.rawQuery(sql, null)) {
            return cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getLong(0) : 0L;
        }
    }

    private byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private void run(Callback callback, Work work) {
        executor.execute(() -> {
            try {
                String message = work.run();
                mainHandler.post(() -> callback.onSuccess(message));
            } catch (Exception exception) {
                String message = friendlyMessage(exception);
                if (message == null || message.trim().isEmpty()) message = "云同步失败";
                String finalMessage = message;
                mainHandler.post(() -> callback.onError(finalMessage));
            }
        });
    }

    private String friendlyMessage(Exception exception) {
        if (exception instanceof java.net.UnknownHostException) {
            return "无法连接云端，请检查地址和网络";
        }
        if (exception instanceof java.net.SocketTimeoutException) {
            return "连接云端超时，请稍后重试";
        }
        if (exception instanceof javax.net.ssl.SSLException) {
            return "云端 HTTPS 证书无效";
        }
        return exception.getMessage();
    }

    private interface Work {
        String run() throws Exception;
    }

    private static class MissingBackupException extends Exception {
    }
}
