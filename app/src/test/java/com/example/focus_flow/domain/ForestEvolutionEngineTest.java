package com.example.focus_flow.domain;

import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.domain.forest.ForestEvolutionEngine;
import com.example.focus_flow.domain.forest.ForestState;
import com.example.focus_flow.domain.forest.ForestTree;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ForestEvolutionEngineTest {
    private static final long DAY = 24L * 60L * 60L * 1000L;
    private static final long NOW = 1_800_000_000_000L;

    @Test
    public void forestLevelAndSceneGrowWithTreesAndStreak() {
        List<FocusSessionRecord> sessions = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            sessions.add(session("math", 30, NOW - (i % 4) * DAY));
        }

        ForestState state = new ForestEvolutionEngine().evaluate(sessions, NOW);

        assertEquals(14, state.trees.size());
        assertTrue(state.level.levelNumber >= 3);
        assertTrue(state.streakDays >= 4);
        assertFalseEmpty(state.level.sceneName);
        assertTrue(state.nextLevelTreeTarget > state.trees.size());
    }

    @Test
    public void differentSubjectsCreateDifferentSpecies() {
        ForestEvolutionEngine engine = new ForestEvolutionEngine();
        List<FocusSessionRecord> sessions = ArraysCompat.list(
                session("math", 30, NOW),
                session("english", 30, NOW),
                session("programming", 30, NOW),
                session("history", 90, NOW));

        ForestState state = engine.evaluate(sessions, NOW);

        assertEquals(4, state.trees.size());
        assertNotEquals(state.trees.get(0).species, state.trees.get(1).species);
        assertNotEquals(state.trees.get(1).species, state.trees.get(2).species);
        assertEquals(ForestTree.SizeTier.GIANT, state.trees.get(3).sizeTier);
    }

    @Test
    public void unqualifiedSessionsDoNotCreateTrees() {
        List<FocusSessionRecord> sessions = ArraysCompat.list(
                session("math", 24, NOW),
                abandonedSession("english", 60, NOW),
                session("physics", 25, NOW));

        ForestState state = new ForestEvolutionEngine().evaluate(sessions, NOW);

        assertEquals(1, state.trees.size());
        assertEquals("physics", state.trees.get(0).subject);
    }

    private FocusSessionRecord session(String subject, int minutes, long startedAt) {
        FocusSessionRecord session = new FocusSessionRecord();
        session.status = FocusSessionStatus.COMPLETED;
        session.subjectSnapshot = subject;
        session.actualFocusSeconds = minutes * 60;
        session.startedAt = startedAt;
        return session;
    }

    private FocusSessionRecord abandonedSession(String subject, int minutes, long startedAt) {
        FocusSessionRecord session = session(subject, minutes, startedAt);
        session.status = FocusSessionStatus.ABANDONED;
        return session;
    }

    private void assertFalseEmpty(String value) {
        assertTrue(value != null && !value.trim().isEmpty());
    }

    private static class ArraysCompat {
        @SafeVarargs
        static <T> List<T> list(T... values) {
            List<T> list = new ArrayList<>();
            for (T value : values) {
                list.add(value);
            }
            return list;
        }
    }
}
