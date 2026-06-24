package com.example.focus_flow.domain;

import com.example.focus_flow.core.model.DistractionReason;
import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.core.model.TimeSegment;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.domain.assistant.AiResponseParser;
import com.example.focus_flow.domain.assistant.AssistantSuggestion;
import com.example.focus_flow.domain.assistant.NoiseRecommendation;
import com.example.focus_flow.domain.assistant.NoiseRecommendationEngine;
import com.example.focus_flow.domain.assistant.NoiseSetting;
import com.example.focus_flow.domain.assistant.StudyStrategyEngine;
import com.example.focus_flow.domain.assistant.SuggestionQueue;
import com.example.focus_flow.domain.assistant.TaskRewriteEngine;
import com.example.focus_flow.domain.assistant.TaskRewriteSession;
import com.example.focus_flow.domain.assistant.TaskRewriteSuggestion;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class AssistantEngineTest {
    private static final long NOW = 1_800_000_000_000L;

    @Test
    public void suggestionQueueKeepsLocalItemsAfterApiMerge() {
        AssistantSuggestion localOne = AssistantSuggestion.local(
                "local-focus", AssistantSuggestion.Category.STUDY_PLAN,
                "本地专注", "先处理最难的待办任务。");
        AssistantSuggestion localTwo = AssistantSuggestion.local(
                "local-break", AssistantSuggestion.Category.STUDY_PLAN,
                "本地短时段", "先用更短的专注块启动。");
        SuggestionQueue queue = new SuggestionQueue(Arrays.asList(localOne, localTwo));

        queue.mergeApiSuggestions(Arrays.asList(
                AssistantSuggestion.api("api-plan", AssistantSuggestion.Category.STUDY_PLAN,
                        "接口建议", "把高优先级任务放在上午完成。"),
                AssistantSuggestion.api("duplicate", AssistantSuggestion.Category.STUDY_PLAN,
                        "本地专注", "先处理最难的待办任务。")));

        assertEquals(3, queue.items().size());
        assertTrue(queue.items().contains(localOne));
        assertTrue(queue.items().contains(localTwo));
        assertEquals("本地专注", queue.current().title);
        assertEquals("本地短时段", queue.next().title);
        assertEquals("接口建议", queue.next().title);
        assertEquals("本地专注", queue.next().title);
    }

    @Test
    public void suggestionQueueFallsBackToLocalWhenApiIsEmpty() {
        AssistantSuggestion local = AssistantSuggestion.local(
                "local-only", AssistantSuggestion.Category.STUDY_PLAN,
                "本地兜底", "保持当前计划。");
        SuggestionQueue queue = new SuggestionQueue(Arrays.asList(local));

        queue.mergeApiSuggestions(java.util.Collections.emptyList());

        assertEquals(1, queue.items().size());
        assertEquals("本地兜底", queue.current().title);
    }

    @Test
    public void parserAcceptsStringObjectAndJsonStringResults() {
        AiResponseParser parser = new AiResponseParser();
        String textBody = "{\"ok\":true,\"result\":\"1. 先做数学：把上午留给数学任务。\\n2. 拆小任务：先完成一个可检查的小步骤。\"}";
        String objectBody = "{\"ok\":true,\"result\":{\"suggestions\":[{\"title\":\"利用上午\",\"content\":\"中午前先完成数学任务\",\"type\":\"study_plan\"}]}}";
        String arrayAsStringBody = "{\"ok\":true,\"result\":\"[{\\\"title\\\":\\\"雨声专注\\\",\\\"content\\\":\\\"只开启轻雨声即可。\\\",\\\"type\\\":\\\"noise\\\"}]\"}";

        List<AssistantSuggestion> textSuggestions = parser.parseSuggestions(
                textBody, AssistantSuggestion.Category.STUDY_PLAN);
        List<AssistantSuggestion> objectSuggestions = parser.parseSuggestions(
                objectBody, AssistantSuggestion.Category.STUDY_PLAN);
        List<AssistantSuggestion> jsonStringSuggestions = parser.parseSuggestions(
                arrayAsStringBody, AssistantSuggestion.Category.NOISE);

        assertEquals(2, textSuggestions.size());
        assertEquals("先做数学", textSuggestions.get(0).title);
        assertEquals(1, objectSuggestions.size());
        assertEquals("利用上午", objectSuggestions.get(0).title);
        assertEquals(1, jsonStringSuggestions.size());
        assertEquals(AssistantSuggestion.Category.NOISE, jsonStringSuggestions.get(0).category);
    }

    @Test
    public void parserRejectsEnglishUiTextAndUsesChineseFallbackTitles() {
        AiResponseParser parser = new AiResponseParser();
        String englishBody = "{\"ok\":true,\"result\":\"1. Start early: Use the morning for math.\"}";
        String missingTitleBody = "{\"ok\":true,\"result\":\"[{\\\"content\\\":\\\"先完成最重要的一步。\\\",\\\"type\\\":\\\"study_plan\\\"}]\"}";
        String missingNoiseTitleBody = "{\"ok\":true,\"result\":\"[{\\\"reason\\\":\\\"适合阅读时降低环境干扰。\\\",\\\"settings\\\":[{\\\"type\\\":\\\"LIGHT_RAIN\\\",\\\"volumePercent\\\":35}]}]\"}";

        assertTrue(parser.parseSuggestions(englishBody, AssistantSuggestion.Category.STUDY_PLAN).isEmpty());
        List<AssistantSuggestion> suggestions = parser.parseSuggestions(
                missingTitleBody, AssistantSuggestion.Category.STUDY_PLAN);
        assertEquals("智能建议 1", suggestions.get(0).title);
        List<NoiseRecommendation> recommendations = parser.parseNoiseRecommendations(missingNoiseTitleBody);
        assertEquals("智能白噪音 1", recommendations.get(0).title);
    }


    @Test
    public void parserAcceptsTaskRewriteMarkdownJsonAndChineseEnums() {
        AiResponseParser parser = new AiResponseParser();
        String body = "{\"ok\":true,\"result\":\"好的：\\n```json\\n[\\n" +
                "{\\\"title\\\":\\\"完成高数第二章习题A组\\\",\\\"subject\\\":\\\"高等数学\\\","
                + "\\\"targetOutcome\\\":\\\"完成10道极限练习并标记错题\\\","
                + "\\\"description\\\":\\\"先做基础题，再整理2个易错点。\\\","
                + "\\\"estimatedMinutes\\\":35,\\\"difficulty\\\":\\\"中等\\\",\\\"priority\\\":\\\"高\\\"},"
                + "{\\\"title\\\":\\\"复盘高数第二章错题\\\",\\\"subject\\\":\\\"高等数学\\\","
                + "\\\"targetOutcome\\\":\\\"写出3条错因和对应修正方法\\\","
                + "\\\"description\\\":\\\"对照答案复盘，把不会的题标为下次专注重点。\\\","
                + "\\\"estimatedMinutes\\\":25,\\\"difficulty\\\":\\\"简单\\\",\\\"priority\\\":\\\"中\\\"}\\n]\\n```\"}";

        List<TaskRewriteSuggestion> suggestions = parser.parseTaskRewrites(body);

        assertEquals(2, suggestions.size());
        assertEquals("完成高数第二章习题A组", suggestions.get(0).title);
        assertEquals("高等数学", suggestions.get(0).subject);
        assertEquals(35, suggestions.get(0).estimatedMinutes);
        assertEquals(TaskDifficulty.NORMAL, suggestions.get(0).difficulty);
        assertEquals(TaskPriority.HIGH, suggestions.get(0).priority);
        assertEquals(TaskDifficulty.EASY, suggestions.get(1).difficulty);
        assertEquals(TaskPriority.NORMAL, suggestions.get(1).priority);
    }
    @Test
    public void localStudyStrategyCombinesDeadlineQualityAndEfficientTime() {
        StudyStrategyEngine.Context context = new StudyStrategyEngine.Context();
        context.nowMillis = NOW;
        context.completionRate = 0.42;
        context.averageQuality = 2.4;
        context.bestSegment = TimeSegment.MORNING;
        context.currentSegment = TimeSegment.EVENING;
        context.tasks = Arrays.asList(
                task("Linear algebra", "math", TaskDifficulty.HARD, TaskPriority.URGENT, 160,
                        NOW + 8L * 60L * 60L * 1000L),
                task("Vocabulary", "english", TaskDifficulty.EASY, TaskPriority.NORMAL, 30, null));

        List<AssistantSuggestion> suggestions = new StudyStrategyEngine().generate(context);

        assertTrue(suggestions.size() >= 3);
        assertTrue(containsContent(suggestions, "紧急"));
        assertTrue(containsContent(suggestions, "短"));
        assertTrue(containsContent(suggestions, "上午"));
    }

    @Test
    public void noiseRulesChooseDifferentScenesInsteadOfAlwaysMixingEverything() {
        NoiseRecommendationEngine engine = new NoiseRecommendationEngine();

        NoiseRecommendationEngine.Context reading = new NoiseRecommendationEngine.Context();
        reading.subject = "reading";
        reading.difficulty = TaskDifficulty.EASY;
        reading.currentSegment = TimeSegment.NIGHT;
        reading.topDistractionReason = DistractionReason.NONE;

        NoiseRecommendationEngine.Context noisyCoding = new NoiseRecommendationEngine.Context();
        noisyCoding.subject = "programming";
        noisyCoding.difficulty = TaskDifficulty.HARD;
        noisyCoding.currentSegment = TimeSegment.AFTERNOON;
        noisyCoding.topDistractionReason = DistractionReason.ENVIRONMENT_NOISE;

        NoiseRecommendation readingRecommendation = engine.recommend(reading).get(0);
        NoiseRecommendation noisyRecommendation = engine.recommend(noisyCoding).get(0);

        assertTrue(readingRecommendation.contains(NoiseType.LIGHT_RAIN)
                || readingRecommendation.contains(NoiseType.STREAM)
                || readingRecommendation.contains(NoiseType.FIREPLACE));
        assertFalse(readingRecommendation.contains(NoiseType.WHITE_NOISE)
                && readingRecommendation.contains(NoiseType.BROWN_NOISE));
        assertTrue(noisyRecommendation.contains(NoiseType.WHITE_NOISE)
                || noisyRecommendation.contains(NoiseType.BROWN_NOISE));
        assertNotEquals(readingRecommendation.settingSignature(), noisyRecommendation.settingSignature());
        for (NoiseSetting setting : noisyRecommendation.settings) {
            assertTrue(setting.volumePercent >= 0);
            assertTrue(setting.volumePercent <= 100);
        }
    }

    @Test
    public void taskRewriteOffersThreeEnhancementsPerChangedRoughTask() {
        TaskRewriteEngine engine = new TaskRewriteEngine();
        TaskRewriteSession session = new TaskRewriteSession(engine);

        List<TaskRewriteSuggestion> suggestions = session.requestOnce("chapter 3 exercises");

        assertEquals(3, suggestions.size());
        assertFalse(suggestions.get(0).title.trim().isEmpty());
        assertFalse(suggestions.get(0).targetOutcome.trim().isEmpty());
        assertTrue(suggestions.get(0).estimatedMinutes >= 10);
        assertEquals(3, session.requestOnce("chapter 3 exercises with notes").size());
        assertTrue(session.requestOnce("chapter 3 exercises with notes").isEmpty());
    }

    @Test
    public void localTaskRewriteSuggestionsAreChinese() {
        List<TaskRewriteSuggestion> suggestions = new TaskRewriteEngine()
                .localRewrites("chapter 3 exercises", TimeSegment.MORNING);

        assertEquals(3, suggestions.size());
        assertTrue(containsChinese(suggestions.get(0).title));
        assertTrue(containsChinese(suggestions.get(0).targetOutcome));
        assertFalse(containsAsciiWord(suggestions.get(0).description));
    }
    private boolean containsContent(List<AssistantSuggestion> suggestions, String needle) {
        for (AssistantSuggestion suggestion : suggestions) {
            String combined = (suggestion.title + " " + suggestion.content).toLowerCase();
            if (combined.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsChinese(String value) {
        return value != null && value.matches(".*[\\u4e00-\\u9fa5].*");
    }

    private boolean containsAsciiWord(String value) {
        return value != null && value.matches(".*[A-Za-z]{3,}.*");
    }

    private TaskRecord task(String title, String subject, TaskDifficulty difficulty,
                            TaskPriority priority, int minutes, Long deadline) {
        TaskRecord task = new TaskRecord();
        task.id = Math.abs(title.hashCode());
        task.title = title;
        task.subject = subject;
        task.targetOutcome = "Finish " + title;
        task.description = "";
        task.difficulty = difficulty;
        task.priority = priority;
        task.estimatedTotalMinutes = minutes;
        task.deadlineAt = deadline;
        task.plannedDate = "2026-06-23";
        task.status = TaskStatus.PENDING;
        return task;
    }
}
