<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Esprito Spring Tools Changelog


## [Unreleased]

### Spring Core/Boot

#### Added

- support ComponentScan to find spring beans
- inspection for methods annotated with `@Async`, `@Transactional`, `@Cacheable`, `@CachePut`, `@CacheEvict` work to prevent calls within the same class.
- bean scope options in autocomplete, including custom scopes.
- inspections for missing or problematic resource files in .properties and .yaml.
- line marker navigates from `getBean` method to bean declaration.

#### Fixed

- remove or comment Bean class action does not recover (return back) bean gutter icon

### Spring Web/MVC

#### Added

- Add Navigation from strings "redirect:" to controller endpoints
- Enhanced MockMvc with better handling of multipart requests and parameter checks.
- linemarkers for quick navigation to URLs defined in controller endpoints.
- Integrated OpenAPI spec navigation for both .yaml and .json formats.

### Spring Security

#### Added

- Included reference checks for Spring beans within Spring Security annotations.

### Other

#### Added

- inspection for getResource method to ensure classpath resource path is correct.
- inspections and tests to improve overall functionality 
- Automated Changelog introduced 
- Extended tests coverage for cases: bean inheritance, bean name navigation, bean as parameter

#### Fixed

- adjust inspection paths and keys
