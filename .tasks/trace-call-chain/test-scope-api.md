# IntelliJ 2026.1 — Test Search Scope API

## Best API for your use case

To search for test references to production methods via `MethodReferencesSearch`, use:

### `module.getModuleTestSourceScope()`
```kotlin
import com.intellij.openapi.module.Module
import com.intellij.psi.search.GlobalSearchScope

val testScope: GlobalSearchScope = module.getModuleTestSourceScope()
```

**Javadoc:** _"scope including only test sources."_  
**Declared in:** `com.intellij.openapi.module.Module` (line 177)  
**Returns:** `GlobalSearchScope` covering **only** test source roots of the module, no production sources, no libraries, no dependencies.

This is a `default` method added directly on `Module`:
```java
default @NotNull GlobalSearchScope getModuleTestSourceScope() {
    throw new UnsupportedOperationException("Not implemented");
}
```
The actual implementation is provided by the platform's Module implementation at runtime.

---

## Full inventory of test-related scope APIs

### On `com.intellij.openapi.module.Module` (instance methods)

| Method | What it includes |
|--------|-----------------|
| `getModuleTestSourceScope()` | **Only test sources** of the module |
| `getModuleScope(true)` | Production + test sources (no libs/deps) |
| `getModuleScope()` (no arg) | Same as `getModuleScope(true)` — includes tests |
| `getModuleTestsWithDependentsScope()` | Test sources + all modules depending on this module |
| `getModuleWithDependenciesAndLibrariesScope(true)` | Source + tests + deps + libs |
| `getModuleRuntimeScope(true)` | Runtime scope including tests |
| `getModuleProductionSourceScope()` | Only production sources (counterpart) |

### On `com.intellij.psi.search.GlobalSearchScope` (static methods)

| Method | Delegates to |
|--------|-------------|
| `moduleScope(module)` | `module.getModuleScope()` — includes both src+test |
| `moduleTestsWithDependentsScope(module)` | `module.getModuleTestsWithDependentsScope()` |
| `moduleWithDependenciesAndLibrariesScope(module, true)` | includes tests |
| `moduleRuntimeScope(module, true)` | includes tests |

**Note:** There is **no** static `GlobalSearchScope.moduleTestSourceScope(module)` convenience method. Use the instance method directly.

---

## Recommendation for `MethodReferencesSearch`

For finding test usages of a production method:

```kotlin
// Option A: Only test sources of a single module (narrowest)
val scope = module.getModuleTestSourceScope()

// Option B: Test sources of module + all dependents (wider — catches tests in other modules that depend on this one)
val scope = module.getModuleTestsWithDependentsScope()

// Option C: Union of all modules' test scopes in the project
val scope = ModuleManager.getInstance(project).modules
    .map { it.getModuleTestSourceScope() }
    .reduceOrNull { acc, s -> acc.union(s) as GlobalSearchScope }
    ?: GlobalSearchScope.EMPTY_SCOPE

// Then use:
MethodReferencesSearch.search(psiMethod, scope, true)
```

**Import paths:**
- `com.intellij.openapi.module.Module`
- `com.intellij.openapi.module.ModuleManager`
- `com.intellij.psi.search.GlobalSearchScope`
- `com.intellij.psi.search.searches.MethodReferencesSearch`
