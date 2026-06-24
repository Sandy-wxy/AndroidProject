package com.example.focus_flow.domain.assistant;

import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TimeSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AiPromptBuilder {
    public static class TaskAdviceContext {
        public int todayTaskCount;
        public int urgentTaskCount;
        public int hardTaskCount;
        public double completionRate;
        public double averageQuality;
        public TimeSegment bestSegment;
        public List<String> topSubjects = Collections.emptyList();
    }

    public static class NoiseAdviceContext {
        public String subject = "";
        public TaskDifficulty difficulty = TaskDifficulty.NORMAL;
        public TimeSegment currentSegment;
        public List<NoiseType> recentPreferredTypes = Collections.emptyList();
    }

    public static class StatsAnalysisContext {
        public String period = "week";
        public int totalFocusMinutes;
        public int completedCount;
        public double completionRate;
        public double averageQuality;
        public List<String> subjectSummaries = Collections.emptyList();
        public List<String> rawTaskTitles = Collections.emptyList();
    }

    public static class TaskRewriteContext {
        public String roughTask = "";
        public TimeSegment currentSegment;
    }

    public String buildTaskAdvicePrompt(TaskAdviceContext context) {
        TaskAdviceContext c = context == null ? new TaskAdviceContext() : context;
        StringBuilder builder = new StringBuilder();
        builder.append("你是 Focus Flow 的学习规划助手。只返回 3 条 JSON 建议。所有 title 和 content 的字段值必须使用简体中文，禁止英文标题、英文解释、Markdown 和代码块。\n");
        builder.append("只使用这些字段：title, content, type=study_plan。字段名保持英文，字段值必须是简体中文。\n");
        builder.append("Context={");
        appendPair(builder, "todayTaskCount", c.todayTaskCount).append(',');
        appendPair(builder, "urgentTaskCount", c.urgentTaskCount).append(',');
        appendPair(builder, "hardTaskCount", c.hardTaskCount).append(',');
        appendPair(builder, "completionRate", round(c.completionRate)).append(',');
        appendPair(builder, "averageQuality", round(c.averageQuality)).append(',');
        appendPair(builder, "bestSegment", c.bestSegment == null ? "" : c.bestSegment.name()).append(',');
        appendArray(builder, "topSubjects", c.topSubjects);
        builder.append("}");
        return builder.toString();
    }

    public String buildNoiseAdvicePrompt(NoiseAdviceContext context) {
        NoiseAdviceContext c = context == null ? new NoiseAdviceContext() : context;
        StringBuilder builder = new StringBuilder();
        builder.append("为 Focus Flow 推荐白噪音设置。只返回 JSON 数组。所有 title 和 reason 的字段值必须使用简体中文，禁止英文标题、英文解释、Markdown 和代码块。\n");
        builder.append("每一项必须包含 title, reason, settings[{type,volumePercent}]。字段名和 type 枚举保持英文，其他展示给用户的字段值必须是简体中文。\n");
        builder.append("allowedNoiseTypes=");
        List<String> allowed = new ArrayList<>();
        for (NoiseType type : NoiseType.values()) {
            allowed.add(type.name());
        }
        appendRawArray(builder, allowed).append('\n');
        builder.append("Context={");
        appendPair(builder, "subject", c.subject).append(',');
        appendPair(builder, "difficulty", c.difficulty == null ? TaskDifficulty.NORMAL.name() : c.difficulty.name()).append(',');
        appendPair(builder, "currentSegment", c.currentSegment == null ? "" : c.currentSegment.name()).append(',');
        List<String> preferred = new ArrayList<>();
        if (c.recentPreferredTypes != null) {
            for (NoiseType type : c.recentPreferredTypes) {
                preferred.add(type.name());
            }
        }
        appendArray(builder, "recentPreferredTypes", preferred);
        builder.append("}");
        return builder.toString();
    }

    public String buildStatsAnalysisPrompt(StatsAnalysisContext context) {
        StatsAnalysisContext c = context == null ? new StatsAnalysisContext() : context;
        StringBuilder builder = new StringBuilder();
        builder.append("分析 Focus Flow 的聚合学习统计。返回简体中文正文，包含分析、周报、月报三个小节，禁止英文标题、英文解释、Markdown 代码块。\n");
        builder.append("不要根据原始任务名推断，只使用聚合数据；不要输出英文小节名。\n");
        builder.append("Context={");
        appendPair(builder, "period", c.period).append(',');
        appendPair(builder, "totalFocusMinutes", c.totalFocusMinutes).append(',');
        appendPair(builder, "completedCount", c.completedCount).append(',');
        appendPair(builder, "completionRate", round(c.completionRate)).append(',');
        appendPair(builder, "averageQuality", round(c.averageQuality)).append(',');
        appendArray(builder, "subjectSummaries", c.subjectSummaries);
        builder.append("}");
        return builder.toString();
    }

    public String buildTaskRewritePrompt(TaskRewriteContext context) {
        TaskRewriteContext c = context == null ? new TaskRewriteContext() : context;
        StringBuilder builder = new StringBuilder();
        builder.append("任务改写。只返回JSON数组，不要解释，不要Markdown，不要代码块。");
        builder.append("围绕roughTask生成3个可直接执行的学习任务，不要换科目。展示值用简体中文，禁止英文解释。");
        builder.append("字段:title,subject,targetOutcome,description,estimatedMinutes,difficulty,priority。");
        builder.append("difficulty只能是EASY/NORMAL/HARD/EXTREME；priority只能是LOW/NORMAL/HIGH/URGENT。");
        builder.append("estimatedMinutes用15-90内合理整数。Context={");
        appendPair(builder, "roughTask", c.roughTask).append(',');
        appendPair(builder, "currentSegment", c.currentSegment == null ? "" : c.currentSegment.name());
        builder.append("}");
        return builder.toString();
    }

    private StringBuilder appendPair(StringBuilder builder, String key, String value) {
        builder.append('"').append(escape(key)).append('"').append(':')
                .append('"').append(escape(value)).append('"');
        return builder;
    }

    private StringBuilder appendPair(StringBuilder builder, String key, int value) {
        builder.append('"').append(escape(key)).append('"').append(':').append(value);
        return builder;
    }

    private StringBuilder appendPair(StringBuilder builder, String key, double value) {
        builder.append('"').append(escape(key)).append('"').append(':').append(value);
        return builder;
    }

    private void appendArray(StringBuilder builder, String key, List<String> values) {
        builder.append('"').append(escape(key)).append('"').append(':');
        appendRawArray(builder, values == null ? Collections.emptyList() : values);
    }

    private StringBuilder appendRawArray(StringBuilder builder, List<String> values) {
        builder.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) builder.append(',');
            builder.append('"').append(escape(values.get(i))).append('"');
        }
        builder.append(']');
        return builder;
    }

    private double round(double value) {
        return Double.parseDouble(String.format(Locale.US, "%.2f", value));
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\n");
    }
}
