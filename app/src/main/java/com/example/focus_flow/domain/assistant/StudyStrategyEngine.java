package com.example.focus_flow.domain.assistant;

import com.example.focus_flow.core.model.TaskPriority;
import com.example.focus_flow.core.model.TimeSegment;
import com.example.focus_flow.data.local.model.TaskRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudyStrategyEngine {
    public static class Context {
        public long nowMillis = System.currentTimeMillis();
        public double completionRate = 0.70;
        public double averageQuality = 3.5;
        public TimeSegment bestSegment = TimeSegment.fromMillis(System.currentTimeMillis());
        public TimeSegment currentSegment = TimeSegment.fromMillis(System.currentTimeMillis());
        public List<TaskRecord> tasks = Collections.emptyList();
    }

    public List<AssistantSuggestion> generate(Context context) {
        Context c = context == null ? new Context() : context;
        List<AssistantSuggestion> result = new ArrayList<>();
        List<TaskRecord> tasks = c.tasks == null ? Collections.emptyList() : c.tasks;
        TaskRecord urgent = mostUrgent(tasks, c.nowMillis);
        if (urgent != null) {
            result.add(AssistantSuggestion.local("study-urgent", AssistantSuggestion.Category.STUDY_PLAN,
                    "先处理紧急任务",
                    "从“" + safeTitle(urgent) + "”开始，启动计时前先写下一个能看见的检查点。"));
        }
        if (c.completionRate < 0.65 || c.averageQuality < 3.0) {
            result.add(AssistantSuggestion.local("study-shorter", AssistantSuggestion.Category.STUDY_PLAN,
                    "先用短专注块启动",
                    "第一轮建议控制在 15 到 25 分钟，注意力和质量稳定后再拉长。"));
        }
        if (c.bestSegment != null && c.currentSegment != null && c.bestSegment != c.currentSegment) {
            result.add(AssistantSuggestion.local("study-best-segment", AssistantSuggestion.Category.STUDY_PLAN,
                    "保护高效时段",
                    "你的高效时段更偏向" + label(c.bestSegment) + "，困难任务尽量移到这个时间段完成。"));
        }
        int hardCount = 0;
        for (TaskRecord task : tasks) {
            if (task.difficulty != null && task.difficulty.ordinal() >= 2) {
                hardCount++;
            }
        }
        if (hardCount > 0) {
            result.add(AssistantSuggestion.local("study-split-hard", AssistantSuggestion.Category.STUDY_PLAN,
                    "拆开困难任务",
                    "把高难度任务拆成产出、练习、复盘三个小块，每一块都要有明确完成标准。"));
        }
        if (tasks.isEmpty()) {
            result.add(AssistantSuggestion.local("study-empty", AssistantSuggestion.Category.STUDY_PLAN,
                    "先创建一个清晰任务",
                    "添加一个带具体结果的任务，完成几次专注后助手会更准确地调整建议。"));
        }
        if (result.isEmpty()) {
            result.add(AssistantSuggestion.local("study-stable", AssistantSuggestion.Category.STUDY_PLAN,
                    "保持当前节奏",
                    "最近节奏比较稳定，把优先级最高的任务放进下一轮专注即可。"));
        }
        return result;
    }

    private TaskRecord mostUrgent(List<TaskRecord> tasks, long nowMillis) {
        TaskRecord best = null;
        long bestDeadline = Long.MAX_VALUE;
        for (TaskRecord task : tasks) {
            boolean priorityUrgent = task.priority == TaskPriority.URGENT || task.priority == TaskPriority.HIGH;
            boolean deadlineSoon = task.deadlineAt != null
                    && task.deadlineAt <= nowMillis + 24L * 60L * 60L * 1000L;
            if (!priorityUrgent && !deadlineSoon) {
                continue;
            }
            long deadline = task.deadlineAt == null ? Long.MAX_VALUE - task.priority.rank() : task.deadlineAt;
            if (best == null || deadline < bestDeadline || task.priority.rank() > best.priority.rank()) {
                best = task;
                bestDeadline = deadline;
            }
        }
        return best;
    }

    private String safeTitle(TaskRecord task) {
        return task.title == null || task.title.trim().isEmpty() ? "选中的任务" : task.title.trim();
    }

    private String label(TimeSegment segment) {
        if (segment == TimeSegment.MORNING) return "上午";
        if (segment == TimeSegment.NOON) return "中午";
        if (segment == TimeSegment.AFTERNOON) return "下午";
        if (segment == TimeSegment.EVENING) return "晚上";
        return "深夜";
    }
}