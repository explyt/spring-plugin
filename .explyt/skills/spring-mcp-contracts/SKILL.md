---
name: "spring-mcp-contracts"
schemaVersion: "v0.1"
description: "Develops and verifies JSON contracts of the Spring plugin's MCP tools using Java/Kotlin PSI, UAST, scoped Spring models and bounded response regression tests. Use when changing an endpoint lookup or contract tool, URL matching, the call-chain trace, a bean lookup or listing tool, a parameter source or requiredness, a response schema, an outcome vocabulary or pagination of any explyt_* MCP tool in spring-plugin, or when a reported MCP answer is wrong on a real project."
agent: "Code"
used-by:
  - "Orchestrator"
  - "General"
  - "Code"
---

# Spring MCP contracts

Change a public Spring MCP JSON contract without inventing framework resolution, source positions, model provenance, nullability, injection outcomes or pagination state.

Tool names, response fields and API names in this skill are **examples from the codebase at the time of writing**. Apply the rule, then verify the actual names, signatures and registrations in the current branch.

## Inputs

- Target tool name, issue/reproducer, expected JSON fields and compatibility constraints.
- Affected IntelliJ Platform and Kotlin versions from this branch's build files.
- Current implementation and directly covering tests in the MCP module; follow `coding-guard` and `test-driven-development` before changing code.
- For bean-model work: the approved design/spec, application and context selection rules, source preference (static analysis versus a loaded runtime snapshot), response budget, and whether the change targets a lookup tool or an existing listing tool.

## Critical constraints

- Prefer the IDE Spring model tools over text search when available, and report whether the model answered more precisely than source search.
- Keep model-relative claims distinct from runtime claims. An empty result means "not found in the selected model", never "the application cannot start". Never promise successful injection or live application-context behaviour from static or snapshot data.
- Select one runtime snapshot/context **before** union, deduplication or conversion into the plugin's bean objects. Never merge contexts to avoid an ambiguity error.
- Resolve declarations and types in the selected application module's classpath. Do not resolve through project-wide caches or scopes (for example a project-level library class cache or `projectScope`), which can return an equally named class from another application.
- Once a scoped candidate set exists, matching and selection must not re-query module- or project-wide bean sources (for example array or framework-bean fallbacks inside the existing resolver). Prepare supported candidates in the snapshot adapter instead.
- Exact name lookup filters the known names/aliases of the scoped set directly. Never route it through a type-less resolver, and never widen an empty result to all candidates.
- Type inventory does not apply primary/priority selection. Injection selection may apply it only when the scoped candidate set is complete enough to support the rule.
- Unknown type, identity or unsupported selection rule yields an explicit partial/indeterminate outcome, not a fabricated negative or confident match.
- Preserve declarations whose produced type has no project module (for example a factory method returning a JDK or library type); derive location from the declaring member or snapshot provenance, not from the produced class alone.
- Measure the final serialized envelope — metadata, revision, continuation and errors included — in UTF-16 code units against the tool's documented budget, and test it inside the client's content wrapper. Never slice scalar values and never skip an oversized record without an explicit overflow result.
- Do not add reverse usage searches to a lookup tool unless the approved design includes them. Do not extend this change into a neighbouring tool whose work is tracked separately.
- Keep a fix to a shared non-MCP helper (for example `SpringWebUtil`, an endpoint loader, `EndpointPathPatterns` once a non-MCP caller uses it) out of the MCP commit. Lines without MCP Server skip MCP commits whole during backports, so the helper fix would silently stay off them. Land it as its own PR from `main` and stack the MCP PR on it.
- Keep existing response shapes byte-identical for the common case: a new field that only some answers need is nullable and serialized `NON_NULL`, or is present in every answer with a documented `null`. Never rename or retype a field silently; a changed meaning of an existing field (for example a line number) is a breaking change named in CHANGELOG and the PR.
- Report a guess and a declared fact in different fields (for example `assumedPrefix` versus `basePath`). Never let a guess reuse a field whose value the configuration states.

## Process

