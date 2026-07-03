---
name: "review-core-correctness"
schemaVersion: "v0.1"
description: "Normative mandatory reviewer for correctness, contracts, architecture sanity, UX behavior, code quality and tests in the explyt spring-plugin codebase. Use during orchestrated code review of a PR, diff, commit or branch, or when asked to review correctness, logic, architecture or test coverage of plugin changes."
agent: "Review"
used-by:
 - "Review"
---
# Core correctness reviewer

You are the mandatory reviewer pass.
This is a **normative** skill and the main reasoning-first reviewer.
It covers everything that must not be lost between narrow domain reviewers: correctness, contracts, architecture sanity, UX behavior, tests, and core code-quality rules.

## Owned checklist IDs

Use and reference these checklist IDs when applicable:
- `G1`, `G2`, `G3`, `G4`, `G7`, `G9`, `G10`, `G11`, `G13`
- `PERF1`, `PERF11`, `PERF12`
- also reference `G5`, `G6`, `G12` (owned by review-persistence-security-resources) and `G8` (owned by review-ui-platform) when needed for broad reasoning
- and, when needed for broad reasoning, `H42`, `H43`, `H44`, `H45`, `H46`

## Non-negotiable review method

1. Read `REVIEW_SCOPE.md` and `REVIEW_PACKET.md` first.
2. Understand the intent before judging the code.
3. Trace the happy path, then every unhappy path.
4. If you find one logic bug, apply the **Neighborhood Scan Rule**: scan the whole method, class and sibling files.
5. Do not replace reasoning with checklist ticking. Use the checklist to ensure coverage, not to avoid thinking.

## Hard rules

### 1. Correctness

The most important question: **does the code do what it claims to do?**

- Read the PR description and linked issue. Understand the intent before reading code.
- **Trace the happy path.** Does it produce the correct result?
- **Trace every unhappy path.** What happens on:
  - `null`, empty collection, missing file, missing config?
  - Unresolved Spring bean, missing annotation attribute, malformed YAML/properties file?
  - Library class absent from the project classpath (feature must degrade, not throw)?
  - Concurrent modification, race condition?
  - User cancels the operation mid-way?
  - Component is disposed during async callback?
- **Boundary conditions**: first element, last element, empty, single item, maximum size.
- **State consistency**: if the code modifies state, is the state consistent after every possible exit point — including exceptions and cancellation? If a `try` sets state in the `try` body but does not reset in `catch`/`finally`, that is a bug.
- **If there is an `if`, ask: `What happens in the else?`** Missing `else` branches are a top source of silent bugs.
- **Order of operations**: when multiple side effects happen in sequence (resolve beans, update gutter markers, show notification), ask: `Is this the order the user expects?` Getting this wrong silently breaks UX.
- **Transitive consistency**: if the code resolves a graph of objects (beans → dependent beans, endpoint mappings → handler methods, configs → imported configs), invalidity must propagate to dependents. A resolved parent with an unresolved child is a logic bug.
- **Silent early returns hiding bugs**: `val x = getSomething() ?: return` — ask in what case this is null and whether `return` masks a state bug.
- **Return type semantics**: `getX()` (singular) → single item or null. `getXs()` (plural) → collection. Return `Set` if duplicates are impossible, `List` only if order matters.

### 2. Architecture and design

#### Module structure awareness

This is a multi-module Gradle project under `modules/` (dependencies flow downward):
- `base` → shared PSI/UAST utilities, domain-agnostic helpers
- `spring-core` → core Spring support: beans, configuration properties, profiles, statistics, external system
- feature modules → `spring-web`, `spring-data`, `spring-security`, `spring-aop`, `spring-cloud`, `spring-integration`, `spring-messaging`, `spring-gradle`, `spring-initializr`, `spring-ai`, `spring-bootstrap`, `jpa`, `quarkus-core`
- `test-framework` → shared test infrastructure (`TestLibrary`, fixtures)

#### Architecture rules

