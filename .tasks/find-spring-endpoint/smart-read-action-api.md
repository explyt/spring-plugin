# IntelliJ Platform: smartReadAction & Coroutine Read APIs

## Platform Version
- **IntelliJ Platform**: 2026.1 (idea:2026.1)
- **Kotlin**: 2.3.20
- **since-build**: 261, **until-build**: 265.*
- **Source**: `gradle.properties`

## Key Finding: `smartReadAction` EXISTS

All coroutine read functions are top-level suspend functions in:
- **Package**: `com.intellij.openapi.application`
- **File**: `CoroutinesKt` (compiled from `coroutines.kt`)
- **JAR**: `intellij.platform.core.jar`

---

## API Signatures (from decompiled CoroutinesKt.class)

### 1. `smartReadAction` — **THE RECOMMENDED API**
```kotlin
// Import:
import com.intellij.openapi.application.smartReadAction

// Signature:
public suspend fun <T> smartReadAction(project: Project, action: () -> T): T
```
This is the suspend equivalent of `DumbService.runReadActionInSmartMode()`. It acquires a read lock AND waits for smart mode (indices ready).

### 2. `readAction` (currently used in project)
```kotlin
import com.intellij.openapi.application.readAction

public suspend fun <T> readAction(action: () -> T): T
```
Only acquires read lock; does NOT wait for smart mode.

### 3. `constrainedReadAction` — generic constrained version
```kotlin
import com.intellij.openapi.application.constrainedReadAction

public suspend fun <T> constrainedReadAction(
    vararg constraints: ReadConstraint, 
    action: () -> T
): T
```
`smartReadAction(project) { ... }` is equivalent to:
```kotlin
constrainedReadAction(ReadConstraint.inSmartMode(project)) { ... }
```

### 4. Blocking variants (for use inside coroutines that need blocking semantics)
```kotlin
import com.intellij.openapi.application.smartReadActionBlocking
import com.intellij.openapi.application.readActionBlocking
import com.intellij.openapi.application.constrainedReadActionBlocking

public suspend fun <T> smartReadActionBlocking(project: Project, action: () -> T): T
public suspend fun <T> readActionBlocking(action: () -> T): T
public suspend fun <T> constrainedReadActionBlocking(vararg constraints: ReadConstraint, action: () -> T): T
```

### 5. Internal/undispatched variants
```kotlin
// @IntellijInternalApi @ApiStatus.Internal
public suspend fun <T> readActionUndispatched(action: () -> T): T
public suspend fun <T> constrainedReadActionUndispatched(vararg constraints: ReadConstraint, action: () -> T): T
```

---

## ReadConstraint Interface

```kotlin
// Import:
import com.intellij.openapi.application.ReadConstraint

interface ReadConstraint {
    @RequiresReadLock
    fun isSatisfied(): Boolean
    
    suspend fun awaitConstraint()

    companion object {
        fun inSmartMode(project: Project): ReadConstraint
        fun withDocumentsCommitted(project: Project): ReadConstraint
    }
}
```

Available constraints:
- `ReadConstraint.inSmartMode(project)` — indices are ready
- `ReadConstraint.withDocumentsCommitted(project)` — PSI matches documents

---

## DumbService.waitForSmartMode()

```kotlin
// BLOCKING ONLY — annotated with @RequiresBlockingContext
abstract fun waitForSmartMode(): Unit

// Internal, also blocking
@ApiStatus.Internal
abstract fun waitForSmartMode(timeoutMillis: Long): Boolean
```

The DumbService docs explicitly say:
> See `Project.waitForSmartMode` for using in a suspend context.  
> Use `com.intellij.openapi.application.smartReadAction` instead of `runReadActionInSmartMode`.

Note: `Project.waitForSmartMode` suspend extension was NOT found — only `DumbService.runInDumbMode` suspend extension exists in `DumbServiceKt.class`.

---

## Project Usage

- **`readAction`**: Used in `modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/mcp/SpringMcpProvider.kt:30`
- **`smartReadAction`**: NOT used anywhere in the project
- **`constrainedReadAction`**: NOT used anywhere in the project
- **`ReadConstraint`**: NOT used anywhere in the project

---

## Migration: readAction → smartReadAction

**Before** (no smart mode guarantee):
```kotlin
import com.intellij.openapi.application.readAction

val result = readAction {
    // May fail with IndexNotReadyException if indices aren't ready
    someIndexDependentOperation()
}
```

**After** (waits for smart mode):
```kotlin
import com.intellij.openapi.application.smartReadAction

val result = smartReadAction(project) {
    // Guaranteed: read lock held AND indices are ready
    someIndexDependentOperation()
}
```

**With multiple constraints**:
```kotlin
import com.intellij.openapi.application.constrainedReadAction
import com.intellij.openapi.application.ReadConstraint

val result = constrainedReadAction(
    ReadConstraint.inSmartMode(project),
    ReadConstraint.withDocumentsCommitted(project)
) {
    // Smart mode + committed documents guaranteed
    someOperation()
}
```
