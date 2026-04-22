# Concise conventions summary for `SpringBootApplicationMcpToolset` tests

## Existing MCP/Spring AI test precedent
- **No MCP test classes found** in this repo.
- **No `modules/spring-ai/src/test` tree exists yet**.
- Closest precedent is **IntelliJ light-fixture integration tests** in neighboring Spring modules.

## Exact files to mirror most closely

### Primary pattern: service-style PSI/project integration tests
1. `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/java/PackageScanServiceTest.kt`
2. `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/kotlin/PackageScanServiceTest.kt`
3. `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/java/SpringSearchServiceTest.kt`
4. `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/kotlin/SpringSearchServiceTest.kt`

Why these are the best match:
- they load a small test project into IntelliJ fixture
- call real project services directly
- use explicit Spring test libraries
- avoid mocks
- assert returned model data

### Secondary pattern: endpoint-oriented Spring Web tests
5. `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/java/ControllerRunLineMarkerProviderTest.kt`
6. `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/kotlin/ControllerRunLineMarkerProviderTest.kt`
7. `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/java/SpringWebImplicitUsageProviderTest.kt`
8. `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/kotlin/SpringWebImplicitUsageProviderTest.kt`

Use these mainly for:
- Spring Web dependency mix
- Java/Kotlin parallel test layout
- file/testdata path conventions

### JPA / Spring Data examples for entity-oriented tools
9. `modules/spring-data/src/test/kotlin/com/explyt/spring/data/langinjection/java/SqlNativeSpringQueryLanguageInjectorTest.kt`
10. `modules/spring-data/src/test/kotlin/com/explyt/spring/data/langinjection/kotlin/SqlNativeSpringQueryLanguageInjectorTest.kt`
11. `modules/jpa/src/test/kotlin/com/explyt/jpa/ql/reference/java/JpqlExternalReferenceCompletionTest.kt`

Use these mainly for:
- explicit JPA/Spring Data test libraries
- inline `configureByText(...)` tests
- Java/Kotlin paired structure

## Framework / library conventions already used

### Build/runtime
- Root test setup: `build.gradle.kts`
  - `useJUnitPlatform()`
  - parallel test execution disabled
- Spring AI module: `modules/spring-ai/spring-ai.gradle.kts`
  - `testImplementation(project(":test-framework"))`
  - `testImplementation("junit:junit:4.13.2")`
  - `testRuntimeOnly("org.junit.vintage:junit-vintage-engine")`
  - `bundledPlugin("com.intellij.mcpServer")`
  - `plugin("com.explyt.test", "4.1.3-IJ-251")`

### Base test helpers
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytBaseLightTestCase.kt`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytJavaLightTestCase.kt`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytKotlinLightTestCase.kt`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytInspectionBaseTestCase.kt`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytInspectionJavaTestCase.kt`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytInspectionKotlinTestCase.kt`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/TestLibrary.kt`

### Mocking/assertion conventions
- **MockK:** not found
- **Mockito:** not found
- **AssertJ:** not found
- **Kotest:** not found
- Assertions are plain JUnit/TestCase style:
  - `assertEquals`, `assertTrue`, `assertNotNull`
  - `junit.framework.TestCase.assertEquals(...)`
  - `org.junit.Assert...`

## Naming / structure conventions to follow
- test classes end with `Test`
- fixture methods are usually `fun test...()`
- Java and Kotlin behavior is often split into separate test classes under parallel packages
- service tests often use:
  - `myFixture.copyDirectoryToProject(...)`
  - direct call to real project service/tool
- smaller edge-case tests often use:
  - `myFixture.configureByText(...)`
- inspections/providers may use `@TestMetadata`, but service-style tests often do not

## Recommended pattern for new `SpringBootApplicationMcpToolset` tests
- Create **light-fixture integration tests** in `modules/spring-ai/src/test/kotlin/...`
- Prefer the **service-test style** from `PackageScanServiceTest` / `SpringSearchServiceTest`
- Use **real fixture mini-projects**, not mocks
- Use `ExplytJavaLightTestCase` / `ExplytKotlinLightTestCase`
- Add only needed `TestLibrary` dependencies per test
- Invoke the toolset methods directly and assert returned serialized output
- Split Java/Kotlin tests only if the tool behavior differs by source language

## Tool groups and likely dependency mix
- Spring Boot app discovery / bean listing:
  - `TestLibrary.springBootAutoConfigure_3_1_1`
  - likely `TestLibrary.springBoot_3_1_1`
- Endpoint tools:
  - `TestLibrary.springContext_6_0_7`
  - `TestLibrary.springWeb_6_0_7`
  - `TestLibrary.springBoot_3_1_1`
  - `TestLibrary.springCloud_4_1_3` if Feign coverage is included
- JPA entity tool:
  - `TestLibrary.springDataJpa_3_4_0` or `TestLibrary.springDataJpa_3_1_0`
  - `TestLibrary.jakarta_persistence_3_1_0`

## Bottom-line recommendation
If only one pattern is copied, copy this one:
- `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/java/PackageScanServiceTest.kt`
- `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/kotlin/PackageScanServiceTest.kt`

These are the closest existing conventions to how `SpringBootApplicationMcpToolset` should be tested in this repo.
