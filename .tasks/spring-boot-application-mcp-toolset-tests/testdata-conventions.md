# Testdata conventions relevant for SpringBootApplicationMcpToolset tests

## Where testdata lives in neighboring modules

Observed module-local `testdata/` layout:

- `modules/spring-core/testdata/...`
- `modules/spring-web/testdata/...`
- `modules/spring-data/testdata/...`
- `modules/jpa/testdata/...`

This matches the `@TestDataPath("$CONTENT_ROOT/../../...")` setup in the shared fixture base classes.

## Important implication
For new `spring-ai` tests, the expected convention is to create testdata under:

- `modules/spring-ai/testdata/java/...`
- `modules/spring-ai/testdata/kotlin/...`

not at repository root.

## Concrete neighboring layouts

### spring-core
From `modules/spring-core`:
- `testdata/java/service/...`
- `testdata/kotlin/service/...`
- `testdata/java/providers/...`
- `testdata/kotlin/providers/...`
- `testdata/java/inspection/...`
- `testdata/kotlin/inspection/...`

### spring-web
From `modules/spring-web`:
- `testdata/java/completion/...`
- `testdata/java/inspection/...`
- `testdata/java/loader/...`
- `testdata/java/providers/...`
- `testdata/kotlin/completion/...`
- `testdata/kotlin/inspection/...`
- `testdata/kotlin/loader/...`
- `testdata/kotlin/providers/...`

### spring-data
From `modules/spring-data`:
- `testdata/java/inspection/...`
- `testdata/java/langinjection/...`
- `testdata/java/reference/...`
- `testdata/java/service/...`
- `testdata/kotlin/inspection/...`
- `testdata/kotlin/langinjection/...`
- `testdata/kotlin/reference/...`
- `testdata/kotlin/service/...`

### jpa
From `modules/jpa`:
- `testdata/java/inspection/...`
- `testdata/java/reference/...`
- `testdata/kotlin/inspection/...`
- `testdata/kotlin/reference/...`
- `testdata/psi/...`

## Best matching testdata pattern for MCP toolset tests

Closest precedent is the service testdata used by spring-core:
- `modules/spring-core/testdata/java/service/...`
- `modules/spring-core/testdata/kotlin/service/...`

Examples referenced by tests:
- `service/packageScan`
- `service/packageScanAndSpringBootApp`
- `service/importComponent`
- `service/importComplexWithComponentScan`
- `service/conditionalOnMissingBean`

These are loaded using:
- `myFixture.copyDirectoryToProject("service/...", "")`

## Recommendation for new Spring AI tests
Use module-local directories such as:
- `modules/spring-ai/testdata/java/service/...`
- `modules/spring-ai/testdata/kotlin/service/...`

If tests are organized by tool instead of generic service bucket, a repo-consistent variant would still be:
- `modules/spring-ai/testdata/java/service/mcp/...`
- `modules/spring-ai/testdata/kotlin/service/mcp/...`

or flat per feature, e.g.:
- `modules/spring-ai/testdata/java/service/springBootApplications/...`
- `modules/spring-ai/testdata/java/service/endpoints/...`
- `modules/spring-ai/testdata/java/service/entities/...`

But the strongest convention remains:
- **module-local `testdata/java/...` and `testdata/kotlin/...`**
- **service-style scenarios under `service/...`** when testing PSI/project queries.

## JSON / parsing convention status
No existing neighboring tests were found using Jackson `ObjectMapper`, `JsonNode`, or `readTree(...)` in test code.

Implication:
- there is **no established JSON assertion helper pattern** for the MCP toolset yet
- any new JSON parsing/assertion style should stay minimal and pragmatic
- if possible, prefer simple assertions on deserialized fields or stable string checks rather than introducing a new assertion framework

## Coroutine convention status
No existing test usage found for:
- `runBlocking`
- `kotlinx-coroutines-test`

Implication:
- suspend MCP tool methods will likely require introducing a direct coroutine bridge inside tests
- there is **no existing repo-wide coroutine testing convention** nearby to copy
- keep it minimal and local if added
