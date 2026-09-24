---
name: "spring-mcp-contracts"
schemaVersion: "v0.1"
description: "Develops and verifies JSON contracts of the Spring plugin's MCP tools using Java/Kotlin PSI, UAST, scoped Spring models and bounded response regression tests. Use when changing an endpoint contract tool, a bean lookup or listing tool, a parameter source, a response schema, an outcome vocabulary or pagination of any explyt_* MCP tool in spring-plugin."
agent: "Code"
used-by:
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
10. After the minimal fix, rerun the focused regression, the full covering class, directly affected sibling classes and IDE inspections. Inspect the complete diff, exit codes and whitespace check. Stop and report if fixture or environment failures prevent verification.

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
- [ ] Changelog, wiki and tool descriptions match the actual fields and limitations, and imply no runtime guarantee.
