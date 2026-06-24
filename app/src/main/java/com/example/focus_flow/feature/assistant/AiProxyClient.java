package com.example.focus_flow.feature.assistant;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiProxyClient {
    private static final String BASE_URL = "http://124.220.100.88";
    private static final String CHAT_PATH = "/api/ai/chat";
    private static final String APP_TOKEN = "ac32a1693e4bdcc9ac4228fc347246fd6eebe7658f7238b3d379309f8baffe24";
    private static final int TIMEOUT_MS = 10_000;

    public interface Callback {
        void onSuccess(String responseBody);
        void onError(Exception error);
    }

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void chat(String message, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                String body = request(message);
                mainHandler.post(() -> callback.onSuccess(body));
            } catch (Exception ex) {
                mainHandler.post(() -> callback.onError(ex));
            }
        });
    }

    private String request(String message) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + CHAT_PATH).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + APP_TOKEN);
        byte[] payload = ("{\"message\":\"" + escape(message) + "\"}").getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(payload.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(payload);
        }
        int code = connection.getResponseCode();
        InputStream input = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String response = readFully(input);
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException("AI proxy returned HTTP " + code);
        }
        return response;
    }

    private String readFully(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
