# Findings

## Current Architecture
- Android app is Java + XML/ViewBinding, with most feature UI built programmatically in fragments.
- Local data is SQLite through `AppSQLiteOpenHelper` and datasource/repository classes.
- Existing rules already include `FocusRecommendationEngine`, `TaskSplitEngine`, `AdviceEngine`, `ProgressEngine`, and `StatsCalculator`.
- Existing white-noise model has 12 `NoiseType` values and preset/custom mix support.
- Existing forest feature uses completed focus sessions of at least 25 minutes to draw trees.
- Existing task cards already support `onComplete`, but completion is hidden in the "more" popup.
- Existing manifest already has `INTERNET`; Android HTTP cleartext must be enabled for the supplied proxy.

## Approved External API Shape
- Base URL: `http://124.220.100.88`
- Endpoint: `POST /api/ai/chat`
- Header: `Authorization: Bearer ac32a1693e4bdcc9ac4228fc347246fd6eebe7658f7238b3d379309f8baffe24`
- Request body: `{ "message": "..." }`
- Current response: `{ "ok": true, "source": "api", "kind": "chat", "model": "deepseek-ai/DeepSeek-V4-Flash", "result": "..." }`
- Android side should parse `result` as text now, but tolerate future object/array JSON values.

## Implementation Notes
- Use pure Java domain classes where possible so JVM tests can cover many cases without emulator.
- For API context minimization, build separate prompt/context builders per request type.
- Avoid adding AI UI to Settings.
- Keep API token isolated in an internal constant/config class; this is acceptable for this project per user approval, but not secure for public APKs.
