package com.example.focus_flow;

import android.app.Application;

import com.example.focus_flow.core.ui.ThemeApplier;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.feature.sync.CloudSyncManager;
import com.example.focus_flow.feature.sync.CloudSyncPreferences;

public class FocusFlowApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RepositoryProvider provider = RepositoryProvider.get(this);
        ThemeApplier.apply(provider.settingsRepository.getThemeMode());
        CloudSyncPreferences cloud = new CloudSyncPreferences(this);
        if (cloud.autoSync() && cloud.isConfigured()) {
            new CloudSyncManager(this).sync(new CloudSyncManager.Callback() {
                @Override
                public void onSuccess(String message) {
                }

                @Override
                public void onError(String message) {
                }
            });
        }
    }
}
