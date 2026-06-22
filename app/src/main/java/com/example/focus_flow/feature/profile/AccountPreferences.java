package com.example.focus_flow.feature.profile;

import android.content.Context;
import android.content.SharedPreferences;

final class AccountPreferences {
    private static final String PREFS = "focus_flow_account";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_AVATAR_URI = "avatar_uri";

    private final SharedPreferences preferences;

    AccountPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    String name() {
        return preferences.getString(KEY_NAME, "专注学习者");
    }

    String email() {
        return preferences.getString(KEY_EMAIL, "");
    }

    String avatarUri() {
        return preferences.getString(KEY_AVATAR_URI, "");
    }

    void login(String name, String email) {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    void updateProfile(String name, String email) {
        preferences.edit()
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    void setAvatarUri(String uri) {
        preferences.edit().putString(KEY_AVATAR_URI, uri == null ? "" : uri).apply();
    }

    void logout() {
        preferences.edit().clear().apply();
    }
}
