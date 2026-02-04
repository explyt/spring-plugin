# Explyt Spring Plugin for IntelliJ IDEA Community Edition
![Workflow status](https://github.com/explyt/spring-plugin/actions/workflows/build.yaml/badge.svg?branch=)
![GitHub Release](https://img.shields.io/github/v/release/explyt/spring-plugin)
[![Marketplace Version](https://img.shields.io/jetbrains/plugin/v/28675?label=Marketplace%20version)](https://plugins.jetbrains.com/plugin/28675-spring-explyt)
[![Marketplace Downloads](https://img.shields.io/jetbrains/plugin/d/28675?label=Downloads)](https://plugins.jetbrains.com/plugin/28675-spring-explyt/versions)
[![telegram_badge](https://img.shields.io/badge/Telegram-Explyt%20Spring-252850?style=plastic&logo=telegram)](https://t.me/explytspring)

Available on JetBrains Marketplace: https://plugins.jetbrains.com/plugin/28675-spring-explyt

**Explyt Spring Plugin** supercharges your IntelliJ IDEA Community Edition with advanced Spring Framework features—usually only available in the Ultimate Edition. Develop Spring applications faster and smarter with enhanced tools for beans, configurations, endpoints, and more.

---

**Plugin Status**: The plugin is completely **FREE** for both non-commercial and commercial use.

---

![Screen](https://raw.githubusercontent.com/explyt/spring-plugin/refs/heads/main/images/screen1.jpg)

👉 Visit the [official Explyt website](https://explyt.ai/docs/category/explyt-spring).

## Table of Contents

- [Features](#features)
    - [Key Features](#key-features)
    - [Our Innovative Approach](#our-innovative-approach)
    - [Built-in HTTP Client Using Swagger UI](#built-in-http-client-using-swagger-ui)
    - [Two Ways to Test HTTP Requests](#two-ways-to-test-http-requests)
    - [Quarkus Support](#quarkus-support)
    - [Spring Core and Boot Enhancements](#spring-core-and-boot-enhancements)
    - [Spring Web Enhancements](#spring-web-enhancements)
    - [Spring Debugger](#spring-debugger)
    - [Spring Data Support](#spring-data-support)
    - [Spring AOP Enhancements](#spring-aop-enhancements)
    - [Spring Initializr](#spring-initializr)
    - [Docker Compose and Kubernetes](#docker-compose-and-kubernetes)
    - [Spring AI](#spring-ai)
    - [Additional Inspections and Features](#additional-inspections-and-features)
- [Tool Windows](#tool-windows)
- [Inlay Hints & Code Vision](#inlay-hints--code-vision)
- [Code Generation](#code-generation)
- [Advanced Features](#advanced-features)
- [Performance](#performance)
- [Installation](#installation)
    - [System Requirements](#system-requirements)
    - [Option 1: JetBrains Marketplace (recommended)](#option-1-jetbrains-marketplace-recommended)
    - [Option 2: Install from Custom Repository](#option-2-install-from-custom-repository)
    - [Option 3: Manual Installation](#option-3-manual-installation)
    - [First-time setup](#first-time-setup)
    - [Troubleshooting](#troubleshooting)
- [Usage](#usage)
- [Learn More](#learn-more)
- [Integrations](#integrations)
- [Contributing](#contributing)
- [Community and Support](#community-and-support)
- [License](#license)
- [Acknowledgments](#acknowledgments)

## Features
### Key Features
- **Accurate Bean Detection**: Understand your Spring application with real bean data, even for complex configurations.
- **Built-in HTTP Client**: Test APIs using Swagger UI directly in your IDE.
- **Advanced Inspections**: Detect and fix issues in Spring configurations, beans, and annotations.
- **Spring Boot & Web Support**: Enhanced tools for Spring Boot, MVC, WebFlux, and more.
- **Kotlin-Friendly**: Full support for Kotlin Spring applications.
- **Lightweight & Fast**: Runs a lightweight version of your app for accurate insights without slowing down your IDE.
- **Quarkus Support**: CDI/DI navigation and inspections, JAX-RS web endpoints, AOP (interceptors/decorators), Endpoints tool window, and a Swagger UI–based HTTP client.

### Our Innovative Approach

**Explyt Spring Plugin** uses a unique method to understand your Spring application thoroughly. Instead of analyzing only the source code, our plugin runs a lightweight version of your application to get accurate information about your Spring beans. This means:

Note: Behind the scenes, we use a javaagent and declarative bytecode patching to hook into Spring’s startup and extract bean metadata early, without fully starting the app. Read more:
- [Stop playing catch-up with Spring — Explyt Spring plugin for IDEA Community (EN)](https://medium.com/@explytspring/stop-playing-catch-up-with-spring-introducing-the-explyt-spring-plugin-for-idea-community-0be380b36a75)
- [Patching Spring bytecode to enhance application context recognition (EN)](https://medium.com/@explytspring/explyt-spring-plugin-patching-spring-bytecode-to-enhance-application-context-recognition-0817fb52b056)

- **Accurate Bean Detection**: We can detect beans that are conditionally loaded or defined through complex configurations.
- **Better Inspections**: By having real bean data, we reduce false warnings and provide more precise code inspections.
- **Improved Navigation and Completion**: Navigate to beans and get code completion suggestions based on actual bean definitions.

This approach ensures that even if your application uses advanced Spring features like `@Conditional`, complex `@ComponentScan` configurations, or custom conditions, the plugin understands them correctly.

### Built-in HTTP Client Using Swagger UI

We are excited to introduce our **built-in HTTP client** for IntelliJ IDEA Community Edition,
integrated directly into the Explyt Spring Plugin.
This feature allows you to test and interact with HTTP APIs seamlessly within your IDE.

#### Key Advantages

- **No Custom DSL Needed**: Use familiar **Spring Web annotations** to define HTTP requests instead of learning a new domain-specific language (DSL).
- **Leverage Swagger UI**: We utilize **Swagger UI** to provide a rich interface for executing and analyzing HTTP requests.
- **Quick Setup**: Write methods with Spring annotations, and the plugin generates OpenAPI specifications automatically.
- **Familiar Tools**: Benefit from well-known technologies like Spring Web, OpenAPI, and Swagger UI.

#### How It Works

1. **Define HTTP Methods Using Spring Annotations**

   Write methods in Java or Kotlin using standard Spring Web annotations to describe the HTTP requests you want to make.

   ```java
   @GetMapping("https://api.openweathermap.org/data/2.5/weather")
   public String getWeather() {
       // Implementation is not important
   }
   ```

2. **Generate OpenAPI Specification**

   The plugin analyzes your annotated methods and generates an OpenAPI file representing the API definitions.

3. **Launch Swagger UI in IntelliJ IDEA**

   With a simple click on the **Run** icon next to your method, the plugin opens Swagger UI inside the IDE using the generated OpenAPI file.

4. **Execute HTTP Requests**

   Use Swagger UI to fill in parameters, execute the requests, and view the responses—all within the IDE.

#### Benefits

- **Simplifies API Testing**: Quickly test external services or APIs without leaving your development environment.
- **No Extra UI Development**: By using Swagger UI, we avoid the need to create and maintain a custom user interface.
- **Familiar Workflow**: Developers comfortable with Spring and Swagger will find this approach intuitive.

#### Additional Features

- **Code Generators**
    - **Spring Web Method** from URL/cURL: Use the **Generate** menu (`Alt`+`Insert` / `⌘`+`N`) to create a Spring Web annotated method.
    - **HttpClient Method** from URL/cURL: Generates a `java.net.http.HttpClient` example (Java 11+), useful outside of Spring.
    - **Supports Java and Kotlin**.

- **Supports OpenAPI Editing**
    - **Edit OpenAPI Files**: If needed, you can directly edit the generated OpenAPI files. The plugin provides code completion and validation support for OpenAPI specifications.

- **Handles Common Issues**
    - **CORS Handling**: We implemented a custom request handler to bypass CORS issues when making web requests from the embedded browser.

#### Use Cases

- **Testing External APIs**: Ideal for experimenting with third-party services like weather APIs, payment gateways, etc.
- **Quick Prototyping**: Easily create and test API calls during development without switching tools.
- **Learning and Documentation**: Helps new team members understand API interactions by providing a visual interface.

### Two Ways to Test HTTP Requests
1. **Swagger UI Integration** (recommended for Spring annotation workflows).
2. **`.http`/`.rest` File Execution** (ideal for users familiar with IntelliJ HTTP Client or HttpYac).

Choose the method that best fits your workflow!

### Quarkus Support

Explyt Spring Plugin now brings first-class Quarkus support to IntelliJ IDEA Community Edition out of the box.

- Dependency Injection (CDI):
  - Detect and navigate injections via @Inject, qualifiers, scopes.
  - Supports producers (@Produces) on fields and methods with navigation both ways.
- AOP:
  - Interceptors and decorators with gutter markers and navigation to/from advised targets.
- Web (JAX-RS):
  - Endpoints tool window for REST resources, navigation to handlers, duplicate-path checks.
  - Swagger UI–based HTTP client for methods with absolute Path URLs; OpenAPI generation and execution inside the IDE.
- Works by static code analysis, so most features are available just by opening a Quarkus project—no app run required.

If you see issues or edge cases, please open an issue in GitHub.

### Spring Core and Boot Enhancements

- **Advanced Inspections and Quick Fixes**:
    - **Property Key Validations**: Detects unused properties, duplicate keys, and keys that don't follow the kebab-case convention.
    - **Configuration Properties Support**: Navigate, complete, and validate `@ConfigurationProperties` classes.
        - **Nullable Parameters Check**: Warns if constructor parameters in `@ConfigurationProperties` classes are not nullable.
        - **Invalid Prefix Detection**: Finds empty, duplicate, or incorrectly formatted prefixes in `@ConfigurationProperties`.
        - **Configuration Verification**: Ensures `@ConfigurationProperties` classes are correctly registered with Spring.
        - **Quick-Fix Improvements**: Smarter suggestions and corrections for common configuration property mistakes (see changelog for demo videos).
    - **Bean Autowiring Checks**:
        - Reports if no beans are found for autowiring.
        - Alerts if multiple candidate beans cause ambiguity.
        - Checks for invalid use of `@Autowired` annotations.
    - **Proxy Method Usage Inspection**: Warns about incorrect use of proxy methods when `proxyBeanMethods` is set to false.
    - **Resource Reference Validation**: Validates file and classpath resource references in annotations like `@Value`.
    - **AOP Method Call Inspection**: Alerts if AOP-advised methods are called within the same class, which may not work as expected.
    - **Meta-Annotation Checks**: Ensures custom annotations used as meta-annotations have the correct settings.

- **Code Completion and Navigation**:
    - **Property Files**: Enhanced suggestions for properties, including relaxed binding and nested properties.
    - **Annotation Attributes**: Completion and navigation for package names in annotations like `@ComponentScan`.
    - **Bean Methods**: Navigate from bean usage to their definitions, even across different configuration classes.
    - **Query Languages**: JPQL plus basic SQL DML out of the box (no external plugin). Works in `JdbcTemplate`, `@Query(nativeQuery = true)`, `@NamedNativeQuery`, and inline strings; `JdbcClient` parameter inspections and navigation.

- **Line Markers and Gutter Icons**:
    - Visual markers for beans, configurations, and `@Scheduled` methods.
    - Easily navigate between bean definitions and their usages.

- **Other Enhancements**:
    - XML configuration support when using native context mode (no extra setup required).
    - **@Profile inlay hints**: shows the computed result of Spring profile expressions inline.
    - **@Scheduled cron completion + hints** (including `zone` attribute support).
    - **Multi-context support**: works with Spring apps that create multiple `ApplicationContext`s.
    - **Library beans DI support**: autowiring/inspections/navigation also consider beans coming from dependencies.
    - **SPI bean navigation** and line markers for SPI-produced beans.
    - **Search Everywhere integration**: quickly find beans by name in a dedicated **Beans** tab (`Shift` `Shift`).
    - **Mark directory as Spring Application Property Folder** to enable completion/validation in non-standard config locations.
    - One-click converter between `.properties` and `.yml` Spring configuration files.

### Spring Web Enhancements

- **Endpoints Tool Window**: Access all your Spring MVC, WebFlux, and **message broker** endpoints in one place.
    View, navigate, and analyze your controllers, router functions, and messaging listeners (e.g. `@KafkaListener`, `@RabbitListener`).

- **Inspections and Quick Fixes**:
    - **Duplicate Endpoint Detection**: Warns about duplicate `@RequestMapping` paths.
    - **Controller Method Checks**: Finds missing `@PathVariable`s, unsupported method signatures, and more.
    - **OpenAPI Specification Support**:
        - Validates OpenAPI files (JSON and YAML).
        - Provides navigation and completion for `$ref` references.
        - Checks OpenAPI version compatibility.

- **Code Completion and Navigation**:
    - **WebClient and WebTestClient**: Enhanced suggestions for methods, including parameters and URI templates.
    - **Router Functions**: Support for `RouterFunctions` and Kotlin `coRouter`.
  - **MockMvc Integration**: Improved handling of multipart requests and parameter checks.

- **Line Markers and Gutter Icons**:
    - Visual indicators for controller methods and router functions.
    - Navigate between HTTP requests and their handling methods.
- **Execute HTTP Requests via `.http` or `.rest` Files**

#### Execute HTTP Requests via `.http` or `.rest` Files
Explyt Spring Plugin now supports **`.http`** and **`.rest`** files with seamless integration for both:
- **JetBrains HttpClient CLI** 
- **HttpYac** (open-source alternative)

**Key Features**:
- **Run Line Markers**: Click the **▶️ Run icon** next to request sections in `.http`/`.rest` files to execute them using your configured runner.
- **Minimal Setup**: No complex configuration needed—just choose your preferred runner.
- **Flexible Execution**: Works with both JetBrains’ HttpClient (Ultimate) and HttpYac.

**Example**:
```http
### Get a sample
GET https://httpbin.org/anything
Accept: application/json
```

Click the **▶️ Run icon** next to the request to execute it.

#### Configure Your Runner
1. **JetBrains HttpClient CLI** (Recommended for Community Edition):
    - Go to **Settings > Tools > Explyt Spring**.
    - Click **Download HttpClient CLI** – the plugin handles installation and path configuration automatically.
    - Done! The CLI is now ready to execute `.http`/`.rest` files.

2. **HttpYac** (Alternative):
    - Install globally via npm:
      ```bash
      npm install -g httpyac
      ```
    - Ensure `httpyac` is in your system PATH.
    - Setup in **Settings > Tools > Explyt Spring** runner executable path `httpyac`

To switch runners, go to **Settings > Tools > Explyt Spring** and select your preferred tool.

#### HTTP Client Options
- Swagger UI integration using Spring annotations (Run icon on endpoints) – details: [HTTP client via annotations and Swagger UI](https://habr.com/ru/companies/explyt/articles/874236/)
- Execute `.http`/`.rest` files via HttpYac or JetBrains HttpClient – details: [HTTP client via HttpYac and JetBrains HttpClient](https://habr.com/ru/companies/explyt/articles/884280/)

### Spring Debugger

- Remote Debugger: attach to a running JVM (started with JDWP) and the Explyt javaagent; full support for breakpoints, runtime PropertySource resolution, BeanDefinition view, transaction info, and auto Web URL detection. [Agent source](https://github.com/explyt/spring-plugin/blob/main/explyt-spring-boot-bean-reader/java-agent/src/main/java/com/explyt/spring/boot/bean/reader/InternalHolderContext.java) · [agent JAR](https://github.com/explyt/spring-plugin/blob/main/modules/spring-core/libs/explyt-java-agent-0.1.jar)
- Runtime property value Code Vision in `.properties` / `.yml` during Explyt debug sessions: shows the actual runtime value and helps locate the `PropertySource` that provided it.
- Debug Spring applications with the Explyt Spring Debugger run configuration.
- Automatically patches build/run to attach a lightweight javaagent and reverts Gradle configuration after debug.
- Dedicated nodes and improved presentation in the debugger tree for Spring beans, including Explyt: Spring Context and Active Transaction variables.
- Evaluate Spring context directly in the debugger via `Explyt.context` and helpers (`getBeanFactory()`, `getEnvironment()`); call any bean methods at breakpoints.
- Inline run markers on Spring Data repository methods during debug to auto‑populate Evaluate Expression and run queries.
- Inactive beans are visually indicated during debug to highlight context membership.
- Compatible with IntelliJ IDEA 2024.1 / 2024.2 (builds 241 / 242) via legacy debug mode ([demo video](https://storage.yandexcloud.net/explyt-web/videos/spring-wiki/debugger/spring-debug-old.mp4)).
- [Learn more about Explyt Spring Debugger](https://habr.com/ru/companies/explyt/articles/933158/)

### Spring Data Support

- **Repository Method Name Validation**: Ensures your repository methods follow naming conventions and match actual entity properties.
- **Return Type and Parameter Checks**: Validates that repository methods have correct return types and parameters that match the query.
- **Language Injection**: Offers JPQL and SQL syntax support within repository query methods.
- **Bean Recognition**: Detects Spring Data repositories as beans, helping with navigation and autowiring checks.
- **Repository Method Autocomplete**: Suggests method names based on entity properties and common Spring Data keywords, for example:
    - Prefixes: `findBy`, `readBy`, `getBy`, `queryBy`, `countBy`, `existsBy`, `deleteBy` / `removeBy`
    - Operators: `And`, `Or`, `Between`, `LessThan`, `GreaterThan`, `Like`, `Containing`, `In`, `IsNull`, `IsNotNull`, `True`, `False`, `IgnoreCase`
    - Sorting/limits: `OrderBy...Asc/Desc`, `Top` / `First`, `Distinct`
- **Generate equals/hashCode for JPA entities** (Code → Generate): follows the commonly recommended Hibernate approach:
    - `equals()` is `final`, checks type, and compares a non-null `@Id`
    - `hashCode()` is `final` and based on the entity class (stable for proxies)
- **Spring Data 4.0 AOT Navigation**: Jump from repository methods to AOT-generated artifacts and back, to better understand what Spring Data executes at runtime.

### Spring AOP Enhancements

- **AOP Method Call Inspection**: Warns when methods with annotations like `@Transactional` or `@Async` are called from within the same class.
- **Line Markers**: Visual markers for aspects, pointcuts, and advice methods, making navigation easier.

### Spring Initializr

- Create new Spring projects directly from the IDE with enhanced error reporting and compatibility fixes.
- Works in Community Edition; supports Kotlin and Java templates.

### Docker Compose and Kubernetes

- Completion for Spring properties **as environment variables** in `docker-compose.yml` under `environment:`.
- Completion for Spring properties in Kubernetes manifests (e.g. `k8s.yml`) under `env.name:`.
- Converts property keys to `ENVIRONMENT_VARIABLE_FORM` (e.g. `server.port` → `SERVER_PORT`).
- Go to declaration for property keys to jump into `application.properties` / `application.yml`.

### Spring AI

- Optional integration with the Explyt AI platform to assist with Spring tasks inside the IDE.
- The AI agent can read/modify project files, explore code, run non-destructive terminal commands, and analyze compilation errors.
- Works with OpenAI‑compatible providers and can be configured to use local or cloud models.
- Requires IntelliJ Platform at least 2025.1 (251+) for AI integrations.
- [Overview of Explyt AI and integrated agents](https://habr.com/ru/companies/explyt/articles/936992/).
- New: AI-augmented Spring actions — details in our article [Neural Networks in Spring Development: Eliminating Routine, Not Intelligence (RU)](https://habr.com/ru/companies/explyt/articles/944266/)

#### AI-augmented Spring actions (when Explyt AI plugin is also installed)
- Convert DB schema to JPA Entity: Right-click Liquibase/Flyway/SQL file → Explyt Spring AI Actions → Convert Liquibase/Flyway File to Entity.
- Entity ↔ DTO conversion: Right-click entity/DTO → Explyt AI Actions → Convert Entity to DTO (and reverse). Supports multiple files, field exclusions, and validation annotations.
- Controller ↔ OpenAPI round-trip: Convert Spring RestController to OpenAPI, and generate controllers from OpenAPI for integrations.
- Generate Spring configs: Boilerplate Kafka and Security configurations via AI for Spring Boot projects (intended as a starting point).
- HTTP format conversions: Convert curl/Postman collections to RFC 7230 .http/.rest files supported by our HTTP client runners.
- Properties ↔ YAML: Convert Spring configuration between properties and YAML formats.

### Additional Inspections and Features

- **Kotlin Support**:
    - Handles Spring-specific features in Kotlin code, such as `internal` modifier usages and constructor validations.
    - Warns against using `object` components as Spring beans.
    - Alerts when internal bean methods need explicit bean names.

- **Configuration Metadata Support**:
    - Completion and validation in `additional-spring-configuration-metadata.json`.
    - Schema support for additional configuration metadata files.

- **Profile Validation**:
    - Checks `@Profile` annotations for errors, empty profiles, and misuse of operators.

- **Async Method Return Type Inspection**:
    - Ensures methods annotated with `@Async` have valid return types (`void` or `Future` inheritors).

- **Cache Annotation Inspection**:
    - Reports incorrect usage of cache annotations on interfaces and suggests using them on classes or methods instead.

- **Dependency Analyzer**:
    - Analyze bean dependencies and navigate through their relationships.
    - Helps understand complex bean interactions and resolve issues.

- **Experimental Scala Support**:
    - Allows you to create and develop projects using Scala.

## Tool Windows

### Explyt Spring (beans, contexts, and the native loader)

The **Explyt Spring** tool window is the central place for working with Spring beans and application contexts.
It is also the entry point for **native context mode** (loading real bean metadata via a lightweight app run).

- Open it from the right sidebar: **Explyt Spring**.
- Link your Spring Boot run configuration: **Link Explyt Spring Boot Project From RunConfiguration**.
- Load/refresh beans: use **Load Beans** / **Refresh** on the toolbar.
- **Bean Analyzer**: open the Dependency Analyzer view for your loaded context (toolbar action labeled **Bean Analyzer**).
- **Aspects**: Spring AOP aspects are shown as dedicated nodes in the tree (since `253.30.53`).

Video: [How Native Context Mode works in Explyt Spring panel](https://github.com/user-attachments/assets/db380852-d239-4e0f-a1b2-15a3a2659f53)

### Explyt Endpoints (Web + Message Brokers)

The **Explyt Endpoints** tool window provides a single view for:
- Spring MVC / WebFlux endpoints
- Router functions
- Spring Messaging listeners (Kafka, RabbitMQ, etc.)

You can filter endpoints, navigate to handlers, and quickly jump between related endpoints.

<img width="800" alt="Endpoints" src="https://github.com/user-attachments/assets/48b80d76-258e-4841-a4dc-fd59a9188d11" />

Video: [How Explyt Endpoints Tool Window works](https://github.com/user-attachments/assets/e38625a3-bc68-4075-9efa-945e64977f36)

## Inlay Hints & Code Vision

### @Profile expression evaluation (inlay hints)

The plugin adds inline hints that show the computed result of Spring `@Profile` expressions.

Example:
```java
@Profile("production & !cloud") // : true
class ProdOnlyBean {}
```

Enable/disable: **Settings | Editor | Inlay Hints | Annotations | Annotation profile hints**.

### Runtime property values in Debug (Code Vision)

When you debug a Spring app with **Explyt debug enabled**, Explyt shows Code Vision entries in `.properties` and `.yml` files:
- the **actual runtime value** of a key (when it differs from the file value)
- a shortcut to locate the `PropertySource` that provided it

The lens name is: **Explyt: property runtime value**.

### @Scheduled cron hints (including `zone`)

- Completion/validation for Spring cron expressions in `@Scheduled`.
- Human-readable hints for cron values.
- Supports the `zone` attribute (since `252.25.15`).

## Code Generation

All generators are available from **Code | Generate** (`Alt+Ins` / `Cmd+N`):

- **Spring Web Method** from URL/cURL: generates a Spring Web annotated method (Java/Kotlin).
- **HttpClient Method** from URL/cURL: generates a `java.net.http.HttpClient` example (Java/Kotlin).
- **JPA equals/hashCode**: generates methods for **Java** entities following best practices.
- **Generate OpenAPI specification** for a project.
- **Properties ↔ YAML** conversion (Editor popup menu): convert Spring config between `.properties` and `.yml`.

## Advanced Features

### Beans in Search Everywhere

Press **Shift** twice to open **Search Everywhere** and use the dedicated **Beans** tab to navigate to any Spring bean by name.

### Mark directory as Spring Application Property Folder

If your project stores configuration in non-standard locations, you can opt in those directories:
- Project View → right-click a directory → **Mark Directory as** → **Spring Application Property Folder**

This enables Spring property completion/validation inside that folder.

### Bean Analyzer (Dependency Analyzer integration)

Open **Explyt Spring** tool window → click **Bean Analyzer** to visualize relationships between loaded beans and navigate to their declarations.

### Multi-context projects

If your application creates multiple Spring contexts, Explyt tracks them separately and keeps inspections/navigation context-aware (since `252.29.43`).

### Library beans DI support

Inspections and autowiring resolution also consider beans coming from project dependencies, not only the project sources.

### SPI beans

Navigation and gutter markers are available for beans produced via Java SPI (`ServiceLoader`).

## Performance

- **Static analysis vs Native context mode**:
    - Static analysis works immediately after opening a project (good for quick feedback and Quarkus support).
    - Native context mode provides the most accurate Spring bean model (conditional beans, profiles, factory beans), but requires a compile + lightweight run.
- **When to use Load Beans**:
    - Use it when you want the highest quality autowiring checks, bean navigation, and tool window data.
    - For large projects, keep only the necessary run configurations linked to minimize background work.
- **Large project tips**:
    - Use **Detach All Spring Boot Projects** when you temporarily don’t need native context data.
    - Prefer **Refresh** over re-linking whenever possible.

## Installation

### Option 1: JetBrains Marketplace (recommended)
- Open IntelliJ IDEA → Settings → Plugins → Marketplace, search for "Spring Explyt" and click Install.
- Or install from the web: https://plugins.jetbrains.com/plugin/28675-spring-explyt

### Option 2: Install from Custom Repository

![Add Repository](https://github.com/user-attachments/assets/1efb7ec4-12d0-4457-ac94-63fab39e1492)

1. Open IntelliJ IDEA and go to **Settings** > **Plugins**.
2. Click the ⚙️ icon and select **Manage Plugin Repositories**.
3. Add the Explyt repository: [https://repository.explyt.dev/](https://repository.explyt.dev/).
4. Search for **Explyt Spring Plugin** in **Marketplace** tab and click **Install**.   
![Install Plugin](https://github.com/user-attachments/assets/7cb36e50-2715-40f1-9f7c-d1fb1cfc87ce)
5. Restart the IDE.

### Option 3: Manual Installation
> *(Note: In this case, you will not receive automatic updates.)*
1. Download the plugin from the [Releases Page](https://github.com/explyt/spring-plugin/releases).
2. Go to **Settings** > **Plugins** > ⚙️ > **Install Plugin from Disk**.
3. Select the downloaded `.zip` file and restart the IDE.

For additional details, go to [Installation Guide](https://github.com/explyt/spring-plugin/wiki/Installation-Guide).

Useful links: [Changelog](./CHANGELOG.md) · [Plugin description](./PLUGIN-DESCRIPTION.md)

### First-time setup

1. Open **Tool Windows** → **Explyt Spring** and **Explyt Endpoints**.
2. Link your Spring Boot run configuration via **Link Explyt Spring Boot Project From RunConfiguration**.
3. Click **Load Beans** to populate the bean tree.
4. Optional: enable **Spring Debug** and start a debug session to get runtime property Code Vision in config files.
5. Optional: configure `.http` / `.rest` runners in **Settings | Tools | Explyt Spring**.

### Troubleshooting

- **No beans / autowiring looks incomplete**: run **Load Beans** in the **Explyt Spring** tool window to build the native model.
- **Sync failed due to build errors**: build the project first (Build → Build Project) and try syncing/loading again.
- **Older Spring Boot versions (< 2.4)**: you can try enabling the registry key `explyt.spring.native.old`.

---

## Usage

- **Explyt Spring tool window (beans & context)**

  Open the **Explyt Spring** tool window (right sidebar). Link a Spring Boot run configuration via **Link Explyt Spring Boot Project From RunConfiguration**, then click **Load Beans** / **Refresh**.

  Tip: click **Bean Analyzer** to open the Dependency Analyzer view for the current context.

- **Explyt Endpoints tool window**

  Open **Explyt Endpoints** to browse and navigate Spring MVC/WebFlux endpoints and message broker listeners.

- **Built-in HTTP Client with Swagger UI**

    - **Create HTTP Methods Using Spring Annotations**

      Write methods in Java or Kotlin using Spring Web annotations to define HTTP requests. These methods can be placed in any class.

      ```java
      @GetMapping("https://api.openweathermap.org/data/2.5/weather")
      public void getWeather() {
          // Implementation is not necessary
      }
      ```

    - **Run HTTP Requests**

      A **Run** icon will appear next to methods with absolute URLs. Clicking this icon opens Swagger UI within IntelliJ IDEA.

    - **Use Swagger UI**

        - Fill in parameters and execute the HTTP request directly from the Swagger UI interface.
        - View the response, headers, and other details.
        - Copy the cURL command generated by Swagger UI for use elsewhere if needed.

    - **Edit OpenAPI Files**

        - Switch to the OpenAPI definition if you need to make manual adjustments.
        - The plugin provides code completion and validation for OpenAPI files.

    - **Generate methods from URL/cURL**

        - Use the **Generate** menu (`Alt+Ins`) inside a Java class or Kotlin file.
        - Choose **Spring Web Method** to generate Spring Web annotated methods.
        - Choose **HttpClient Method** to generate a standalone `java.net.http.HttpClient` example.

- **Inspections and Quick Fixes**

  The plugin automatically detects potential issues in your Spring application. Use `Alt+Enter` when an issue is highlighted to see quick fixes.

- **Code Completion and Navigation**

  Enjoy improved code completion throughout your Spring application, including in properties files and annotations. Navigate easily between beans, configurations, and endpoints.

- **Line Markers and Gutter Icons**

  Visual markers appear in the gutter to help you identify Spring components, endpoints, scheduled tasks, and more. Click these icons to navigate directly to related code.

- **Bean Analyzer (Dependency Analyzer)**

  Open **Explyt Spring** tool window and click **Bean Analyzer** to explore the loaded bean graph and navigate between related components.

- **Beans in Search Everywhere**

  Press `Shift` twice → open the **Beans** tab to navigate to any bean by name.

- **Mark directory as Spring Application Property Folder**

  Project View → right-click a directory → **Mark Directory as** → **Spring Application Property Folder**.

- **Inlay Hints & Code Vision**

  Use inline hints for `@Profile` expressions and runtime property value Code Vision during debug (see sections above).

- **Kotlin Support**

  Take advantage of specialized features and inspections designed for Kotlin Spring applications.

- **Execute `.http` or `.rest` Files with JetBrains HttpClient**
  1. **Create a `.http` file**:
      - Right-click your project > **New** > **File** 
      - Name it with extension `.http`
  2. **Write requests**:
     ```http
     ### Get weather data
     GET https://api.openweathermap.org/data/2.5/weather?q=London
     Accept: application/json
     ```
  3. Run requests:
     - Click the ▶️ Run icon next to a request section.
     - View responses directly in the IDE’s Run Tool Window.

## Learn More

For a detailed overview of the plugin's features and how it can improve your development experience, check out our articles:

- **[Explyt Spring Release: SQL, Docker-Compose, Debugger (RU)](https://habr.com/ru/companies/explyt/articles/962536/)** — overview SQL DML, Docker Compose completions, and Remote Debugger.
- **[Neural Networks in Spring Development: Eliminating Routine, Not Intelligence (RU)](https://habr.com/ru/companies/explyt/articles/944266/)**
- **[Explyt AI Platform and integrated agents (RU)](https://habr.com/ru/companies/explyt/articles/936992/)**
- **[Explyt Spring Plugin: Quarkus support (RU)](https://habr.com/ru/companies/explyt/articles/926484/)**
- **[Explyt Spring Debugger (RU)](https://habr.com/ru/companies/explyt/articles/933158/)**
- **[Explyt Spring plugin: *.http files support in IntelliJ IDEA Community (RU)](https://habr.com/ru/companies/explyt/articles/884280/)**
- **[Explyt Spring Plugin — our take on the HTTP client for IntelliJ IDEA (RU)](https://habr.com/ru/companies/explyt/articles/874236/)**
- **[Patching Spring bytecode to enhance application context recognition (EN)](https://medium.com/@explytspring/explyt-spring-plugin-patching-spring-bytecode-to-enhance-application-context-recognition-0817fb52b056)** — deep dive into our javaagent and declarative bytecode patching.
- **[Stop playing catch-up with Spring — Explyt Spring plugin for IDEA Community (EN)](https://medium.com/@explytspring/stop-playing-catch-up-with-spring-introducing-the-explyt-spring-plugin-for-idea-community-0be380b36a75)** — background and approach to using native Spring logic for accurate context.

The articles include explanations, screenshots, and examples showing how the Explyt Spring Plugin can boost your productivity.

## Integrations

- Lombok support for Java projects
- JSR-330 annotations (jakarta.inject / javax.inject)
- JAX-RS annotations (jakarta.ws.rs / javax.ws.rs)
- Retrofit and OpenFeign annotations
- JPA/JPQL support (query language inspections and highlighting)
- Spring Security, Spring Cloud, Spring Integration, Spring Messaging modules
- Quarkus support: CDI, JAX-RS, interceptors, decorators, Endpoints tool window, Swagger UI–based HTTP client
- IntelliJ IDEA Ultimate: if you use Ultimate, disable the built‑in Spring plugin to avoid conflicts.

## Contributing

We welcome contributions! If you'd like to contribute to this project, please follow these steps:

1. **Fork the Repository**

   Click the **Fork** button at the top of the [GitHub repository](https://github.com/explyt/spring-plugin) page to create your own copy.

2. **Clone Your Fork**

   ```bash
   git clone https://github.com/your-username/spring-plugin.git
   ```

3. **Create a Feature Branch**

   ```bash
   git checkout -b feature/your-feature-name
   ```

4. **Make Changes**

   Implement your feature or fix a bug.

5. **Commit and Push**

   ```bash
   git add .
   git commit -m "Description of your changes"
   git push origin feature/your-feature-name
   ```

6. **Create a Pull Request**

   Open a pull request against the `main` branch of the original repository.

Thank you for helping us make Explyt Spring Plugin better! ⭐

## Community and Support

- **GitHub Issues**: Found a bug or have a feature request? [Open an issue](https://github.com/explyt/spring-plugin/issues).
- **Telegram**: Join our [Telegram channel](https://t.me/explytspring_en) for real-time support and discussions.
  - *RU*: [Channel](https://t.me/explytspring), [Chat](https://t.me/explytspringchat)
- **Contributing**: Want to contribute? Check out our [Contributing Guide](https://github.com/explyt/spring-plugin/blob/main/CONTRIBUTING.md).

## License

Explyt Spring Plugin is **free for both commercial and non-commercial use**. However, redistribution or modification of the plugin is not allowed without explicit permission. For details, see the [Explyt License](https://github.com/explyt/spring-plugin/blob/main/LICENSE.md).

And if you want to utilise the source code of the plugin, you must read and accept our [Explyt Source License](https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md).

## Acknowledgments

- **Inspiration**: Created to enhance Spring development in the IntelliJ IDEA Community Edition.
- **Contributors**: Thank you to all contributors who have helped improve this plugin. Your efforts are greatly appreciated.

---

For more information, issues, or to contribute, please visit the [GitHub repository](https://github.com/explyt/spring-plugin).

---

*Made with ❤️ by the Explyt Team.*
