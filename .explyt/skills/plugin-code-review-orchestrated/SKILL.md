---
name: "plugin-code-review-orchestrated"
schemaVersion: "v0.1"
description: "Orchestrates a code review of the spring-plugin repository through scope/context passes, routing, domain reviewer subskills, and per-artifact validation. Use when asked to review a PR, diff, commit, branch, module, or file set, to run a full orchestrated review, or to produce a validated review report."
agent: "General"
used-by:
  - "Orchestrator"
  - "General"
---
# Orchestrated code review

You are the review orchestrator.
You do not replace the reviewer subskills.
Your job:
1. normalize the review target;
2. build a shared review packet;
3. select the relevant reviewer subskills;
4. run them in parallel;
5. hand the results to validators;
6. assemble a single final report.

You do not fix code within this skill.
You organize the review, persist artifacts, and show the user the outcome.

## Default loop

`clarify minimal -> scope resolve -> optional context -> review packet -> routing -> review wave -> per-artifact validation wave -> aggregate final report`

## Mandatory agent/skill passes

Always run these pairs:
- `call_ask_agent` + skill `review-scope-resolver`
- `call_review_agent` + skill `review-core-correctness`
- `call_code_agent` + skill `review-findings-validator` for each reviewer artifact from the review wave

## Optional agent/skill passes (by routing decision)

- `call_ask_agent` + skill `review-context-builder`
- `call_review_agent` + skill `review-async-lifecycle`
- `call_review_agent` + skill `review-psi-vfs-indexing`
- `call_review_agent` + skill `review-ui-platform`
- `call_review_agent` + skill `review-persistence-security-resources`
- `call_review_agent` + skill `review-statistic-coverage`
- `call_review_agent` + skill `review-ui-leak-via-listeners`

Every subagent call in every wave must state its expected result shape: **a brief summary in chat plus the exact path of the artifact file written**.

## 1. Minimal clarification

First determine what exactly must be reviewed.

If the user already gave a technical reference, use it as the input for the `call_ask_agent` handoff with skill `review-scope-resolver`: PR, diff, commit, branch, issue, URL, file set, directory or module, or a specific file/class/function.

Ask the user at most one short question, and only if:
- the review target is not defined at all;
- or a blocking ambiguity remains after scope resolution.

Do not ask the user to retell context that can be obtained by investigation.
Do not treat currently open files as an implicit review target without explicit confirmation.

## 2. Scope resolve

First run `call_ask_agent` with skill `review-scope-resolver`. First handoff line:

`Use only the skill \`review-scope-resolver\`. Do not use any other skill.`

Its task: normalize the technical reference; determine the exact review target; separate in-scope and out-of-scope; list files, diffs, modules, and related artifacts; return routing signals for the domain reviewer subskills.

The result must be saved to `REVIEW_SCOPE.md` inside the workspace; the subagent returns a brief summary plus the artifact path, and the orchestrator must then read that artifact itself.

## 3. Optional context

After scope resolution decide whether shared reusable context is needed.

Run `call_ask_agent` with skill `review-context-builder` (first handoff line: `Use only the skill \`review-context-builder\`. Do not use any other skill.`) if:
- the review target spans several modules (e.g. `modules/spring-core` + `modules/spring-web`);
- there are integrations and side effects;
- a shared set of architectural or test constraints is needed;
- the review would be shallow without a system-level picture.

You may run it once or several times (in parallel) with different focuses, split by focus area: `architecture`, `tests-conventions`, `freeze-safety` (run the latter if the target touches user-perceived responsiveness — see routing signals below). Each context pass returns a brief summary plus the artifact path.

## 4. Workspace

Create a single workspace folder: `.tasks/review-[kebab-case-target-slug]/`

Minimal structure:

```text
.tasks/review-[target-slug]/
├── REVIEW_SCOPE.md
├── REVIEW_CONTEXT.md
├── REVIEW_PACKET.md
├── ROUTING_DECISION.md
├── review/
├── validation/
└── FINAL_REVIEW_REPORT.md
```

## 5. Review packet

Assemble `REVIEW_PACKET.md` after the scope/context wave. It must contain:
- the exact review target;
- in-scope / out-of-scope;
- a short reusable summary from the scope/context artifacts;
- key modules, dependencies, and contracts;
- important risks;
- unresolved unknowns;
- the list of paths, files, diffs, modules, and tests to use as the baseline.

