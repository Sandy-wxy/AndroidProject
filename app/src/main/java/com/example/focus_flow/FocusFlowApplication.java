package com.example.focus_flow;

import android.app.Application;

import com.example.focus_flow.core.ui.ThemeApplier;
import com.example.focus_flow.data.repository.RepositoryProvider;

public class FocusFlowApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RepositoryProvider provider = RepositoryProvider.get(this);
        ThemeApplier.apply(provider.settingsRepository.getThemeMode());
    }
}
