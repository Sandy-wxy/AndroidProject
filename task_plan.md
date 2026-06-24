# Focus Flow AI Assistant Implementation Plan

## Goal
Build the approved Focus Flow feature set: visible task completion, forest evolution, robust local study assistant rules, API-backed suggestion enhancement through the proxy, task rewrite suggestions, white-noise recommendations, and stats analysis.

## Phase Gates

### Phase 1: Planning And Current-State Audit
- Status: completed
- Objective: lock feature scope, inspect current code boundaries, and identify files to change.
- Expected result: plan files exist, key implementation touchpoints are known, and no user-approved constraint is missing.
- Verification: read planning files and run targeted `rg`/file reads for affected modules.
- Minimum cases: at least 20 concrete requirement checks against the approved scope.
- Risks: feature scope is broad; must keep API context minimal per request type.

### Phase 2: Domain And API Tests First
- Status: completed
- Objective: add failing tests for local assistant rules, API response parsing, suggestion queue merge behavior, forest evolution, and task rewrite one-shot behavior where testable outside UI.
- Expected result: new tests fail for missing classes/methods or missing behavior.
- Verification: run targeted unit tests and confirm expected RED failures.
- Minimum cases: at least 200 meaningful generated/table-driven checks across rule engines and validators.
- Risks: Android dependencies may make some behavior unsuitable for JVM tests; isolate pure Java logic.

### Phase 3: Domain And API Implementation
- Status: completed
- Objective: implement pure Java engines and client helpers used by UI.
- Expected result: domain tests pass; API parsing accepts string and future JSON object `result`; 10s timeout and context minimization are represented in code.
- Verification: run targeted unit tests and relevant existing tests.
- Minimum cases: at least 200 meaningful checks.
- Risks: network calls must not block UI; token must be treated as proxy token, not model provider key.

### Phase 4: UI Integration
- Status: completed
- Objective: integrate visible completion button, suggestion queues, task rewrite chips, white-noise apply suggestions, stats analysis loading/result states, and forest evolution UI.
- Expected result: app compiles and feature surfaces are reachable.
- Verification: build, instrumented tests where possible, emulator/manual checks with screenshots if the environment supports it.
- Minimum cases: targeted UI flows plus existing instrumented coverage.
- Risks: current UI is programmatic and large; keep changes local and avoid unrelated refactors.

### Phase 5: Full Validation
- Status: completed
- Objective: verify the final system coherently.
- Expected result: unit tests pass, Android build passes, and emulator validation is attempted rigorously. Any blocker is diagnosed before escalation.
- Verification: run `gradlew test`, assemble/debug or connected checks as environment allows, and emulator/manual validation.
- Minimum cases: strongest feasible validation, including generated rule coverage and UI smoke flows.
- Risks: local Android SDK/emulator availability may block device validation; if so, stop with exact required user action.

## Approved Constraints
- Local rules produce usable suggestions immediately.
- API enhancement runs by default in the background through `http://124.220.100.88/api/ai/chat`.
- API timeout is 10 seconds.
- API failures keep the local suggestion queue active.
- API success merges API suggestions with local suggestions; local suggestions remain in the rotation.
- API result currently may be a string; parser must also tolerate future JSON object/array results.
- API requests must be purpose-specific and minimal; do not send unrelated user/app data.
- Settings page must not expose AI settings or status.
- Task rewrite triggers only once after the first rough user input; tapping a rewrite fills the form for further editing.
- White-noise suggestions must be tappable and apply sounds/volumes.
- Stats analysis shows an animated loading state before API result, then displays analysis/report content.
- Forest evolution includes levels and scene unlocks, with subject-specific tree species.

## Final Verification Results
- `.\gradlew.bat :app:testDebugUnitTest` passed.
- `.\gradlew.bat :app:compileDebugAndroidTestJavaWithJavac` passed.
- `.\gradlew.bat :app:connectedDebugAndroidTest` passed on `Pixel_10_Pro_XL` with 17 tests.
- UIAutomator hierarchy confirmed the bottom navigation entries and home assistant section after installing/running the debug app.
