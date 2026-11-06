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
     - migration inspections gate on the REPLACEMENT symbol being resolvable: a gate on the legacy symbol disables itself once the upgrade removes that symbol, exactly when stale source most needs reporting. Gate on the OLD class only when the inspection reports use of a legacy API that must still be present (e.g. Jackson 2 `ObjectMapper` injection) or a package rename where a fresh project without legacy `javax.*` should skip entirely. Use the `isClassAvailable` / `isAnyClassAvailable` helpers from the version-gated base (create them there if missing: `JavaPsiFacade.findClass(fqn, file.resolveScope) != null`);
     - a replacement gate is not a version discriminator (Framework can be upgraded under an older Boot): keep the version floor too, and change it per inspection by overriding the base's `isSupportedBootVersion(file)` hook, never per PSI element;
     - visitors must recognise UNRESOLVED legacy source (null annotation FQN, short written type name); reuse the base's `legacyAnnotationFqn` / `legacyTypeFqn` / `writtenAnnotationName` and give removal-case fixtures only the replacement classes; cover the deprecation window separately with both APIs present;
     - test-only features (e.g. `@SpringBootTest` checks): require `ExplytPsiUtil.isTestFiles(file)`.

4. **Implement detection + quick-fix.** Reuse existing quick-fixes before writing new ones:
   - annotation rename / FQN swap preserving attributes → `RewriteAnnotationQuickFix(newFqn, ownerJavaPsi, attributes, oldFqn)`; rebuild attributes via `factory.createAnnotationFromText("@$newFqn($argsText)", owner).parameterList.attributes`;
   - package move with unchanged simple name → highlight the type reference, fix with `MigrateImportQuickFix(oldFqn, newFqn)`;
   - property key rename → highlight `propertyKeyPsiElement()`, fix with `ReplacementKeyQuickFix` (robust for `.properties`; for YAML show the suggested key in the message);
   - add a class-level annotation → `LocalQuickFix.from(AddAnnotationModCommandAction(fqn, javaPsi))`.
   - Only propose a migration the project can satisfy: check the NEW target is resolvable before registering the problem.

5. **Register.** In the module's plugin.xml:
   - `language="UAST"` for UAST-based inspections; `language="Properties"` / `language="yaml"` for single-format property inspections (repo precedent); omit `language` only when one class must serve multiple file types (e.g. `.properties` + YAML via `checkFile`, or raw-PSI visitors);
   - `shortName` must equal the description HTML filename in `inspectionDescriptions/<ShortName>.html` — create it; never rename an existing `shortName` (user profiles persist suppressions and severities by it), change the display name instead;
   - while a legacy symbol is only deprecated (it still resolves) the platform already reports the deprecation: use `WEAK_WARNING` with a `<base>.deprecated` bundle key; keep full strength once it is gone;
   - messages go to the module bundle (`messages/SpringCoreBundle.properties`); no-arg messages must not double apostrophes, parameterized (`{0}`) messages must double them.

6. **Test.** Base classes `ExplytInspectionJavaTestCase` / `ExplytInspectionKotlinTestCase`; declare `override val libraries: Array<TestLibrary>` (add a new `TestLibrary` coordinate if the version is missing — it downloads from Maven Central). Cover: positive highlight, negative (new API / unrelated code), quick-fix result. Known gotchas:
   - the highlight marker must wrap the WHOLE visible element: `<warning>@Annotation(args)</warning>` including `@` and arguments; for type references just the type name token;
   - stub classes removed/relocated from real jars with `myFixture.addClass(...)`; annotation stubs need `@Retention(RUNTIME)` or annotation-rewrite fixes won't offer in-code placement;
   - `configureByText("src/test/java/X.java", ...)` fails ("Invalid file name"); for test-source gating mark a root in `setUp`: `PsiTestUtil.addSourceRoot(module, myFixture.tempDirFixture.findOrCreateDir("src/test/java"), true)`, then `myFixture.addFileToProject(path, text)` + `configureFromExistingVirtualFile(...)`;
   - For removal-case fixtures, keep legacy imports unresolved and suppress only expected unresolved-reference diagnostics in the highlighting check; add legacy stubs only in separate deprecation-window fixtures.
   - prefer semantic quick-fix assertions on `myFixture.file.text` (contains new annotation/import, old one gone) over exact `checkResult`; find intentions with `availableIntentions.firstOrNull { it.text.contains(...) }`; the fix may legitimately leave the now-unused old import for the IDE's unused-import inspection;
   - run the single test class (`./gradlew :spring-core:test --tests "...TestName"` or the IDE runner). Fix-and-rerun loop: max 4 iterations, then stop and report.

