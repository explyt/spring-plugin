# Spring-Web Dependencies & Parameter Extraction Patterns

## 1. spring-ai already depends on spring-web

**File:** `modules/spring-ai/spring-ai.gradle.kts`

```kotlin
// Line 27
evaluationDependsOn(":spring-web")

// Line 36
val springWebProject = project(":spring-web")

// Line 59
implementation(springWebProject)
```

**Conclusion:** `spring-ai` already has a compile dependency on `spring-web`. You can directly use classes from `spring-web` (e.g., `SpringWebEndpointsSearcher`, `SpringWebUtil`, `SpringWebClasses`).

---

## 2. Parameter Extraction Methods in SpringWebUtil

**File:** `modules/spring-web/src/main/kotlin/com/explyt/spring/web/util/SpringWebUtil.kt` (795 lines)

### `collectRequestParameters(psiMethod)` — Lines 129–183
Extracts `@RequestParam` annotated parameters:
- Filters method params by `SpringWebClasses.REQUEST_PARAM` meta-annotation
- Uses `SpringSearchService.getMetaAnnotations()` for meta-annotation resolution
- Extracts: name (from `value`/`name` attr), `required`, `defaultValue`, type FQN, isMap, isOptional
- Returns `Collection<PathArgumentInfo>`

### `collectPathVariables(psiMethod)` — Lines 243–296
Extracts `@PathVariable` annotated parameters:
- Same pattern as `collectRequestParameters` but for `SpringWebClasses.PATH_VARIABLE`
- Extracts: name, `required`, type FQN, isMap, isOptional
- Returns `Collection<PathArgumentInfo>`

### `getRequestBodyInfo(psiMethod)` — Lines 298–332
Extracts `@RequestBody` annotated parameter:
- Filters by `SpringWebClasses.REQUEST_BODY`
- Returns first match as `PathArgumentInfo?`

### `getEndpointInfo(uMethod, prefix)` — Lines 328–376
**The high-level method that aggregates everything:**
```kotlin
fun getEndpointInfo(uMethod: UMethod, prefix: String = ""): EndpointInfo? {
    // ...validates @RequestMapping, gets path, methods, produces, consumes...
    return EndpointInfo(
        fullPath,
        requestMethods,
        psiMethod,
        uMethod.name,
        controllerName,
        description,
        returnTypeFqn,
        collectPathVariables(psiMethod),      // line 368
        collectRequestParameters(psiMethod),   // line 369
        getRequestBodyInfo(psiMethod),         // line 370
        collectRequestHeaders(psiMethod),      // line 371
        produces,
        consumes
    )
}
```

### `PathArgumentInfo` data class — Lines 699–706
```kotlin
data class PathArgumentInfo(
    val name: String,
    val psiElement: PsiElement,
    val isRequired: Boolean,
    val isMap: Boolean,
    val typeFqn: String,
    val defaultValue: String? = null
)
```

### `EndpointInfo` data class — `AddEndpointToOpenApiIntention.kt:220–234`
**File:** `modules/spring-web/src/main/kotlin/com/explyt/spring/web/inspections/quickfix/AddEndpointToOpenApiIntention.kt`
```kotlin
data class EndpointInfo(
    val path: String,
    val requestMethods: List<String>,
    val psiElement: PsiElement,
    val methodName: String,
    val tag: String,
    val description: String,
    val returnTypeFqn: String,
    val pathVariables: Collection<SpringWebUtil.PathArgumentInfo> = emptyList(),
    val requestParameters: Collection<SpringWebUtil.PathArgumentInfo> = emptyList(),
    val requestBodyInfo: SpringWebUtil.PathArgumentInfo? = null,
    val requestHeaders: Collection<SpringWebUtil.PathArgumentInfo> = emptyList(),
    val produces: Collection<String> = emptyList(),
    val consumes: Collection<String> = emptyList(),
)
```

---

## 3. SpringWebClasses — Annotation FQN Constants

**File:** `modules/spring-web/src/main/kotlin/com/explyt/spring/web/SpringWebClasses.kt` (92 lines)

