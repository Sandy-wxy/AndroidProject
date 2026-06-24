package com.example.focus_flow.domain.assistant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SuggestionQueue {
    private final List<AssistantSuggestion> items = new ArrayList<>();
    private int currentIndex;

    public SuggestionQueue(List<AssistantSuggestion> localSuggestions) {
        addUnique(localSuggestions);
    }

    public AssistantSuggestion current() {
        if (items.isEmpty()) {
            return null;
        }
        currentIndex = Math.max(0, Math.min(currentIndex, items.size() - 1));
        return items.get(currentIndex);
    }

    public AssistantSuggestion next() {
        if (items.isEmpty()) {
            return null;
        }
        currentIndex = (currentIndex + 1) % items.size();
        return items.get(currentIndex);
    }

    public void mergeApiSuggestions(List<AssistantSuggestion> apiSuggestions) {
        if (apiSuggestions == null || apiSuggestions.isEmpty()) {
            return;
        }
        addUnique(apiSuggestions);
        if (currentIndex >= items.size()) {
            currentIndex = 0;
        }
    }

    public boolean moveTo(AssistantSuggestion suggestion) {
        int index = items.indexOf(suggestion);
        if (index < 0) {
            return false;
        }
        currentIndex = index;
        return true;
    }

    public List<AssistantSuggestion> items() {
        return Collections.unmodifiableList(items);
    }

    private void addUnique(List<AssistantSuggestion> suggestions) {
        Set<String> existing = new LinkedHashSet<>();
        for (AssistantSuggestion item : items) {
            existing.add(item.dedupeKey());
        }
        if (suggestions == null) {
            return;
        }
        for (AssistantSuggestion suggestion : suggestions) {
            if (suggestion == null || suggestion.title.isEmpty() && suggestion.content.isEmpty()) {
                continue;
            }
            String key = suggestion.dedupeKey();
            if (existing.add(key)) {
                items.add(suggestion);
            }
        }
    }
}
