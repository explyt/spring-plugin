<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Esprito Spring Tools Changelog


## [Unreleased]

### Spring Core/Boot

#### Added

- inspection for methods annotated with `@Async`, `@Cacheable`, `@CachePut`, `@CacheEvict` work to prevent calls within the same class.
- bean scope options in autocomplete, including custom scopes.
- inspections for missing or problematic resource files in .properties and .yaml.

### Spring Web/MVC

#### Added

- Enhanced MockMvc with better handling of multipart requests and parameter checks.
- linemarkers for quick navigation to URLs defined in controller endpoints.
- Integrated OpenAPI spec navigation for both .yaml and .json formats.

### Spring Security

#### Added

- Included reference checks for Spring beans within Spring Security annotations.

### Other

#### Added

- inspection for getResource method to ensure classpath resource path is correct.
- New inspections and tests were added to improve overall functionality and to adjust inspection paths and keys.
- Automated Changelog introduced 