The review packet must be brief but lossless: do not lose important facts even while cutting noise.

## 6. Routing decision

After the review packet, decide which reviewer agent/skill passes are actually needed.
Save the decision to `ROUTING_DECISION.md`.

### 6a. Skill invocation contract

Each pass must be launched as **a separate call of the right subagent tool with the exact skill name stated explicitly**.

The first line of every handoff prompt must forbid all other skills, using the canonical template:

`Use only the skill \`<exact-skill-name>\`. Do not use any other skill.`

Mandatory orchestration rules: one pass = one subagent call = one explicitly named skill = a first handoff line that forbids all other skills. A skill name must never be implied by description or focus; never replace a skill invocation with loose text like "do a review focused on async".

Every handoff must state explicitly:
- which subagent tool to call;
- (first line) which exact skill to activate and that no other skill may be used;
- the exact review target;
- which artifacts to read;
- where to write the full result;
- the expected result shape: brief summary + artifact path.

Agent distribution by pass type:
- `review-scope-resolver` — **`call_ask_agent`**;
- `review-context-builder` — **`call_ask_agent`**;
- reviewer passes (`review-core-correctness` and the domain reviewer subskills) — **`call_review_agent`**;
- `review-findings-validator` — a separate **`call_code_agent`** per reviewer artifact.

### Exact skill names

Use only these exact skill names: `review-scope-resolver`, `review-context-builder`, `review-core-correctness`, `review-async-lifecycle`, `review-psi-vfs-indexing`, `review-ui-platform`, `review-persistence-security-resources`, `review-statistic-coverage`, `review-ui-leak-via-listeners`, `review-findings-validator`.

Never send a downstream pass an instruction like "act as the async reviewer" or "do a validator pass" — always the canonical first-line template with the exact skill name.

### Base rule

Always run `call_review_agent` with skill `review-core-correctness`.
Run domain reviewer passes only when there are explicit signals in the target, diff, or surrounding code.

### Routing matrix

#### `review-async-lifecycle`
Run when you see: coroutines, `launch`, `async`, `withContext`; `Dispatchers.*`; `runBlocking`; `readAction`, `runReadAction`, `writeAction`, `runWriteAction`; `invokeLater`, `invokeAndWait`; `Disposable`, `dispose`, listeners, async callbacks; `messageBus`, `subscribe`, `connect`.

#### `review-psi-vfs-indexing`
Run when the target touches: `Psi*`, `UAST`, `VirtualFile`, `Document`; `FileBasedIndex`, `StubIndex`, `CachedValuesManager`; completion, inspections, intentions, references, resolve, line markers; VFS listeners and file traversal.

#### `review-ui-platform`
Run when the target touches: Swing / JB UI components; `AnAction`, `ActionGroup`, `ToolWindowFactory`; `DialogWrapper`, popups, notifications, renderers; `JBCef*`, accessibility, HiDPI, theme-aware UI; UI-related `plugin.xml` registrations: actions, groups, tool windows, notifications, status bar widgets, icons.

#### `review-ui-leak-via-listeners`
Run when the target touches:
- `ObservableProperty` / `ObservableMutableProperty` / `.transform { }` / `.and(...)` / `.or(...)` / `.not()`;
- `Cell.visibleIf` / `Cell.enabledIf` / `Cell.bindText` / `Cell.bindIntText` / `Cell.bindSelected`;
- `JComponent.bindEnabled` / `JComponent.bindVisible` (single-argument IntelliJ overloads from `com.intellij.openapi.observable.util`);
- `afterChange { }` / `afterNonEqChange { }` without an explicit `parentDisposable`;
- listener registrations on properties of long-lived APP/Project services;
- any `var holder` / `var listeners` in an `object` / `companion object` without `WeakReference`.

#### `review-persistence-security-resources`
Run when the target touches: `@State`, `PersistentStateComponent`, settings; credentials, `PasswordSafe`, secrets, privacy; caches, client reuse, resource lifetime; logging hygiene; process/path logic, cross-platform behavior; long-lived state; `@Service`, service lifetime, light services, cached service references; extension points, `EP_NAME`, `extensionList`, `plugin.xml`, optional `depends`, dynamic EP behavior; `FileType` / `Language` used as map keys; NOTICE / licensing-sensitive changes.

