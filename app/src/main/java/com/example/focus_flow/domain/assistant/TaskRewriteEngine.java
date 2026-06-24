package com.example.focus_flow.domain.assistant;

import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.core.model.TimeSegment;

import java.util.ArrayList;
import java.util.List;

public class TaskRewriteEngine {
    public List<TaskRewriteSuggestion> localRewrites(String roughTask, TimeSegment segment) {
        String rough = clean(roughTask);
        List<TaskRewriteSuggestion> result = new ArrayList<>();
        if (rough.length() < 2) {
            return result;
        }
        String subject = inferSubject(rough);
        int baseMinutes = inferMinutes(rough);
        TaskDifficulty difficulty = inferDifficulty(rough);

        result.add(new TaskRewriteSuggestion(
                trimTitle("明确产出：" + rough),
                subject,
                "完成“" + rough + "”并留下一个可检查的结果",
                "先写清交付结果，再完成一轮专注。",
                baseMinutes,
                difficulty,
                TaskPriority.NORMAL,
                AssistantSuggestion.Source.LOCAL));
        result.add(new TaskRewriteSuggestion(
                trimTitle("练习复盘：" + rough),
                subject,
                "完成练习、标记错误，并总结薄弱点",
                "如果任务还不清晰，先用较短的一轮启动。",
                Math.max(25, baseMinutes + 15),
                difficulty == TaskDifficulty.EASY ? TaskDifficulty.NORMAL : difficulty,
                TaskPriority.HIGH,
                AssistantSuggestion.Source.LOCAL));
        result.add(new TaskRewriteSuggestion(
                trimTitle("拆成步骤：" + rough),
                subject,
                "拆成 2 到 3 个专注块，每块都有明确检查点",
                "先从阻力最小的一步开始，降低启动难度。",
                Math.max(30, baseMinutes + 25),
                TaskDifficulty.NORMAL,
                segment == TimeSegment.MORNING ? TaskPriority.HIGH : TaskPriority.NORMAL,
                AssistantSuggestion.Source.LOCAL));
        return result;
    }

    private String inferSubject(String rough) {
        String lower = rough.toLowerCase();
        if (containsAny(lower, "math", "algebra", "calculus", "geometry", "exercise", "数学", "代数", "几何")) {
            return "数学";
        }
        if (containsAny(lower, "english", "vocabulary", "reading", "essay", "英语", "单词", "阅读", "作文")) {
            return "英语";
        }
        if (containsAny(lower, "code", "program", "android", "java", "python", "编程", "代码")) {
            return "编程";
        }
        if (containsAny(lower, "history", "politics", "geography", "历史", "政治", "地理")) {
            return "文科";
        }
        return "学习";
    }

    private int inferMinutes(String rough) {
        String lower = rough.toLowerCase();
        if (containsAny(lower, "chapter", "project", "paper", "章节", "项目", "论文")) {
            return 60;
        }
        if (containsAny(lower, "exercise", "practice", "review", "练习", "复习", "错题")) {
            return 45;
        }
        return 25;
    }

    private TaskDifficulty inferDifficulty(String rough) {
        String lower = rough.toLowerCase();
        if (containsAny(lower, "hard", "exam", "project", "paper", "难", "考试", "项目", "论文")) {
            return TaskDifficulty.HARD;
        }
        if (containsAny(lower, "review", "read", "vocabulary", "复习", "阅读", "单词")) {
            return TaskDifficulty.EASY;
        }
        return TaskDifficulty.NORMAL;
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String trimTitle(String value) {
        String text = clean(value);
        return text.length() <= 30 ? text : text.substring(0, 30);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}