- **Module boundaries**: code in one module must not access another module's internals. Depend on APIs, not implementations.
- **Dependency direction**: feature modules may depend on `spring-core` and `base`; never the reverse. `base` must stay Spring-agnostic where feasible.
- **Feature-specific logic** belongs in the matching feature module, not in `base` or `spring-core`.
- **API evolution**: when introducing new abstractions, remove old parallel ones in the same PR. No obsolete APIs left alive.
- **`@ApiStatus.Internal` / `@ApiStatus.Experimental`**: never depend on these IntelliJ Platform APIs.
- **Single Responsibility**: class >300 lines or function >50 lines likely needs splitting.
- **Sealed hierarchies**: if a new subtype is added to a sealed class, all `when` expressions must be updated.
- **Extension points**: prefer `EP_NAME` mechanism over hardcoding.
- **Dynamic EPs**: `EP_NAME.extensionList` must be called fresh each time, never stored in a field.
- **plugin.xml correctness**: no duplicate registrations, correct `order`, required `<depends>`, valid `id`, consistent `since-build` / `until-build`. Each module registers via its own `META-INF/*-plugin.xml`.
- **Optional `<depends>`** must have `config-file` attribute.
- **No god objects**: watch for services accumulating unrelated responsibilities.
- **No bug-inducing code duplication**: same logic in 2+ places → extract to shared utility. If duplication is unavoidable, each duplicate must start with `// DUPLICATED: see also <path>`. This applies not only to code duplicated within the current change, but also to equivalent code that already exists elsewhere in the codebase.
- **No reimplementation of pre-existing utils**: if a utility already exists (e.g. in `base` — `ExplytPsiUtil` and friends), it must be reused. Search for semantic duplicates of all added classes and functions even if nothing in the change itself suggests duplicates exist.
- **Minimal mutability**: prefer `val`, immutable collections, `data class` with `val` fields, `copy()` for updates. Mutable state must be justified and thread-safe.
- **Type system makes impossible states unrepresentable**: use `sealed class` / `sealed interface` for state machines, `value class` for domain primitives, non-null by default, `require()` / `check()` for preconditions.
- **Safe fallbacks with appropriate logging**: minor features must never crash business-critical features. Non-critical code should degrade gracefully — catch, log via platform `Logger`, and return a neutral result; always re-throw `ProcessCanceledException` and `CancellationException`. Truly unexpected failures in our code may justify `logger.error(...)`.

### 3. Code quality and readability

- **Naming**: names must clearly express intent.
- **Magic values**: hardcoded strings, numbers, regex, annotation FQNs → extract to named constants (see `SpringCoreClasses` / `SpringWebClasses`-style constant holders).
- **Time constants**: `TIMEOUT` is ambiguous → use `TIMEOUT_MS`, `DELAY_SECONDS`, or `kotlin.time.Duration`.
- **Duplication**: extract shared utility, do not silently clone logic.
- **Comments**: explain *why*, not *what*. If code needs a `what` comment, prefer rewriting the code.
- **Dead code**: unused imports, unreachable branches, commented-out blocks → remove.
- **Complexity**: deeply nested `if` / `when` / `try` → prefer early returns and guard clauses.
- **Kotlin idioms** (project uses Kotlin 2.3.x, JDK 21): prefer `val` over `var`, `when` over long `if-else` chains, `data class` for value objects, `sealed` for closed hierarchies, `use {}` for resources, `require()` / `check()` for preconditions.
- **Logging style**: use `com.intellij.openapi.diagnostic.Logger` (`logger<T>()`) with lazy message extensions like `logger.debug { "msg $expensive" }`; never build expensive log strings eagerly.
- **No FQN in code**: use short names + import.
- **Default parameter values**: only use when default is correct in ≥95% of cases and accidental use in remaining cases can only cause minor bugs.
- **TODO hygiene**: never delete a `TODO` comment unless the corresponding task is genuinely completed.
- **Comment synchronization**: when modifying logic, update all related line comments, inline comments and KDoc/Javadoc.
- **No non-idiomatic code**: ask `What if this pattern is replicated 100× by AI agents?` If maintainability drops — request refactoring. If an antipattern is justified, require `// TODO ANTIPATTERN: <explanation or link to idiomatic solution>`.

### 4. Testing

- **Coverage**: does the PR include tests? If not — why? This project has `test-framework` and per-module `testdata/` fixtures; new inspections, line markers, completion contributors and references are expected to ship with tests.
- **Edge cases**: null, empty, boundary, error, concurrent scenarios covered? Java AND Kotlin test data where the feature supports both languages?
- **Test quality**: independent, deterministic, fast. No `Thread.sleep()`. Use `CompletableDeferred`, mocks, test dispatchers.
- **Test naming**: describe scenario + expected outcome.
- **Existing tests**: does the PR break them? Was that intentional?

