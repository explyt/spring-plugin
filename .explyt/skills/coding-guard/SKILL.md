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

### 0.2 Simplicity first

- **Minimum code that solves the problem.** No features beyond what was asked, no abstractions for single-use code, no unrequested configurability.
- **No defensive code for scenarios the type system or existing invariants already rule out** (null checks on non-nullable Kotlin types, unreachable branches after an exhaustive `when` over a sealed type). Apply this only when an external explicit mechanism rules the case out — not your own "this can't happen" reasoning.
- **Do not remove platform-mandated guards**: PSI `isValid` checks, `VirtualFile` validity checks, read/write-action wrappers, `ProgressManager.checkCanceled()` calls, and dumb-mode guards are required even if they look defensive.
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

- **Every source file starts with the Apache-2.0 SPDX header:**

  ```kotlin
  /*
   * Copyright (c) 2024 Explyt Ltd
   * SPDX-License-Identifier: Apache-2.0
   */
  ```

- **User-visible strings go through the module's message bundle** (`SpringCoreBundle`, `SpringWebBundle`, `BaseBundle`, ...) with keys like `explyt.spring.inspection.kotlin.object.title`, backed by `messages/*.properties` in the module resources. No hardcoded user-facing English in code.
- Known Spring class FQNs come from constants holders such as `SpringCoreClasses` — don't inline `"org.springframework..."` string literals when a constant exists.

---

## 3. IntelliJ Platform threading & PSI

"Slow operation on EDT" is a dedicated component in this repo's bug-report form — it is a known recurring defect class. Rules:

- **Never** perform file I/O, network calls, indexes access, or heavy PSI work on the EDT.
- PSI reads happen under a read action; PSI/document/VFS writes under a write action (`WriteCommandAction` for document/PSI modifications, as quick fixes here do).
- Prefer `smartReadAction` / dumb-mode-aware code for index-dependent work; expect and handle `IndexNotReadyException` paths by design, not by catching blindly.
- Add `ProgressManager.checkCanceled()` inside loops that run under a read action or in long computations.
- Never swallow `ProcessCanceledException` — always re-throw. Never log it.
- Don't store raw `PsiElement` in fields or pass it across async/invokeLater boundaries — use `SmartPsiElementPointer` and check `isValid` on access.
- Cache PSI getter results in local `val`s inside hot code (line markers, inspections, completion) — getters traverse the tree on every call.
- Line marker providers, inspections, and completion contributors are hot paths: no resolve-heavy work in `getLineMarkerInfo` for leaf elements beyond what the platform contract allows; follow the patterns of existing providers in the same package.

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
- Branch names like `feature/kotlin-bean-inspection` or `fix/openapi-npe`.
- Clean, Kotlin-idiomatic code with the SPDX header present in every new file.
- Update docs/messages when behavior or usage changes; state how you verified the change.