7. **Branch, commit, PR.**
   - One feature = one branch off `public/main` = one PR. Stage only the feature's files explicitly (never `git add -A` on a dirty tree); conventional commit message (`feat: ...`).
   - If several sibling features need shared code (helpers, base classes, shared quick-fixes), put ALL shared pieces in one foundational branch/PR and stack the feature PRs on it (`gh pr edit <n> --base <foundational-branch>`); verify each feature branch has an empty `git diff --stat <base>..<branch> -- <shared files>`.
   - PR body: summary, rationale, covered mappings, tests; note any expected conflicts with sibling PRs. No internal links (e.g. sentry) in public PRs/issues.
   - If review comments arrive: apply fixes per branch, re-run that branch's tests, force-push, and reply to each review thread via `gh api .../replies` stating what changed.

## Native settings inside configuration maps

For a diagnostic on a passthrough map such as `spring.jpa.properties`, verify two separate boundaries before implementing:

1. Bind real properties/YAML through Boot `Binder` into the declared map type, then pass the result to the library's actual setting consumer. Do not replace this with an isolated `Map.get` test. Record exact artifact versions and observed output; keep credentials and project values out of the evidence.
2. Distinguish the map prefix's relaxed binding from the native map-key spelling. With Boot 4.1.1/Hibernate 7.4.5 and Boot 3.5.8/Hibernate 6.6.36, `hibernate.show_sql=true` enables `SqlStatementLogger` stdout, while `hibernate.show_SQL=true` and `hibernate.show-sql=true` do not. Hibernate reads `JdbcSettings.SHOW_SQL` through exact `ConfigurationHelper.getBoolean` lookup.
3. Test simultaneous case variants separately. Unbracketed Spring configuration names can alias source values even while the bound map retains both original spellings; bracket keys keep their identities independent. Do not infer collision precedence for every source/version from one probe. Test environment-variable name adaptation independently: underscores can become dots in the native key. A correctly spelled file key with an environment-backed value avoids that ambiguity.
4. Inspect catalogue completeness. Hibernate `StandardServiceRegistryBuilder` retains arbitrary settings, and extension services can read the complete map. An absent `AvailableSettings` constant does not prove a setting is invalid. Never reject every unknown map key; a unique case-only match to a known native setting can justify a targeted warning and an exact-spelling quick fix. Keep ordinary navigation exact so it does not conceal the typo.
5. Check the existing inspection's map exemption before diagnosing a missing reference. `SpringBasePropertyInspection.getMapKeys` deliberately accepts arbitrary entries, and `PropertyUtil.isKebabCaseInMapKey` skips their canonical-form check. Native-key spelling diagnostics need a provider-aware path, not removal of the general map exemption.
6. Distinguish an unavailable/loading catalogue from a ready catalogue with no match. `ConfigurationMapKeyTargets` currently returns an empty list in both cases, including cold asynchronous Hibernate lookup. Do not report an unknown-key diagnostic solely from that empty list; require established catalogue readiness/completeness.
7. For a key quick fix, preserve the map prefix, the value and surrounding structure. Test properties, nested/flat YAML and bracket syntax; a native setting key must not be rewritten by the generic kebab-case fix.

### Choose the native configuration consumer before its catalogue

- Verify the Boot wrapper and forwarding code, then the actual library version/consumer. A guessed `.properties` suffix is not a supported integration. Measure key-only occurrences separately in main/shared config and tests before ranking features; exclude generated files and report parse failures.
- For Kafka, include Spring serializer/deserializer extension holders, not only Apache client `ConfigDef`/constants: real `spring.kafka.properties.spring.json.*` keys are consumed by Spring Kafka and can be absent from Apache's catalogue. Route common/producer/consumer/admin/streams maps to the relevant consumer set, preserve aliases and security/prefixed namespaces, and do not classify every static String or documentation constant as a key. Inspect PSI/metadata instead of executing arbitrary project classes in the plugin JVM.
- For Mail, verify the selected provider/protocol. Standard property tables and concatenated reads such as `mail.` + protocol + suffix do not form a Hibernate-like constant holder; prefer documented completion/value hints and honest consumer targets.
- For Hikari/UCP driver properties, recognize `java.util.Properties` wrappers and establish driver/DataSource identity before using PostgreSQL `PGProperty`, MySQL `PropertyKey` or implementation setters. Tomcat `connection-properties` is a serialized String rather than a map in verified Boot metadata. Do not copy Hibernate case semantics to every JDBC driver.
- For Quartz, combine fixed core keys with the configured job-store/thread-pool/plugin class's reflective bean properties. Dynamic extension namespaces make a closed allowlist unsafe.
- Treat Flyway placeholders and Liquibase parameters as application-defined names: link their SQL/changelog usages rather than inventing library-key catalogues. Verify a real wrapper before proposing generic JCache/Netty/Redis/Rabbit passthrough maps; typed Boot options or Java customizers are different entry points.

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
