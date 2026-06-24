package com.example.focus_flow.domain.assistant;

import com.example.focus_flow.core.model.DistractionReason;
import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TimeSegment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NoiseRecommendationEngine {
    public static class Context {
        public String subject = "";
        public TaskDifficulty difficulty = TaskDifficulty.NORMAL;
        public TimeSegment currentSegment = TimeSegment.fromMillis(System.currentTimeMillis());
        public DistractionReason topDistractionReason = DistractionReason.NONE;
        public List<NoiseType> recentPreferredTypes = Collections.emptyList();
    }

    public List<NoiseRecommendation> recommend(Context context) {
        Context c = context == null ? new Context() : context;
        List<NoiseRecommendation> result = new ArrayList<>();
        String subject = c.subject == null ? "" : c.subject.toLowerCase();

        if (c.topDistractionReason == DistractionReason.ENVIRONMENT_NOISE) {
            result.add(recommendation("noise-shield", "稳态屏蔽",
                    "最近专注受环境噪声影响，适合用稳定声底做遮蔽。",
                    setting(NoiseType.WHITE_NOISE, 42), setting(NoiseType.BROWN_NOISE, 32)));
        } else if (subject.contains("read") || subject.contains("english")
                || subject.contains("history") || c.currentSegment == TimeSegment.NIGHT) {
            result.add(recommendation("quiet-reading", "轻阅读声场",
                    "语言和记忆类任务需要轻一点的纹理，避免盖住思路。",
                    setting(NoiseType.LIGHT_RAIN, 38), setting(NoiseType.STREAM, 24)));
        } else if (subject.contains("program") || subject.contains("code") || subject.contains("android")) {
            result.add(recommendation("coding-room", "编程自习室",
                    "低刺激空间声加少量键盘声，适合进入持续编码节奏。",
                    setting(NoiseType.LIBRARY_AMBIENCE, 45), setting(NoiseType.KEYBOARD_TYPING, 16)));
        } else if (c.difficulty == TaskDifficulty.HARD || c.difficulty == TaskDifficulty.EXTREME) {
            result.add(recommendation("deep-focus", "深度专注",
                    "高难度任务适合平稳声音，减少突然的听觉峰值。",
                    setting(NoiseType.BROWN_NOISE, 36), setting(NoiseType.WIND, 18)));
        } else {
            result.add(recommendation("soft-nature", "柔和自然",
                    "常规学习用自然底噪即可，不需要过度遮蔽。",
                    setting(NoiseType.FOREST_BIRDS, 34), setting(NoiseType.STREAM, 28)));
        }

        if (c.currentSegment == TimeSegment.NIGHT) {
            result.add(recommendation("night-low", "夜间低音量",
                    "夜间降低音量，避免强遮蔽带来疲劳。",
                    setting(NoiseType.FIREPLACE, 26), setting(NoiseType.BROWN_NOISE, 18)));
        }

        if (c.recentPreferredTypes != null && !c.recentPreferredTypes.isEmpty()) {
            NoiseType preferred = c.recentPreferredTypes.get(0);
            result.add(recommendation("recent-favorite", "沿用近期偏好",
                    "沿用近期完成专注时常出现的声音纹理。",
                    setting(preferred, 40)));
        }
        return result;
    }

    private NoiseRecommendation recommendation(String id, String title, String reason, NoiseSetting... settings) {
        return new NoiseRecommendation(id, title, reason, AssistantSuggestion.Source.LOCAL, Arrays.asList(settings));
    }

    private NoiseSetting setting(NoiseType type, int volume) {
        return new NoiseSetting(type, volume, true);
    }
}
