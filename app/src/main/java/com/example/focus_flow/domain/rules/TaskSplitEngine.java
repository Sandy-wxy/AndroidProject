package com.example.focus_flow.domain.rules;

import com.example.focus_flow.core.model.FocusBlockStatus;
import com.example.focus_flow.core.model.RecommendationConfidence;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.model.TaskRecord;

import java.util.ArrayList;
import java.util.List;

public class TaskSplitEngine {
    private static final int MAX_BLOCKS = 12;
    private static final int MIN_BLOCK = 10;

    private final FocusRecommendationEngine recommendationEngine = new FocusRecommendationEngine();

    public List<FocusBlockRecord> splitTask(TaskRecord task, Recommendation recommendation,
                                            double effectiveProgressMinutes) {
        int remaining = Math.max(0, (int) Math.ceil(task.estimatedTotalMinutes - effectiveProgressMinutes));
        List<FocusBlockRecord> result = new ArrayList<>();
        if (remaining <= 0) {
            return result;
        }

        int base = recommendation.focusMinutes;
        if (!task.autoSplitEnabled) {
            int oneBlockMinutes = clamp(Math.min(base, remaining), MIN_BLOCK, 60);
            result.add(block(1, oneBlockMinutes, recommendation));
            return result;
        }

        int blockCount = (int) Math.ceil(remaining / (double) base);
        if (blockCount > MAX_BLOCKS) {
            blockCount = MAX_BLOCKS;
        }
        base = recommendationEngine.roundToAllowed(clamp((int) Math.ceil(remaining / (double) blockCount), MIN_BLOCK, 60));

        List<Integer> minutes = new ArrayList<>();
        int rest = remaining;
        while (rest > 0) {
            int current = Math.min(base, rest);
            int restAfter = rest - current;
            if (restAfter > 0 && restAfter < MIN_BLOCK) {
                current += restAfter;
                restAfter = 0;
            }
            if (current > 60) {
                int left = current;
                while (left > 0) {
                    int split = Math.min(60, left);
                    if (left - split > 0 && left - split < MIN_BLOCK) {
                        split -= MIN_BLOCK - (left - split);
                    }
                    minutes.add(split);
                    left -= split;
                }
            } else {
                minutes.add(current);
            }
            rest = restAfter;
        }

        for (int i = 0; i < minutes.size(); i++) {
            result.add(block(i + 1, minutes.get(i), recommendation));
        }
        return result;
    }

    private FocusBlockRecord block(int sequenceIndex, int focusMinutes, Recommendation recommendation) {
        FocusBlockRecord block = new FocusBlockRecord();
        block.sequenceIndex = sequenceIndex;
        block.plannedFocusMinutes = clamp(focusMinutes, MIN_BLOCK, 60);
        block.plannedBreakMinutes = recommendation.shortBreakMinutes;
        block.status = FocusBlockStatus.PENDING;
        block.recommendationConfidence = recommendation.confidence == null
                ? RecommendationConfidence.LOW
                : recommendation.confidence;
        block.recommendationReasons = String.join(",", recommendation.reasonCodes);
        return block;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
