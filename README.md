# Explyt Spring Plugin for IntelliJ IDEA Community Edition

[![Marketplace Version](https://img.shields.io/jetbrains/plugin/v/28675?label=Marketplace)](https://plugins.jetbrains.com/plugin/28675-spring-explyt)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/28675?label=Downloads)](https://plugins.jetbrains.com/plugin/28675-spring-explyt/versions)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/28675?label=Rating)](https://plugins.jetbrains.com/plugin/28675-spring-explyt/reviews)
[![GitHub stars](https://img.shields.io/github/stars/explyt/spring-plugin?label=Stars)](https://github.com/explyt/spring-plugin/stargazers)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](./LICENSE.md)
![Build](https://github.com/explyt/spring-plugin/actions/workflows/build.yaml/badge.svg)
[![Telegram EN](https://img.shields.io/badge/Telegram-EN-26A5E4?logo=telegram)](https://t.me/explytspring_en)
[![Telegram RU](https://img.shields.io/badge/Telegram-RU-26A5E4?logo=telegram)](https://t.me/explytspring)

> ### Runtime-accurate Spring tooling for IntelliJ IDEA Community — free.

Explyt Spring brings real Spring understanding to **IntelliJ IDEA Community Edition**: bean navigation and inspections backed by your app's **actual** context, a Spring debugger, an in-IDE Swagger/HTTP client, Quarkus support, Kotlin-first checks, and Spring-aware **MCP tools** for AI agents. **Free for commercial and non-commercial use.**

```text
Settings → Plugins → Marketplace → search "Spring Explyt" → Install
```

**Love it?** ⭐ [**Star us on GitHub**](https://github.com/explyt/spring-plugin) · 🗳️ [**Rate on the Marketplace**](https://plugins.jetbrains.com/plugin/28675-spring-explyt/reviews) — it takes 10 seconds and genuinely helps other developers find the plugin.

<!-- ASSET-TODO [P1] Hero GIF — Native Context Mode (the differentiator)
     Capture: click "Load Beans" -> bean tree fills -> click a @Conditional/profile bean -> jump to declaration
     Format: looping GIF ~15-25s, <= ~8 MB | Theme: Darcula | Commit to images/hero-native-context.gif
     Then REPLACE the <img> below with it. Alt: "Explyt Spring loading real Spring beans via Native Context Mode" -->
<img width="820" alt="Explyt Endpoints tool window" src="https://github.com/user-attachments/assets/48b80d76-258e-4841-a4dc-fd59a9188d11" />

▶ **See it in action:** [Native Context Mode (30s)](https://github.com/user-attachments/assets/db380852-d239-4e0f-a1b2-15a3a2659f53) · [Endpoints tool window](https://github.com/user-attachments/assets/e38625a3-bc68-4075-9efa-945e64977f36)

---

## Why Explyt?

A free way to get Spring-focused tooling in IDEA Community. Explyt is **not** a full IDEA Ultimate replacement — it is strongest when runtime bean accuracy, Kotlin Spring support, HTTP testing, and AI-agent/MCP workflows matter.

Legend: ✅ first-class · ◐ partial / setup-dependent · ❌ not available

| Capability | **Explyt (CE)** | IDEA Ultimate | VS Code + STS4 | IDEA CE alone |
|---|:---:|:---:|:---:|:---:|
| **Native context mode** — real bean metadata via javaagent (conditional/factory/library beans) | ✅ | ◐ static model; runtime via Actuator | ◐ needs running app + Actuator | ❌ |
| **Spring Debugger** — context tree, transactions, runtime property resolution | ✅ | ◐ | ◐ | ❌ |
| **Swagger UI HTTP client + `.http`/`.rest` runners** in CE | ✅ | ◐ | ◐ via extensions | ❌ |
| **Spring-aware MCP tools** for AI agents (7 tools, 2025.2+) | ✅ | ❌ | ❌ | ❌ |
| **Kotlin-first Spring inspections** — `internal`/`object` beans, `coRouter` | ✅ | ◐ | ◐ | ❌ |

<!-- ASSET-TODO [P1] Static analysis vs Native mode — side-by-side proof
     Capture: a conditional/factory bean shown as a false "no bean" warning under static analysis -> after Load Beans the warning clears and autowiring resolves
     Format: split PNG or short MP4 | Theme: Darcula | Commit to images/static-vs-native.png
     Place it right under this table. Alt: "Static-analysis false warning vs resolved bean after Native Context Mode" -->

---

## Table of Contents

- [Features](#features)
- [Tool Windows](#tool-windows)
- [Installation](#installation)
- [Usage](#usage)
- [Learn More](#learn-more)
- [Contributing](#contributing)
- [Community and Support](#community-and-support)
- [License](#license)

---

## Features

> Highlights below. For the complete feature list — Spring Data, AOP, Docker/K8s, inlay hints, code generation, SPI beans, Kotlin inspections, and more — see the [**Features wiki**](https://github.com/explyt/spring-plugin/wiki/Features).

### 🧠 Native context mode — the differentiator

Instead of analyzing source only, Explyt runs a **lightweight version of your app** and uses a javaagent + declarative bytecode patching to extract **real** bean metadata at Spring startup. That gives accurate detection of `@Conditional`, profile, factory, library, and multi-context beans.

→ [How Native Context Mode works](https://github.com/explyt/spring-plugin/wiki/Native-Context-Mode)

### 🐞 Spring Debugger

A dedicated debug mode shows the **Spring Context** and **Active Transaction** in the debugger tree, resolves **runtime property values** as Code Vision in `.properties` / `.yml`, and lets you evaluate bean methods at breakpoints.

### 🌐 Built-in HTTP client: Swagger UI + `.http` / `.rest`

Generate OpenAPI from Spring Web methods and open **Swagger UI inside the IDE**; execute, inspect, and copy cURL. Or run `.http` / `.rest` files via **JetBrains HttpClient CLI** or **httpyac**.

→ [HTTP client guide](https://github.com/explyt/spring-plugin/wiki/HTTP-Client)

### 🧭 Endpoints & beans navigation

The **Explyt Endpoints** tool window lists MVC/WebFlux endpoints, router functions, and message broker listeners (Kafka/RabbitMQ). Jump to any bean via the **Beans** tab in Search Everywhere (`Shift Shift`) and explore dependencies with **Bean Analyzer**.

### 🤖 Kotlin-first & AI-agent ready

Kotlin-specific Spring inspections catch an `internal` `@Bean` whose JVM name is mangled and an `object` declared as `@Component`, with quick-fixes. Kotlin `coRouter` endpoints are supported. For agentic clients, Explyt ships **7 Spring-aware MCP tools** on the bundled JetBrains MCP Server (IDEA 2025.2+).

→ [MCP tools](https://github.com/explyt/spring-plugin/wiki/MCP-Tools)

**Also included:** Quarkus CDI/JAX-RS/AOP, Spring Initializr, Docker Compose/K8s property completion, `@Profile` / `@Scheduled` inlay hints, code generation, and many inspections & quick fixes.

---

## Tool Windows

- **Explyt Spring** — load real bean metadata, browse contexts, view aspects, open **Bean Analyzer**. Entry point for native context mode: **Link … From RunConfiguration** → **Load Beans**.
- **Explyt Endpoints** — Web + message-broker endpoints in one navigable view.

▶ [Native Context Mode](https://github.com/user-attachments/assets/db380852-d239-4e0f-a1b2-15a3a2659f53) · [Endpoints](https://github.com/user-attachments/assets/e38625a3-bc68-4075-9efa-945e64977f36)

---

## Installation

**Recommended:** IntelliJ IDEA → **Settings → Plugins → Marketplace**, search **"Spring Explyt"**, click **Install**. Or install from the web: <https://plugins.jetbrains.com/plugin/28675-spring-explyt>.

Other options: [custom repository](https://github.com/explyt/spring-plugin/wiki/Installation-Guide) · [manual ZIP from Releases](https://github.com/explyt/spring-plugin/releases).

**First-time setup:** open **Explyt Spring** → **Link Explyt Spring Boot Project From RunConfiguration** → **Load Beans**. Full guide: [Installation & Setup wiki](https://github.com/explyt/spring-plugin/wiki/Installation-Guide).

> Using IntelliJ IDEA Ultimate? Disable its built-in Spring plugin if you see feature conflicts.

Useful links: [Changelog](./CHANGELOG.md) · [Marketplace description source](./PLUGIN-DESCRIPTION.md)

---

## Usage

1. **Load beans:** open **Explyt Spring**, link your run configuration, click **Load Beans**.
2. **Explore endpoints:** open **Explyt Endpoints**; navigate Web + broker handlers.
3. **Test APIs:** add Spring Web annotations and click the **Run** gutter icon → Swagger UI; or run `.http` / `.rest` files.
4. **Fix issues:** `Alt+Enter` on highlights for Spring-aware quick fixes.
5. **Generate:** `Alt+Ins` → Spring Web Method / HttpClient Method / JPA `equals`/`hashCode` / Properties↔YAML.

Step-by-step walkthroughs: [**Usage wiki**](https://github.com/explyt/spring-plugin/wiki/Usage).

---

## Learn More

**In English**

- [Patching Spring bytecode to enhance application-context recognition](https://medium.com/@explytspring/explyt-spring-plugin-patching-spring-bytecode-to-enhance-application-context-recognition-0817fb52b056) — deep dive into the javaagent + declarative bytecode patching.
- [Stop playing catch-up with Spring — Explyt Spring for IDEA Community](https://medium.com/@explytspring/stop-playing-catch-up-with-spring-introducing-the-explyt-spring-plugin-for-idea-community-0be380b36a75) — the approach to accurate context.
- [Wiki home](https://github.com/explyt/spring-plugin/wiki) — detailed docs that back this concise README.

<details>
<summary><strong>In Russian (Habr)</strong></summary>

- [Explyt Spring Release: SQL, Docker-Compose, Debugger](https://habr.com/ru/companies/explyt/articles/962536/)
- [Neural Networks in Spring Development](https://habr.com/ru/companies/explyt/articles/944266/)
- [Explyt AI Platform and integrated agents](https://habr.com/ru/companies/explyt/articles/936992/)
- [Tools for the MCP Server plugin](https://habr.com/ru/articles/986226/)
- [Quarkus support](https://habr.com/ru/companies/explyt/articles/926484/)
- [Explyt Spring Debugger](https://habr.com/ru/companies/explyt/articles/933158/)
- [`*.http` files support in IDEA Community](https://habr.com/ru/companies/explyt/articles/884280/)
- [Our take on the HTTP client for IntelliJ IDEA](https://habr.com/ru/companies/explyt/articles/874236/)

</details>

---

## Contributing

Contributions are welcome! Fork the repo, create a feature branch, and open a pull request against `main`. See the [Contributing Guide](https://github.com/explyt/spring-plugin/blob/main/CONTRIBUTING.md) for setup and conventions.

---

## Community and Support

- 🐛 **Issues / feature requests:** [GitHub Issues](https://github.com/explyt/spring-plugin/issues)
- 💬 **Telegram:** [English](https://t.me/explytspring_en) · RU [channel](https://t.me/explytspring) / [chat](https://t.me/explytspringchat)
- 📚 **Docs:** [explyt.ai/docs](https://explyt.ai/docs/category/explyt-spring?utm_campaign=explytai&utm_source=github&utm_medium=springexplyt) · [Wiki](https://github.com/explyt/spring-plugin/wiki)

If Explyt saves you time, please ⭐ [**star the repo**](https://github.com/explyt/spring-plugin) and 🗳️ [**leave a Marketplace rating**](https://plugins.jetbrains.com/plugin/28675-spring-explyt/reviews).

---

## License

Licensed under the [Apache License 2.0](https://github.com/explyt/spring-plugin/blob/main/LICENSE.md). Free to use, modify, and distribute under the terms of that license.

---

*Made with ❤️ by the Explyt Team.*