#### `review-statistic-coverage`
Run when the target touches:
- a new `AnAction`, intention action, quick-fix, gutter icon handler, completion insert handler, or other user-visible entry point;
- a new outcome branch next to already-tracked branches;
- `StatisticActionId`, `StatisticService`, `StatisticUtil`, `StatisticInsertHandler` (in `modules/spring-core/.../statistic/`);
- changes that add user-facing features without any statistics call.

#### Freeze-safety routing (aggregate trigger)

If the target contains any signal below, you must:
1. run `call_ask_agent` with skill `review-context-builder`, focus `freeze-safety`, before the review wave;
2. read the produced context artifact;
3. include the reviewer subskills listed in its `Routing Hints For Orchestrator` (at minimum `review-async-lifecycle` + `review-ui-platform`; `review-psi-vfs-indexing` when a PSI hot path is touched);
4. attach the freeze-safety artifact path to every reviewer handoff.

Signals that require the freeze-safety pass:
- renderers: `ListCellRenderer`, `TreeCellRenderer`, `TableCellRenderer`, `ColoredListCellRenderer`, `ColoredTreeCellRenderer`, `paintComponent`, `customizeCellRenderer`;
- action update: any `AnAction.update` override, `ActionPromoter`, `EditorActionHandler.isEnabledForCaret`;
- service init: a new or changed `@Service` constructor / `init {}`, `StartupActivity`, `ProjectActivity`, `projectOpened`, `postStartupActivity`;
- UI open path: `ToolWindowFactory.createToolWindowContent`, `DialogWrapper.createCenterPanel`, `Configurable.createComponent`, `createUIComponents`;
- typing hot path: `CompletionContributor`, `Annotator`, `ExternalAnnotator`, `TypedHandlerDelegate`, `LineMarkerProvider`, `DocumentListener.documentChanged`;
- hot-path event handlers: `BulkFileListener`, `CaretListener`, `SelectionListener`;
- dispose path: `dispose()`, `disposeUIResources()`, `projectClosed` with logic;
- the code already has or adds `SlowOperations.allowSlowOperations` / `invokeAndWait`;
- `logs/threadDumps-freeze-*` folders are present in the project root (regardless of the diff), or the user explicitly complains about lags / freezes / jitter / slow window opening.

Freeze-safety routing does not replace the domain reviewer subskills — it only adds them to the review wave with extra freeze-safety context.

Do not run all subskills indiscriminately just for symmetry.

## 7. Review wave

### 7a. Parallelism and scope limits

#### Parallelism limits
- **Max 6 domain reviewer agents in one wave.** There are exactly 6 domain reviewer skills + `review-core-correctness` = 7 passes maximum. `review-core-correctness` is always mandatory and does not count toward the limit.
- **Max 2 review sessions in parallel.** If `review-scope-resolver` returned several independent clusters, run at most 2 clusters in parallel; the rest wait so artifacts do not get mixed.

#### Scope limit per reviewer
**Max 15 significant files per reviewer pass.** Significant files carry business logic, UI, services, inspections. Not counted: tests, XML/`plugin.xml`, configs, `.gradle`, `.md`, resources.

If a single cluster has more than 15 significant files:
1. `review-scope-resolver` must have returned `priority_core_files` (15 or fewer) and `periphery_files`;
2. give the reviewer both lists with explicit instructions: `priority_core_files` — deep review; `periphery_files` — surface-level check for obvious Critical/High patterns only;
3. never slice scope mechanically by file count — only by semantic cohesion.

#### Cluster limit
File-count chunking is forbidden. Splitting is allowed only when `review-scope-resolver` explicitly identified **independent semantic clusters** (different features, unrelated bugfixes, modules without shared contracts). One coherent flow across 40 files is **one** cluster with prioritization, not 3 chunks.

### 7b. Launch

After routing, launch in parallel in one message (respecting the limits of section 7a):
- `call_review_agent` with skill `review-core-correctness`;
- `call_review_agent` for every selected domain reviewer skill — split by domain skill, one call per skill.

