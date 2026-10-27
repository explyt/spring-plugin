---
name: "coding-guard"
schemaVersion: "v0.1"
description: "Compact coding guidelines for the explyt/spring-plugin repository - covers process rules (think first, simplicity, surgical changes), module layout, IntelliJ Platform threading and PSI rules, inspection and test conventions, message bundles, statistics tracking, and PR hygiene. You must call this skill before any code modification in this repository."
agent: null
used-by:
  - "Code"
  - "General"
---

# Coding Guard — explyt/spring-plugin

Rules to follow when writing or modifying code in this repository (Explyt Spring IntelliJ plugin). Grounded in `CONTRIBUTING.md`, `build.gradle.kts`, `gradle.properties`, and existing sources.

---

## 0. Before coding (process rules)

These apply **before** any technical rule below. Bias toward caution over speed; for trivial tasks use judgement.

### 0.1 Think before coding

- **State assumptions explicitly.** If uncertain about intent, data shape, or an API contract — ask. Don't guess.
- **Surface ambiguity, don't resolve it silently.** If the request has multiple reasonable interpretations, list them and let the user choose.
- **Push back when warranted.** If a simpler approach exists, say so before implementing the requested one.
- **Stop and name confusion.** Don't patch over unclear behavior with plausible-looking code.
- **Separate runtime correctness from the intended incomplete-code IDE workflow.** Before changing bean registration or matching, apply the consumer contracts in §0.5. If the intended behavior is unclear, ask the user before tightening a shared model; a permissive navigation candidate is not automatically a bug.

### 0.2 Simplicity first

- **Minimum code that solves the problem.** No features beyond what was asked, no abstractions for single-use code, no unrequested configurability.
- **No defensive code for scenarios the type system or existing invariants already rule out** (null checks on non-nullable Kotlin types, unreachable branches after an exhaustive `when` over a sealed type). Apply this only when an external explicit mechanism rules the case out — not your own "this can't happen" reasoning.
- **Preserve guards required by the specific platform contract**: read/write actions, cancellation, dumb-mode restrictions, and PSI/VirtualFile validity checks at deferred-use boundaries (for example after scheduling, coroutine suspension or a user edit). Keep existing guards when their necessity is unresolved; never remove them as an unrelated cleanup.
- **Do not substitute stale-PSI filtering for cache recovery.** Trace the cache holder, modification dependencies and read-action lifetime. Before claiming a cache fix or removing a cache-related validity guard, test query → real edit → query without manual invalidation; assert the full updated result set, not merely the absence of an exception.
- If you wrote 200 lines where 50 would do — rewrite.

### 0.3 Surgical changes

- **Touch only what's needed.** Don't "improve" adjacent code, comments, formatting, or imports outside your change.
- **Don't refactor code that isn't broken.** If a rule below flags nearby code you are not editing — mention it to the user, don't silently rewrite it.
- **Match existing style**, even if you'd write it differently.
- **Clean up only your own orphans**: imports/variables your edit made unused. Flag pre-existing dead code instead of deleting it.
- **Traceability test**: every changed line should trace directly to the user's request.

### 0.4 Goal-driven execution

- Convert tasks into verifiable goals before coding: "Fix the bug" → "Write a test that reproduces it, then make it pass"; "Refactor X" → "Tests pass before and after".
- For multi-step tasks, state a brief plan with a verification per step.

### 0.5 Navigation, diagnostics and context analysis

Before changing bean discovery, navigation, autowiring or context analysis, identify the affected consumers, their existing entry points, the module dependency graph, and whether an application/context is established. Apply these contracts; do not infer one from another:

| Consumer | Required contract |
|---|---|
| Navigation | Find suitable declarations in incomplete code; do not require proof of runtime registration to offer a transition. |
| Registration inspection | Explain an established configuration problem at its declaration and offer a valid quick fix; account for registration by a consuming application. |
| Context-dependent inspection | Evaluate registration, profiles and conditions relative to an established application/context; without one, do not turn context uncertainty into a definitive missing-bean error. For ordinary static-editor autowiring, apply the properties diagnostic rule below. |
| MCP bean query | Use the explicitly selected application/model or native snapshot, preserve its registration identities and conditions, and report unknowns and static/snapshot limitations. Never present a navigation candidate as a registered bean. |

