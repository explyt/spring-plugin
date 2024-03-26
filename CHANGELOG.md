<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Esprito Spring Tools Changelog


## [Unreleased]

### Spring Data

#### Fixed

- Spring Data methods name inspection (Repository: findAll property is unknown)

### Spring Core/Boot

- Detect `context.register` as context root

## [2024.233.1020] - 2024-03-22

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
