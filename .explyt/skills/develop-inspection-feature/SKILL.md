---
name: "develop-inspection-feature"
schemaVersion: "v0.1"
description: "End-to-end workflow for adding a new inspection or quick-fix feature to the explyt/spring-plugin repository: duplicate check, fact verification against authoritative javadoc, base-class and gating choice (isAvailableForFile), visible-element highlighting, registration, platform tests, branch and PR. Use when the user asks to add a new inspection, implement a Spring Boot migration check, add a quick-fix, ship a version-gated inspection, or implement a plugin feature from a migration guide."
agent: General
---

# Develop an Inspection Feature (explyt/spring-plugin)

Produce a working, registered, tested inspection (with optional quick-fix) on its own branch with a PR to `main`, following the lessons learned from the Spring Boot 3/4 migration inspection series.

## Critical constraints

- Call the `coding-guard` skill first and follow it for style, threading, SPDX headers, bundles, and PR hygiene.
- Never trust migration guides or wiki summaries for FQNs or API moves. Verify every class/annotation FQN against the authoritative javadoc or the actual artifact before hardcoding it (lesson: `EnvironmentPostProcessor` did NOT move in Boot 4 despite the guide summary implying it).
- Never highlight import statements — the IDE folds them and users never see the warning. Highlight the visible element: the annotation usage, the declaration type reference (field / parameter / return type), the property key, or the class name identifier.
- Never do per-PSI-element availability checks (library version, class presence). All gating belongs in `LocalInspectionTool#isAvailableForFile`, which runs once per file and short-circuits the whole inspection.
- Do not duplicate an existing inspection. Check first (see Algorithm step 1).
- Do not push feature branches to `origin` (internal GitLab). Push to the `public` remote (`explyt/spring-plugin`); PRs target its `main`.
- Use the `gh` CLI for GitHub operations (issues, PRs, review replies), not the GitHub MCP tools.
- After any `git checkout` / `rebase` / `stash` via terminal, do not trust cached file reads: verify each file edit landed on disk (`grep -c`), and verify staged content via `git show :<path>` before committing.

## Inputs

Resolve before starting:

- **Feature definition**: what the inspection detects, the exact old→new mapping (FQNs, property keys), and the quick-fix behavior.
- **Version gate**: which framework/library version enables the check (e.g. Spring Boot 4+), if any.
- **Target module**: usually `spring-core`; feature modules (`spring-web`, `spring-data`, …) per their area.
- **Branch name**: `username/feature-name` (e.g. `imuromtsev/boot4-mockbean-migration`), branched off `public/main`.

## Algorithm

1. **Duplicate check.** Search `modules/*/src/main/resources/META-INF/*plugin*.xml` for `<localInspection>` shortNames and grep the inspections packages for related class names / FQN constants. If an equivalent inspection exists (e.g. `EnableAutoConfigureSpringFactoryInspection` already covers `spring.factories` → `AutoConfiguration.imports`), stop and report it instead of duplicating.

2. **Verify facts.** For every FQN, property key, or API the inspection will hardcode:
   - confirm against the official javadoc (`docs.spring.io/.../api/java/...`) or the real artifact on Maven Central;
   - record old FQN → new FQN pairs explicitly; a package prefix rewrite is almost always wrong (exceptions exist per class, e.g. `JsonTest` stayed while `WebMvcTest` moved).

3. **Choose the base class and gating.**
   - UAST checks (`checkClass` / `checkField` / `checkMethod`) → extend the UAST base (`SpringBaseUastLocalInspectionTool`, or a version-gated subclass like `Spring4UastLocalInspectionTool`).
   - File-level checks (properties/YAML) → extend `SpringBaseLocalInspectionTool` / `Spring4LocalInspectionTool` and gate with `SpringCoreUtil.isConfigurationPropertyFile(file)`.
   - If a version-gated base for your version does not exist, create it once in a shared/foundational change: `isAvailableForFile = super.isAvailableForFile(file) && SpringBootUtil.isAtLeast<Version>(file)` (version lookup is cached per module).
   - Add narrower `isAvailableForFile` overrides on top of `super`:
     - migration-of-old-class inspections: require the OLD class to be resolvable (`isClassAvailable` / `isAnyClassAvailable`) — fresh projects without the legacy class must skip the inspection entirely;
     - test-only features (e.g. `@SpringBootTest` checks): require `ExplytPsiUtil.isTestFiles(file)`.

