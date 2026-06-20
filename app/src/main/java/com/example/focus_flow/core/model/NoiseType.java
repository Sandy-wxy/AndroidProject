package com.example.focus_flow.core.model;

public enum NoiseType {
    LIGHT_RAIN("轻雨声", "noise_light_rain"),
    HEAVY_RAIN("暴雨声", "noise_heavy_rain"),
    OCEAN_WAVES("海浪声", "noise_ocean_waves"),
    FOREST_BIRDS("森林鸟鸣", "noise_forest_birds"),
    CAFE_AMBIENCE("咖啡馆环境", "noise_cafe_ambience"),
    LIBRARY_AMBIENCE("图书馆环境", "noise_library_ambience"),
    FIREPLACE("篝火声", "noise_fireplace"),
    WIND("柔风声", "noise_wind"),
    STREAM("溪流水声", "noise_stream"),
    WHITE_NOISE("白噪声", "noise_white"),
    BROWN_NOISE("棕噪声", "noise_brown"),
    KEYBOARD_TYPING("键盘敲击", "noise_keyboard_typing");

    public final String displayName;
    public final String resourceName;

    NoiseType(String displayName, String resourceName) {
        this.displayName = displayName;
        this.resourceName = resourceName;
    }

    public static NoiseType fromString(String value) {
        try {
            return value == null ? LIGHT_RAIN : NoiseType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return LIGHT_RAIN;
        }
    }
}
