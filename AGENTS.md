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

## Commits and pull requests

- Conventional commit prefixes: `feat:`, `fix:`, `docs:`, `chore:`, `ci:`; reference issues/PRs, e.g. `fix: EDT while navigation (#227)`.
- Branches: `username/feature-name`, e.g. `imuromtsev/kotlin-bean-inspection`.
- PRs: one logical change, link the issue (`Closes #123`), describe the purpose, include tests, and state how you verified the change. See `CONTRIBUTING.md`.
