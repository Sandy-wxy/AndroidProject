# Focus Flow AI Assistant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a hybrid local/API learning assistant, visible task completion, forest evolution, task rewrite suggestions, white-noise recommendations, and stats analysis to Focus Flow.

**Architecture:** Put most decision logic in pure Java domain classes under `domain/assistant` and `domain/forest` so it is testable without Android UI. UI fragments consume those classes and call a small API client through the supplied proxy, merging remote results into local rotating suggestion queues without blocking local recommendations.

**Tech Stack:** Java 11, Android appcompat/material/navigation, SQLite repositories, `HttpURLConnection` for proxy calls, JUnit for JVM rule tests, Espresso/instrumented tests for UI smoke flows.

## Global Constraints

- Local rule engines must produce useful suggestions immediately and remain robust if API fails.
- API enhancement runs by default through `POST http://124.220.100.88/api/ai/chat`.
- API timeout is 10 seconds.
- API success merges remote suggestions with local suggestions; local suggestions remain in the rotation.
- API result currently is a string but parsing must tolerate future JSON object or array result values.
- API request context must be purpose-specific and minimal.
- Settings page must not show AI settings or AI status.
- Task rewrite suggestions trigger only once after first rough user input.
- White-noise suggestions are tappable and apply sounds/volumes.
- Stats page shows animated analysis loading before API result, then displays analysis/report content.
- Forest evolution includes levels, scene unlocks, subject-specific tree species, and streak/weekly growth feedback.

---

### Task 1: Pure Domain Contracts And Tests

**Files:**
- Create: `app/src/main/java/com/example/focus_flow/domain/assistant/AssistantSuggestion.java`
- Create: `app/src/main/java/com/example/focus_flow/domain/assistant/SuggestionQueue.java`
- Create: `app/src/main/java/com/example/focus_flow/domain/assistant/AiResponseParser.java`
- Create: `app/src/main/java/com/example/focus_flow/domain/assistant/StudyStrategyEngine.java`
- Create: `app/src/main/java/com/example/focus_flow/domain/assistant/NoiseRecommendationEngine.java`
- Create: `app/src/main/java/com/example/focus_flow/domain/assistant/TaskRewriteModels.java`
- Create: `app/src/main/java/com/example/focus_flow/domain/assistant/TaskRewriteEngine.java`
- Create: `app/src/main/java/com/example/focus_flow/domain/forest/ForestEvolutionEngine.java`
- Test: `app/src/test/java/com/example/focus_flow/domain/AssistantEngineTest.java`
- Test: `app/src/test/java/com/example/focus_flow/domain/ForestEvolutionEngineTest.java`

**Interfaces:**
- Produces: pure Java assistant and forest engine APIs for UI and later API integration.

- [ ] Write failing unit tests covering queue merging, API parser string/object compatibility, local strategy variety, noise recommendation variety, task rewrite model limits, and forest level/species decisions.
- [ ] Run targeted tests and confirm expected RED failures.
- [ ] Implement minimal domain classes.
- [ ] Run targeted tests and confirm GREEN.

### Task 2: API Proxy Client And Prompt Builders

**Files:**
- Create: `app/src/main/java/com/example/focus_flow/feature/assistant/AiProxyClient.java`
- Create: `app/src/main/java/com/example/focus_flow/feature/assistant/AiPromptBuilder.java`
- Test: `app/src/test/java/com/example/focus_flow/domain/AiPromptBuilderTest.java`

**Interfaces:**
- Consumes: `AiResponseParser`, `AssistantSuggestion`.
- Produces: async proxy request helper and minimal-context prompt builders for each request type.

- [ ] Write failing tests proving task prompts exclude white-noise/distraction data, white-noise prompts include allowed sounds, stats prompts use aggregate data, and parser accepts string/object results.
- [ ] Run tests and confirm RED.
- [ ] Implement prompt builder and proxy client with 10s timeouts.
- [ ] Run tests and confirm GREEN.

### Task 3: Task Cards And Task Rewrite UI

**Files:**
- Modify: `app/src/main/java/com/example/focus_flow/feature/tasks/TaskCards.java`
- Modify: `app/src/main/java/com/example/focus_flow/feature/tasks/TaskFormBottomSheet.java`
- Test: `app/src/androidTest/java/com/example/focus_flow/TaskFlowInstrumentedTest.java`

**Interfaces:**
- Consumes: `TaskRewriteEngine`, `AiProxyClient`, `AiPromptBuilder`.
- Produces: visible completion action and first-input rewrite suggestions.

- [ ] Add tests or UI checks for visible completion and rewrite fill behavior where feasible.
- [ ] Implement task card complete button.
- [ ] Implement one-shot rewrite suggestions and tap-to-fill behavior.
- [ ] Run relevant tests/build.

### Task 4: Home Assistant Queue And White-Noise Recommendations

**Files:**
- Modify: `app/src/main/java/com/example/focus_flow/feature/home/HomeFragment.java`
- Modify: `app/src/main/java/com/example/focus_flow/feature/noise/NoiseFragment.java`
- Test: `app/src/androidTest/java/com/example/focus_flow/NoiseAndSettingsInstrumentedTest.java`

**Interfaces:**
- Consumes: `StudyStrategyEngine`, `NoiseRecommendationEngine`, `SuggestionQueue`, `AiProxyClient`.
- Produces: local-first/API-merged rotating suggestions and tappable sound recommendations.

- [ ] Add tests/smoke checks for local suggestions and sound apply behavior.
- [ ] Implement home suggestion queue.
- [ ] Implement noise recommendation cards and apply actions.
- [ ] Run relevant tests/build.

### Task 5: Stats Analysis And Forest Evolution UI

**Files:**
- Modify: `app/src/main/java/com/example/focus_flow/feature/stats/StatsFragment.java`
- Modify: `app/src/main/java/com/example/focus_flow/feature/forest/ForestFragment.java`
- Modify: `app/src/main/java/com/example/focus_flow/feature/forest/FocusForestView.java`
- Test: `app/src/androidTest/java/com/example/focus_flow/StatsInstrumentedTest.java`

**Interfaces:**
- Consumes: `ForestEvolutionEngine`, `AiProxyClient`, `AiPromptBuilder`.
- Produces: stats loading/result analysis and evolved forest scenes.

- [ ] Add tests/smoke checks for stats loading text and forest level display where feasible.
- [ ] Implement stats analysis card with API enhancement.
- [ ] Implement forest levels, scenes, and subject species.
- [ ] Run relevant tests/build.

### Task 6: Full Verification

**Files:**
- All changed files.

**Interfaces:**
- Consumes: all previous tasks.
- Produces: verified final state.

- [ ] Run JVM tests.
- [ ] Run Android build.
- [ ] Run instrumented tests or emulator/manual validation if environment supports it.
- [ ] Diagnose and fix all failures that are locally solvable.