- **Preserve the properties workflow.** Treat a plain `@ConfigurationProperties` declaration as an IDE candidate for injection-to-declaration and reverse navigation even before registration is complete. In ordinary static-editor autowiring, diagnose missing properties registration at its declaration with the registration inspection and a valid quick fix, not with a duplicate missing-bean error at injection; this applies whether registration is unknown or established as absent. Preserve unrelated, established injection errors. MCP and explicit-context analysis must still report the actual model result; do not change native-snapshot behavior. Do not diagnose missing registration merely because the class's own module has no enabler or application.
- **Treat shared modules as reusable, not as applications.** For `app1 → core ← app2`, each application can register the same core properties class differently. Do not silently select the first application, union their beans for a context verdict, or infer absence from searching only core and its dependencies. If a context-dependent verdict needs a choice that cannot be established, report uncertainty or defer that verdict while keeping navigation available.
- **Keep precision at the consumer boundary.** Reuse discovery and registration primitives; do not introduce duplicate full models or a generic policy framework by default. Keep permissive editor candidates out of MCP and explicit-context matching and condition evaluation before those operations run, not just out of final JSON; ordinary static-editor candidate matching follows the properties workflow above. Never augment a native snapshot with statically imagined candidates. Follow `spring-mcp-contracts` for MCP changes.
- **Do not make navigation pay for context simulation.** Trace the actual call path before adding profile, condition or application-registration analysis. If its result is not needed to find navigation targets, keep it off that path; adding permissive results after expensive analysis is not an optimization. Preserve profile/condition semantics for consumers that need context analysis; do not disable them globally.
- **Measure performance claims.** Treat reported CPU load, fan noise or perceived slowdown as a reason to investigate, not proof of a cause. Before claiming a performance improvement, compare the same project, interaction and indexing state before/after; report cold/warm cache conditions, elapsed time and profiler evidence or computation counts. If measurements are unavailable, state that performance is unverified; moving work off the EDT or adding a cache alone is not evidence.
- **Test both contracts on the same project.** For properties changes, cover unregistered and registered classes, both navigation directions, autowiring diagnostics, registration inspection and quick fix, and the contrasting MCP result through real entry points. Include sibling applications depending on a shared core with different registration/profile choices; assert dependency directions, no candidate/bean duplicates and no cross-application leakage. Apply production/test-source isolation and Java/Kotlin coverage where affected. Use query → edit → query tests without manual invalidation for cache changes.

---

## 1. Project facts (verify before assuming)

- **Kotlin 2.3.20, JDK 21** (`kotlinVersion` in `gradle.properties`; JDK toolchain in root `build.gradle.kts`). Kotlin-idiomatic code is expected; latest language features are welcome.
- Multi-module Gradle build: each module lives in `modules/<name>/` and is configured by `modules/<name>/<name>.gradle.kts` (not `build.gradle.kts`).
- `spring-bootstrap` is the assembly module. Run the sandbox with `./gradlew :spring-bootstrap:runIde` (the root `runIde` is intentionally disabled); build the ZIP with `./gradlew :spring-bootstrap:buildPlugin`. Default sandbox IDE is IntelliJ IDEA Community 2026.1 (`defaultIdeaVersion` / `defaultIdeaType=IC`).
- Package roots: `com.explyt.spring.<module>`, `com.explyt.quarkus.core`, `com.explyt.jpa`, `com.explyt.base`.

### Module boundaries

| Module | Owns |
|---|---|
| `spring-core` | Bean model, inspections, navigation, properties, debugger, statistics — most changes land here |
| `spring-web` / `spring-data` / `spring-security` / `spring-messaging` / `spring-integration` / `spring-cloud` / `spring-aop` / `spring-ai` / `spring-initializr` / `spring-gradle` | Feature areas named accordingly |
| `quarkus-core`, `jpa` | Quarkus support; shared JPA helpers |
| `base` | Low-level utilities shared by every module — must stay dependency-light and Spring-agnostic |
| `test-framework` | Shared test fixtures (`Explyt*TestCase`, `TestLibrary`) |
| `spring-bootstrap` | Assembly only; no feature code |

Put new code in the module that owns the feature area; shared utilities go to `base` only if genuinely generic.

---

## 2. File conventions

- **Every source file starts with the Apache-2.0 SPDX header.** Use the current year for new files; do not bump the year when editing existing files.

  ```kotlin
  /*
   * Copyright (c) 2026 Explyt Ltd
   * SPDX-License-Identifier: Apache-2.0
   */
  ```

