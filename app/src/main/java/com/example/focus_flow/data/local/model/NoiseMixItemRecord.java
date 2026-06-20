package com.example.focus_flow.data.local.model;

import com.example.focus_flow.core.model.NoiseType;

public class NoiseMixItemRecord {
    public long id;
    public long mixId;
    public NoiseType noiseType;
    public int volumePercent;
    public boolean enabled;

    public NoiseMixItemRecord() {
        noiseType = NoiseType.LIGHT_RAIN;
        volumePercent = 60;
        enabled = true;
    }
}
