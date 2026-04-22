# Research: Call Chain Tracing APIs & Spring Stereotype Detection

## 1. Method Call Resolution (Finding callees from a PsiMethod body)

### Best Pattern: UAST-based traversal (used in the project)

The project already has an excellent pattern for finding method calls within a method body. The key example is:

**`modules/spring-core/src/main/kotlin/com/explyt/spring/core/inspections/SpringConfigurationProxyBeanMethodsInspection.kt:109-119`**
```kotlin
private fun findCallsToLocalBeans(psiMethod: PsiMethod, surroundingClass: PsiClass): List<UCallExpression> {
    val beanMethods = surroundingClass.methods
        .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
        .toSet()

    return psiMethod.toSourcePsi()?.findChildrenOfType<PsiElement>()?.asSequence()
        ?.mapNotNull { it.toUElement() as? UCallExpression }
        ?.filter { beanMethods.contains(it.resolve()) }
        ?.toList()
        ?: emptyList()
}
```

**Key building blocks:**
- `psiMethod.toSourcePsi()` — converts PsiMethod to its source PSI element (defined in `ExplytPsiUtil.kt:235`: `this?.let { it.toUElement()?.sourcePsi }`)
- `findChildrenOfType<PsiElement>()` — wrapper around `PsiTreeUtil.findChildrenOfType()` (defined in `ExplytPsiUtil.kt:166-168`)
- `it.toUElement() as? UCallExpression` — converts each child PSI to UAST and filters for call expressions
- `it.resolve()` — resolves the `UCallExpression` back to a `PsiMethod`

### Alternative: Visitor pattern (used for inspections)

**`modules/spring-core/src/main/kotlin/com/explyt/spring/core/inspections/CallBeanMethodFromSameClassInspection.kt:39-59`**
```kotlin
private class CallExpressionVisitor(private val holder: ProblemsHolder) : AbstractUastNonRecursiveVisitor() {
    override fun visitCallExpression(node: UCallExpression): Boolean {
        checkCallExpression(node)
        return true
    }
    private fun checkCallExpression(node: UCallExpression) {
        if (node.kind != UastCallKind.METHOD_CALL) return
        val containingUClass = node.getContainingUClass() ?: return
        val sourcePsi = node.sourcePsi ?: return
        val psiMethod = node.resolve() ?: return
        // ...
    }
}
```

### Another approach: PsiMethodCallExpression + resolveMethod() (Java-only)

**`modules/spring-web/src/main/kotlin/com/explyt/spring/web/loader/SpringWebRouterFunctionLoader.kt:56`**
```kotlin
val methods = methodCallException.resolveMethod() ?: return emptyList()
```

### Recommended approach for the MCP tool