- **User-visible strings go through the module's message bundle** (`SpringCoreBundle`, `SpringWebBundle`, `BaseBundle`, ...) with keys like `explyt.spring.inspection.kotlin.object.title`, backed by `messages/*.properties` in the module resources. No hardcoded user-facing English in code.
- Known Spring class FQNs come from constants holders such as `SpringCoreClasses` — don't inline `"org.springframework..."` string literals when a constant exists.

---

## 3. IntelliJ Platform threading & PSI

"Slow operation on EDT" is a dedicated component in this repo's bug-report form — it is a known recurring defect class. Rules:

- **Never** perform file I/O, network calls, slow/index-backed/blocking or unknown-cost PSI work on the EDT. A separately proven short read-only access is allowed only when the specific IntelliJ callback contract permits it and its cost is bounded.
- PSI reads happen under a read action; PSI/document/VFS writes under a write action (`WriteCommandAction` for document/PSI modifications, as quick fixes here do).
- Prefer `smartReadAction` / dumb-mode-aware code for index-dependent work; expect and handle `IndexNotReadyException` paths by design, not by catching blindly.
- Add `ProgressManager.checkCanceled()` inside loops that run under a read action or in long computations.
- Never swallow `ProcessCanceledException` — always re-throw. Never log it.
- Don't store raw `PsiElement` in fields or pass it across async/invokeLater boundaries — use `SmartPsiElementPointer` and check `isValid` on access.
- Cache PSI getter results in local `val`s inside hot code (line markers, inspections, completion) — getters traverse the tree on every call.
- Line marker providers, inspections, and completion contributors are hot paths: no resolve-heavy work in `getLineMarkerInfo` for leaf elements beyond what the platform contract allows; follow the patterns of existing providers in the same package.
- **Do not make state-synchronizing platform callbacks asynchronous without proving the callback contract permits it.** For `ExplytRunManagerListener.runConfigurationChanged`, update links synchronously only when `configuration.mainClassName` matches the stored `qualifiedMainClassName` using stable metadata. Legacy and Kotlin-façade links have no equivalent non-PSI identity: schedule `NativeLinkRepairService`'s smart-mode background repair instead of resolving `configuration.mainClass` with `runReadActionBlocking` on the EDT. `RunConfigurationExtractor`'s path fallback must keep a sync started before repair working with the renamed configuration.
- **`AnAction.getActionUpdateThread() = BGT` applies only to `update()`, not `actionPerformed()`.** Any PSI, index, project-model, UAST, or VFS work in `actionPerformed()` must be moved to a cancellable background read action or pooled task, with only UI/write operations returned to the EDT.
- **Treat static slow-operation reports as reachability hypotheses.** Before changing code, verify loader/type filters, guards, dynamic dispatch and read-lock branches; a broad interface sink or a sink after an excluded branch is not proof that the runtime path is reachable.

---

## 4. Inspections & quick fixes

- New inspections follow the existing pattern: extend the project base class (e.g. `SpringBaseUastLocalInspectionTool` for UAST inspections that serve both Java and Kotlin), live under `<module>/inspections/`, and register in the module's `plugin.xml` resources.
- Quick fixes live in `inspections/quickfix/` next to their inspection.
- Problem messages come from the module bundle.
- Copy a minimal existing example before writing from scratch: `SpringKotlinObjectInspection.kt` (~48 lines) plus its test is the reference pair named in `CONTRIBUTING.md`.

---

## 5. Tests

