# Contributing to Explyt Spring Plugin

Thank you for your interest in contributing to Explyt Spring Plugin. This document provides guidelines for submitting contributions. By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](https://github.com/explyt/spring-plugin/blob/main/LICENSE.md).

> New here? Jump to the [Quickstart](#3-quickstart-5-minutes) and pick up a [`good first issue`](https://github.com/explyt/spring-plugin/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22).

## Table of Contents
1. [Code of Conduct](#1-code-of-conduct)
2. [How we handle issues (two-lane policy)](#2-how-we-handle-issues-two-lane-policy)
3. [Quickstart (5 minutes)](#3-quickstart-5-minutes)
4. [Project layout (module map)](#4-project-layout-module-map)
5. [Running and writing tests](#5-running-and-writing-tests)
6. [Submitting contributions](#6-submitting-contributions)
7. [Pull request guidelines](#7-pull-request-guidelines)
8. [Review and approval process](#8-review-and-approval-process)
9. [Where to ask questions](#9-where-to-ask-questions)
10. [License agreement](#10-license-agreement)

## 1. Code of Conduct
By participating in this project, you agree to help maintain a welcoming and respectful environment for everyone.

## 2. How we handle issues (two-lane policy)

We want the repository to be contribution-friendly **without putting important features on hold**. To make that explicit, issues live in one of two lanes:

- **Lane A — Roadmap items.** Anything important to the plugin is implemented by the Explyt team on its normal schedule. These are **never** blocked waiting for an external contributor. If you comment on a roadmap issue and we are already working on it, we will say so.
- **Lane B — [`good first issue`](https://github.com/explyt/spring-plugin/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) / [`help wanted`](https://github.com/explyt/spring-plugin/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22).** Genuinely optional, well-scoped tasks (small UX, docs, messages, a focused inspection) where a longer wait costs nothing. These are the best entry points for new contributors, and each one carries an onboarding comment with the entry-point file, an example to copy, and the test to add.

**Claiming etiquette.** Comment on a Lane-B issue to claim it. We will assign it to you for a soft hold of about **two weeks**. If life happens and you cannot finish, just say so (or go quiet) and we will reopen it for others — no hard feelings.

## 3. Quickstart (5 minutes)

### Prerequisites
- **JDK 21.** The build targets Java 21. Gradle's toolchain support (foojay resolver) can auto-provision it, but having a JDK 21 installed avoids surprises.
- **Git** and a **GitHub account** (fork + PR workflow).
- **IntelliJ IDEA** (Community is fine) to edit the code. The sandbox the plugin launches into is **IntelliJ IDEA Community 2026.1** by default (`defaultIdeaVersion` / `defaultIdeaType=IC` in `gradle.properties`).

### Clone and launch a sandbox IDE
```bash
git clone https://github.com/<your-username>/spring-plugin.git
cd spring-plugin
./gradlew :spring-bootstrap:runIde
```
This downloads the IntelliJ Platform on first run (a few minutes), then opens a sandbox IDE with the plugin loaded. Open any Spring Boot project inside it to try your changes.

> ⚠️ Always use the **module-qualified** task `:spring-bootstrap:runIde`. The root `runIde` is intentionally disabled and will fail with a reminder — `spring-bootstrap` is the assembly module that wires all the others into the final plugin.

### Build the installable plugin ZIP
```bash
./gradlew :spring-bootstrap:buildPlugin
# output: modules/spring-bootstrap/build/distributions/*.zip
```
You can install that ZIP via **Settings → Plugins → ⚙ → Install Plugin from Disk…**.

On Windows use `gradlew.bat` instead of `./gradlew`.

## 4. Project layout (module map)

The plugin is a multi-module Gradle build. Each module lives in `modules/<name>/` and is built with `<name>.gradle.kts`. Most contributions touch **`spring-core`**.

| Module | What lives there |
|---|---|
| `spring-core` | The heart of the plugin: bean model, inspections, navigation, properties, debugger, statistics. Start here. |
| `spring-web` | Spring MVC / WebFlux endpoints, OpenAPI / Swagger UI, `coRouter`. |
| `spring-data` | Spring Data / JPA repositories, debug run markers. |
| `spring-security` | Spring Security support. |
| `spring-messaging` | Kafka / RabbitMQ listeners and messaging endpoints. |
| `spring-integration` | Spring Integration support. |
| `spring-cloud` | Spring Cloud support. |
| `spring-aop` | AOP / AspectJ navigation and inspections. |
| `spring-initializr` | Spring Initializr (start.spring.io) project wizard. |
| `spring-ai` | AI actions and the Spring-aware **MCP tools**. |
| `quarkus-core` | Quarkus CDI / JAX-RS / AOP support. |
| `jpa` | JPA/persistence helpers shared across modules. |
| `spring-gradle` | Gradle-specific integration. |
| `base` | Low-level utilities shared by every module. |
| `test-framework` | Shared test fixtures and light-test base classes. |
| `spring-bootstrap` | Assembly module: depends on the others, builds the published plugin, owns `runIde` / `buildPlugin` / `patchPluginXml`. |

The runtime javaagent that powers **Native Context Mode** is a separate Maven project under `explyt-spring-boot-bean-reader/`.

## 5. Running and writing tests

These are **IntelliJ Platform tests** — they spin up a headless IDE fixture and a shared index, so they run single-forked and are slower than plain unit tests. That is expected.

```bash
# All tests (slow)
./gradlew test

# One module
./gradlew :spring-core:test

# One test class (fastest feedback loop)
./gradlew :spring-core:test --tests "com.explyt.spring.core.inspections.kotlin.SpringKotlinObjectInspectionTest"
```

**Pattern to copy for a new inspection.** A minimal, self-contained example is:
- Inspection: [`modules/spring-core/src/main/kotlin/com/explyt/spring/core/inspections/SpringKotlinObjectInspection.kt`](modules/spring-core/src/main/kotlin/com/explyt/spring/core/inspections/SpringKotlinObjectInspection.kt) (~48 lines)
- Test: [`modules/spring-core/src/test/kotlin/com/explyt/spring/core/inspections/kotlin/SpringKotlinObjectInspectionTest.kt`](modules/spring-core/src/test/kotlin/com/explyt/spring/core/inspections/kotlin/SpringKotlinObjectInspectionTest.kt) (~42 lines)
- Test data lives next to each module under `testdata/`.

Most inspection tests have a Java and a Kotlin twin under `inspections/java/` and `inspections/kotlin/`. Add (or update) a test for any behavior change.

**Code style.** Keep it Kotlin-idiomatic and consistent with the surrounding code. Every source file carries the Apache-2.0 SPDX header:
```kotlin
/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */
```

## 6. Submitting contributions
1. **Fork the repository** to create your own working copy.
2. **Create a branch** named `username/feature-name`, e.g. `imuromtsev/kotlin-bean-inspection`.
3. **Make your changes** following the guidelines above.
4. **Add or update tests** and run the relevant module's tests locally.
5. **Commit** with clear, descriptive messages.
6. **Push** your branch and open a pull request against `main`.

## 7. Pull request guidelines
Please make sure your pull request (PR) meets the following criteria:
- **Link an issue (if applicable):** reference any related issue (e.g. `Closes #123`).
- **Detailed description:** explain the purpose of the PR and what problem it solves.
- **Scope:** keep PRs focused — one logical change per PR is easiest to review.
- **Code style:** clean, well-structured, Kotlin-idiomatic, SPDX header present.
- **Documentation:** update relevant docs (README / wiki / messages) when behavior or usage changes.
- **Testing:** include or update tests; state how you verified the change.

## 8. Review and approval process
After you submit a pull request, the Explyt team will:
- **Conduct an initial review** to ensure the PR aligns with the project's purpose and standards.
- **Provide feedback and request changes** if adjustments are needed.
- **Approve and merge** the PR once it is ready. Only maintainers have merge permissions.

## 9. Where to ask questions
- 💬 **Telegram:** [English](https://t.me/explytspring_en) · [Russian](https://t.me/explytspring)
- 🗨️ **GitHub Discussions:** <https://github.com/explyt/spring-plugin/discussions> — Q&A, ideas, and show & tell.
- 🐛 **Bugs / features:** [open an issue](https://github.com/explyt/spring-plugin/issues/new/choose).

## 10. License agreement
By contributing to this repository, you confirm that you have the right to submit your contribution and that it may be distributed under the [Apache License 2.0](https://github.com/explyt/spring-plugin/blob/main/LICENSE.md).

---

By following these guidelines, you help maintain the quality of Explyt Spring Plugin and make contributions easier to review and merge. Thank you for contributing!
