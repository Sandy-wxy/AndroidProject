package com.example.focus_flow.domain.forest;

public class ForestLevel {
    public final int levelNumber;
    public final String sceneName;
    public final int requiredTrees;

    public ForestLevel(int levelNumber, String sceneName, int requiredTrees) {
        this.levelNumber = levelNumber;
        this.sceneName = sceneName == null ? "" : sceneName;
        this.requiredTrees = requiredTrees;
    }
}
