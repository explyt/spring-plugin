# Explyt Spring Plugin — Free Spring Boot plugin for IntelliJ IDEA Community (Java, Kotlin, Quarkus)

Bring powerful Spring and Spring Boot tooling to IntelliJ IDEA Community Edition. Build, navigate, test, and debug faster with:
- Accurate bean discovery and real context understanding
- Built‑in HTTP client (OpenAPI/Swagger UI + .http/.rest runners)
- Spring Debugger
- Quarkus CDI/JAX‑RS/AOP support
- AI helpers for Spring tasks

<img width="800" alt="Screen" src="https://raw.githubusercontent.com/explyt/spring-plugin/refs/heads/main/images/screen1.jpg">

## Why developers choose Explyt
- **Faster feedback**: precise inspections, completion and navigation eliminate wiring guesswork.
- **Seamless API testing**: run HTTP requests via **Swagger UI in the IDE** or execute **.http/.rest** files.
- **True Spring context awareness**: lightweight native context run (javaagent) extracts real bean/config metadata, including conditional and factory beans.

## Highlights
**Accurate Bean Detection & Context Analysis**
  - Detects beans via annotations, factory methods and conditional logic (@Conditional, profiles, complex scans).
  - Native context mode: runs a lightweight app with a javaagent to collect real bean metadata and eliminate false positives.
  - [Video: How Native Context Mode works in Explyt Spring panel](https://github.com/user-attachments/assets/db380852-d239-4e0f-a1b2-15a3a2659f53)

**HTTP Client: OpenAPI/Swagger UI + .http/.rest**
  - Generate OpenAPI from Spring Web–annotated methods; open Swagger UI directly in the IDE.
  - Execute requests, inspect responses, copy cURL, and generate methods from URL/cURL (Java & Kotlin).
  - Execute **.http/.rest** files with **JetBrains HttpClient CLI** or **httpyac**.
  - [Video: How Explyt Endpoints Tool Window works](https://github.com/user-attachments/assets/e38625a3-bc68-4075-9efa-945e64977f36)

<img width="800" alt="OpenAPI" src="https://github.com/user-attachments/assets/a2c0d94c-9361-474d-a29b-07c7cb5e5e0c">

**Spring Boot & Web Enhancements**
  - **Endpoints Tool Window**: view and navigate controllers, routes and router functions (MVC/WebFlux).
  - Duplicate endpoint detection, OpenAPI validation, and better MockMvc/WebClient/WebTestClient support.

<img width="800" alt="Endpoints" src="https://github.com/user-attachments/assets/48b80d76-258e-4841-a4dc-fd59a9188d11" />

**Spring Initializr**
  - Create Spring Boot projects (Java/Kotlin) with improved templates from [start.spring.io](https://start.spring.io/).
  - Integrated user experience: create the Spring projects within IDE. 
  - Will be always actual.  
  - [Video: Create New Project with Spring Explyt Initializr](https://github.com/user-attachments/assets/a58ee561-356f-43cd-8db9-1fd5f3f62653)

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

<img width="800" alt="JPQL" src="https://github.com/user-attachments/assets/c8d2058a-fe3e-458d-a06f-14e463528f8c">

**Spring AOP**
  - Detect risky internal calls to `@Transactional`/`@Async` methods; 
  - line markers and navigation for aspects/advice.  

<img width="800" alt="AOP" src="https://github.com/user-attachments/assets/4398e82f-262e-4802-9ebe-e92170dc939b">


**Quarkus support**
  - CDI/DI navigation and inspections, JAX-RS endpoints, interceptors/decorators, Endpoints tool window.
  - Works by static analysis, many features available without running the app.

**AI integration**
  - AI actions: 
    - DB schema ↔ JPA entity, 
    - Entity ↔ DTO, 
    - controller ↔ OpenAPI round‑trips, 
    - Spring config generators, 
    - HTTP conversions.
  - Works with OpenAI‑compatible providers or local models; requires IntelliJ Platform 2025.1+ for AI features.
  - Requires [Explyt plugin](https://explyt.ai/download?utm_campaign=explytai&utm_source=marketplace&utm_medium=springexplyt) to enable AI features

## Getting started
- Open the **Explyt Endpoints** tool window (right sidebar) to explore and navigate all HTTP endpoints.
- Use **Link Explyt Spring Boot Project From RunConfiguration** (native context) to extract runtime bean metadata for accurate inspections/navigation. 

<img width="639" alt="image" src="https://github.com/user-attachments/assets/206a86b7-7e85-4756-8d24-a12add4fac96">


- Run HTTP requests from Spring‑annotated methods via **Swagger UI** preview or from **.http/.rest** files using your configured runner.
- Use **Generate (Alt+Insert / Command+N)** to create Spring Web methods from URL or cURL.

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
spring boot intellij community plugin, idea community spring plugin, spring plugin for intellij, swagger ui http client, openapi client intellij, http client .http files, jetbrains http client cli, httpyac runner, spring debugger community, spring endpoints explorer, spring data repository inspection, configuration properties validation, bean injection navigation, autowiring inspection, spring initializr community, kotlin spring plugin, quarkus intellij community, java backend development, spring framework tooling

### 

_Thank you for using Explyt Spring Plugin!_ — [please star](https://github.com/explyt/spring-plugin), contribute and share to help more developers discover it.


