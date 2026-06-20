package com.example.focus_flow.data.repository;

import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.focus_flow.data.local.datasource.NoiseMixLocalDataSource;
import com.example.focus_flow.data.local.model.NoiseMixRecord;
import com.example.focus_flow.data.preferences.SettingsPreferences;

import java.util.ArrayList;
import java.util.List;

public class NoiseMixRepository {
    private final NoiseMixLocalDataSource localDataSource;
    private final SettingsPreferences settingsPreferences;
    private final MutableLiveData<List<NoiseMixRecord>> allMixes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<NoiseMixRecord> currentMix = new MutableLiveData<>();

    public NoiseMixRepository(NoiseMixLocalDataSource localDataSource, SettingsPreferences settingsPreferences) {
        this.localDataSource = localDataSource;
        this.settingsPreferences = settingsPreferences;
    }

    public LiveData<List<NoiseMixRecord>> observeAllMixes() {
        return allMixes;
    }

    public LiveData<NoiseMixRecord> observeCurrentMix() {
        return currentMix;
    }

    public void restorePresetMixesIfMissing() {
        localDataSource.restorePresetMixesIfMissing();
        refresh();
    }

    public long saveCustomMix(NoiseMixRecord mix) {
        mix.isPreset = false;
        mix.createdAt = System.currentTimeMillis();
        mix.updatedAt = mix.createdAt;
        long id = localDataSource.insertMixWithItems(mix).id;
        refresh();
        return id;
    }

    public void applyMix(long mixId) {
        settingsPreferences.setCurrentNoiseMixId(mixId);
        refresh();
    }

    public void deleteCustomMix(long mixId) {
        localDataSource.deleteCustomMix(mixId);
        if (settingsPreferences.getCurrentNoiseMixId() == mixId) {
            settingsPreferences.setCurrentNoiseMixId(-1);
        }
        refresh();
    }

    public List<NoiseMixRecord> getAllMixes() {
        return localDataSource.getAllMixes();
    }

    public NoiseMixRecord getCurrentMix() {
        long id = settingsPreferences.getCurrentNoiseMixId();
        NoiseMixRecord mix = id > 0 ? localDataSource.getMixWithItems(id) : null;
        if (mix == null) {
            List<NoiseMixRecord> mixes = localDataSource.getAllMixes();
            mix = mixes.isEmpty() ? null : mixes.get(0);
            if (mix != null) {
                settingsPreferences.setCurrentNoiseMixId(mix.id);
            }
        }
        return mix;
    }

    public void refresh() {
        publish(allMixes, localDataSource.getAllMixes());
        publish(currentMix, getCurrentMix());
    }

    private <T> void publish(MutableLiveData<T> liveData, T value) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            liveData.setValue(value);
        } else {
            liveData.postValue(value);
        }
    }
}
