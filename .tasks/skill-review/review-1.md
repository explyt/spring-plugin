# Review: .explyt/skills/develop-inspection-feature/SKILL.md

Reviewed against the provided checklist. File is 91 lines; frontmatter lines 1–6.

## PASS/FAIL Matrix

### Frontmatter
| Item | Verdict |
|---|---|
| Starts with `---`, closed with `---` (lines 1 and 6) | PASS |
| `name: "develop-inspection-feature"` matches folder name, 26 chars ≤50, kebab-case, no spaces/capitals | PASS |
| `schemaVersion: "v0.1"` exactly | PASS |
| `description` has WHAT ("End-to-end workflow for adding a new inspection or quick-fix feature…") and WHEN ("Use when the user asks to add a new inspection, implement a Spring Boot migration check…"); ~494 chars ≤1024; no `<` or `>` | PASS |
| `agent: General` present and valid; `used-by` OMITTED (required for all-agents mode) | PASS |

### Clarity and determinism
| Item | Verdict |
|---|---|
| Imperative, unambiguous steps (numbered Algorithm 1–7) | PASS |
| Explicit if/else branching (step 1 "If an equivalent inspection exists… stop and report"; step 3 "If a version-gated base… does not exist, create it"; step 6 legacy-import fallback) | PASS |
| Inputs stated explicitly (## Inputs: feature definition, version gate, target module, branch name) | PASS |
| Returned result format specified (## Output format) | PASS |
| Acceptance criteria present (## Acceptance checklist, 8 items) | PASS |

### Complexity and loops
| Item | Verdict |
|---|---|
| No unreachable steps; branches converge or terminate (duplicate check terminates with report; else continue) | PASS |
| Loop bounded: step 6 "Fix-and-rerun loop: max 4 iterations, then stop and report" | PASS |

### Brevity/DRY/safety
| Item | Verdict |
|---|---|
| Under 400 lines (91) | PASS |
| No filler prose (dense, lesson-based content) | PASS |
| No destructive actions without confirmation (explicitly forbids `git add -A` on dirty tree; force-push only to own feature branch after review fixes — standard PR practice) | PASS |
| No absolute paths, secrets, or internal URLs (only public docs.spring.io; explicitly forbids internal sentry links in PRs) | PASS |
| No README.md in skill folder (folder contains only SKILL.md) | PASS |

### Subagents
| Item | Verdict |
|---|---|
| No delegation to unnamed subagents. Only reference is "Call the `coding-guard` skill first" — a named skill, not a subagent | PASS |

### Domain correctness (verified against repo)
| Item | Verdict |
|---|---|
| `SpringBaseUastLocalInspectionTool`, `SpringBaseLocalInspectionTool` exist in `modules/base/src/main/kotlin/com/explyt/inspection/` | PASS |
| `RewriteAnnotationQuickFix` exists (`modules/spring-core/.../inspections/quickfix/RewriteAnnotationQuickFix.kt`); skill's call shape `(newFqn, ownerJavaPsi, attributes, oldFqn)` matches the `(fqn, modifierListOwner, values, vararg annotationsToRemove)` constructor | PASS |
| `ReplacementKeyQuickFix` exists | PASS |
| `SpringCoreUtil.isConfigurationPropertyFile` (SpringCoreUtil.kt:78), `ExplytPsiUtil.isTestFiles` (ExplytPsiUtil.kt:300) exist | PASS |
| `TestLibrary`, `ExplytInspectionJavaTestCase`, `ExplytInspectionKotlinTestCase` exist in test-framework | PASS |
| `Spring4UastLocalInspectionTool` / `Spring4LocalInspectionTool` / `MigrateImportQuickFix` absent from working tree but described as version-gated pieces to create ("If a version-gated base for your version does not exist, create it once…") — acceptable per task context | PASS |
| plugin.xml claims: `language="UAST"` used on all UAST inspections in spring-core-plugin.xml; `shortName` == `inspectionDescriptions/<ShortName>.html` (e.g. SpringPropertiesInspection.html exists) | PASS |
| Git guidance matches AGENTS.md/CONTRIBUTING (branch `username/feature-name`, conventional commits, one change per PR) and repo practice (PRs to `public` remote = explyt/spring-plugin, target `main`) | PASS |

## Blocking Issues

None.

## Non-blocking notes

1. **"Omit `language` for property/file-level" deviates from repo precedent.** Existing property inspections in spring-core-plugin.xml set `language="Properties"` (SpringPropertiesInspection, EnableAutoConfigureSpringFactoryInspection) or `language="yaml"` (SpringYamlInspection); no existing `<localInspection>` omits `language`. Omitting is only justified when one class covers both .properties and .yaml (the migration-series case). Suggest rewording to "set `language=\"Properties\"`/`\"yaml\"`, or omit when one inspection covers both formats". Not in the blocking checklist (which only requires the UAST and shortName claims to match — they do).
2. `isClassAvailable` / `isAnyClassAvailable` helpers are not in the working tree (branch-only, like the Spring4 bases); they appear inside the same "create the version-gated base" step, so plausibly covered by "create if missing", but an explicit note would help.

## Evidence

- `.explyt/skills/develop-inspection-feature/SKILL.md` lines 1–6 (frontmatter), 91 total lines.
- `modules/base/src/main/kotlin/com/explyt/inspection/SpringBaseLocalInspectionTool.kt`, `SpringBaseUastLocalInspectionTool.kt` (with `isAvailableForFile` override, confirming the gating pattern).
- `modules/spring-core/src/main/kotlin/com/explyt/spring/core/inspections/quickfix/RewriteAnnotationQuickFix.kt` (3 constructors incl. `(fqn, owner, values, vararg annotationsToRemove)`), `ReplacementKeyQuickFix.kt`.
- `SpringCoreUtil.kt:78` `isConfigurationPropertyFile`; `ExplytPsiUtil.kt:300` `isTestFiles`; `PropertyUtil.kt:253` `propertyKeyPsiElement`; `QuickFixUtil.kt` uses `AddAnnotationModCommandAction`.
- `modules/test-framework/.../TestLibrary.kt`, `ExplytInspectionJavaTestCase.kt`, `ExplytInspectionKotlinTestCase.kt`.
- `modules/spring-core/src/main/resources/META-INF/spring-core-plugin.xml`: 37 `<localInspection>` entries, UAST ones use `language="UAST"`, property ones use `language="Properties"`/`"yaml"`; `inspectionDescriptions/SpringPropertiesInspection.html` exists matching shortName.
- Sibling frontmatter convention confirmed via `.explyt/skills/coding-guard/SKILL.md`.
- Grep: no `<localInspection` line without `language=` in any module plugin.xml (basis for note 1).

## Final blocking_status: pass
