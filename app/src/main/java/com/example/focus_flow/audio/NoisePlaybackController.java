package com.example.focus_flow.audio;

import android.content.Context;
import android.media.MediaPlayer;

import com.example.focus_flow.R;
import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.data.local.model.NoiseMixItemRecord;
import com.example.focus_flow.data.local.model.NoiseMixRecord;

import java.util.EnumMap;
import java.util.Map;

public class NoisePlaybackController {
    private static NoisePlaybackController instance;

    private final Map<NoiseType, MediaPlayer> players = new EnumMap<>(NoiseType.class);

    private NoisePlaybackController() {
    }

    public static synchronized NoisePlaybackController get() {
        if (instance == null) {
            instance = new NoisePlaybackController();
        }
        return instance;
    }

    public synchronized void applyMix(Context context, NoiseMixRecord mix, int masterVolume) {
        stopAll();
        if (mix == null) {
            return;
        }
        for (NoiseMixItemRecord item : mix.items) {
            if (item.enabled && item.volumePercent > 0) {
                playType(context, item.noiseType, item.volumePercent, masterVolume);
            }
        }
    }

    public synchronized void applyLevels(Context context,
                                         Map<NoiseType, Integer> volumes,
                                         Map<NoiseType, Boolean> enabled,
                                         int masterVolume) {
        stopAll();
        for (NoiseType type : NoiseType.values()) {
            boolean shouldPlay = Boolean.TRUE.equals(enabled.get(type));
            int volume = volumes.containsKey(type) ? volumes.get(type) : 0;
            if (shouldPlay && volume > 0) {
                playType(context, type, volume, masterVolume);
            }
        }
    }

    public synchronized void stopAll() {
        for (MediaPlayer player : players.values()) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
                // Player may already be stopped by the platform.
            }
            player.release();
        }
        players.clear();
    }

    public synchronized boolean isPlaying() {
        for (MediaPlayer player : players.values()) {
            if (player.isPlaying()) {
                return true;
            }
        }
        return false;
    }

    private void playType(Context context, NoiseType type, int volumePercent, int masterVolume) {
        int resId = rawResFor(type);
        if (resId == 0) {
            return;
        }
        MediaPlayer player = MediaPlayer.create(context.getApplicationContext(), resId);
        if (player == null) {
            return;
        }
        float volume = Math.max(0f, Math.min(1f, volumePercent / 100f * masterVolume / 100f));
        player.setLooping(true);
        player.setVolume(volume, volume);
        player.start();
        players.put(type, player);
    }

    private int rawResFor(NoiseType type) {
        switch (type) {
            case HEAVY_RAIN:
                return R.raw.noise_heavy_rain;
            case OCEAN_WAVES:
                return R.raw.noise_ocean_waves;
            case FOREST_BIRDS:
                return R.raw.noise_forest_birds;
            case CAFE_AMBIENCE:
                return R.raw.noise_cafe_ambience;
            case LIBRARY_AMBIENCE:
                return R.raw.noise_library_ambience;
            case FIREPLACE:
                return R.raw.noise_fireplace;
            case WIND:
                return R.raw.noise_wind;
            case STREAM:
                return R.raw.noise_stream;
            case WHITE_NOISE:
                return R.raw.noise_white;
            case BROWN_NOISE:
                return R.raw.noise_brown;
            case KEYBOARD_TYPING:
                return R.raw.noise_keyboard_typing;
            case LIGHT_RAIN:
            default:
                return R.raw.noise_light_rain;
        }
    }
}
