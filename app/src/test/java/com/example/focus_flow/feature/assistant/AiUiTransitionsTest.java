package com.example.focus_flow.feature.assistant;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AiUiTransitionsTest {
    @Test
    public void crossFadeDurationsAreVisibleAndTunedToFiveHundredMillis() {
        assertEquals(500L, AiUiTransitions.TEXT_CROSS_FADE_MS);
        assertEquals(500L, AiUiTransitions.CONTAINER_CROSS_FADE_MS);
    }
}