Key constants:
```kotlin
// Line 22: const val MODEL_ATTRIBUTE = "org.springframework.web.bind.annotation.ModelAttribute"
// Line 26: const val CONTROLLER = "org.springframework.stereotype.Controller"
// Line 27: const val RESPONSE_BODY = "org.springframework.web.bind.annotation.ResponseBody"
// Line 28: const val REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping"
// Line 29: const val PATH_VARIABLE = "org.springframework.web.bind.annotation.PathVariable"
// Line 30: const val REQUEST_PARAM = "org.springframework.web.bind.annotation.RequestParam"
// Line 31: const val REQUEST_HEADER = "org.springframework.web.bind.annotation.RequestHeader"
// Line 32: const val COOKIE_VALUE = "org.springframework.web.bind.annotation.CookieValue"
// Line 33: const val REQUEST_BODY = "org.springframework.web.bind.annotation.RequestBody"
// Line 37: const val HTTP_EXCHANGE = "org.springframework.web.service.annotation.HttpExchange"
```

---

## 4. Getting File Path and Line Number from PsiElement

### Pattern: `getLineNumber()` from Kotlin PSI utils

**File:** `modules/spring-web/src/main/kotlin/com/explyt/spring/web/providers/HttpRunLineMarkerProvider.kt`
```kotlin
// Line 33: import
import org.jetbrains.kotlin.idea.base.psi.getLineNumber

// Line 53: usage
val line = httpRequest.getLineNumber(true)  // true = 1-based
```

**File:** `modules/spring-core/src/main/kotlin/com/explyt/spring/core/providers/SpringBeanLineMarkerProvider.kt`
```kotlin
// Line 60: import
import org.jetbrains.kotlin.idea.base.psi.getLineNumber

// Line 378: usage — combining file name + line number
return methodName ?: (file?.name + ":" + element.getLineNumber())
```

### Pattern: `containingFile.virtualFile` for file path

**File:** `modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/action/ConvertDtoToEntityAction.kt`
```kotlin
// Line 67
val virtualFiles = dtoPsiClasses.mapNotNull { it.javaPsi.containingFile?.virtualFile }
```

**File:** `HttpRunLineMarkerProvider.kt:52`
```kotlin
val file = leafElement.containingFile.virtualFile ?: return null
```

### Recommended pattern for your MCP tool:
```kotlin
import org.jetbrains.kotlin.idea.base.psi.getLineNumber

// Get file path:
val filePath = psiElement.containingFile?.virtualFile?.path

// Get line number (1-based):
val lineNumber = psiElement.getLineNumber(true) + 1  // getLineNumber(true) is 0-based when param is `true` for start

// Or using PsiDocumentManager:
val document = PsiDocumentManager.getInstance(project).getDocument(psiElement.containingFile)
val lineNumber = document?.getLineNumber(psiElement.textOffset)?.plus(1)
```

**Note:** No dedicated utility exists in the MCP package for this — the existing code uses the Kotlin PSI `getLineNumber()` extension directly inline.

---

## 5. SpringWebEndpointsSearcher — Service Access

**File:** `modules/spring-web/src/main/kotlin/com/explyt/spring/web/service/SpringWebEndpointsSearcher.kt` (102 lines)

```kotlin
@Service(Service.Level.PROJECT)
class SpringWebEndpointsSearcher(private val project: Project) {
    companion object {
        fun getInstance(project: Project): SpringWebEndpointsSearcher = project.service()
    }

    // Get all endpoints for a module (filtered by type)
    fun getAllEndpoints(module: Module, types: List<EndpointType> = emptyList()): List<EndpointElement>

    // Get all endpoints across project
    fun getAllEndpoints(): List<EndpointElement>

    // Get endpoints matching a URL path
    fun getAllEndpointElements(urlPath: String, module: Module, types: List<EndpointType> = emptyList()): List<EndpointElement>
}
```

### Usage pattern from existing code (`SpringWebProjectOpenApiGenerateAction.kt:49`):
```kotlin
SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module, listOf(EndpointType.SPRING_MVC))
```

---

## Summary for MCP Tool Implementation

1. **Dependency:** Already resolved — `spring-ai` depends on `spring-web`
2. **Endpoint discovery:** Use `SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)`
3. **Parameter extraction:** Use `SpringWebUtil.getEndpointInfo(uMethod, prefix)` which returns `EndpointInfo` with all params pre-extracted, OR use individual methods: `collectPathVariables()`, `collectRequestParameters()`, `getRequestBodyInfo()`
4. **Annotation FQNs:** All in `SpringWebClasses` object
5. **File/line from PsiElement:** `psiElement.containingFile?.virtualFile?.path` + `psiElement.getLineNumber(true)` (import from `org.jetbrains.kotlin.idea.base.psi`)