4. **Implement detection + quick-fix.** Reuse existing quick-fixes before writing new ones:
   - annotation rename / FQN swap preserving attributes → `RewriteAnnotationQuickFix(newFqn, ownerJavaPsi, attributes, oldFqn)`; rebuild attributes via `factory.createAnnotationFromText("@$newFqn($argsText)", owner).parameterList.attributes`;
   - package move with unchanged simple name → highlight the type reference, fix with `MigrateImportQuickFix(oldFqn, newFqn)`;
   - property key rename → highlight `propertyKeyPsiElement()`, fix with `ReplacementKeyQuickFix` (robust for `.properties`; for YAML show the suggested key in the message);
   - add a class-level annotation → `LocalQuickFix.from(AddAnnotationModCommandAction(fqn, javaPsi))`.
   - Only propose a migration the project can satisfy: check the NEW target is resolvable before registering the problem.

5. **Register.** In the module's plugin.xml:
   - `language="UAST"` for UAST-based inspections; omit `language` for property/file-level and raw-PSI (import/type visitor) inspections;
   - `shortName` must equal the description HTML filename in `inspectionDescriptions/<ShortName>.html` — create it;
   - messages go to the module bundle (`messages/SpringCoreBundle.properties`); no-arg messages must not double apostrophes, parameterized (`{0}`) messages must double them.

6. **Test.** Base classes `ExplytInspectionJavaTestCase` / `ExplytInspectionKotlinTestCase`; declare `override val libraries: Array<TestLibrary>` (add a new `TestLibrary` coordinate if the version is missing — it downloads from Maven Central). Cover: positive highlight, negative (new API / unrelated code), quick-fix result. Known gotchas:
   - the highlight marker must wrap the WHOLE visible element: `<warning>@Annotation(args)</warning>` including `@` and arguments; for type references just the type name token;
   - stub classes removed/relocated from real jars with `myFixture.addClass(...)`; annotation stubs need `@Retention(RUNTIME)` or annotation-rewrite fixes won't offer in-code placement;
   - `configureByText("src/test/java/X.java", ...)` fails ("Invalid file name"); for test-source gating mark a root in `setUp`: `PsiTestUtil.addSourceRoot(module, myFixture.tempDirFixture.findOrCreateDir("src/test/java"), true)`, then `myFixture.addFileToProject(path, text)` + `configureFromExistingVirtualFile(...)`;
   - if the legacy import cannot resolve, add the legacy library/stub too, or the Kotlin compiler errors pollute `testHighlighting`;
   - prefer semantic quick-fix assertions on `myFixture.file.text` (contains new annotation/import, old one gone) over exact `checkResult`; find intentions with `availableIntentions.firstOrNull { it.text.contains(...) }`; the fix may legitimately leave the now-unused old import for the IDE's unused-import inspection;
   - run the single test class (`./gradlew :spring-core:test --tests "...TestName"` or the IDE runner). Fix-and-rerun loop: max 4 iterations, then stop and report.

7. **Branch, commit, PR.**
   - One feature = one branch off `public/main` = one PR. Stage only the feature's files explicitly (never `git add -A` on a dirty tree); conventional commit message (`feat: ...`).
   - If several sibling features need shared code (helpers, base classes, shared quick-fixes), put ALL shared pieces in one foundational branch/PR and stack the feature PRs on it (`gh pr edit <n> --base <foundational-branch>`); verify each feature branch has an empty `git diff --stat <base>..<branch> -- <shared files>`.
   - PR body: summary, rationale, covered mappings, tests; note any expected conflicts with sibling PRs. No internal links (e.g. sentry) in public PRs/issues.
   - If review comments arrive: apply fixes per branch, re-run that branch's tests, force-push, and reply to each review thread via `gh api .../replies` stating what changed.

## Output format

Report at the end:
- branch name and PR URL;
- inspection class, registration shortName, and bundle keys added;
- test class with pass count;
- any facts verified (FQN sources) and any scope deliberately excluded (with reason).

## Acceptance checklist

- [ ] No existing inspection covers the same case.
- [ ] Every hardcoded FQN/property key verified against authoritative javadoc or artifact.
- [ ] All gating (version, old-class presence, test-source) lives in `isAvailableForFile`; nothing per PSI element.
- [ ] Highlight targets a visible element, never an import statement.
- [ ] Quick-fix preserves annotation attributes where applicable and only fires when the new target resolves.
- [ ] `shortName` == description HTML filename; messages in the bundle; registration language matches the inspection kind.
- [ ] Positive, negative, and quick-fix tests pass.
- [ ] Branch `username/feature-name` pushed to `public`; PR targets `main` (or the foundational branch when stacked); only feature files committed.
