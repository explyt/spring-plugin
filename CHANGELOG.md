<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Explyt Spring Changelog


## [Unreleased]

### Spring Core/Boot

- Fixed: wrong bean method navigation
- Fixed: Don't warn about kebab case for property values
- Fixed: profile change (#6)

### Other

- Prepare code configurations for open source

## [242.1.2334] - 2024-10-29

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
