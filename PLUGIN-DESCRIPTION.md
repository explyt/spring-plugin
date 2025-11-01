# Explyt Spring Plugin — Free Spring Boot plugin for IntelliJ IDEA Community (Java, Kotlin, Quarkus)

Bring powerful Spring and Spring Boot tooling to IntelliJ IDEA Community Edition. Build, navigate, test, and debug faster with:
- Accurate bean discovery and real context understanding
- Built‑in HTTP client (Swagger UI + .http/.rest runners)
- Spring Debugger
- Quarkus CDI/JAX‑RS/AOP support
- Optional AI helpers for Spring tasks

![Screen](https://raw.githubusercontent.com/explyt/spring-plugin/refs/heads/main/images/screen1.jpg)

## Why developers choose Explyt
- **Faster feedback**: precise inspections, completion and navigation eliminate wiring guesswork.
- **Seamless API testing**: run HTTP requests via **Swagger UI in the IDE** or execute **.http/.rest** files.
- **True Spring context awareness**: lightweight native context run (javaagent) extracts real bean/config metadata, including conditional and factory beans.

## Highlights
**Accurate Bean Detection & Context Analysis**
  - Detects beans via annotations, factory methods and conditional logic (@Conditional, profiles, complex scans).
  - Native context mode: runs a lightweight app with a javaagent to collect real bean metadata and eliminate false positives.

**HTTP Client: Swagger UI + .http/.rest**
  - Generate OpenAPI from Spring Web–annotated methods; open Swagger UI directly in the IDE.
  - Execute requests, inspect responses, copy cURL, and generate methods from URL/cURL (Java & Kotlin).
  - Execute **.http/.rest** files with **JetBrains HttpClient CLI** or **httpyac**.

**Spring Boot & Web Enhancements**
  - **Endpoints Tool Window**: view and navigate controllers, routes and router functions (MVC/WebFlux).
  - Duplicate endpoint detection, OpenAPI validation, and better MockMvc/WebClient/WebTestClient support.

**Spring Initializr (Community)**
  - Create Spring Boot projects (Java/Kotlin) with improved templates, compatibility checks, and better error reporting.

**Advanced Inspections & Quick Fixes**
  - ConfigurationProperties validation; property key suggestions; duplicate key detection; relaxed binding completions.
  - Autowiring checks (missing/multiple beans), proxyBeanMethods warnings, AOP-related inspections.
  - Resource reference verification and meta-annotation checks.

**Spring Debugger**
  - Dedicated run configuration with lightweight javaagent; shows Spring Context and Active Transaction in debugger tree.
  - Evaluate Spring context and call bean methods at breakpoints (**Explyt.context** helpers).
  - Inline run markers on Spring Data repositories to auto-populate Evaluate Expression during debug.

**Spring Data**
  - Repository method name validation; parameter/return type checks.
  - JPQL/SQL language injection and assistance; repository beans recognized for navigation/autowiring.

**Spring AOP**
  - Detect risky internal calls to `@Transactional`/`@Async` methods; line markers and navigation for aspects/advice.

**Quarkus support**
  - CDI/DI navigation and inspections, JAX-RS endpoints, interceptors/decorators, Endpoints tool window.
  - Works by static analysis, many features available without running the app.

**Optional Spring AI integration**
  - AI actions: DB schema ↔ JPA entity, Entity ↔ DTO, controller ↔ OpenAPI round‑trips, Spring config generators, HTTP conversions.
  - Works with OpenAI‑compatible providers or local models; requires IntelliJ Platform 2025.1+ for AI features.
  - Requires [Explyt plugin]() to enable AI features

## Getting started
- Open the **Endpoints** tool window (right sidebar) to explore and navigate all HTTP endpoints.
- Use **Load Beans** (native context) to extract runtime bean metadata for accurate inspections/navigation.
- Run HTTP requests from Spring‑annotated methods via **Swagger UI** or from **.http/.rest** files using your configured runner.
- Use **Generate (Alt+Ins)** to create Spring Web methods from URL or cURL.

## Installation
- **.http/.rest runners**: install **JetBrains HttpClient CLI** from plugin settings, or use **httpyac**.

## Compatibility
- IntelliJ IDEA **Community 2023.3+** (latest recommended).
- IntelliJ Platform **2025.1+** for AI features.
- Languages/runtimes: Java, Kotlin; experimental Scala.

## Links
- **Documentation**: https://explyt.ai/docs/category/explyt-spring
- **Source**: https://github.com/explyt/spring-plugin
- **Changelog**: https://github.com/explyt/spring-plugin/blob/main/CHANGELOG.md

## Support & community
- **Issues/feature requests**: https://github.com/explyt/spring-plugin/issues
- **Telegram**: https://t.me/explytspring

## License
- Free for personal and commercial use. [EULA and source license](https://github.com/explyt/spring-plugin/blob/main/LICENSE.md)

---

### Search keywords
spring boot intellij community plugin, idea community spring plugin, spring plugin for intellij, swagger ui http client, openapi client intellij, http client .http files, jetbrains http client cli, httpyac runner, spring debugger community, spring endpoints explorer, spring data repository inspection, configuration properties validation, bean injection navigation, autowiring inspection, spring initializr community, kotlin spring plugin, quarkus intellij community, java backend development, spring framework tooling

---

Thank you for using Explyt Spring Plugin — please star, contribute and share to help more developers discover it.
