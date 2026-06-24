package com.example.focus_flow.domain.forest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForestState {
    public final List<ForestTree> trees;
    public final ForestLevel level;
    public final int streakDays;
    public final int nextLevelTreeTarget;
    public final int totalFocusSeconds;
    public final int todayTrees;

    public ForestState(List<ForestTree> trees, ForestLevel level, int streakDays,
                       int nextLevelTreeTarget, int totalFocusSeconds, int todayTrees) {
        this.trees = Collections.unmodifiableList(new ArrayList<>(
                trees == null ? Collections.emptyList() : trees));
        this.level = level;
        this.streakDays = streakDays;
        this.nextLevelTreeTarget = nextLevelTreeTarget;
        this.totalFocusSeconds = totalFocusSeconds;
        this.todayTrees = todayTrees;
    }
}
