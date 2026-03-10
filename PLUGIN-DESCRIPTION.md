# Explyt Spring Plugin — Free Spring Boot plugin for IntelliJ IDEA Community (Java, Kotlin, Quarkus)

Bring powerful Spring and Spring Boot tooling to IntelliJ IDEA Community Edition. Build, navigate, test, and debug faster with:
- Accurate bean discovery and real context understanding (native context mode via javaagent)
- Tool windows for **Beans/Contexts** and **Endpoints**
- Built‑in HTTP client (OpenAPI/Swagger UI + .http/.rest runners)
- Spring Debugger (including runtime Spring property lenses)
- Spring Data, JPA, AOP, Docker/K8s, and lots of inspections & quick fixes
- Quarkus CDI/JAX‑RS/AOP support
- Optional AI helpers for Spring tasks

<img width="800" alt="Screen" src="https://raw.githubusercontent.com/explyt/spring-plugin/refs/heads/main/images/screen1.jpg">

## Why developers choose Explyt
- **Faster feedback**: precise inspections, completion and navigation eliminate wiring guesswork.
- **True Spring context awareness**: lightweight native context run extracts real bean/config metadata, including conditional and factory beans.
- **Seamless API testing**: run HTTP requests via **Swagger UI in the IDE** or execute **.http/.rest** files.

## Highlights

### Tool Windows
- **Explyt Spring**: load real bean metadata, browse contexts, navigate beans, view aspects, and open **Bean Analyzer**.
  - [Video: How Native Context Mode works in Explyt Spring panel](https://github.com/user-attachments/assets/db380852-d239-4e0f-a1b2-15a3a2659f53)
- **Explyt Endpoints**: view and navigate endpoints for MVC/WebFlux, router functions, and **message brokers** (Kafka/RabbitMQ listeners).
  - [Video: How Explyt Endpoints Tool Window works](https://github.com/user-attachments/assets/e38625a3-bc68-4075-9efa-945e64977f36)

<img width="800" alt="Endpoints" src="https://github.com/user-attachments/assets/48b80d76-258e-4841-a4dc-fd59a9188d11" />

### Accurate Bean Detection & Context Analysis
- Detects beans via annotations, factory methods and conditional logic (`@Conditional`, profiles, complex scans).
- Native context mode eliminates many false positives by using real Spring logic.
- Supports projects with **multiple application contexts**.
- Autowiring/inspections also consider **library beans** (from dependencies), not only project sources.

### Inlay Hints & Code Vision
- `@Profile` inlay hints show the computed result of profile expressions (e.g. `"prod & !cloud"` → `true/false`).
- `@Scheduled` cron completion + hints (including `zone` attribute).
- During Explyt debug sessions: **runtime property value Code Vision** in `.properties` / `.yml` (helps locate the `PropertySource`).

### HTTP Client: OpenAPI/Swagger UI + .http/.rest
- Generate OpenAPI from Spring Web–annotated methods; open Swagger UI directly in the IDE.
- Execute requests, inspect responses, copy cURL.
- Execute **.http/.rest** files with **JetBrains HttpClient CLI** or **httpyac**.

<img width="800" alt="OpenAPI" src="https://github.com/user-attachments/assets/a2c0d94c-9361-474d-a29b-07c7cb5e5e0c">

### Code Generation
Use **Generate (Alt+Insert / Command+N)**:
- Generate **Spring Web Method** from URL/cURL (Java & Kotlin).
- Generate **HttpClient Method** from URL/cURL (standalone `java.net.http.HttpClient` code, Java & Kotlin).
- Generate **JPA equals/hashCode** for Java entities using a proxy-safe approach.
- Properties ↔ YAML conversion.

### Advanced Navigation & Analysis
- **Beans in Search Everywhere**: dedicated *Beans* tab in Search Everywhere (`Shift` `Shift`).
- **Bean Analyzer**: visualize relationships between loaded beans and navigate to declarations.
- **Mark directory as Spring Application Property Folder**: enable completion/validation for non-standard config locations.
- **SPI beans**: navigation and gutter markers for beans produced via Java SPI (`ServiceLoader`).

### Spring Boot & Web Enhancements
- Duplicate endpoint detection, OpenAPI validation, and better MockMvc/WebClient/WebTestClient support.
- Docker Compose + Kubernetes: completion for Spring properties as environment variables (`server.port` → `SERVER_PORT`).

### Spring Debugger
- Dedicated run configuration with a lightweight javaagent; shows Spring Context and Active Transaction in debugger tree.
- Evaluate Spring context and call bean methods at breakpoints (**Explyt.context** helpers).
- Inline run markers on Spring Data repositories to auto-populate Evaluate Expression during debug.

### Spring Initializr
- Create Spring Boot projects (Java/Kotlin) with improved templates from [start.spring.io](https://start.spring.io/).
- Integrated UX: create Spring projects inside the IDE.
- Always up to date.

### Quarkus support
- CDI/DI navigation and inspections, JAX‑RS endpoints, interceptors/decorators, Endpoints tool window.
- Works by static analysis, many features available without running the app.

### AI integration
- AI actions:
  - DB schema ↔ JPA entity,
  - Entity ↔ DTO,
  - controller ↔ OpenAPI round‑trips,
  - Spring config generators,
  - HTTP conversions.
- Works with OpenAI‑compatible providers or local models; requires IntelliJ Platform 2025.1+ for AI features.
- Requires [Explyt plugin](https://explyt.ai/download?utm_campaign=explytai&utm_source=marketplace&utm_medium=springexplyt) to enable AI features.

## Getting started
1. Open the **Explyt Spring** tool window → **Link Explyt Spring Boot Project From RunConfiguration** → **Load Beans**.
2. Open the **Explyt Endpoints** tool window to explore web + message broker endpoints.
3. Use **Search Everywhere** (`Shift` `Shift`) → **Beans** tab to jump to any bean.
4. Use **Bean Analyzer** to explore the bean dependency graph.
5. Use **Generate** to create Spring Web methods or HttpClient code from URL/cURL.

## Installation
- **.http/.rest runners**: install **JetBrains HttpClient CLI** from plugin settings, or use **httpyac**.

## Links
- **Documentation**: [https://explyt.ai/docs/category/explyt-spring](https://explyt.ai/docs/category/explyt-spring?utm_campaign=springexplyt&utm_source=marketplace&utm_medium=springexplyt)
- **Source**: https://github.com/explyt/spring-plugin
- **Full Changelog**: https://github.com/explyt/spring-plugin/blob/main/CHANGELOG.md

## Support & community
- **Issues/feature requests**: https://github.com/explyt/spring-plugin/issues
- **Telegram**: https://t.me/explytspring

## License
- Free for personal and commercial use. [EULA and source license](https://github.com/explyt/spring-plugin/blob/main/LICENSE.md)

---

### Search keywords
spring boot intellij community plugin, idea community spring plugin, spring plugin for intellij, explyt spring, swagger ui http client, openapi client intellij, http client .http files, jetbrains http client cli, httpyac runner, spring debugger community, spring endpoints explorer, kafka listener navigation, rabbit listener navigation, bean dependency analyzer, search everywhere beans, spring profile inlay hints, runtime property value code vision, configuration properties validation, spring data repository inspection, jpa equals hashcode generator, kotlin spring plugin, quarkus intellij community

_Thank you for using Explyt Spring Plugin!_ — [please star](https://github.com/explyt/spring-plugin), contribute and share to help more developers discover it.
