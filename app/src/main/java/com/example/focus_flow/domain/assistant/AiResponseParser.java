package com.example.focus_flow.domain.assistant;

import com.example.focus_flow.core.model.NoiseType;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AiResponseParser {
    public String parseResultText(String responseBody) {
        String result = extractResult(responseBody);
        if (result.trim().startsWith("{")) {
            String structured = structuredStatsText(result);
            if (!structured.isEmpty()) {
                return structured;
            }
        }
        return hasChinese(result) ? result.trim() : "";
    }

    public List<AssistantSuggestion> parseSuggestions(String responseBody,
                                                       AssistantSuggestion.Category fallbackCategory) {
        String result = extractResult(responseBody);
        if (result.isEmpty()) {
            return new ArrayList<>();
        }
        List<AssistantSuggestion> structured = parseSuggestionValue(result, fallbackCategory);
        if (!structured.isEmpty()) {
            return structured;
        }
        return parsePlainSuggestions(result, fallbackCategory);
    }

    public List<TaskRewriteSuggestion> parseTaskRewrites(String responseBody) {
        String result = extractResult(responseBody);
        List<String> objects = objectTexts(result);
        List<TaskRewriteSuggestion> suggestions = new ArrayList<>();
        for (String object : objects) {
            String title = chineseOrEmpty(stringField(object, "title"));
            String subject = chineseOrEmpty(stringField(object, "subject"));
            String target = chineseOrEmpty(firstNonEmpty(stringField(object, "targetOutcome"), stringField(object, "target")));
            String description = chineseOrEmpty(stringField(object, "description"));
            int minutes = intField(object, "estimatedMinutes", 25);
            TaskDifficulty difficulty = parseDifficulty(stringField(object, "difficulty"));
            TaskPriority priority = parsePriority(stringField(object, "priority"));
            if (!title.isEmpty() && !target.isEmpty()) {
                suggestions.add(new TaskRewriteSuggestion(title, subject, target, description, minutes,
                        difficulty, priority, AssistantSuggestion.Source.API));
            }
        }
        return suggestions;
    }

    public List<NoiseRecommendation> parseNoiseRecommendations(String responseBody) {
        String result = extractResult(responseBody);
        List<String> objects = objectTexts(result);
        List<NoiseRecommendation> recommendations = new ArrayList<>();
        int index = 0;
        for (String object : objects) {
            List<NoiseSetting> settings = parseNoiseSettings(object);
            if (settings.isEmpty()) {
                continue;
            }
            String title = firstNonEmpty(chineseOrEmpty(stringField(object, "title")),
                    "智能白噪音 " + (index + 1));
            String reason = firstNonEmpty(chineseOrEmpty(firstNonEmpty(stringField(object, "reason"),
                    stringField(object, "content"))), "已根据当前学习场景匹配声音组合。");
            recommendations.add(new NoiseRecommendation("api-noise-" + index, title, reason,
                    AssistantSuggestion.Source.API, settings));
            index++;
        }
        return recommendations;
    }

    private List<AssistantSuggestion> parseSuggestionValue(String raw,
                                                           AssistantSuggestion.Category fallbackCategory) {
        String value = raw.trim();
        if (value.startsWith("{") && value.contains("\"suggestions\"")) {
            value = arrayField(value, "suggestions");
        }
        if (!(value.startsWith("[") || value.startsWith("{"))) {
            return new ArrayList<>();
        }
        List<String> objects = objectTexts(value);
        List<AssistantSuggestion> suggestions = new ArrayList<>();
        int index = 0;
        for (String object : objects) {
            AssistantSuggestion.Category category = categoryFrom(firstNonEmpty(stringField(object, "type"),
                    stringField(object, "category")), fallbackCategory);
            String title = firstNonEmpty(chineseOrEmpty(stringField(object, "title")), fallbackTitle(category, index));
            String content = chineseOrEmpty(firstNonEmpty(stringField(object, "content"), stringField(object, "reason")));
            if (!content.isEmpty()) {
                suggestions.add(AssistantSuggestion.api("api-" + category.name().toLowerCase(Locale.US)
                        + "-" + index, category, title, content));
                index++;
            }
        }
        return suggestions;
    }

    private List<AssistantSuggestion> parsePlainSuggestions(String text,
                                                            AssistantSuggestion.Category fallbackCategory) {
        List<AssistantSuggestion> suggestions = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        int index = 0;
        for (String line : lines) {
            String cleaned = line.trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            cleaned = cleaned.replaceFirst("^[-*\\s]*\\d+[\\.)、]\\s*", "");
            int colon = firstColon(cleaned);
            String title = fallbackTitle(fallbackCategory, index);
            String content = cleaned;
            if (colon > 0) {
                String rawTitle = cleaned.substring(0, colon).trim();
                String rawContent = cleaned.substring(colon + 1).trim();
                title = firstNonEmpty(chineseOrEmpty(rawTitle), title);
                content = rawContent;
            }
            content = chineseOrEmpty(content);
            if (content.isEmpty()) {
                continue;
            }
            suggestions.add(AssistantSuggestion.api("api-text-" + index, fallbackCategory, title, content));
            index++;
        }
        if (suggestions.isEmpty() && hasChinese(text)) {
            suggestions.add(AssistantSuggestion.api("api-text-0", fallbackCategory,
                    fallbackTitle(fallbackCategory, 0), text.trim()));
        }
        return suggestions;
    }

    private TaskDifficulty parseDifficulty(String value) {
        String text = value == null ? "" : value.trim().toUpperCase(Locale.US);
        if (text.contains("简单") || text.contains("容易") || text.equals("EASY")) {
            return TaskDifficulty.EASY;
        }
        if (text.contains("极难") || text.contains("高压") || text.equals("EXTREME")) {
            return TaskDifficulty.EXTREME;
        }
        if (text.contains("困难") || text.equals("难") || text.contains("高难") || text.equals("HARD")) {
            return TaskDifficulty.HARD;
        }
        if (text.contains("中等") || text.equals("中") || text.contains("普通")
                || text.contains("正常") || text.equals("NORMAL")) {
            return TaskDifficulty.NORMAL;
        }
        return TaskDifficulty.fromString(value == null ? "" : value.trim().toUpperCase(Locale.US));
    }

    private TaskPriority parsePriority(String value) {
        String text = value == null ? "" : value.trim().toUpperCase(Locale.US);
        if (text.contains("紧急") || text.equals("急") || text.equals("URGENT")) {
            return TaskPriority.URGENT;
        }
        if (text.equals("高") || text.contains("重要") || text.equals("HIGH")) {
            return TaskPriority.HIGH;
        }
        if (text.equals("低") || text.equals("LOW")) {
            return TaskPriority.LOW;
        }
        if (text.equals("中") || text.contains("普通") || text.contains("正常") || text.equals("NORMAL")) {
            return TaskPriority.NORMAL;
        }
        return TaskPriority.fromString(value == null ? "" : value.trim().toUpperCase(Locale.US));
    }
    private List<NoiseSetting> parseNoiseSettings(String object) {
        String array = arrayField(object, "settings");
        List<NoiseSetting> result = new ArrayList<>();
        for (String settingObject : objectTexts(array)) {
            NoiseType type = NoiseType.fromString(firstNonEmpty(stringField(settingObject, "type"),
                    stringField(settingObject, "noiseType")));
            int volume = intField(settingObject, "volumePercent", intField(settingObject, "volume", 0));
            if (volume > 0) {
                result.add(new NoiseSetting(type, volume, true));
            }
        }
        return result;
    }

    private String structuredStatsText(String object) {
        String analysis = chineseOrEmpty(stringField(object, "analysis"));
        String weekly = chineseOrEmpty(stringField(object, "weeklyReport"));
        String monthly = chineseOrEmpty(stringField(object, "monthlyReport"));
        StringBuilder builder = new StringBuilder();
        if (!analysis.isEmpty()) {
            builder.append("分析：").append(analysis);
        }
        if (!weekly.isEmpty()) {
            if (builder.length() > 0) builder.append('\n');
            builder.append("周报：").append(weekly);
        }
        if (!monthly.isEmpty()) {
            if (builder.length() > 0) builder.append('\n');
            builder.append("月报：").append(monthly);
        }
        return builder.toString();
    }

    private String fallbackTitle(AssistantSuggestion.Category category, int index) {
        AssistantSuggestion.Category safe = category == null ? AssistantSuggestion.Category.STUDY_PLAN : category;
        if (safe == AssistantSuggestion.Category.NOISE) {
            return "智能白噪音 " + (index + 1);
        }
        if (safe == AssistantSuggestion.Category.TASK_REWRITE) {
            return "智能改写 " + (index + 1);
        }
        if (safe == AssistantSuggestion.Category.STATS) {
            return "智能分析 " + (index + 1);
        }
        return "智能建议 " + (index + 1);
    }

    private String chineseOrEmpty(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || !hasChinese(text) || mostlyEnglish(text)) {
            return "";
        }
        return text;
    }

    private boolean hasChinese(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '\u4e00' && c <= '\u9fff') {
                return true;
            }
        }
        return false;
    }

    private boolean mostlyEnglish(String value) {
        int latin = 0;
        int chinese = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                latin++;
            } else if (c >= '\u4e00' && c <= '\u9fff') {
                chinese++;
            }
        }
        return chinese == 0 || latin > chinese * 2;
    }

    private String extractResult(String body) {
        String value = jsonValue(body == null ? "" : body, "result");
        if (value.isEmpty()) {
            return body == null ? "" : body.trim();
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("[") || trimmed.startsWith("{")) && trimmed.contains("\\\"")) {
            return unescape(trimmed);
        }
        return trimmed;
    }

    private String jsonValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex < 0) {
            return "";
        }
        int colon = json.indexOf(':', keyIndex);
        if (colon < 0) {
            return "";
        }
        int start = skipWhitespace(json, colon + 1);
        if (start >= json.length()) {
            return "";
        }
        char first = json.charAt(start);
        if (first == '"') {
            ParseResult parsed = parseQuoted(json, start);
            return parsed.value;
        }
        if (first == '{' || first == '[') {
            int end = balancedEnd(json, start);
            return end > start ? json.substring(start, end + 1) : "";
        }
        int end = start;
        while (end < json.length() && ",}\n\r".indexOf(json.charAt(end)) < 0) {
            end++;
        }
        return json.substring(start, end).trim();
    }

    private String arrayField(String object, String key) {
        String value = jsonValue(object, key);
        return value.startsWith("[") ? value : "";
    }

    private String stringField(String object, String key) {
        String value = jsonValue(object, key);
        if (value.startsWith("{") || value.startsWith("[")) {
            return "";
        }
        return value.trim();
    }

    private int intField(String object, String key, int fallback) {
        String value = stringField(object, key);
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private List<String> objectTexts(String value) {
        List<String> objects = new ArrayList<>();
        String text = value == null ? "" : value.trim();
        if (text.startsWith("{") && !text.contains("\"suggestions\"")) {
            objects.add(text);
            return objects;
        }
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '{') {
                int end = balancedEnd(text, i);
                if (end > i) {
                    objects.add(text.substring(i, end + 1));
                    i = end;
                }
            }
        }
        return objects;
    }

    private int balancedEnd(String text, int start) {
        char open = text.charAt(start);
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == open) depth++;
            if (c == close) depth--;
            if (depth == 0) return i;
        }
        return -1;
    }

    private ParseResult parseQuoted(String text, int start) {
        StringBuilder builder = new StringBuilder();
        boolean escape = false;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) {
                if (c == 'n') builder.append('\n');
                else if (c == 'r') builder.append('\r');
                else if (c == 't') builder.append('\t');
                else builder.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                return new ParseResult(builder.toString(), i);
            }
            builder.append(c);
        }
        return new ParseResult(builder.toString(), text.length());
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
    }

    private int skipWhitespace(String text, int index) {
        int i = index;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return i;
    }

    private int firstColon(String text) {
        int english = text.indexOf(':');
        int chinese = text.indexOf('：');
        if (english < 0) return chinese;
        if (chinese < 0) return english;
        return Math.min(english, chinese);
    }

    private AssistantSuggestion.Category categoryFrom(String raw, AssistantSuggestion.Category fallback) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.US);
        if (value.contains("NOISE")) return AssistantSuggestion.Category.NOISE;
        if (value.contains("REWRITE")) return AssistantSuggestion.Category.TASK_REWRITE;
        if (value.contains("STAT")) return AssistantSuggestion.Category.STATS;
        if (value.contains("STUDY")) return AssistantSuggestion.Category.STUDY_PLAN;
        return fallback == null ? AssistantSuggestion.Category.STUDY_PLAN : fallback;
    }

    private String firstNonEmpty(String first, String second) {
        return first == null || first.trim().isEmpty() ? (second == null ? "" : second.trim()) : first.trim();
    }

    private static class ParseResult {
        final String value;
        final int endIndex;

        ParseResult(String value, int endIndex) {
            this.value = value;
            this.endIndex = endIndex;
        }
    }
}