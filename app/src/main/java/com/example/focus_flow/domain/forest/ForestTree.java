package com.example.focus_flow.domain.forest;

public class ForestTree {
    public enum Species {
        BROADLEAF,
        PINE,
        BLOOM,
        CYPRESS,
        MAPLE,
        BAMBOO
    }

    public enum SizeTier {
        SAPLING,
        TALL,
        GIANT
    }

    public final String subject;
    public final Species species;
    public final SizeTier sizeTier;
    public final int focusSeconds;
    public final long startedAt;

    public ForestTree(String subject, Species species, SizeTier sizeTier, int focusSeconds, long startedAt) {
        this.subject = subject == null || subject.trim().isEmpty() ? "study" : subject.trim();
        this.species = species == null ? Species.BROADLEAF : species;
        this.sizeTier = sizeTier == null ? SizeTier.SAPLING : sizeTier;
        this.focusSeconds = Math.max(0, focusSeconds);
        this.startedAt = startedAt;
    }
}
