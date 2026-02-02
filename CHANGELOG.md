<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Explyt Spring Changelog

## [Unreleased]

### Spring AI

- feat: Spring AI Tools for bundled [MCP Server plugin](https://plugins.jetbrains.com/plugin/26071-mcp-server)

### Spring Core/Boot

- feat: Kotlin: Quick-fix for data class @ConfigurationProperties without `@ConstructorBinding` [#31](https://github.com/explyt/spring-plugin/issues/31)
- fix: Slow operation in EDT [#154](https://github.com/explyt/spring-plugin/issues/154)
- fix: ConfigurationProperties for kotlin inspection [#163](https://github.com/explyt/spring-plugin/issues/163)
- fix: PathVariable in Class URL [#161](https://github.com/explyt/spring-plugin/issues/161)
- fix: Bean Autowired gutter icon for lombok

## [253.30.53] - 2025-12-22

### Spring Core/Boot

- feat: [Endpoints](https://github.com/explyt/spring-plugin/pull/143#issuecomment-3641799841) support for message brokers and Spring Boot (demo video)
- feat: Added Spring Aspect nodes to the Explyt Spring
  [Tool Window](https://github.com/explyt/spring-plugin/pull/151#issuecomment-3641786335) (demo video)
- feat: Properties converter between `.yml` and `.properties`. [#153](https://github.com/explyt/spring-plugin/pull/153)
- fix: `@ConfigurationProperties` quick-fix improvements. [details](https://github.com/explyt/spring-plugin/pull/153#issuecomment-3664193934) (demo video)
- fix: Redundant property inspection and gutter icon behavior. [Issue](https://github.com/explyt/spring-plugin/issues/114)
- fix: Kotlin template string inspection for `@PathVariable`. [Issue](https://github.com/explyt/spring-plugin/issues/142)
- fix: YAML kebab-case quick fix. [Issue](https://github.com/explyt/spring-plugin/issues/147)

### Spring Data

- feat: Spring Data 4.0 [AOT navigation](https://github.com/explyt/spring-plugin/pull/148#issuecomment-3641770937) from
  repository methods (demo video).

### Spring Debugger

- feat: [Support](https://storage.yandexcloud.net/explyt-web/videos/spring-wiki/debugger/spring-debug-old.mp4) for IntelliJ IDEA versions 241 / 242 (demo video).

## [252.29.43] - 2025-11-24

### Spring Core/Boot

- fix: load beans by -javaagent on [Windows](https://github.com/explyt/spring-plugin/issues/125)
- feat: multi context [support](https://github.com/explyt/spring-plugin/issues/116)

### Docker

- fix: improve 'env' variables [completions](https://github.com/explyt/spring-plugin/issues/119)
- feat: support 'env' completions in k8s.yml files

### Spring Web

- fix: [improve](https://github.com/explyt/spring-plugin/issues/115) @PathVariable support. Regexp & QuickFixes

## 252.28.37 - 2025-11-05

### Spring Core/Boot
- fix: configuration properties field should not be autowired

### Spring Initialzr
- fix: download indicator exception

### Spring Debugger
- feat: Spring Remote Debugger

### Docker 
- feat: env variables completion in docker compose files

### SQL
- feat: introduce basic SQL DML support (no plugin required)

### Other
- chore: Update plugin name to Spring Explyt
- fix: Remove internal API usages and access to FUS 

## 252.27.26 - 2025-10-02

### Spring Core/Boot
- fix: log pattern error & additional bean search
- fix: Race error in case if several inspections were started at once

### Spring AI
- feat: AI add action Entity to DB Script

### Spring Data
- feat: JDBC client inspections (#68)

### Spring Debugger
- feat: Spring Debugger runtime property - code vision
- feat: Spring Debugger show server Web URL in console
- chore: Update debugger hint icon

## 252.26.18 - 2025-09-18

### Spring Core/Boot
- feat: Support Spring 7: Bean Registrar

### Spring AI
- fix: ai prompts

### Spring Data
- feat: JDBC client params navigation & inspections

## 252.25.15 - 2025-09-05

### Spring AI 
- feat: Explyt AI integration

### Spring Core/Boot
- feat: library beans DI support
- feat: Scheduled cron's zone attribute support 
- fix: Property definition inspection 
- fix: property cache (#74)
- fix: ConditionalOnMissingBeanStrategy - method from the same class (#76)

### Spring Data
- JPA generate equals & hashCode methods

### Spring Web / OpenApi
- fix: Spring web parse url

### Spring Debugger
- Improve spring debugger node

## 252.24.11 - 2025-08-15

### Spring Core/Boot
- Add an icon to the "Mark directory as Spring Configuration Root" action
- fix: Maps with string key/value in properties wasn't resolved
- fix: settings ref for agent mode #59
- fix: various EDT issues

### Spring Web / OpenApi
- fix: Use the correct server port

### HttpClient
- feat: HTTP completion in .http files

### Spring Debugger
- feat: revert gradle configuration after debug
- fix: remove setStoreExternally for debugger
- fix: Debugger run configuration - empty map is immutable

### Other
- fix: 252 only! remove same warnings (#61)

## 252.23.8 - 2025-08-05

### Spring Core/Boot
- feat: Spring Debugger
- feat: Mark/unmark folder as Spring Configuration Root 
- fix: Invalid references when there are comment and placeholder arguments (#47)
- fix: Only first object referenced without warning in properties for fields with the same type in @ConfigurationProperties
- fix: Various bug fixes

### Spring Web / OpenApi
- feat: Support latest OpenApi in swagger-ui (#50)

### Spring Data
- feat: Mark some strings as SQL (#54)

### Other
- fix: Error reporting issues
- fix: Invalid injection host exception (#51)
- chore: support 252

## 251.22.6 - 2025-06-09

### Quarkus
- feat: Detect Quarkus endpoints in the Endpoint panel
- fix: Various bug fixes

### Spring Core/Boot
- fix: Minor fixes

### Other
- feat: Add Spring tool for the Explyt AI platform
- fix: GlitchTip error reporting

## 251.1.15986533443 - 2025-07-01

### Quarkus
- Feature: Support Quarkus DI (#34)

### Spring Core/Boot
- feat: application.yaml enum key error (#32)

### Spring Initializr
- fix: since 2025.1 (251) DownloadHandler in JCefBrowser does not work (#35)
- chore: display verbose errors while Spring Project creation

### Other
- feat: 2025.2 (252) initial support
- chore: migrate from sentry to glichtip

## 251.1.3531 - 2025-04-17

### Spring Core/Boot
- Added: SPI bean navigation support (#29)
- Added: Support properties mapped to Map<Enum, V> (#28)
- Fixed: generic bean navigation for beans with java fragments
- Fixed: escape command line to start javaagent

### Spring Web / OpenApi
- Added: add to an openapi file all possible servers and ports from configs
- Fixed: optimize endpoints search
- Fixed: optimize editing of temporal openapi files

### Other
- HttpClient: added variable and line comment support in http files. (#26)
- Improve test build

## 243.1.3438 - 2025-03-25

### Spring Core/Boot
- Added: migrate agent to declarative-bytecode-patcher

### Other
- Fixed: various bugfixes

## 243.1.3391 - 2025-03-14

### Spring Core/Boot
- Added: use -javaagent for getting spring context

### Http Client
- Added: Baseline HTTP parser + lexer + syntax highlighter. (#23)

### Spring Data
- Fixed: sql was tried to be injected in invalid places 

## 243.1.3351 - 2025-02-20

### Spring Data
- Added: Support inject sql to `sql` named parameter functions (#16) 

## 243.1.3333 - 2025-02-11

### Spring Core/Boot

- Added: rename property for @ConfigurationProperties class methods (java) or constructor parameters (kotlin)
- Added: completion for Hibernate settings in configuration property
- Added: in properties/yaml completion for map value
- Fixed: rename property name in properties/yaml files and @Value
- Fixed: configuration: show comment for property from field in case of missing setter method
- Fixed: configuration: no property linemarkers for java record `@ConfigurationProperties`

### Spring Web / OpenAPI

- Added: Retrofit run linemarker (spec generation)
- Added: Retrofit endpoint actions
- Fixed: Endpoint's RunLinemarker is always available, removed `Run in Swagger` action from endpoint's actions
- Added: JAX-RS run linemarker (spec generation)
- Added: JAX-RS endpoint actions
- Added: Endpoint Tool Window shows JAX-RS endpoints
- Fixed: Openapi spec generation supports MultipartFile usage
- Added: HttpExchange endpoint actions
- Added: Endpoint Tool Window shows HttpExchange endpoints
- Fixed: `Generate OpenApi Doc` intention - create dir 'resources' if it does not exist
- Added Inspection: `Unknown reference` for openapi `$ref` (yaml, json)

### Other

- Added: Spring Boot Panel: Generate special starter code to connect the panel to scan project beans for complex projects
- Added: Support 'http' and 'rest' file extensions
- Added: Ability to specify cli runner (like JetBrains HttpClient or HttpYac) to execute http-scripts and other RFC 2616 scripts
- Added: Linemarkers in 'http' and 'rest' to execute statement

## 243.1.3155 - 2025-01-17

### Spring Core/Boot

- Fixed: Properties: quick-fix to kebab-case didn't work
- Fixed: Show correct linemarkers in library files with an enabled Spring Boot panel. 

### Spring Web / OpenAPI

- Added: Swagger is able to download a picture or binary file
- Fixed: Navigation from endpoint to MockMvc usage (kotlin)
- Chore: Now swagger uses JCEF browser through HttpClient
- Fixed: Navigation between MockMvc urls for a new version of MockMvc library
- Added: Use localhost as default server if server wasn't set in an OpenAPI file

### Other

- Added: Kotlin Run Configuration can be linked to Explyt Spring Boot Panel
- Added: Show progress while collecting project's endpoints in the Spring Endpoints panel 
- Added: Sync project with the Explyt Spring Boot panel using linemarker on @SpringBootApplication

## 243.1.3083 - 2024-12-29

### Spring Core/Boot

- Added: support for syntax highlighting in application.yaml and application.properties
- Added: completion/reference in property starting with logging.level
- Fixed: inspection kebab-case false positive for logging level property
- Fixed: fix inspection resource in tests (#15)

### Spring Web / OpenAPI

- Added: Action `Generate OpenApi Specification` for project
- Added: Run in Swagger action for Controller/FeignClient
- Added: Generate requestBody as custom type schema for Run controller/endpoint action
- Added: Another design for Spring Endpoints, new Line Marker icons
- Added: Generation of Http Client and OpenApi spec using cURL

### Other

- Added: Auto Configurations and Message Brokers folders to Spring Explyt View
- Fixed: stack overflow on jpql file find in EntityAttributeSearcher

## 243.1.2927 - 2024-12-16

### Spring Core/Boot

- Added: completion in property for field with type Resource the class with @ConfigurationProperties
- Fixed: support in property for boolean starting with "is"
- Fixed: reference in property Relaxed Binding
- Fixed: reference in property for Map values
- Fixed: include in search for class-reference not only compile dependencies, but runtime as well
- Fixed: The ExplytSpringBoot settings lead to the incorrect direction (#13)
- Fixed: inspection for fields and methods super classes (#14)

### Spring Web / OpenAPI

- Fixed: Removed navigation from endpoint to openapi path
- Added: Run in Swagger endpoint action
- Added: Generate OpenAPI from coRouter function (#10)
- Added: Floating button for refresh ExplytSpringBoot panel
- Added: Generate Spring MVC controller method by url in Generate intention

## 243.1.2801 - 2024-12-01

### Spring Core/Boot

- Added: Line Markers in library files
- Fixed: Improve Yaml/properties completion

### Other

- Added: User usage statistics gathering

## 243.1.2727 - 2024-11-23

### Spring Core/Boot

- Added: @EnableConfigurationProperties annotation marks @ConfigurationProperties as beans
- Added: Inspection and QuickFix checks if @ConfigurationProperties class is correctly configured
- Fixed: Yaml property value completion doesn't work (#9)

### OpenAPI

- Fixed: backward navigation to `$ref` usage
- Added: endpoint linemarker suggests openapi generation when there is no place to navigate to (#12)
- Added: intention to generate description from controller endpoint

### Other

- Fixed: fix EDT while editing json schema files

## 243.1.2662 - 2024-11-15

### Spring Core/Boot

- Added: completion and inspection in `additional-metadata.json`
- Added: native SQL support integration with Database Navigator
- Added: schema to additional configuration metadata
- Fixed: inspection kebab-case in properties for Map
- Fixed: navigate in properties for Map
- Fixed: inserting one parameter instead of several
- Fixed: wrong bean method navigation
- Fixed: Don't warn about kebab case for property values
- Fixed: profile change (#6)
- Fixed: Bug: bean line marker native fix
- Fixed: Inspection SpringBeanIncorrectAutowiringInspection false positive ConfigurationProperties
- Fixed: Property navigation from application.properties from tests
- Fixed: module AlreadyDisposedExceptional

### Other

- Prepare code configurations for open source
- Fixed: incorrect CachedValue use for ConfigurationPropertyDataRetriever
- Support 243 idea plugin sdk
- Enable K2 support mode

### Spring Web/MVC

- Show/hide the Explyt Endpoints if the project has/does a web dependencies
- Fixed: in Explyt Endpoints show in path one slash (not empty path)
- Fixed: in Explyt Endpoints not show empty list
- Fixed: gutter calculation for coRouter route

### OpenAPI

- Fixed: inspection for OpenAPI version 

## 242.1.2334 - 2024-10-29

### Spring Web/MVC

- Added ToolWindow with endpoints
- Added inspection for @LoadBalanced annotation in the FeignClient interface
- Fixed: Inspection in @ConfigurationProperties with @ConstructorBinding is in use, by binding to the constructor
  parameters
- Fixed: PatternSyntaxException while parsing controller
- Added endpoint loaders for FeignClient, RestClient, and WebClients

### OpenAPI

- Added inspection for a supported version of OpenApi
- Added completion in OpenAPI specification
- Automatically apply OpenAPI JSON schema (Specification 3.0.0/3.1.0)
- Supported dark theme for SwaggerUI panel
- Added navigation from OpenAPI http request to preview (SwaggerUI panel)
- Added OpenAPI files preview (SwaggerUI panel)
- Added OpenAPI `$ref` navigation and completion for .yaml/.json formats
- Added endpoint loaders for OpenAPI
- Added special icons for OpenAPI files
- Fixed: Dismiss Ultimate promo bar for OpenAPI files

### Spring Core/Boot

- Added the Spring Native Beans panel for Spring Boot applications
- Added support Spring Boot versions before 2.4.0 via explyt.spring.native.old flag
- Added icon for a configuration file for Spring Boot application
- Added reference to package from annotation parameters with names `basePackages`, `scanBasePackages`. AntPattern supported
- Changed behaviour of `Can't find package` inspection. Now it shows inspection till first unknown segment. AntPattern supported
- Added linemarker from property to library hint
- Fixed: property completion for class-reference provider from libraries
- Fixed: prefixFromUsage cache bug
- Fixed: AlreadyDisposedException
- Added: Generator for @PostConstruct methods
- Fixed: autowired fields in abstract class
- Fixed: esprito.spring.root.runConfiguration
- Fixed: Detect @Component bean for abstract class

### Spring AOP

- Added Spring AOP processing using Spring Boot Native

### Other

- Upgrade to intellij 242 branch
- Endpoints tool icon light

## 241.1.1834 - 2024-08-06

### Spring Core/Boot

- Fixed: Autocomplete in application.properties produces an error: NoSuchElementException: List is empty.
- Fixed: @ConfigurationProperties with lombok @Setter and Lombok plugin installed produces an error
- Added Inspection: `Should be kebab-case` for property key
- Fixed: Yaml property autocomplete inserts into previous line
- Fixed: kebab-case inspection description
- Fixed: Yaml PropertyLineMarker only for leaf elements
- Added beans search by name in `Search Everywhere`
- Performance: ConfigurationProperty prefix calculation was too slow
- Performance: Improves after performance tests on big projects
- Fixed: ConditionOnProperty is enabled even if it was disabled in properties
- Fixed @ConfigurationProperties - navigation/line markers

### Spring Data

- Fixed: support CoroutineCrudRepository
- Fixed: Repository injected through @Autowired and package enabled via @EnableJpaRespoitory
- Inspection: @EntityScan package support
- Fixed: Spring Data method name inspection - default interface methods were not analyzed

### Spring Web/MVC

- Updated: navigation between endpoint with template parameters and  `WebTestClient`, `MockMvcBuilders` methods
- Add navigation considering `WebTestClient.method` to endpoint. Add elements to endpoint's linemarker navigating to uri considering `WebTestClient.method`, `MockMvcBuilders.request`, `MockMvcBuilder.multipart`
- Add a gutter to the methods `RouterFunctions`: `coRouter`, `route`
- Fixed: navigation from endpoint's gutter
- Add `WebTestController` `expectBody[List]` methods autocompletion
- Add `RouterFunctions` `coRouter` methods autocompletion
- Add `RouterFunctions` `route` methods autocompletion
- Performance: endpoint usage search speed-up

### Other

- compatibility with internal 242 api 
- gradle kotlin module test problem
- Add error tracking via Sentry setup

## 241.1.1581 - 2024-06-10

### Spring Core/Boot

- Fixed: Inspection: Duplicate properties keys in different relaxed binding forms do not show error
- Find Usages: for property keys when used in different relaxed binding forms
- Fixed: Navigate to autowired candidates and bean declarations for wildcard type
- Fixed: Navigate to autowired candidates and bean declarations in Kotlin: arrays, collections and maps
- Make Spring `@Scheduled` description human-readable
- Inspection: property placeholder not in a kebab-case
- Fixed: Inspection "Cannot resolve key property" for non-kebab-case keys
- Fixed: Inspection "Autowire failed" for `ApplicationContext`
- Inspection: Kotlin `internal` modifier mangling
- The inspection text has been adjusted for `@ConfigurationProperties`
- Intention: create property description in `additional-metadata.json`
- Added reference from `DynamicPropertyRegistry.add` method to property
- Fixed inspection `resource name must begin with a slash`. Added valid prefixes
- Fixed `@Value` folding for Kotlin

### Spring Web/MVC

- Inspection: `WebController` `bodyToMono/Flux`, `awaitBody` type doesn't match endpoint
- Add `WebController` `bodyToMono/Flux`, `awaitBody` methods autocompletion
- Inspection: `WebController/WebTestController` uri parameters count
- Add Navigation from `WebClient/WebTestClient` to Controller endpoints

### Other

- Migrate Esprito to Explyt
- Update plugin description
- Fixed: Stale UAST cache

## 241.1.1303 - 2024-04-24

### Spring Core/Boot

- Added a gutter to the constructors of other classes that are used in the class with @ConfigurationProperties
- Added autocompletion in properties files for all classes fields
- Inspection `unresolved property key` supports Relaxed Binding
- Fixed inspection for `@Retention` on Spring annotations. Supported meta-annotations
- Fixed stackoverflow error while property calculation on some cases
- Support `@Scheduled` annotation

## 241.1.1263 - 2024-04-17

### Spring Core/Boot

- Fixed inspection for constructor parameters with default value: Class constructor properties annotated
  with `@ConfigurationProperties` must be nullable
- Property reference supports Relaxed Binding
- Auto-detection for Kotlin SpringBoot run configurations

### Other

- Added license check scheduler
- Fix: EDT exception in SpringToolRunConfigurationConfigurable

## 241.1.1199 - 2024-04-11

### Spring Core/Boot

- Inspection `unresolved property key` supports Relaxed Binding
- Fix: Kotlin constructor with default values counts as autowired
- Inspection: Warning: Spring @Value annotation string should start with "#{" "${" or resource prefixes

### Other

- License URL fix

## 241.1.1167 - 2024-04-09

### Spring Core/Boot

- LineMarker: Navigation from Kotlin constructor parameters to property declaration

### Spring Data

- Bean method doesn't navigate to autowired collection

### Other

- Upgrade to intellij 241 branch

## 2024.233.1151 - 2024-04-08

### Spring Data

- Spring Data methods name inspection (Repository: findAll property is unknown)
- Parse run configuration profile arguments
- LineMarker: Navigate to Bean Declaration for class/type injected through the @Bean annotation

### Spring Core/Boot

- Added inspection: Class constructor properties annotated with `@ConfigurationProperties` must be nullable
- Added `'ConditionalOn' bean filtering` setting with `true` as default value. On activation bean filtering works
  without auto-configurations
- Detect `context.register` as context root
- Support @SpringBootTest annotation
- LineMarker: Inactive bean
- Fix: reference publishEvent to inherited annotation listener
- Fix: inspection reference placeholder in yaml

### Spring Web/MVC

- Inspection for duplicated `@RequestMapping` endpoints

### Other

- Invalid name in Tools settings
- Added validate license panel

## 2024.233.1020 - 2024-03-22

### Spring Core/Boot

- Support `@ComponentScan` scope to find spring beans.
- Support `@Import` to find configurations.
- Inspection for methods annotated with `@Async`, `@Transactional`, `@Cacheable`, `@CachePut`, `@CacheEvict` work to prevent calls within the same class.
- Bean scope options in autocomplete, including custom scopes.
- Inspections for missing or problematic resource files in .properties and .yaml.
- Inspection for interfaces annotated with `@Cacheable`, `@CacheConfig`, `@CachePut`, `@CacheEvict`, `@Caching`: prohibit cache annotations on interfaces.
- Line marker navigates from `getBean` method to bean declaration.
- Fix: Remove or comment Bean class action does not recover (return back) bean gutter icon

### Spring Web/MVC

- Add Navigation from strings "redirect:" to controller endpoints.
- Enhanced MockMvc with better handling of multipart requests and parameter checks.
- Linemarkers for quick navigation to URLs defined in controller endpoints.
- Integrated OpenAPI spec navigation for both .yaml and .json formats.

### Spring Security

- Included reference checks for Spring beans within Spring Security annotations.

### Spring Data

- Detect JpaRepositories as beans.

### Other

- Switch to versioning 2024.{platformVersion}.{buildNumber}.
- Inspection for getResource method to ensure classpath resource path is correct.
- Inspections and tests to improve overall functionality.
- Automated Changelog introduced.
- Extended tests coverage for cases: bean inheritance, bean name navigation, bean as parameter.
- Added validate license panel in Settings/Tools/Esprito Spring Tools.
- Fix: Adjust inspection paths and keys.
- Fix: Set name Esprito Spring Tools in Settings/Tools.
