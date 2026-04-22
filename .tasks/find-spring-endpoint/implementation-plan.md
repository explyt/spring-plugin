# Implementation Plan: explyt_find_spring_endpoint

## Single file to edit
`modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/mcp/SpringMcpProvider.kt`

No XML changes needed — the toolset class `SpringBootApplicationMcpToolset` is already registered.

## What to add

### 1. New MCP tool method in `SpringBootApplicationMcpToolset`

```kotlin
@McpTool("explyt_find_spring_endpoint")
suspend fun findEndpoint(urlPattern, projectPath, httpMethod?): String
```

### 2. Response DTOs

```kotlin
data class EndpointJson(...)
data class EndpointParameterJson(...)
```

## Key APIs to use (all verified to exist)

- `SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()` — returns all `EndpointElement` across all modules
- `SpringWebUtil.simplifyUrl(path)` — normalizes URLs
- `SpringWebUtil.isEndpointMatches(endpoint, path)` — regex match with `{param}` support
- `SpringWebUtil.collectRequestParameters(psiMethod)` — `@RequestParam` params
- `SpringWebUtil.collectPathVariables(psiMethod)` — `@PathVariable` params
- `SpringWebUtil.getRequestBodyInfo(psiMethod)` — `@RequestBody` param
- `psiElement.getLineNumber(start=true)` from `org.jetbrains.kotlin.idea.base.psi.getLineNumber`
- `psiElement.containingFile?.virtualFile?.path` — file path

## URL matching strategy

Both substring and pattern match (covers partial URLs and full paths with different variable names):
- `simplifyUrl(endpoint.path).contains(normalizedPattern)` — substring
- `isEndpointMatches(endpoint.path, normalizedPattern)` — regex with `{param}` wildcards

## Relative file paths

Strip `project.basePath + "/"` prefix to get project-relative paths.