Use the UAST-based pattern (approach #1). It works for both Java and Kotlin. To get all method calls from a `PsiMethod`:

```kotlin
fun findCalledMethods(psiMethod: PsiMethod): List<PsiMethod> {
    return psiMethod.toSourcePsi()
        ?.findChildrenOfType<PsiElement>()
        ?.asSequence()
        ?.mapNotNull { it.toUElement() as? UCallExpression }
        ?.mapNotNull { it.resolve() }
        ?.distinct()
        ?.toList()
        ?: emptyList()
}
```

### No CallHierarchyProvider usage found

The project does not use `CallHierarchyProvider`, `CalleeTreeStructure`, or `JavaCallHierarchyProvider`. The UAST-based approach above is the project's standard pattern.

---

## 2. Spring Stereotype Detection

### FQN Constants

**`modules/spring-core/src/main/kotlin/com/explyt/spring/core/SpringCoreClasses.kt`** (full file, 158 lines):

Key stereotype constants:
```kotlin
const val COMPONENT = "org.springframework.stereotype.Component"          // line 69
const val SERVICE = "org.springframework.stereotype.Service"              // line 70
const val CONTROLLER = "org.springframework.stereotype.Controller"        // line 71
const val REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController"  // line 72
const val REPOSITORY = "org.springframework.stereotype.Repository"        // line 73
```

Also in **`modules/spring-web/src/main/kotlin/com/explyt/spring/web/SpringWebClasses.kt:27`**:
```kotlin
const val CONTROLLER = "org.springframework.stereotype.Controller"
```

### isMetaAnnotatedBy pattern

**`modules/base/src/main/kotlin/com/explyt/util/ExplytPsiUtil.kt:76-80`**:
```kotlin
fun PsiModifierListOwner.isMetaAnnotatedBy(annotations: Collection<String>) =
    MetaAnnotationUtil.isMetaAnnotated(this, annotations)

fun PsiModifierListOwner.isMetaAnnotatedBy(annotation: String) =
    MetaAnnotationUtil.isMetaAnnotated(this, listOf(annotation))
```

Import: `import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy`

### Complete bean type detection pattern

**`modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/mcp/McpBeanSearchService.kt:66-92`** — already implements Spring stereotype detection:
```kotlin
private fun getBeanType(psiClass: PsiClass, messageMappingClasses: Collection<PsiClass> = emptyList()): McpBeanTypes {
    return if (messageMappingClasses.contains(psiClass)) {
        McpBeanTypes.MESSAGE_MAPPING
    } else if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONTROLLER)) {
        McpBeanTypes.CONTROLLER
    } else if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.REPOSITORY) || ...) {
        McpBeanTypes.REPOSITORY
    } else {
        McpBeanTypes.COMPONENT
    }
}
```

**Note:** `McpBeanTypes` enum currently lacks `SERVICE`. It has: `ASPECT, MESSAGE_MAPPING, CONTROLLER, AUTO_CONFIGURATION, CONFIGURATION_PROPERTIES, CONFIGURATION, REPOSITORY, COMPONENT`.

### For call chain tracing, detect layer with:
```kotlin
fun getSpringLayer(psiClass: PsiClass): String? = when {
    psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONTROLLER) -> "Controller"
    psiClass.isMetaAnnotatedBy(SpringCoreClasses.REST_CONTROLLER) -> "Controller"
    psiClass.isMetaAnnotatedBy(SpringCoreClasses.SERVICE) -> "Service"
    psiClass.isMetaAnnotatedBy(SpringCoreClasses.REPOSITORY) -> "Repository"
    psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT) -> "Component"
    psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION) -> "Configuration"
    else -> null
}
```

### MetaAnnotationsHolder (for attribute-level analysis)

**`modules/spring-core/src/main/kotlin/com/explyt/spring/core/service/MetaAnnotationsHolder.kt:34-232`**

This is a more advanced utility for traversing meta-annotation hierarchies and extracting attribute values. The `of(module, parentFqn)` factory method (line 165) builds a holder that knows about all annotations that are meta-annotated by the given parent. For simple stereotype detection, `isMetaAnnotatedBy` is sufficient.

---

## 3. Finding References to a PsiMethod (including test scope)

### MethodReferencesSearch

**`modules/spring-core/src/main/kotlin/com/explyt/spring/core/service/SpringSearchService.kt:727-733`**:
```kotlin
fun searchReferenceByMethod(
    module: Module,
    method: PsiMethod,
    scope: SearchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
): Collection<PsiReference> {
    return MethodReferencesSearch.search(method, GlobalSearchScopeTestAware.getScope(module, scope), true)
        .findAll()
}
```

### ReferencesSearch (generic)

**`modules/spring-core/src/main/kotlin/com/explyt/spring/core/service/SpringSearchService.kt:754-762`**:
```kotlin
fun getAllReferencesToElement(element: PsiElement): Set<PsiReference> {
    val project = element.project
    return CachedValuesManager.getManager(project).getCachedValue(element) {
        CachedValueProvider.Result(
            ReferencesSearch.search(element).toSet(),
            ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
        )
    }
}
```

### Test scope handling

**`modules/spring-core/src/main/kotlin/com/explyt/spring/core/util/GlobalSearchScopeTestAware.kt:25-30`**:
```kotlin
object GlobalSearchScopeTestAware {
    fun getScope(module: Module, scope: SearchScope): SearchScope {
        return if (ApplicationManager.getApplication().isUnitTestMode)
            GlobalSearchScope.allScope(module.project) else scope
    }
}
```

### Getting a test-scoped search scope from a Module

IntelliJ API provides:
```kotlin
// Test sources only:
GlobalSearchScope.moduleTestsWithDependenciesScope(module)

// All module content including tests:
module.moduleWithDependenciesAndLibrariesScope
```

The project does NOT currently use `moduleTestsWithDependenciesScope` (search returned no results), but it's the standard IntelliJ API for this purpose.

### To search for usages of a method only in test files:
```kotlin
val testScope = GlobalSearchScope.moduleTestsWithDependenciesScope(module)
MethodReferencesSearch.search(psiMethod, testScope, true).findAll()
```

---

## 4. Resolving Injected Bean Types

### McpBeanSearchService pattern

**`modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/mcp/McpBeanSearchService.kt`** (full 109 lines) — uses `SpringSearchService.getProjectBeans(module)` to get all beans, then maps them to their PsiClass types.

### Getting all project beans

**`modules/spring-core/src/main/kotlin/com/explyt/spring/core/service/SpringSearchService.kt:81-90`**:
```kotlin
private fun searchAllBeanClasses(module: Module): Set<PsiBean> {
    val allPsiClassesAnnotatedByComponent = getBeanPsiClassesAnnotatedByComponent(module)
    val methodsAnnotatedByBeanReturnTypes = searchComponentPsiClassesByBeanMethods(module)
    val staticBeans = getStaticBeans(module)
    return allPsiClassesAnnotatedByComponent + methodsAnnotatedByBeanReturnTypes + staticBeans
}
```

### Resolving constructor-injected field types

When a controller has `private val service: MyService`, the type `MyService` is already a concrete class reference in the PSI tree. To resolve it:

1. Get the `PsiMethod` being called (from `UCallExpression.resolve()`)
2. Get `psiMethod.containingClass` — this gives you the concrete class the method belongs to
3. Check its stereotype with `isMetaAnnotatedBy`

For deeper resolution (interface → implementation), the project uses:
- `ClassInheritorsSearch` (imported in `SpringSearchService.kt:64`)
- `SpringSearchService.getProjectBeans(module)` — returns all beans including their `PsiClass`

### Pattern to resolve bean type from a field:
```kotlin
// From a PsiField or parameter type:
val fieldType: PsiType = field.type
val psiClass = (fieldType as? PsiClassType)?.resolve()
// psiClass is the declared type (e.g., MyService interface)

// To find concrete implementations that are Spring beans:
val beans = SpringSearchService.getInstance(project).getProjectBeans(module)
val implementations = beans.filter { bean ->
    psiClass != null && InheritanceUtil.isInheritor(bean.psiClass, psiClass.qualifiedName)
}
```

---

## 5. PsiElement to File/Line

### Confirmed pattern from SpringMcpProvider

**`modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/mcp/SpringMcpProvider.kt:190-198`**:
```kotlin
val basePath = project.basePath?.let { "$it/" }
val filePath = (endpoint.containingFile ?: endpoint.psiElement.containingFile)?.virtualFile?.path
val relativePath = if (basePath != null && filePath != null && filePath.startsWith(basePath)) {
    filePath.removePrefix(basePath)
} else {
    filePath
}
val rawLine = endpoint.psiElement.getLineNumber(start = true)
val lineNumber = if (rawLine >= 0) rawLine + 1 else 1
```

### Import needed:
```kotlin
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
```

### Summary of the pattern:
1. **File path**: `psiElement.containingFile.virtualFile.path` → remove `project.basePath` prefix for relative path
2. **Line number**: `psiElement.getLineNumber(start = true) + 1` (getLineNumber is 0-based, add 1 for 1-based)

---

## 6. Key Imports Summary

```kotlin
// UAST call resolution
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.toUElement

// PSI tree traversal
import com.explyt.util.ExplytPsiUtil.findChildrenOfType
import com.explyt.util.ExplytPsiUtil.toSourcePsi

// Spring stereotype detection
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy

// Method references search
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.search.GlobalSearchScope

// Bean resolution
import com.explyt.spring.core.service.SpringSearchService

// File/line
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
```

---

## 7. Architecture Notes

- The new MCP tool should go in `modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/mcp/SpringMcpProvider.kt`
- Follow the existing `@McpTool` / `@McpDescription` annotation pattern (see `explyt_find_spring_endpoint` at line 129)
- Use `smartReadAction(project)` + `withContext(Dispatchers.IO)` for async PSI access (same as `findEndpoint()`)
- `McpBeanSearchService` is already in the same module and provides bean type detection
- `McpBeanTypes` enum may need a `SERVICE` entry added (currently it's mapped to `COMPONENT`)