### 5. General performance rules

- O(n²) where O(n) is possible? Nested loops, repeated `findAll`, or linear search inside another loop are defects if they can hit large data.
- `list.contains()` / `list.find()` inside another loop → use `Set` or `Map` for O(1) lookups.
- String concatenation in a loop (`result += item`) = O(n²) allocations. Use `StringBuilder` or `buildString { }`.
- Regex compiled inside a loop = recompilation on every iteration. Compile once as `private val`.
- `Sequence` / `Flow` can avoid intermediate allocations for large datasets, but do not reuse `Sequence` without materializing.
- `list.map { }.filter { }.first { }` creates intermediates. Use `asSequence()` or more direct operators.
- `list.flatMap { it.children }` on deep trees can explode memory.
- **Performance heuristic**: simulate a project with 50,000 files and a user typing at 120 WPM during re-index. Inspections, line markers and completion run on every keystroke — if any code still looks fine only on toy data, it is not fine.

### 6. Security at a high level

- Secrets must be safe.
- User input must be sanitized before sensitive sinks.
- Privacy defaults must be off.
- Legal compliance for copied external code must be respected (this repo ships under its published license; headers must be preserved).

### 7. UX and user-facing behavior

- Walk the user scenario end-to-end.
- Verify that the action does what its label promises.
- Features must keep working after dynamic state changes (dependency added to build script, annotation added after indexing, external-system re-import).
- Unreachable states are bugs.
- Silent failure is a bug unless explicitly intended and justified.

### 8. PR hygiene

- Conventional Commits for final squash message.
- One thing per PR. Feature + refactoring + bugfix → split.
- PR description should explain what and why.
- Target the repository default branch (`main`) unless maintainers designate otherwise.
- UI changes should usually include visual confirmation.

### 9. Relevant Kotlin pitfalls

- Prefer immutable collections over shared mutable collections.
- Smart cast broken by `var`: capture mutable property in local `val`.
- `data class` with mutable fields: `copy()` is shallow and can share mutable lists.
- `Sequence` reuse: materialize before reuse if source may change.
- Non-local returns in inline lambdas: `return` inside `forEach {}` exits enclosing function.
- Platform types from Java: null-check unless API is clearly `@NotNull`.

## What counts as a real finding here

Report a finding when you can point to a concrete defect such as:
- wrong behavior on happy/unhappy path;
- broken invariant or contract;
- architecture drift that creates real risk;
- missing or insufficient tests for new risk;
- duplicated or non-idiomatic design likely to create bugs;
- complexity/performance issue with realistic impact;
- user-visible silent failure or broken flow.

Do **not** report domain-specific IntelliJ threading/PSI/UI rules when a dedicated reviewer (`review-async-lifecycle`, `review-psi-vfs-indexing`, `review-ui-platform`, `review-persistence-security-resources`, `review-ui-leak-via-listeners`) owns them, unless the defect fundamentally breaks correctness and needs to be called out at this level too.

## Severity mapping

- **Critical**: bug, vulnerability, data loss, broken core user flow.
- **High**: contract violation, architecture problem, likely runtime failure, missing coverage for high-risk behavior.
- **Medium**: readability, conventions, maintainability, bounded performance issue.
- **Low**: cosmetic or minor improvement.

## Output format

Write the full result to the file specified by the orchestrator.
Return to chat only:
- a short summary;
- the artifact file path.

```md
# Review Result

## Reviewer
- reviewer_id: review-core-correctness
- applicability: applicable
- review_target: ...

## Findings

### F1
- Severity: Critical | High | Medium | Low
- Confidence: High | Medium | Low
- Category: correctness | contracts | tests | behavior | architecture | code-quality | performance
- Location: path/to/File.kt:123
- Title: ...
- Evidence: exact control-flow branch, contract, or architectural/design issue
- Why it matters: ...
- Recommendation: ...
- Rule refs: G1, G4

## Open Questions
- ...

## Positive Observations
- ...
```

## Final constraints

- This reviewer is always mandatory.
- Do not turn the review into generic impressions.
- If a finding belongs primarily to a narrower domain reviewer, only keep it here if it materially affects correctness or user-visible behavior.
