# Existing test conventions table

## Core framework/helpers

| Purpose | Exact path | Notes |
|---|---|---|
| Base IntelliJ light fixture | `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytBaseLightTestCase.kt` | Extends `LightJavaCodeInsightFixtureTestCase`, JDK 21, supports `libraries: Array<TestLibrary>` |
| Java fixture base | `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytJavaLightTestCase.kt` | Testdata root `testdata/java/` |
| Kotlin fixture base | `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytKotlinLightTestCase.kt` | Testdata root `testdata/kotlin/` |
| Inspection base | `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytInspectionBaseTestCase.kt` | For inspection-based tests only |
| Java inspection base | `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytInspectionJavaTestCase.kt` | Testdata root `testdata/java/inspection/` |
| Kotlin inspection base | `modules/test-framework/src/main/kotlin/com/explyt/spring/test/ExplytInspectionKotlinTestCase.kt` | Testdata root `testdata/kotlin/inspection/` |
| Reusable Maven test deps | `modules/test-framework/src/main/kotlin/com/explyt/spring/test/TestLibrary.kt` | Spring Boot / Web / Data / JPA / Kotlin helper coordinates |

## Closest existing tests to mirror

| Exact path | Base class | Structure / why relevant | Libraries used |
|---|---|---|---|
| `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/java/PackageScanServiceTest.kt` | `ExplytJavaLightTestCase` | Best match: service-style integration test; uses `copyDirectoryToProject(...)`, direct service call, no mocks | `TestLibrary.springBootAutoConfigure_3_1_1` |
| `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/kotlin/PackageScanServiceTest.kt` | `ExplytKotlinLightTestCase` | Kotlin twin of above | `TestLibrary.springBootAutoConfigure_3_1_1` |
| `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/java/SpringSearchServiceTest.kt` | `ExplytJavaLightTestCase` | Direct PSI/project service assertions, fixture project loading | `TestLibrary.springBootAutoConfigure_3_1_1` |
| `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/kotlin/SpringSearchServiceTest.kt` | `ExplytKotlinLightTestCase` | Kotlin twin of above | `TestLibrary.springBootAutoConfigure_3_1_1` |
| `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/java/ControllerRunLineMarkerProviderTest.kt` | `ExplytJavaLightTestCase` | Endpoint-oriented file fixture pattern; `TEST_DATA_PATH`, `@TestMetadata`, `configureByFile(...)` | `springContext_6_0_7`, `springWeb_6_0_7`, `springBoot_3_1_1`, `springCloud_4_1_3` |
| `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/kotlin/ControllerRunLineMarkerProviderTest.kt` | `ExplytKotlinLightTestCase` | Kotlin twin of above | `springContext_6_0_7`, `springWeb_6_0_7`, `springBoot_3_1_1`, `springCloud_4_1_3` |
| `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/java/SpringWebImplicitUsageProviderTest.kt` | `ExplytInspectionJavaTestCase` | Inspection pattern only; useful if MCP tests ever need inspection-style fixture metadata | `springContext_6_0_7`, `springWeb_6_0_7`, `springGraphQl_1_0_4` |
| `modules/spring-web/src/test/kotlin/com/explyt/spring/web/providers/kotlin/SpringWebImplicitUsageProviderTest.kt` | `ExplytInspectionKotlinTestCase` | Kotlin twin of above | `springContext_6_0_7`, `springWeb_6_0_7`, `springGraphQl_1_0_4` |
| `modules/spring-data/src/test/kotlin/com/explyt/spring/data/langinjection/java/SqlNativeSpringQueryLanguageInjectorTest.kt` | `ExplytJavaLightTestCase` | Example of compact inline snippet tests using `configureByText(...)` | `springDataJpa_3_4_0` |
| `modules/spring-data/src/test/kotlin/com/explyt/spring/data/langinjection/kotlin/SqlNativeSpringQueryLanguageInjectorTest.kt` | `ExplytKotlinLightTestCase` | Kotlin twin of above | `springDataJpa_3_4_0`, `jakarta_persistence_3_1_0` |
| `modules/jpa/src/test/kotlin/com/explyt/jpa/ql/reference/java/JpqlExternalReferenceCompletionTest.kt` | `ExplytJavaLightTestCase` | Example of `TEST_DATA_PATH` + `@TestMetadata` + directory copy | `jakarta_persistence_3_1_0` or `javax_persistence_2_2` in subclasses |

## Build/test dependencies in target module

| Exact path | Relevant finding |
|---|---|
| `modules/spring-ai/spring-ai.gradle.kts` | `testImplementation(project(":test-framework"))` |
| `modules/spring-ai/spring-ai.gradle.kts` | `testImplementation("junit:junit:4.13.2")` |
| `modules/spring-ai/spring-ai.gradle.kts` | `testRuntimeOnly("org.junit.vintage:junit-vintage-engine")` |
| `modules/spring-ai/spring-ai.gradle.kts` | `bundledPlugin("com.intellij.mcpServer")` |
| `modules/spring-ai/spring-ai.gradle.kts` | `plugin("com.explyt.test", "4.1.3-IJ-251")` |
| `build.gradle.kts` | `useJUnitPlatform()` and no test parallelism |

## Mocking / assertions

- Mocking frameworks used in nearby module tests: **none found** (`mockk`, `Mockito`, `mockito` searches empty)
- Assertion style found:
  - JUnit/TestCase assertions (`assertEquals`, `assertTrue`, `assertNotNull`)
  - `junit.framework.TestCase.assert...`
  - occasional `org.junit.Assert...`

## Recommended pattern to mirror

**Mirror the `spring-core` service tests first**:
- `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/java/PackageScanServiceTest.kt`
- `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/kotlin/PackageScanServiceTest.kt`
- `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/java/SpringSearchServiceTest.kt`
- `modules/spring-core/src/test/kotlin/com/explyt/spring/core/service/kotlin/SpringSearchServiceTest.kt`

Recommended style for `SpringBootApplicationMcpToolset`:
1. add new tests under `modules/spring-ai/src/test/kotlin/...`
2. use `ExplytJavaLightTestCase` / `ExplytKotlinLightTestCase`
3. stage realistic mini-projects with `myFixture.copyDirectoryToProject(...)`
4. use `configureByText(...)` only for very small edge cases
5. avoid mocks; call real toolset methods/services
6. assert returned serialized data with plain JUnit/TestCase assertions