1. Read the tool's public description, response DTO, implementation and registration. Trace every output field to its PSI/UAST/model input. Check direct callers and tests before changing a signature or response shape.
2. Identify the contract mode, for example: an endpoint contract (composed path, binding source, DTO schema, called service method), a bean lookup (selector arguments versus a source position), or an inventory listing. Reject mixed or partially specified modes explicitly instead of guessing intent.
3. Write one end-to-end regression test against the registered tool or the real scoped service, asserting parsed JSON rather than a private mapper. Include a positive and a contrast case, and assert fixture preconditions so the test cannot pass vacuously. Run it before production edits; a setup, index or dependency-download failure is not a valid red phase.
4. For a called-service field, inspect calls in source order but accept only a receiver proved to be an injected bean. Reject framework helpers, standard-library calls and exception constructors that merely appear first. Kotlin constructor-property receivers may resolve through a constructor parameter rather than a field; cover Java fields, Kotlin properties, interface injection, framework-only handlers and locally created non-bean fields. Return null when the bean cannot be established, and take the callee path and line from the same source anchor.
5. For Kotlin DTOs, derive nested nullability recursively from the Kotlin origin type reference matched against the resolved type arguments. Do not infer it from canonical text or a top-level nullable flag, and preserve canonical text when no Kotlin projection is available. Cover nullable element, non-null element and nullable container fixtures with literal expected JSON.
6. For request binding, distinguish the wire source and the web stack from the annotation name: servlet multipart file and part types, the unnamed map-of-parts form, the reactive stack where the same annotation means query binding, and the dedicated part annotation with its own wire name and requiredness. Never infer a form source from an HTTP verb alone, and verify both listing and contract for dropped or duplicated parameters.
7. For model snapshots, separate identity facts from optional details. Enumeration must not parse per-declaration evidence for the whole context; read details only for records attempted on the current page, inside the same read action, and return detached JSON rather than PSI-bearing callbacks or server-side cursors.
8. For injection analysis, report dependency shape, requiredness, language-level default facts and their evidence basis as separate fields. A default value does not resolve an ambiguous pair, a missing candidate for an optional dependency is not automatically a failure, multi-valued shapes are a candidate set rather than ambiguity, provider shapes are deferred, and incomplete evidence outranks all of these.
9. For pagination, derive the continuation token from the model state and the normalized query, excluding only page-size controls. Reject continuation after a changed selector, source, context, position or projection mode. Sort deterministically before paging and generate only the current page's nodes.
10. For URL input, accept what a caller copies from a browser, a log or a curl: strip scheme, host, query and fragment first. Read the path as written, then under a base path a module's configuration declares, then under a guessed leading prefix. Settle the reading by path before any HTTP-method filter, so a filter never pushes the lookup onto a different URL.
    - If a declared base path was stripped, admit only routes of a module that declares that same base path.
    - If a prefix was guessed, admit only routes whose first segment is a literal equal to the first segment of the remaining path; a `{template}` first segment would absorb whatever the guess left over.
    - In both prefixed readings allow only whole-path matches, never a substring match - the tool cut that fragment itself.
    - An empty answer means "no route answers this URL", not "the route does not exist".
11. For a mapping path, report the path statically computed from the declaration, on the stacks where the framework resolves placeholders (verify from library sources or bytecode first):
    - If the module configuration defines the key, use its value.
    - Else if the placeholder has a default, use the default.
    - Else keep the placeholder verbatim; never invent a value or drop the route.
    - Keep the declaration in a separate field whenever it differs from the reported path. Never describe the result as the runtime path: a profile or an environment variable can override the value.
