package com.example.focus_flow.domain.assistant;

import com.example.focus_flow.core.model.NoiseType;

public class NoiseSetting {
    public final NoiseType type;
    public final int volumePercent;
    public final boolean enabled;

    public NoiseSetting(NoiseType type, int volumePercent, boolean enabled) {
        this.type = type == null ? NoiseType.LIGHT_RAIN : type;
        this.volumePercent = Math.max(0, Math.min(100, volumePercent));
        this.enabled = enabled;
    }
}
