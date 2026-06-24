package com.example.focus_flow.domain;

import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TimeSegment;
import com.example.focus_flow.domain.assistant.AiPromptBuilder;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AiPromptBuilderTest {
    @Test
    public void taskAdvicePromptOnlyIncludesTaskPlanningSignals() {
        AiPromptBuilder.TaskAdviceContext context = new AiPromptBuilder.TaskAdviceContext();
        context.todayTaskCount = 4;
        context.urgentTaskCount = 1;
        context.hardTaskCount = 2;
        context.completionRate = 0.62;
        context.averageQuality = 3.4;
        context.bestSegment = TimeSegment.MORNING;
        context.topSubjects = Arrays.asList("math", "english");

        String prompt = new AiPromptBuilder().buildTaskAdvicePrompt(context);

        assertTrue(prompt.contains("todayTaskCount"));
        assertTrue(prompt.contains("urgentTaskCount"));
        assertTrue(prompt.contains("math"));
        assertFalse(prompt.toLowerCase().contains("noise"));
        assertFalse(prompt.toLowerCase().contains("distraction"));
        assertFalse(prompt.contains("WHITE_NOISE"));
    }

    @Test
    public void noisePromptIncludesAllowedNoiseTypesAndRelevantContextOnly() {
        AiPromptBuilder.NoiseAdviceContext context = new AiPromptBuilder.NoiseAdviceContext();
        context.subject = "programming";
        context.difficulty = TaskDifficulty.HARD;
        context.currentSegment = TimeSegment.AFTERNOON;
        context.recentPreferredTypes = Arrays.asList(NoiseType.LIBRARY_AMBIENCE, NoiseType.KEYBOARD_TYPING);

        String prompt = new AiPromptBuilder().buildNoiseAdvicePrompt(context);

        for (NoiseType type : NoiseType.values()) {
            assertTrue(prompt.contains(type.name()));
        }
        assertTrue(prompt.contains("programming"));
        assertFalse(prompt.contains("rawTaskTitle"));
        assertFalse(prompt.toLowerCase().contains("deadline"));
    }

    @Test
    public void statsPromptUsesAggregatesAndOmitsRawTaskTitles() {
        AiPromptBuilder.StatsAnalysisContext context = new AiPromptBuilder.StatsAnalysisContext();
        context.period = "week";
        context.totalFocusMinutes = 260;
        context.completedCount = 8;
        context.completionRate = 0.73;
        context.averageQuality = 3.8;
        context.subjectSummaries = Arrays.asList("math:120", "english:80");
        context.rawTaskTitles = Arrays.asList("Private thesis draft", "Hidden exam list");

        String prompt = new AiPromptBuilder().buildStatsAnalysisPrompt(context);

        assertTrue(prompt.contains("totalFocusMinutes"));
        assertTrue(prompt.contains("math:120"));
        assertFalse(prompt.contains("Private thesis draft"));
        assertFalse(prompt.contains("Hidden exam list"));
    }

    @Test
    public void taskRewritePromptIsConciseAnchoredAndJsonOnly() {
        AiPromptBuilder.TaskRewriteContext context = new AiPromptBuilder.TaskRewriteContext();
        context.roughTask = "高数第二章习题";
        context.currentSegment = TimeSegment.EVENING;

        String prompt = new AiPromptBuilder().buildTaskRewritePrompt(context);

        assertTrue("prompt should stay compact for faster API responses", prompt.length() <= 520);
        assertTrue(prompt.contains("高数第二章习题"));
        assertTrue(prompt.contains("EVENING"));
        assertTrue(prompt.contains("只返回JSON数组"));
        assertTrue(prompt.contains("不要解释"));
        assertTrue(prompt.contains("difficulty只能是EASY/NORMAL/HARD/EXTREME"));
        assertTrue(prompt.contains("priority只能是LOW/NORMAL/HIGH/URGENT"));
        assertFalse(prompt.contains("completionRate"));
        assertFalse(prompt.contains("recentPreferredTypes"));
        assertFalse(prompt.contains("subjectSummaries"));
    }
    @Test
    public void promptsRequireSimplifiedChineseUiValues() {
        AiPromptBuilder builder = new AiPromptBuilder();

        assertChineseOnlyInstruction(builder.buildTaskAdvicePrompt(new AiPromptBuilder.TaskAdviceContext()));
        assertChineseOnlyInstruction(builder.buildNoiseAdvicePrompt(new AiPromptBuilder.NoiseAdviceContext()));
        assertChineseOnlyInstruction(builder.buildStatsAnalysisPrompt(new AiPromptBuilder.StatsAnalysisContext()));
        assertChineseOnlyInstruction(builder.buildTaskRewritePrompt(new AiPromptBuilder.TaskRewriteContext()));
    }

    private void assertChineseOnlyInstruction(String prompt) {
        assertTrue(prompt.contains("简体中文"));
        assertTrue(prompt.contains("禁止英文"));
    }
}