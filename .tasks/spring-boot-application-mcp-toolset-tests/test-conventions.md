# SpringBootApplicationMcpToolset test-convention discovery

## Direct findings

- **No existing MCP or Spring AI tests were found** under `modules/spring-ai/src/test`. The module currently has `src/main` only.
- `SpringBootApplicationMcpToolset` lives in:
  - `modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/mcp/SpringMcpProvider.kt`
- The toolset exposes 7 suspend tools:
  - `getAllSpringBootApplications`
  - `applicationBeans`
  - `findEndpoint`
  - `getHttpEndpoints`
  - `getEndpointContract`
  - `traceCallChain`
  - `getSpringDataEntities`

## Framework / runtime conventions

### Build + test runtime
- Root test runtime config: `build.gradle.kts`
  - `useJUnitPlatform()`
  - JUnit parallel disabled for IntelliJ Platform tests
- Spring AI module test deps: `modules/spring-ai/spring-ai.gradle.kts`
  - `testImplementation(project(":test-framework"))`
  - `testImplementation("junit:junit:4.13.2")`
  - `testRuntimeOnly("org.junit.vintage:junit-vintage-engine")`
  - `bundledPlugin("com.intellij.mcpServer")`
  - `plugin("com.explyt.test", "4.1.3-IJ-251")`

### Base test helpers to reuse
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytBaseLightTestCase.kt`
  - base IntelliJ light-fixture class (`LightJavaCodeInsightFixtureTestCase`)
  - JDK 21
  - supports Maven test libraries via `libraries`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytJavaLightTestCase.kt`
  - Java-oriented fixture tests, root testdata path `testdata/java/`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytKotlinLightTestCase.kt`
  - Kotlin-oriented fixture tests, root testdata path `testdata/kotlin/`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytInspectionBaseTestCase.kt`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytInspectionJavaTestCase.kt`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytInspectionKotlinTestCase.kt`
- `modules/test-framework/src/main/kotlin/com/explyt/spring/test/TestLibrary.kt`
  - reusable Maven coordinates for Spring Boot / Spring Web / Spring Data / JPA / Kotlin libs

## Nearby tests that best match SpringBootApplicationMcpToolset

### Best structural match: service-style light-fixture tests
These directly load a miniature project into the IntelliJ fixture and call project services.

1. `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/java/PackageScanServiceTest.kt`
2. `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/kotlin/PackageScanServiceTest.kt`
3. `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/java/SpringSearchServiceTest.kt`
4. `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/kotlin/SpringSearchServiceTest.kt`

Observed pattern:
- extend `ExplytJavaLightTestCase` / `ExplytKotlinLightTestCase`
- override `libraries` with only needed `TestLibrary` entries
- sometimes use `setUp` / `tearDown` to manage registry flags
- stage sample project via `myFixture.copyDirectoryToProject("service/...", "")`
- call real project service directly
- assert with JUnit/TestCase assertions (`assertEquals`, `assertTrue`, `TestCase.assertEquals`, etc.)
- no mocking

### Endpoint/provider-style fixtures
Useful for endpoint-oriented MCP tools because they show the Spring Web library mix and file-based testdata layout.

5. `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/java/ControllerRunLineMarkerProviderTest.kt`
6. `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/kotlin/ControllerRunLineMarkerProviderTest.kt`
7. `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/java/SpringWebImplicitUsageProviderTest.kt`
8. `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/kotlin/SpringWebImplicitUsageProviderTest.kt`

Observed pattern:
- use `private const val TEST_DATA_PATH = ...`
- often annotate class/methods with `@TestMetadata`
- override `getTestDataPath()` to append module-specific folder
- use `myFixture.configureByFile(...)` or inspection `doTest(...)`
- `libraries` typically include:
  - `TestLibrary.springContext_6_0_7`
  - `TestLibrary.springWeb_6_0_7`
  - `TestLibrary.springBoot_3_1_1`
  - sometimes `TestLibrary.springCloud_4_1_3`

### JPA / Spring Data fixture examples for entity-oriented MCP tools
9. `modules/spring-data/src/test/kotlin/com/explyt/spring/data/langinjection/java/SqlNativeSpringQueryLanguageInjectorTest.kt`
10. `modules/spring-data/src/test/kotlin/com/explyt/spring/data/langinjection/kotlin/SqlNativeSpringQueryLanguageInjectorTest.kt`
11. `modules/jpa/src/test/kotlin/com/explyt/jpa/ql/reference/java/JpqlExternalReferenceCompletionTest.kt`

