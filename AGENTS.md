# AGENTS.md — Contributor Guide

Explyt Spring: a multi-module IntelliJ Platform plugin for Spring/Quarkus/JPA support. Kotlin 2.3.20, JDK 21, Gradle.
Public open-source project (Apache-2.0): <https://github.com/explyt/spring-plugin> — issues, discussions, and PRs live there.

## Project structure

- `modules/<name>/` — one Gradle module each, configured by `modules/<name>/<name>.gradle.kts` (not `build.gradle.kts`).
- `spring-core` owns the bean model, inspections, navigation, properties, statistics — most changes land here. Feature modules (`spring-web`, `spring-data`, `spring-security`, …) match their names. `base` is Spring-agnostic shared utilities; `test-framework` holds test fixtures; `spring-bootstrap` is assembly-only (no feature code).
- Sources: `modules/<name>/src/main/kotlin/com/explyt/...`; tests: `src/test/kotlin`; test data: `modules/<name>/testdata/`.
- `explyt-spring-boot-bean-reader/` is a separate Maven project (runtime javaagent for Native Context Mode).

## Build, test, run

```bash
./gradlew :spring-bootstrap:runIde       # launch sandbox IDE (root runIde is disabled)
./gradlew :spring-bootstrap:buildPlugin  # ZIP → modules/spring-bootstrap/build/distributions/
./gradlew :spring-core:test              # one module's tests
./gradlew :spring-core:test --tests "com.explyt.spring.core.inspections.kotlin.SpringKotlinObjectInspectionTest"
```

Tests are IntelliJ Platform tests (headless IDE fixture) — slow and single-forked by design.

## Code style and conventions

- Kotlin-idiomatic code; match surrounding style. Every source file starts with the Apache-2.0 SPDX header (`Copyright (c) 2024 Explyt Ltd`).
- User-visible strings go through module message bundles (`SpringCoreBundle`, …); Spring FQNs come from constants holders like `SpringCoreClasses`.
- IntelliJ threading rules apply: no slow work on EDT, PSI access under read/write actions, re-throw `ProcessCanceledException`, use `SmartPsiElementPointer` across async boundaries.
- New inspections extend existing base classes (e.g. `SpringBaseUastLocalInspectionTool`), live in `<module>/inspections/`, and are registered in the module's `plugin.xml`. Reference pair: `SpringKotlinObjectInspection.kt` + its test.
- Inspection tests use `test-framework` base classes (`ExplytInspectionKotlinTestCase`, …), declare `libraries: Array<TestLibrary>`, and usually have Java and Kotlin twins under `inspections/java/` and `inspections/kotlin/`. Add or update tests for any behavior change.
- Track new user-facing actions via `StatisticActionId` + `StatisticService.addActionUsage(...)`.

## Releases, branches and versions

- `main` is the trunk and targets the **latest supported IDEA line** (currently 2026.2 / `262`). Every release branch is an ancestor of `main`; fixes land on `main` first and are cherry-picked outward, never the reverse.
- `pluginVersion` in `gradle.properties` is the **generation** of the *next* release, not of the last one. `main` opens the next generation right after a release; release branches keep the generation they shipped.
- A published version is `<line>.<generation>.<CI run>` — e.g. `262.34.101` = line 262, generation 34, run 101. The tag is named after the **newest** line in that release, but one release ships **one asset per supported line** (`262.34.101` shipped `242`, `243`, `251`, `252`, `253`, `261`, `262`).
- A generation is released **once**. Re-releasing an already-published generation is reserved for hotfixes and is not the normal flow; ordinary fixes go into the next generation.
- `CHANGELOG.md`: only `[Unreleased]` and the **current** release carry brackets; older sections are plain (`## 261.33.80 - 2026-04-28`). `[Unreleased]` must contain only entries for the generation now in `pluginVersion` — never mix them into an already-shipped section.
- When cutting a release, add the released section on `main`, bump `pluginVersion`, and pin `untilVersion` on the older-line branches (`untilVersion=261.*`, not `265.*`).

## Commits and pull requests

- Conventional commit prefixes: `feat:`, `fix:`, `docs:`, `chore:`, `ci:`; reference issues/PRs, e.g. `fix: EDT while navigation (#227)`.
- Branches: `username/feature-name`, e.g. `imuromtsev/kotlin-bean-inspection`.
- PRs: one logical change, link the issue (`Closes #123`), describe the purpose, include tests, and state how you verified the change. See `CONTRIBUTING.md`.
