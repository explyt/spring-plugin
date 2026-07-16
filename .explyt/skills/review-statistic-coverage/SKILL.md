---
name: "review-statistic-coverage"
schemaVersion: "v0.1"
description: "Usage-statistics coverage check for explyt/spring-plugin code reviews — verify that new user-visible features (quick fixes, intentions, gutter actions, completions, actions, tool-window controls, wizard steps) register usage via StatisticService and StatisticActionId. Use when reviewing a diff or PR that adds or changes user-facing plugin features, or when asked to check statistics or usage-tracking coverage."
agent: "Review"
used-by:
 - "Review"
---
# Usage Statistics Coverage Check (for explyt/spring-plugin reviews)

> **Why this skill exists.** Without usage counters we cannot measure how the plugin is actually used, which means we cannot tell whether a new feature ships dead, which quick fixes users actually apply, or how often a tool-window control is touched. Missing statistics on a new user-visible action is treated as a **Warning-severity (High)** finding for this project: existing coverage is broad but not enforced as a hard error, so flag gaps prominently without blocking on them.
>
> **Scope.** This skill applies only to reviews in the `explyt/spring-plugin` repository. Apply this check on every diff that touches user-facing features before producing the final review.

---

## 1. Where usage statistics live in this project

All infrastructure is in `modules/spring-core/src/main/kotlin/com/explyt/spring/core/statistic/`:

| Component | Role |
|---|---|
| `StatisticActionId` | Enum of all tracked actions; each entry has a human-readable description string. The single source of truth for what is tracked. |
| `StatisticService` | APP-level `@Service`. `addActionUsage(actionId)` increments a persisted counter; gated by `SpringToolRunConfigurationsSettingsState.isCollectStatistic` and skipped in unit-test/headless/non-production modes. |
| `StatisticUtil.registerActionUsage(fixKey, previewKey)` | `Editor?` extension for quick fixes: logs `fixKey` in the main editor and `previewKey` (if non-null) in the intention-preview editor. |
| `StatisticInsertHandler(actionId)` | `InsertHandler` for completion lookup elements: logs on actual insertion, not on mere suggestion. |
| `StatisticState` | `SimplePersistentStateComponent` cache holding the counter map. |
| `StatisticStartupActivity` | Flushes counters to file on startup. |

Canonical call patterns to model after:

- **Action / gutter handler / tool-window button**: `StatisticService.getInstance().addActionUsage(StatisticActionId.GENERATE_WEB_METHOD)` — see `SpringWebKotlinMethodGenerateAction`, `EndpointIconGutterHandler`, `RunInSwaggerAction`.
- **Quick fix with preview**: `editor.registerActionUsage(QUICK_FIX_..., PREVIEW_...)` at the start of `invoke()` — see `AddPathVariableQuickFix`.
- **Completion insertion**: `.withInsertHandler(StatisticInsertHandler(COMPLETION_...))` — see `OpenApiYamlRefCompletionContributor`, `ProfilesCompletionContributor`.
- **Completion provider that always contributes**: `addActionUsage` inside `addCompletions` — see `SpringDataMethodCompletionProvider` (use this only when showing the lookup itself is the signal; prefer `StatisticInsertHandler` when acceptance is the signal).
- **Wizard/settings interaction**: `addActionUsage` in the event handler — see `SpringInitializrWizardStep`, `SETTINGS_OPEN`/`SETTINGS_CHANGED`.

---

## 2. Trigger conditions — when this check applies

For EVERY change in the diff, ask:

1. **New user-visible feature?**
   - A new inspection quick fix or intention (`LocalQuickFix`, `IntentionAction`, `LocalQuickFixAndIntentionActionOnPsiElement`).
   - A new line-marker / gutter action or gutter navigation handler.
   - A new completion contributor, completion provider, or reference with completion variants.
   - A new `AnAction`, generate-menu entry, or floating toolbar button.
   - A new tool-window control (refresh button, filter, search field, navigation).
   - A new wizard step or wizard-field interaction (Spring Initializr).
   - A new settings page or a new user-changeable setting.

2. **New sibling of an already-tracked feature?**
   - A new quick fix in a package where sibling quick fixes call `registerActionUsage`.
   - A new gutter provider in a module where sibling providers log `GUTTER_*`.
   - A new completion path next to tracked `COMPLETION_*` ones.

3. **Renamed/removed tracked feature?**
   - If a tracked feature is removed, its `StatisticActionId` entry stays (historical counters), but verify no dead `addActionUsage` calls remain.
   - If a feature is renamed/moved, verify the same `StatisticActionId` is still logged from the new location.

If yes to any of 1–2, the diff MUST contain a corresponding `addActionUsage` / `registerActionUsage` / `StatisticInsertHandler` call plus, for new actions, a new `StatisticActionId` entry.

---

## 3. The symmetry heuristic — the strongest signal

If feature X is already tracked and the diff adds a sibling Y following the same pattern, Y must also be tracked. Concrete precedents in this codebase:

- `QUICK_FIX_REQUEST_MAPPING_ADD_PATH_VARIABLE` is paired with `PREVIEW_REQUEST_MAPPING_ADD_PATH_VARIABLE` → every new quick fix that supports intention preview should log a `QUICK_FIX_*`/`PREVIEW_*` pair via `registerActionUsage`.
- `GUTTER_ASPECTJ_USAGE` and `GUTTER_ASPECTJ_DECLARATION` are logged from `AspectMethodsLineMarkerProvider` / `PointCutMethodsLineMarkerProvider` → a new line-marker navigation must log its own `GUTTER_*` id.
- `COMPLETION_OPENAPI_YAML_ENDPOINT` / `COMPLETION_OPENAPI_JSON_ENDPOINT` are symmetric across file types → a new completion for another format must add its own `COMPLETION_*` id.
- `SETTINGS_OPEN` / `SETTINGS_CHANGED` and `RUN_CONFIGURATION_OPEN` / `RUN_CONFIGURATION_CHANGED` → a new configurable should log both open and change events.