Each reviewer handoff must include:
- the first line with the exact skill name and the prohibition of other skills;
- the precise review goal and exact review target;
- the paths to `REVIEW_SCOPE.md` and `REVIEW_PACKET.md` (plus the freeze-safety artifact when applicable);
- the explicit reviewer focus;
- the expected findings format;
- the output path inside `review/`;
- the expected result shape: brief summary + artifact path.

Each reviewer must: activate only the one named skill; read the surrounding code itself; write the full result to its own file inside `review/`; return only a brief summary and the exact artifact path.

## 8. Validation wave

After the review wave, run a separate `call_code_agent` with skill `review-findings-validator` **for each reviewer artifact separately** — split per artifact, at most 7 validator passes (one per reviewer artifact).

Never run one shared validator pass over all reviewer artifacts.
Each validator pass receives **exactly one** reviewer artifact and writes **exactly one** validated artifact.

The first line of every validator handoff must be:

`Use only the skill \`review-findings-validator\`. Do not use any other skill.`

Each validator handoff must include: the exact review target; the paths to `REVIEW_SCOPE.md` and `REVIEW_PACKET.md`; the path to exactly one reviewer artifact inside `review/`; the output path inside `validation/`; the expected result shape: brief summary + artifact path.

Each validator must: read `REVIEW_SCOPE.md`, `REVIEW_PACKET.md`, and exactly one reviewer artifact; verify only that artifact's claims; classify findings as `TP`, `FP`, or `Need more data`; save the result to its own file inside `validation/`.

Naming pattern:
- `review/review-core-correctness.md` -> `validation/review-core-correctness.validated.md`
- `review/review-async-lifecycle.md` -> `validation/review-async-lifecycle.validated.md`

The orchestrator must wait for **all** validator passes, then read **all** validated artifacts inside `validation/` itself.

## 9. Final report

After validation you yourself:
1. read all validated artifacts inside `validation/`;
2. aggregate them into a single truth set;
3. deduplicate across reviewer streams at the validated-findings level;
4. write a detailed `FINAL_REVIEW_REPORT.md`;
5. generate the Bug Roast section (below) — only from confirmed findings, inventing nothing;
6. show the user a short summary.

### Canonical chat report format

