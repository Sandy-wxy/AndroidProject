package com.example.focus_flow.domain.rules;

import com.example.focus_flow.core.model.RecommendationConfidence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Recommendation {
    public final int focusMinutes;
    public final int shortBreakMinutes;
    public final int longBreakMinutes;
    public final int blocksBeforeLongBreak;
    public final RecommendationConfidence confidence;
    public final List<String> reasonCodes;

    public Recommendation(int focusMinutes, int shortBreakMinutes, int longBreakMinutes,
                          int blocksBeforeLongBreak, RecommendationConfidence confidence,
                          List<String> reasonCodes) {
        this.focusMinutes = focusMinutes;
        this.shortBreakMinutes = shortBreakMinutes;
        this.longBreakMinutes = longBreakMinutes;
        this.blocksBeforeLongBreak = blocksBeforeLongBreak;
        this.confidence = confidence;
        this.reasonCodes = Collections.unmodifiableList(new ArrayList<>(reasonCodes));
    }
}