When applying this heuristic, point at the sibling call site (file:line) so the author can model after a concrete existing pattern.

---

## 4. Naming and placement rules for `StatisticActionId`

New enum entries must follow the existing conventions:

- Name prefix encodes the feature surface: `GUTTER_*`, `QUICK_FIX_*`, `PREVIEW_*`, `COMPLETION_*`, `ACTION_*`, `GENERATE_*`, `SETTINGS_*`, `RUN_CONFIGURATION_*`, `SPRING_BOOT_PANEL_*`, `ENDPOINTS_TOOLWINDOW_*`, `SPRING_INITIALIZR_*`.
- `SCREAMING_SNAKE_CASE`, feature surface first, then the specific action (e.g. `GUTTER_TARGET_PUBLISH_EVENT`).
- The description string is a short human-readable sentence in the style of neighbors (e.g. `"Quick fixed - added Qualifier"`, `"Gutter line marker for ..."`; for `ACTION_*` entries: `"An action that ..."`).
- Place the entry in the enum next to its group (all `GUTTER_*` together, all `COMPLETION_*` together, etc.), not appended at the end.
- A quick fix with intention preview gets **two** ids: `QUICK_FIX_X` and `PREVIEW_X`, logged through `StatisticUtil.registerActionUsage` so preview invocations are not counted as real fixes.

Placement rules for the logging call:

- Log at the point of the actual user action: `invoke()` of a quick fix, `navigate`/handler of a gutter marker, `actionPerformed` of an action, insert handler of a completion item.
- Place the call **after** early-return guards (null data keys, unavailable context) so only real executions are counted, not aborted invocations.
- Do not log in `isAvailable()`, `getFamilyName()`, availability checks, or during highlighting/inspection visiting — that counts renders, not usage.
- Do not call `SpringToolRunConfigurationsSettingsState.isCollectStatistic` at the call site — `StatisticService.addActionUsage` already gates it centrally.
- Never log user data (paths, class names, code) — the infrastructure counts action ids only; keep it that way.

---

## 5. Skip list — where the check does NOT apply

Skip this check for:
- Pure internal refactors with no user-visible change.
- Inspections/highlighting themselves (only their quick fixes are tracked, not the diagnostics).
- References/navigation resolved by the platform without an explicit plugin handler (plain `PsiReference` resolve).
- Helper / util / parser / model changes.
- Test-only changes (`src/test`, `testData`).
- Documentation-only changes.
- Build / CI / Gradle changes.
- Bug fixes that do not introduce new user-visible actions or surfaces.

---

## 6. Severity policy

- **High (Warning)**: a new user-visible feature (trigger list in section 2) ships with no statistics call at all, while comparable siblings are tracked.
- **Medium**: statistics call exists but is misplaced (counts previews as fixes, logs in availability checks, double-counts) or the new `StatisticActionId` violates naming/grouping conventions.
- **Low**: minor issues — description string style inconsistent with neighbors, entry placed outside its group.
- Do **not** escalate a missing statistic to Critical/blocker: the repo treats statistics as important but not as a merge-blocking contract.
- When several untracked surfaces appear in one diff (e.g. two new actions in one file), report **one finding per surface** — each needs its own `StatisticActionId` and call site.
- IDE severity mapping when embedded in an orchestrated review: High → `Warning`, Medium → `Warning`, Low → `WeakWarning`; never `Error`.

## 7. No-finding is a valid outcome

It is an explicitly valid outcome to conclude that no statistics change is required for the diff. Do **NOT** invent a finding to satisfy this check. Only report a finding when there is a concrete missing or misplaced call you can point to (file:line + the closest existing tracked sibling to model after). Otherwise stay silent on statistics.

## Output format

If the orchestrator specified an artifact file, write the full result there and return to chat only a short summary and the file path. If run standalone, return the full result inline.

```md
# Review Result

## Reviewer
- reviewer_id: review-statistic-coverage
- applicability: applicable | partially_applicable | not_applicable
- review_target: ...

## Findings

### F1
- Severity: High | Medium | Low
- Confidence: High | Medium | Low
- Category: missing-statistic | misplaced-statistic | naming-convention | dead-call
- Location: path/to/File.kt:123
- Title: ...
- Evidence: the new user-visible surface, and the tracked sibling call site (file:line) proving the pattern
- Why it matters: which usage signal is lost
- Recommendation: exact call to add (addActionUsage / registerActionUsage / StatisticInsertHandler) + proposed StatisticActionId name and description following section 4
- Rule refs: statistic-coverage

## Open Questions
- ...

## Positive Observations
- ...
```

## Acceptance checklist

- [ ] Every trigger-list surface in the diff was classified as tracked, gap, or skip-listed.
- [ ] Every finding cites the missing/misplaced call with `file:line` and a concrete tracked sibling to model after.
- [ ] Every recommended new `StatisticActionId` follows the prefix, casing, grouping, and description conventions of section 4.
- [ ] Preview-capable quick fixes are checked for the `QUICK_FIX_*`/`PREVIEW_*` pair via `registerActionUsage`.
- [ ] No finding invented when the diff genuinely requires no statistics change.
