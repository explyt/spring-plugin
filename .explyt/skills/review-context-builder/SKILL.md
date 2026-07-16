---
name: "review-context-builder"
schemaVersion: "v0.1"
description: "Builds reusable context for reviewer subskills: architecture, dependencies, tests, conventions, freeze-safety hot paths, and risks. Use during an orchestrated code review after scope resolution, when asked to build review context, map affected modules, or inventory freeze-safety hot paths before a review wave."
agent: "Ask"
used-by:
  - "Ask"
---
# Review context builder

You do not produce final review findings.
Your job is to build shared reusable context that benefits all reviewer subskills.

## Inputs

The orchestrator must pass you:
- the exact review target;
- the path to `REVIEW_SCOPE.md`;
- an explicit focus for this context pass:
  - `architecture`
  - `tests-conventions`
  - `freeze-safety`
  - `combined`
- the path where the full result must be written.

## What to collect

Depending on the focus:

### `architecture`
Collect:
- affected modules (e.g. `modules/spring-core`, `modules/spring-web`, `modules/spring-data`) and their dependencies;
- integration points;
- contracts between components;
- side effects and hidden couplings;
- systemic risks for the review.

### `tests-conventions`
Collect:
- related tests and test patterns (including `modules/test-framework` usage);
- lifecycle and disposal expectations;
- project conventions relevant to the target;
- observability, logging, and maintainability expectations;
- known risk areas.

### `freeze-safety`
A specialized focus for user-perceived responsiveness (typing latency, scroll stutter, slow window opening, EDT freezes).
Do not duplicate the reviewer subskill rules — only inventory hot paths and provide reasoning anchors.

Collect from the scope and surrounding code:

1. **Hot-path inventory** — explicitly list the in-scope elements of each category with paths/names:
   - renderers: `ListCellRenderer`, `TreeCellRenderer`, `TableCellRenderer`, `ColoredListCellRenderer`, `ColoredTreeCellRenderer`, `paintComponent`, icons in hot render paths;
   - action updates: `AnAction.update`, `ActionPromoter`, `EditorActionHandler.isEnabledForCaret`;
   - service init: `@Service` constructor / `init {}`, `projectOpened`, `StartupActivity`, `ProjectActivity`;
   - UI open paths: `ToolWindowFactory.createToolWindowContent`, `DialogWrapper.createCenterPanel`, `Configurable.createComponent`;
   - typing hot path: `CompletionContributor`, `Annotator`, `ExternalAnnotator`, `TypedHandlerDelegate`, `DocumentListener.documentChanged`, `LineMarkerProvider`;
   - event handlers: `BulkFileListener`, `DocumentListener`, `CaretListener`, `SelectionListener`, `messageBus` subscribers;
   - dispose paths: `dispose()`, `disposeUIResources()`, `projectClosed`.

2. **Existing freeze markers** in surrounding code:
   - existing `knownIssue {}` / `SlowOperations.allowSlowOperations` in the target and neighboring files;
   - existing `@RequiresEdt` / `@RequiresBackgroundThread` / `@RequiresWriteLock`;
   - existing `MergingUpdateQueue` / `Alarm` / repeated-alarm utilities.

3. **Latency heuristics** — use as a reasoning reference, not as measured metrics (the agent does not run a profiler). These are the thresholds at which a typical pattern becomes a freeze; apply them when arguing findings:
   - renderer: target under 1 ms per row, zero allocations — any I/O or `resolve()` inside = violation;
   - `AnAction.update`: target under 10 ms, zero FS/network — any `ReadAction` walking PSI = violation;
   - service constructor / `init {}`: zero blocking I/O — any synchronous read/network = violation;
   - `StartupActivity`: target under 50 ms to first interactive — heavy initialization without `launch { }` = violation;
   - typing hot path: target under 10 ms per keystroke — any `runBlocking` / network call = violation;
   - any EDT callback over 16 ms = dropped frame.

4. **Freeze evidence**, when available:
   - thread dumps from `logs/threadDumps-freeze-*` in the project root (or the sandbox log directory) — extract the top 3 frames of each AWT-EventQueue dump;
   - match stack frames against in-scope files — mark direct intersections;
   - if there is no evidence, write `no freeze evidence available` explicitly.

5. **Routing hints** — tell the orchestrator which reviewer subskills must join:
   - hot path touches renderer / `update()` / tool window / dialog — `review-ui-platform`;
   - hot path touches service init / startup / locks / dispose / EDT dispatch — `review-async-lifecycle`;
   - hot path touches PSI walks / `resolve()` / `element.text` / `findChildOfType` / indexes — `review-psi-vfs-indexing`.

Do not make findings, do not propose fixes. This is a data packet, not a review.

### `combined`
Collect `architecture` + `tests-conventions` without useless duplication.
If the orchestrator explicitly requested `combined` with freeze context, also add the `freeze-safety` block.

## Rules

- The context must be reusable, not narrowly bound to a single reviewer.
- Do not substitute review findings for context.
- Do not duplicate raw scope output without added value.
- Do not copy large file fragments without necessity.
- Do not invent architectural links that the code does not confirm.

## What to read

Read only what is needed to build the shared picture:
- `REVIEW_SCOPE.md`;
- related modules and configs;
- key classes and their usages;
- relevant tests;
- project conventions when they are clearly important for the target.

## Output format

Write the full result to the file specified by the orchestrator (normally inside `.tasks/review-.../`).
Return to chat only:
- a short summary;
- the file path.

Full result template:

```md
# Review Context

## Focus
- architecture | tests-conventions | freeze-safety | combined

## Reusable Summary
- ...

## Modules and Components
- ...

## Key Contracts and Invariants
- ...

## Integrations and Side Effects
- ...

## Relevant Tests and Test Patterns
- ...

## Relevant Conventions
- ...

## Risks for Reviewers
- ...

## Useful Paths
- ...

## Unknowns
- ...

<!-- the block below is required only for focus=freeze-safety (or combined with freeze) -->
## Freeze Hot-Path Inventory
- renderers: [...]
- action_updates: [...]
- service_inits: [...]
- ui_open_paths: [...]
- typing_hot_path: [...]
- event_handlers: [...]
- dispose_paths: [...]

## Freeze Markers In Surrounding Code
- knownIssue / SlowOperations: [...]
- threading annotations: [...]
- throttling utilities: [...]

## Latency Heuristics (reasoning reference, not measured)
- renderer: <1ms/row target, zero alloc — I/O/resolve here = violation
- AnAction.update: <10ms target, no FS/network — heavy ReadAction = violation
- service init: zero blocking I/O
- StartupActivity: <50ms target to first interactive
- typing hot-path: <10ms/keystroke target — runBlocking/network = violation
- EDT callback: >16ms = dropped frame

## Freeze Evidence
- thread_dumps: [paths or `no freeze evidence available`]
- matched_frames: [stack frame -> scope file] or `none`

## Routing Hints For Orchestrator
- required_reviewers: [review-async-lifecycle | review-psi-vfs-indexing | review-ui-platform]
- reasoning: [why exactly these]
```

## Acceptance checklist

- [ ] The declared focus matches what the orchestrator requested.
- [ ] The context is reusable across reviewers and contains no review findings or fix proposals.
- [ ] Architectural links are backed by code, not invented.
- [ ] For `freeze-safety`, the hot-path inventory, freeze markers, heuristics, evidence, and routing hints sections are all present.
- [ ] The full result is written to the file specified by the orchestrator; chat contains only the summary and the path.
