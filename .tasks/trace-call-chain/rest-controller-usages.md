# REST_CONTROLLER Usages Analysis

## Definition
- **File:** `modules/spring-core/src/main/kotlin/com/explyt/spring/core/SpringCoreClasses.kt`
- **Line 53:** `const val REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController"`

## All Usages of `SpringCoreClasses.REST_CONTROLLER`

| # | File | Line | Content |
|---|------|------|---------|
| 1 | `modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/mcp/SpringMcpProvider.kt` | 385 | `psiClass.isMetaAnnotatedBy(SpringCoreClasses.REST_CONTROLLER) -> "CONTROLLER"` |

**Total project usages: 1** (excluding task files)

## Does `SpringWebClasses` already have `REST_CONTROLLER`?

**No.** `SpringWebClasses.kt` does not contain a `REST_CONTROLLER` constant.

## Does `SpringWebClasses` already have `CONTROLLER`?

**Yes.** Line 27 of `SpringWebClasses.kt`:
```kotlin
const val CONTROLLER = "org.springframework.stereotype.Controller"
```

## Duplication Note

Both files define `CONTROLLER` with the same value:
- `SpringCoreClasses.kt` line 52: `const val CONTROLLER = "org.springframework.stereotype.Controller"`
- `SpringWebClasses.kt` line 27: `const val CONTROLLER = "org.springframework.stereotype.Controller"`

## Migration Impact Summary

Moving `REST_CONTROLLER` from `SpringCoreClasses` to `SpringWebClasses` would require:
1. Add `const val REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController"` to `SpringWebClasses.kt`
2. Remove the constant from `SpringCoreClasses.kt` line 53
3. Update **1 file**: `modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/mcp/SpringMcpProvider.kt` line 385 — change `SpringCoreClasses.REST_CONTROLLER` → `SpringWebClasses.REST_CONTROLLER`

The precedent is clear: `SpringWebClasses` already has `CONTROLLER` (the `@Controller` stereotype), so `REST_CONTROLLER` (`@RestController` from `org.springframework.web.bind.annotation`) belongs there too.
