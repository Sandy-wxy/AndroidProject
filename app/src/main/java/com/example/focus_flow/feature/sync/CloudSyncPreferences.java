package com.example.focus_flow.feature.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CloudSyncPreferences {
    private static final String PREFS = "focus_flow_cloud_sync";
    private static final String KEY_ALIAS = "focus_flow_cloud_credentials";
    private static final String KEY_URL = "url";
    private static final String KEY_USER = "user";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_AUTO = "auto_sync";
    private static final String KEY_LAST_SYNC = "last_sync";

    private final SharedPreferences preferences;

    public CloudSyncPreferences(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String url() {
        return preferences.getString(KEY_URL, "");
    }

    public String username() {
        return decrypt(preferences.getString(KEY_USER, ""));
    }

    public String password() {
        return decrypt(preferences.getString(KEY_PASSWORD, ""));
    }

    public boolean autoSync() {
        return preferences.getBoolean(KEY_AUTO, false);
    }

    public long lastSyncAt() {
        return preferences.getLong(KEY_LAST_SYNC, 0L);
    }

    public boolean isConfigured() {
        return url().startsWith("https://") && !username().isEmpty() && !password().isEmpty();
    }

    public void save(String url, String username, String password, boolean autoSync) {
        preferences.edit()
                .putString(KEY_URL, url.trim())
                .putString(KEY_USER, encrypt(username.trim()))
                .putString(KEY_PASSWORD, encrypt(password))
                .putBoolean(KEY_AUTO, autoSync)
                .apply();
    }

    public void setLastSyncAt(long value) {
        preferences.edit().putLong(KEY_LAST_SYNC, value).apply();
    }

    private String encrypt(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey());
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "."
                    + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception exception) {
            throw new IllegalStateException("无法安全保存云端凭据", exception);
        }
    }

    private String decrypt(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            String[] parts = value.split("\\.", 2);
            if (parts.length != 2) return "";
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(),
                    new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
                    StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return "";
        }
    }

    private SecretKey secretKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) store.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
