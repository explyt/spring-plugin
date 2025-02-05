# Explyt Spring Plugin for IntelliJ IDEA Community Edition
![example workflow](https://github.com/explyt/spring-plugin/actions/workflows/build.yaml/badge.svg)
![GitHub Release](https://img.shields.io/github/v/release/explyt/spring-plugin)
[![telegram_badge](https://img.shields.io/badge/Telegram-Explyt%20Spring%20EN-252850?style=plastic&logo=telegram)](https://t.me/explytspring_en)
[![telegram_badge](https://img.shields.io/badge/Telegram-Explyt%20Spring%20RU-252850?style=plastic&logo=telegram)](https://t.me/explytspring_en)

**Explyt Spring Plugin** supercharges your IntelliJ IDEA Community Edition with advanced Spring Framework features—usually only available in the Ultimate Edition. Develop Spring applications faster and smarter with enhanced tools for beans, configurations, endpoints, and more.

---

**Plugin Status**: The plugin is completely **FREE** for both non-commercial and commercial use.

---

![Screen](https://raw.githubusercontent.com/explyt/spring-plugin/refs/heads/main/images/screen1.jpg)

👉 Visit the [official Explyt website](https://explyt.com/spring/).

## Table of Contents

- [Features](#features)
    - [Key Features](#key-features)
    - [Our Innovative Approach](#our-innovative-approach)
    - [New Feature: Built-in HTTP Client Using Swagger UI](#new-feature-built-in-http-client-using-swagger-ui)
    - [Spring Core and Boot Enhancements](#spring-core-and-boot-enhancements)
    - [Spring Web Enhancements](#spring-web-enhancements)
    - [Spring Data Support](#spring-data-support)
    - [Spring AOP Enhancements](#spring-aop-enhancements)
    - [Additional Inspections and Features](#additional-inspections-and-features)
- [Installation](#installation)
    - [Installing Explyt Spring Plugin from Custom Repository](#installing-explyt-spring-plugin-from-custom-repository)
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

### Our Innovative Approach

**Explyt Spring Plugin** uses a unique method to understand your Spring application thoroughly. Instead of analyzing only the source code, our plugin runs a lightweight version of your application to get accurate information about your Spring beans. This means:

- **Accurate Bean Detection**: We can detect beans that are conditionally loaded or defined through complex configurations.
- **Better Inspections**: By having real bean data, we reduce false warnings and provide more precise code inspections.
- **Improved Navigation and Completion**: Navigate to beans and get code completion suggestions based on actual bean definitions.

This approach ensures that even if your application uses advanced Spring features like `@Conditional`, complex `@ComponentScan` configurations, or custom conditions, the plugin understands them correctly.

### New Feature: Built-in HTTP Client Using Swagger UI

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
    - **Generate Methods from URLs or cURL Commands**: Use the **Generate** menu (`Alt+Ins`) inside a Java class or Kotlin file to create Spring Web methods based on a given URL or cURL command.
    - **Supports Java and Kotlin**: Works seamlessly with both languages.

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

### Spring Core and Boot Enhancements

- **Advanced Inspections and Quick Fixes**:
    - **Property Key Validations**: Detects unused properties, duplicate keys, and keys that don't follow the kebab-case convention.
    - **Configuration Properties Support**: Navigate, complete, and validate `@ConfigurationProperties` classes.
        - **Nullable Parameters Check**: Warns if constructor parameters in `@ConfigurationProperties` classes are not nullable.
        - **Invalid Prefix Detection**: Finds empty, duplicate, or incorrectly formatted prefixes in `@ConfigurationProperties`.
        - **Configuration Verification**: Ensures `@ConfigurationProperties` classes are correctly registered with Spring.
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
    - **Query Languages**: Supports JPQL and SQL queries with syntax highlighting and code assistance.

- **Line Markers and Gutter Icons**:
    - Visual markers for beans, configurations, and `@Scheduled` methods.
    - Easily navigate between bean definitions and their usages.

### Spring Web Enhancements

- **Endpoints Tool Window**: Access all your Spring MVC and WebFlux endpoints in one place. View, navigate, and analyze your controllers and router functions.

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

### Spring Data Support

- **Repository Method Name Validation**: Ensures your repository methods follow naming conventions and match actual entity properties.
- **Return Type and Parameter Checks**: Validates that repository methods have correct return types and parameters that match the query.
- **Language Injection**: Offers JPQL and SQL syntax support within repository query methods.
- **Bean Recognition**: Detects Spring Data repositories as beans, helping with navigation and autowiring checks.
- **Repository Method Autocomplete**: Offers autocompletion for new Repository methods using method naming conventions and entity properties.

### Spring AOP Enhancements

- **AOP Method Call Inspection**: Warns when methods with annotations like `@Transactional` or `@Async` are called from within the same class.
- **Line Markers**: Visual markers for aspects, pointcuts, and advice methods, making navigation easier.

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
    - Allows to create and develop projects using Scala lang. 

## Installation


### Option 1: Install from Custom Repository

![Add Repository](https://github.com/user-attachments/assets/1efb7ec4-12d0-4457-ac94-63fab39e1492)

1. Open IntelliJ IDEA and go to **Settings** > **Plugins**.
2. Click the ⚙️ icon and select **Manage Plugin Repositories**.
3. Add the Explyt repository: [https://repository.explyt.dev/](https://repository.explyt.dev/).
4. Search for **Explyt Spring Plugin** in **Marketplace** tab and click **Install**.   
![Install Plugin](https://github.com/user-attachments/assets/7cb36e50-2715-40f1-9f7c-d1fb1cfc87ce)
5. Restart the IDE.

### Option 2: Manual Installation
> *(Note: In this case, you will not receive automatic updates.)*
1. Download the plugin from the [Releases Page](https://github.com/explyt/spring-plugin/releases).
2. Go to **Settings** > **Plugins** > ⚙️ > **Install Plugin from Disk**.
3. Select the downloaded `.zip` file and restart the IDE.

For additional details, go to [Installation Guide](https://github.com/explyt/spring-plugin/wiki/Installation-Guide).

---

## Usage

- **Endpoints Tool Window**

  Access the **Explyt Endpoints** tool window from the right sidebar to see all endpoints in your project, including controllers and REST endpoints.

- **Native Bean Loading**

  Use the **Explyt Spring Run Configuration** to load bean definitions directly from your Spring application. This process compiles your project and runs it in a special mode to get bean information without fully starting the application.

    - Click on the **Load Beans** icon (a Spring Boot icon with a magnifying glass) to start loading.
    - After loading, a panel showing all beans in your application will appear. Double-click any bean to navigate to it.

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

    - **Generate Methods from URLs or cURL Commands**

        - Use the **Generate** menu (`Alt+Ins`) inside a Java class or Kotlin file.
        - Choose **"Spring Web Method from URL"** or **"Spring Web Method from cURL"**.
        - This helps you quickly create methods without manually typing code.

- **Inspections and Quick Fixes**

  The plugin automatically detects potential issues in your Spring application. Use `Alt+Enter` when an issue is highlighted to see quick fixes.

- **Code Completion and Navigation**

  Enjoy improved code completion throughout your Spring application, including in properties files and annotations. Navigate easily between beans, configurations, and endpoints.

- **Line Markers and Gutter Icons**

  Visual markers appear in the gutter to help you identify Spring components, endpoints, scheduled tasks, and more. Click these icons to navigate directly to related code.

- **Dependency Analyzer**

  Analyze bean dependencies and navigate through their relationships. This is useful for understanding complex interactions and solving dependency issues.

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

- **[Enhancing Spring Development in IntelliJ IDEA Community Edition with Explyt Spring Plugin](https://habr.com/ru/companies/explyt/articles/854304/)**
- **[Our Version of the HTTP Client for IntelliJ IDEA Community Edition](https://habr.com/ru/companies/explyt/articles/874236/)**

  *(Note: The articles are in Russian.)*

The articles include explanations, screenshots, and examples showing how the Explyt Spring Plugin can boost your productivity.

## Integrations

- Lombok support for Java projects
- Support for Jakarta and Javax annotations from JSR-330 standard
- JAX-RS annotations
- Retrofit annotations 
- IntelliJ IDEA Ultimate
  - To use Explyt Spring Plugin with IntelliJ IDEA Ultimate, you have to disable the built-in Spring plugin to avoid conflicts.

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

And if You want to utilise the source code of the Plugin, you must read and accept our [Explyt Source License](https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md).

## Acknowledgments

- **Inspiration**: Created to enhance Spring development in the IntelliJ IDEA Community Edition.
- **Contributors**: Thank you to all contributors who have helped improve this plugin. Your efforts are greatly appreciated.

---

For more information, issues, or to contribute, please visit the [GitHub repository](https://github.com/explyt/spring-plugin).

---

*Made with ❤️ by the Explyt Team.*