12. For a parameter's `required`, follow the framework's own rule rather than the annotation attribute alone: for Spring MVC query parameters and headers a `defaultValue` or an optional declaration (`Optional`, `@Nullable`, a Kotlin nullable type or default value) means not required.
13. For a call trace, follow project sources only; list a call where the request leaves the application (a library method on an injected dependency, a framework-implemented repository method) as an external leaf without following it. Count depth in calls into other classes, follow interface calls to every project implementation, report the call-site line and the source name, and identify methods by class, name and parameter types - a Kotlin light method is re-created by every resolve. Search test references for traced project methods only.
14. Report the line of a declaration's name, not the start of its text range, which begins at the KDoc or Javadoc.
15. After the minimal fix, rerun the focused regression, the full covering class, directly affected sibling classes and IDE inspections. For every independent production change run one isolated red phase (commit first, mutate one change, run its covering classes, restore from `HEAD`); a mutation that stays green means the test is vacuous and must be fixed before the change is reported. Inspect the complete diff, exit codes and whitespace check. Stop and report if fixture or environment failures prevent verification.
16. A heavy `JavaCodeInsightFixtureTestCase` defaults to mock JDK 1.7; raise it in `tuneFixture` when the fixture calls `java.time` or other newer JDK APIs, or JDK-exclusion assertions pass vacuously.

## Contract minimum facts

Whatever the field names in the current branch, a successful lookup-style response must make these explicit (current naming shown as an example):

- Selected model provenance and precision, application and module or context identity, plus known limitations (`model.source`, `model.precision`, `model.limitations`).
- Result classification and completeness, including how many entries could not be classified (`outcome`, `matchCompleteness`, `unresolvedCount`).
- Pagination state: total, offset, truncation, continuation and the token binding it to this query (`totalCount`, `offset`, `truncated`, `nextOffset`, `revision`).
- A fixed compact projection per entry with a stable identifier, canonical name, known type, kind and same-source declaration; add the matched alias only when the query used one.
- Optional evidence behind an explicit flag; unknown stays absent rather than becoming a guessed `false` or empty list.
- Bounded structured errors for invalid arguments, selection problems, missing targets, changed results and oversized responses, without leaking long inputs or absolute machine paths.

## Output format

Report changed paths, the demonstrated red failure, green commands with exit codes and test counts, the precise JSON behaviour, model limitations, compatibility limits and any deferred scope. Do not claim tests pass without fresh output.

## Acceptance checklist

- [ ] The intended JSON regression fails before the production edit and passes afterwards; fixture preconditions are asserted.
- [ ] A reported called service is proved to be an injected bean; no framework helper or fabricated source position is returned.
- [ ] Kotlin nested nullability and Java DTO output keep their exact supported semantics.
- [ ] Servlet multipart binding, wire names and requiredness are correct; reactive and plain query parameters stay query-bound.
- [ ] One context is selected before union, and declarations/types resolve in the selected application's classpath.
- [ ] Unknown exact names do not widen to unrelated candidates; aliases group only on proven identity.
- [ ] Inventory keeps all complete candidates despite primary markers; selection stays inside the scoped set and handles collection and provider shapes correctly.
- [ ] Unknown types or unsupported rules produce a partial/indeterminate outcome, not a confident verdict.
- [ ] Declarations producing library types stay visible with their declaring member or an explicit no-source limitation.
- [ ] Projections are fixed; evidence is computed only for the attempted page and triggers no reverse usage scan.
- [ ] The measured budget covers metadata, errors and continuation; pages advance monotonically, tokens reject changed queries, and an oversized single item yields an explicit overflow result.
- [ ] Unrelated tool responses and existing successful response shapes are unchanged; sequential covering tests and inspections report no new errors.
- [ ] A deployed URL resolves through as-written, declared base path and guessed prefix readings, in that order; guesses and declared facts use different fields, and the method filter applies after the reading is settled.
- [ ] A resolved mapping path and its declaration are both available when they differ; `required` follows the framework rule; reported lines point at declaration names.
- [ ] A call trace holds project methods only, marks external leaves, reports call-site lines and source names, and searches tests for traced project methods only.
- [ ] Every independent production change has its own failing red phase; no mutation stayed green.
- [ ] A shared non-MCP helper change ships in its own PR from `main`, with the MCP PR stacked on it.
- [ ] Changelog, wiki and tool descriptions match the actual fields and limitations, and imply no runtime guarantee.
