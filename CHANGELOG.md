<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Esprito Spring Tools Changelog


## [Unreleased]

### Spring Core/Boot

- Added autocompletion in properties files for all classes fields
- Inspection `unresolved property key` supports Relaxed Binding
- Fixed inspection for `@Retention` on Spring annotations. Supported meta-annotations
- Fixed stackoverflow error while property calculation on some cases
- Support @Scheduled annotation

## [241.1.1263] - 2024-04-17

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
