package com.example.focus_flow.domain.forest;

import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.data.local.model.FocusSessionRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ForestEvolutionEngine {
    private static final int TREE_SECONDS = 25 * 60;
    private static final ForestLevel[] LEVELS = new ForestLevel[]{
            new ForestLevel(1, "Seed field", 0),
            new ForestLevel(2, "Young grove", 3),
            new ForestLevel(3, "Quiet woodland", 8),
            new ForestLevel(4, "Layered forest", 15),
            new ForestLevel(5, "Misty valley", 25),
            new ForestLevel(6, "Ancient forest", 40)
    };

    public ForestState evaluate(List<FocusSessionRecord> sessions, long nowMillis) {
        List<ForestTree> trees = new ArrayList<>();
        int total = 0;
        int today = 0;
        long todayStart = DateTimeUtils.startOfDayMillis(nowMillis);
        for (FocusSessionRecord session : sessions == null ? new ArrayList<FocusSessionRecord>() : sessions) {
            if (!qualifies(session)) {
                continue;
            }
            ForestTree tree = treeFor(session);
            trees.add(tree);
            total += tree.focusSeconds;
            if (session.startedAt >= todayStart) {
                today++;
            }
        }
        ForestLevel level = levelFor(trees.size());
        return new ForestState(trees, level, streakDays(trees, nowMillis),
                nextTargetFor(trees.size()), total, today);
    }

    private boolean qualifies(FocusSessionRecord session) {
        return session != null
                && session.status == FocusSessionStatus.COMPLETED
                && session.actualFocusSeconds >= TREE_SECONDS;
    }

    private ForestTree treeFor(FocusSessionRecord session) {
        return new ForestTree(session.subjectSnapshot, speciesFor(session.subjectSnapshot),
                sizeFor(session.actualFocusSeconds), session.actualFocusSeconds, session.startedAt);
    }

    private ForestTree.Species speciesFor(String subject) {
        String value = subject == null ? "" : subject.toLowerCase(Locale.US);
        if (containsAny(value, "math", "algebra", "physics")) {
            return ForestTree.Species.PINE;
        }
        if (containsAny(value, "english", "language", "reading")) {
            return ForestTree.Species.BLOOM;
        }
        if (containsAny(value, "program", "code", "android", "computer")) {
            return ForestTree.Species.CYPRESS;
        }
        if (containsAny(value, "history", "politic", "geography")) {
            return ForestTree.Species.MAPLE;
        }
        if (containsAny(value, "science", "biology", "chemistry")) {
            return ForestTree.Species.BAMBOO;
        }
        return ForestTree.Species.BROADLEAF;
    }

    private ForestTree.SizeTier sizeFor(int seconds) {
        if (seconds >= 90 * 60) {
            return ForestTree.SizeTier.GIANT;
        }
        if (seconds >= 45 * 60) {
            return ForestTree.SizeTier.TALL;
        }
        return ForestTree.SizeTier.SAPLING;
    }

    private ForestLevel levelFor(int treeCount) {
        ForestLevel current = LEVELS[0];
        for (ForestLevel level : LEVELS) {
            if (treeCount >= level.requiredTrees) {
                current = level;
            }
        }
        return current;
    }

    private int nextTargetFor(int treeCount) {
        for (ForestLevel level : LEVELS) {
            if (treeCount < level.requiredTrees) {
                return level.requiredTrees;
            }
        }
        return LEVELS[LEVELS.length - 1].requiredTrees + 20;
    }

    private int streakDays(List<ForestTree> trees, long nowMillis) {
        Set<Long> days = new HashSet<>();
        for (ForestTree tree : trees) {
            days.add(DateTimeUtils.startOfDayMillis(tree.startedAt));
        }
        int streak = 0;
        long day = DateTimeUtils.startOfDayMillis(nowMillis);
        while (days.contains(day)) {
            streak++;
            day -= 24L * 60L * 60L * 1000L;
        }
        return streak;
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