```text
## Review Summary

**Scope:** [what was reviewed]
**Reviewers:** [core + list of domain reviewer subskills]
**Validated Artifacts:** [file list from `validation/`]

### Statistics
- Total reviewer findings: [n]
- Total validated findings: [n]
- Confirmed (TP): [n]
- False positives (FP): [n]
- Need more data: [n]
- Unique issues after cross-artifact deduplication: [n]

### Confirmed Issues
#### [SEVERITY]
**[Title]** — Validated in: [validated artifact path]; Original source: [reviewer id];
Location: [file:line or area]; Description: [what is wrong and why]; Recommendation: [what to fix]

### Needs More Data
**[Title]** — Validated in: [validated artifact path]; Original source: [reviewer id];
Description: [essence]; What's missing: [what is lacking]

### False Positives
- [short list or `none`]

### Positive Observations
- [if any]

### Bug Roast 🔥
[see rules below]

### Artifacts
- `REVIEW_SCOPE.md`, `REVIEW_PACKET.md`, `validation/*.validated.md`, `FINAL_REVIEW_REPORT.md`
```

### Bug Roast

A satirical closing section based only on confirmed findings. Structure:
- **🏆 Bug of the Day** — the most expressive Critical/High finding: title, 1–2 sentences of sports-commentator praise, evidence file:line.
- **🏅 Awards** — 1 to 5 nominations, only for real confirmed findings (e.g. 🧊 The Great EDT Freezer, 💣 The Silent Crash for a `catch (Exception)` eating `CancellationException`, 👻 Ghost In The Dispose, 🎩 The Magician of NPE, 🌪️ O(n²) Storm, 🕒 Eternal `runBlocking`, 🎨 HiDPI Denier, 🧹 The Hoarder for a cache without eviction). Skip empty nominations.
- **🎭 Overheard In The Stack Trace** — 1–3 short imaginary quotes by the affected classes (classes speak, not people).
- **📊 Bug Tasting Notes** — severity distribution from Statistics, a one-line sommelier flavour profile, one ironic "pairs well with" recommendation.
- **🥹 Mood Meter** — one line of 5–7 emoji reflecting the PR state (clean: `🌟✨👏🚲🌿`; average: `👀🧵🛠️🔍🌫️`; with Critical: `🚨🔥💥🧯⛑️💀`).

Hard rules:
1. **Roast the code, not people.** No git blame, commit authors, reviewers, handles, emails, or names. A class may "speak"; a developer may not.
2. **Zero fabricated findings.** Every nomination, quote, and tasting note must rest on a concrete confirmed finding with file:line; no phantom evidence.
3. **Severity is never masked by humor.** Critical stays Critical; rewrite any joke that reads as "it's fine".
4. **No offensive humor** (interns, juniors, nationalities, competing projects) and **no NSFW** (profanity, politics, religion).
5. **Brevity over humor.** Bug Roast is always last and shorter than the technical part; it never replaces Confirmed Issues.
6. **Clean-PR case.** If Confirmed (TP) = 0, replace the whole section with one congratulatory line: "Nothing to roast — the code is as clean as a fresh VFS refresh."
7. **Language** follows the main report language. **Technical accuracy** is mandatory: every term (`EDT`, `runBlocking`, `CancellationException`, `readAction`) must be used correctly.

If any rule is violated, delete that line from the Bug Roast — do not rewrite it as a compromise.

## 10. Hard handoff rules

Results of previous passes are not automatically visible to the next passes.

Every downstream pass must receive: the exact subagent tool to call; a first line naming the exact skill and forbidding all others; and either the actual content it needs or the exact path of the file where that content is stored.

If a subskill wrote its result to a file:
- do not rely on its summary alone;
- read the file yourself;
- only then build the next handoff.

For the validation wave the rule is stricter: one validator pass gets exactly one reviewer artifact; one validator pass writes exactly one validated artifact; the orchestrator must read all validated artifacts itself before assembling the final report.

A handoff without an exact agent tool, an exact skill name, or the first-line prohibition of other skills is incomplete.

Names are not data. Identifiers are not payload.

## 11. Anti-patterns

Do not:
- skip scope resolution;
- run `review-scope-resolver` or `review-context-builder` through `call_review_agent` instead of `call_ask_agent`;
- run `review-findings-validator` through `call_review_agent` instead of a separate `call_code_agent`;
- run all reviewer subskills without routing;
- replace an explicit skill invocation with a free-text reviewer role description;
- send a handoff without the first-line skill prohibition, the exact agent tool, or the exact skill name;
- run one validator pass over all reviewer artifacts at once;
- output the final report before reading all validated artifacts yourself;
- replace findings with general impressions, or treat an indirect signal as a confirmed problem;
- start code changes instead of reviewing;
- put the Bug Roast above Confirmed Issues, let it replace the technical part, or violate its hard rules.

## 12. Evidence rules

- Every finding must rest on verifiable code evidence.
- Do not invent user intent, issue details, or missing context.
- If data is insufficient, use `Need more data`.
- If a subskill is not applicable, it must explicitly return `not_applicable`.
- The final report must be concrete, verifiable, and usable for follow-up actions.

## Output format

- All artifacts live in `.tasks/review-[target-slug]/` as described in section 4.
- The final deliverables are `FINAL_REVIEW_REPORT.md` plus the canonical chat report from section 9.

## Acceptance checklist

- [ ] `REVIEW_SCOPE.md` exists and was produced by `call_ask_agent` + `review-scope-resolver`.
- [ ] `REVIEW_PACKET.md` and `ROUTING_DECISION.md` exist before the review wave.
- [ ] Every handoff first line names exactly one skill and forbids all others.
- [ ] `review-core-correctness` ran; domain reviewers ran only per routing signals.
- [ ] Parallelism limits respected: max 6 domain reviewers per wave, max 2 cluster sessions, max 15 significant files deep-reviewed per pass.
- [ ] One `call_code_agent` validator pass per reviewer artifact; one validated artifact each.
- [ ] The orchestrator read all validated artifacts before writing `FINAL_REVIEW_REPORT.md`.
- [ ] The chat report follows the canonical format, and the Bug Roast obeys its hard rules.