Observed pattern:
- extend `ExplytJavaLightTestCase` / `ExplytKotlinLightTestCase`
- inline source snippets via `myFixture.configureByText(...)` when full directories are unnecessary
- library selection is explicit and minimal
- Java/Kotlin test pairs are common when language behavior matters

### Misc helper / alternate base examples
12. `modules/spring-core/src/test/kotlin/com/explyt/spring/core/providers/PropertyLineMarkerProviderTest.kt`
13. `modules/spring-core/src/test/kotlin/com/explyt/spring/core/externalsystem/process/SpringBootOpenProjectProviderTest.kt`
14. `modules/spring-web/src/test/kotlin/com/explyt/spring/web/TestUtil.kt`

## Mocking / assertion conventions

- **No MockK found** in module tests or Gradle files searched.
- **No Mockito found** in module tests or Gradle files searched.
- **No AssertJ / Kotest found**.
- Assertions are standard JUnit / TestCase style:
  - inherited `assertEquals`, `assertTrue`, `assertNotNull`
  - `junit.framework.TestCase.assertEquals(...)`
  - `org.junit.Assert...` in some older tests
- Some pure unit tests use `@Test`, but IntelliJ fixture tests usually rely on method names starting with `test...` without annotation.

## Naming / layout conventions

- Class names end in `Test`
- Fixture test methods usually start with `test...`
- Java and Kotlin variants are often split into parallel packages:
  - `.../java/...Test.kt`
  - `.../kotlin/...Test.kt`
- File-based fixture tests often define `TEST_DATA_PATH` and override `getTestDataPath()`
- Service-style tests often skip `@TestMetadata` and directly use `copyDirectoryToProject(...)`

## Recommended pattern to mirror for SpringBootApplicationMcpToolset

### Recommended base pattern
Mirror the **spring-core service tests** first, especially:
- `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/java/PackageScanServiceTest.kt`
- `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/kotlin/PackageScanServiceTest.kt`
- `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/java/SpringSearchServiceTest.kt`
- `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/kotlin/SpringSearchServiceTest.kt`

Why this is the closest fit:
- `SpringBootApplicationMcpToolset` is a **service/query toolset**, not an inspection or gutter provider
- its methods query the IntelliJ PSI/project model and return serialized data
- existing service tests already use fixture-loaded mini-projects and direct service calls without mocks

### Practical recommendation
- Create new tests under `modules/spring-ai/src/test/kotlin/...`
- Prefer `ExplytJavaLightTestCase` / `ExplytKotlinLightTestCase`
- Use **real fixture projects**, not mocks
- Use `myFixture.copyDirectoryToProject(...)` for realistic scenarios spanning multiple files
- Use `myFixture.configureByText(...)` only for compact one-file edge cases
- Keep dependencies explicit via `override val libraries`
- Follow JUnit 4 / IntelliJ fixture method naming (`fun test...()`)
- If tool behavior is language-sensitive, create **parallel Java and Kotlin test classes**

### Library combinations likely needed by tool group
- Spring Boot application discovery / beans:
  - `TestLibrary.springBootAutoConfigure_3_1_1`
  - likely `TestLibrary.springBoot_3_1_1`
- Endpoint tools:
  - `TestLibrary.springContext_6_0_7`
  - `TestLibrary.springWeb_6_0_7`
  - `TestLibrary.springBoot_3_1_1`
  - `TestLibrary.springCloud_4_1_3` when Feign coverage is needed
- Entity tool:
  - `TestLibrary.springDataJpa_3_4_0` or `TestLibrary.springDataJpa_3_1_0`
  - `TestLibrary.jakarta_persistence_3_1_0` (or `javax_persistence_2_2` for compatibility cases)

## Gaps / no precedent found

- No existing `SpringBootApplicationMcpToolset` tests
- No existing tests for any `McpToolset`
- No existing MCP-specific assertion helpers
- No coroutine-test convention found in tests (`runBlocking` / `runTest` not present)
- No existing JSON parsing/assertion convention found in tests

## Bottom line

For new `SpringBootApplicationMcpToolset` tests, the project’s strongest convention is:
**IntelliJ light-fixture integration tests + real mini-project testdata + explicit `TestLibrary` deps + JUnit 4/TestCase assertions + no mocks**.

The closest files to imitate are the `PackageScanServiceTest` / `SpringSearchServiceTest` pairs, with endpoint/entity-specific library setup borrowed from the Spring Web and Spring Data tests listed above.