- Tests are **IntelliJ Platform tests**: headless IDE fixture, shared index, single-forked, slow — that is expected.
- Base classes come from `test-framework`: `ExplytInspectionJavaTestCase`, `ExplytInspectionKotlinTestCase`, `ExplytJavaLightTestCase`, `ExplytKotlinLightTestCase`, `ExplytBaseLightTestCase`.
- Declare required libraries via `override val libraries: Array<TestLibrary>` (e.g. `TestLibrary.springContext_6_0_7`).
- Inspection tests enable the inspection in `setUp()` and use `myFixture.configureByText` + `myFixture.testHighlighting` with `<warning>...</warning>` markers.
- Most inspection tests have a **Java and a Kotlin twin** under `inspections/java/` and `inspections/kotlin/` — add or update both when behavior affects both languages.
- Test data lives in the module's `testdata/` directory.
- Fast feedback loop: `./gradlew :spring-core:test --tests "com.explyt.spring.core.inspections.kotlin.SpringKotlinObjectInspectionTest"`.
- Before claiming no test covers a changed symbol, search `modules/<module>/src/test` for the class and symbol names. Run every directly covering existing test; compile-only or per-file lint is not sufficient.
- Run `:spring-core:test` classes **sequentially**, never as concurrent Gradle invocations and preferably not as broad wildcard batches. The IntelliJ fixture shares its sandbox, indices, test-result files, and library caches; concurrent/broad runs produce environmental `NoSuchFileException`, `StorageException`, `ZipException`, and `ServiceNotReadyException` failures. Re-run the exact failing class alone before attributing such failures to a code change.
- Add or update a test for any behavior change — PRs without tests are questioned in review.

---

## 6. Statistics

- User-facing actions (gutter clicks, generate actions, completions applied, tool-window buttons) are tracked: add an enum constant to `StatisticActionId` (`spring-core/.../statistic/`) with a human-readable description and call `StatisticService.getInstance().addActionUsage(...)` at the action point.
- When adding a new user-visible action or intention, check whether an analogous existing action is tracked — if yes, track yours the same way.

---

## 7. Kotlin pitfalls

| Pitfall | Rule |
|---|---|
| `catch (e: Exception)` | Re-throw `ProcessCanceledException`/`CancellationException` first |
| `var` property smart cast | Capture to local `val` before the null check |
| Java platform types (`T!`) | Treat as nullable unless annotated `@NotNull` |
| `return` inside `forEach` | Non-local return exits the enclosing function — use `return@forEach` |
| Sequences reused twice | Materialize with `toList()` before reuse |
| Mutable collections in shared state | Prefer immutable collections + `map`/`filter`/`flatMap` |
| `data class` with `var` fields | Use `val` + `copy()` |

---

## 8. PR hygiene (from CONTRIBUTING.md)

- One logical change per PR; reference the related issue (`Closes #123`).
- Branch names `username/feature-name`, e.g. `imuromtsev/kotlin-bean-inspection` (CONTRIBUTING.md).
- Clean, Kotlin-idiomatic code with the SPDX header present in every new file.
- Update docs/messages when behavior or usage changes; state how you verified the change.

---

## 9. Documentation synchronization

Choose documentation by audience instead of copying the full changelog everywhere:

- `CHANGELOG.md`: record all release-worthy user-facing changes and relevant maintenance work.
- `README.md`: keep a concise overview of durable differentiators and first-use workflows.
- `PLUGIN-DESCRIPTION.md`: highlight durable Marketplace-discovery features and benefits; avoid implementation details and minor fixes.
- Public GitHub wiki (`explyt/spring-plugin.wiki`, commonly checked out as sibling `../spring-plugin.wiki`): document detailed workflows, examples, inspection matrices, limitations, and migration guidance.

When a release adds a durable feature such as migration inspections or editor actions, update the concise entry points (`README.md` and `PLUGIN-DESCRIPTION.md`) and put the detailed usage in the public wiki. Keep isolated correctness fixes, EDT/PSI repairs, message fixes, and internal infrastructure changelog-only unless they change documented behavior.

## Output and acceptance

Report changed paths, the intended consumer behavior, verification commands/results, and unresolved limitations. If blocked or unverified, state that explicitly rather than claiming completion. For bean-model changes, distinguish IDE candidates from context-model results in the report.

Before reporting a change complete, check each applicable item; mark unrelated items not applicable:
- [ ] The affected consumer contracts are explicit; navigation remains available without proving runtime registration.
- [ ] Shared-core diagnostics do not assume one application, and context/MCP results do not include permissive candidates or another application's beans.
- [ ] Properties tests cover the IDE workflow and contrasting MCP behavior, including registration inspection/quick fix and multi-module cases; known absent registration does not reintroduce a duplicate missing-bean error at ordinary static-editor injection.
- [ ] Cache tests assert the full updated result after real edits, not just filtered stale PSI or absence of crashes.
- [ ] Performance claims include comparable measurements; otherwise performance is explicitly unverified.
- [ ] Existing platform guards, threading rules and directly covering tests are preserved or their intentional changes are verified.
