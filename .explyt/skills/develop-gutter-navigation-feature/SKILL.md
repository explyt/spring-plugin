---
name: "develop-gutter-navigation-feature"
schemaVersion: "v0.1"
description: "End-to-end workflow for adding or repairing a navigation gutter / line marker in the explyt/spring-plugin repository: the two independent provider entry points, the lazy-targets contract of NavigationGutterIconBuilder, module resolution and search scope for library PSI, honest empty-state wording, target renderers for expression targets, and wiring-level tests through SpringGutterTestUtil. Use when the user asks to add a line marker or gutter icon, reports that a gutter finds nothing or says 'No matching ... found', asks why a gutter popup shows raw code or an odd separator, wants navigation to work in both directions, or asks to fix Navigate to event publisher / Related Symbol behaviour."
agent: General
---

# Develop or Repair a Gutter Navigation Feature (explyt/spring-plugin)

Produce a working, registered, wiring-tested `RelatedItemLineMarkerProvider` (or repair an existing one) on its own branch with a PR to `main`, using the platform facts verified in the #410/#411/#427 series.

## Critical constraints

- Call the `coding-guard` skill first and follow it for style, threading, SPDX headers, bundles, statistics and PR hygiene. Do not restate its rules here.
- A provider has **two independent entry points**: highlighting (`collectSlowLineMarkers` → `collectNavigationMarkers`) and `Navigate | Related Symbol` (`RelatedItemLineMarkerGotoAdapter` → `collectNavigationMarkers` **directly**). Any gate must exist in both; never "move the per-element gate up" into the batch method.
- `setTargetRenderer` is **inert** on the Related Symbol path — it renders `GotoRelatedItem.customName` / `customContainerName` via `DefaultPsiElementCellRenderer`. Presentation fixes that must hold on both surfaces need two mechanisms.
- Never widen `moduleWithDependenciesAndLibrariesScope` to find a reference declared in library **source**: `ModuleWithDependenciesScope.RootCalculator.calcRoots` maps every non-module order entry to `OrderRootType.CLASSES`, so a `*-sources.jar` root is not in that scope at all. Never let a feature's answer depend on whether the user pressed *Download Sources*.
- Never phrase an empty state as a fact about the world. `No usages found` (still live in `SpringCoreBundle`) claims nothing exists; the code proved only that nothing was found in indexed project sources. The event-publisher key was fixed this way in #426: `No publisher found in project sources`. Assert the installed text with `SpringGutterTestUtil.getGutterEmptyText`.
- Do not build the renderer/title inside a test to assert it. Assert through the wiring (see Algorithm step 6).
- Push feature branches to the `public` remote (`explyt/spring-plugin`); PRs target its `main`. Never push to `origin` — it is an internal GitLab mirror. Use the `gh` CLI with **both** `--repo explyt/spring-plugin` and `--head <branch>`, not the GitHub MCP tools.

## Inputs

Resolve before starting:

- **Direction(s)**: which element carries the icon and what the targets are. For a bidirectional feature, both directions are separate target suppliers and usually separate defects.
- **Target kind**: declarations (`PsiMethod`, `PsiClass`) or expressions/statements. This decides whether a target renderer is mandatory.
- **Anchor origin**: can the element carrying the icon live inside a library? If yes, module resolution and scope are part of the design, not an afterthought.
- **Target module** and **branch name** `username/feature-name` off `public/main`.

## Algorithm

1. **Duplicate and prior-art check.** Grep `modules/*/src/main/resources/META-INF/*plugin*.xml` for `codeInsight.lineMarkerProvider` and read the closest provider in `modules/*/src/main/kotlin/**/providers/`. For a repair, `git blame` the suspect line first: a guard inside a bulk squash-import commit carries no design intent, and attributing intent to it makes a cheap fix look risky.

2. **Classify the defect (repair only).** Decide which of these the report is, because the fix site differs:
   - *icon present, popup empty* → target supplier returns nothing (steps 4–5), or the empty state is honest and the wording is the defect (step 7);
   - *popup rows show raw source* → missing target renderer (step 3);
   - *separator reads "XML"* → single-argument `NavigationGutterIconBuilder.create(icon)`; use `create(icon, navigationGroup)` with a bundle key. That two-argument factory hardcodes `element -> List.of(new GotoRelatedItem(element, navigationGroup))`, so a custom `GotoRelatedItem` subclass needs the three-argument `create(icon, converter, gotoRelatedItemProvider)` instead;
   - *works one way only* → treat each direction as its own defect with its own test.

3. **Build the marker.** Reuse the neighbouring provider's builder chain. Two non-obvious contracts:
   - `setTargets(NotNullLazyValue)` sets `myLazy = true` as a **side effect**, which (a) disables the empty-targets early-out `if (!myLazy && myTargets.getValue().isEmpty()) return null`, (b) makes `isEmpty()` always `false`, and (c) enables `computeTargetsInBackground` → `navigateTargetsAsync` (app executor + `runInReadActionWithWriteActionPriority` + gutter loading icon). Consequence: an expensive target search is acceptable **on the gutter-click path**, which is off the EDT; the same supplier is also reached from `Navigate | Related Symbol`, where that guarantee has not been verified. And the icon is installed even when there are no targets — so "icon present, popup empty" is the normal consequence of lazy targets, and the empty-state text is load-bearing.
   - If targets are **not** `PsiNamedElement` (call expressions, statements), `setTargetRenderer` is mandatory: the platform's default presentation falls through to `element.text` with a `null` container. Copy `SpringBeanLineMarkerProvider.getTargetRender()` or `SpringWebUtil.getTargetRenderer()`; do not write a third.
   - Anchor the marker on a **leaf** element. `LineMarkerInfo` logs `Performance warning: LineMarker is supposed to be registered for leaf elements only` for anything with a first child — and `LOG.error` in unit-test mode, so a non-leaf anchor fails the tests. This is why `EventListenerLineMarkerProvider` anchors on `uCallExpression.methodIdentifier.sourcePsiElement`, not on `sourcePsi`.
   - Keep marker creation cheap (highlighting hot path): read only declared types there and leave meta-annotation / index resolution to the lazy supplier.
   - Put `StatisticService.addActionUsage` **inside the lazy target supplier**, not at marker creation: the marker is built on every highlighting pass, so counting there measures file opening rather than clicks (`coding-guard` §6 does not say where).

4. **Resolve the module correctly.** `ModuleUtilCore.findModuleForPsiElement` reaches its library branch only for a `PsiFileSystemItem`, so an ordinary expression PSI inside a jar resolves to `null` and the idiom `?: return emptyList()` silently kills the feature there. If the anchor can come from a library:

   ```kotlin
   val elementModule = ModuleUtilCore.findModuleForPsiElement(element)
   val module = elementModule
       ?: element.containingFile?.originalFile?.let { ModuleUtilCore.findModuleForPsiElement(it) }
       ?: return emptyList()
   ```

5. **Choose the search scope by anchor origin.** A library-anchored call has no owning module: the platform answers with the minimum by `moduleDependencyComparator`, whose dependency closure is an arbitrary slice of the project. Use `elementModule == null` as the signal and widen to `GlobalSearchScope.projectScope` **only** in that case, so the in-project path stays byte-identical. Cache the widened result under its own `Key` with the same modification tracker, so it cannot collide with the per-module cached value on the same holder.

6. **Test through the wiring.** Reuse `SpringGutterTestUtil` (module `test-framework`) — `getGutterTargetsStrings`, `getGutterTargetPresentations`, `getGutterPopupTitle`, `getGutterTargetGroups` — and add new traversals there rather than in a test. The platform exposes no public accessor for the installed renderer or title, so reflection is required; every reflective read must raise a **named** `AssertionError` both for an absent value and for an absent field.
   - Assert the **exact expected target count**, never `isNotEmpty()`.
   - For a library-specific defect use `TestLibrarySourceRoot` (`modules/test-framework/.../test/util/`) and keep its preconditions: `ProjectFileIndex.isInLibrarySource(vFile)` is true, and `ModuleUtilCore.findModuleForPsiElement(anchor)` is `null`. Its root must live outside every content root; a cross-module test must *ask* which module the platform anchored the call in and place the other side in the module it did **not** pick. Full trap list: memory `Library-sources test fixture traps`.
   - Before concluding a scope defect is untestable, grep the path under test for `GlobalSearchScopeTestAware` — it returns `allScope()` in unit-test mode and hides scope defects on the paths that use it.
   - Java/Kotlin twins are the default; skipping one is allowed only when the defect is provably before UAST (module resolution, scope) — say so in the PR.

7. **Word the empty state as a fact about the search.** Prefer `No publisher found in project sources` over `No matching event publisher found`. Keys live in the module bundle; a parameterized variant needs its own key, and apostrophe doubling differs between zero-argument and parameterized keys.

8. **Register, verify, PR.**
   - Register in the **owning module's** plugin descriptor, `modules/<name>/src/main/resources/META-INF/<name>-plugin.xml`: `codeInsight.lineMarkerProvider language="UAST"` for UAST providers, `Properties` / `yaml` for config formats (one entry per language, same class).
   - Red-green proof, **per production change, not per file**: revert one change at a time and confirm exactly the tests that own it fail. Use `git stash push -- <file>` only when a change occupies a whole file; when two changes share one file (common here — provider logic and a bundle value, or two methods of the same provider), revert the individual lines with an editor and confirm with `git diff`. Never a `/tmp` copy: it goes stale and silently restores a half-old file. Before stashing, check `git stash list` and that the worktree is clean on unrelated paths; on a conflicting restore use `git stash apply`, not `pop`. Count red failures against tests written: fewer means the passing ones are unproven.
   - Run tests sequentially via `run_command` with `./gradlew --no-configuration-cache <module>:test --rerun --tests "..."`. Add `--offline` from the second run onwards; the first run of a test that declares a new `TestLibrary` must fetch it from Maven Central and fails offline. Believe only the exit code and the `N tests completed, M failed` line; verify the XML mtime is newer than the run start. Fix-and-rerun loop: max 4 iterations, then stop and report.
   - One logical change per PR; stage explicit paths; add one `[Unreleased]` CHANGELOG line citing the issue.

## Output format

Report at the end:
- branch name and PR URL;
- provider class, registration entries added/changed, bundle keys added;
- for each direction: the anchor element, the target kind, and whether a target renderer is installed;
- red-phase evidence: exit code, `N tests completed, M failed`, and the failure message of each new test;
- green-phase evidence: per-class `tests`/`failures` counts;
- anything deliberately out of scope, with the reason.

## Acceptance checklist

- [ ] Any suppression/eligibility gate exists in **both** `collectSlowLineMarkers` and `collectNavigationMarkers`.
- [ ] Targets that are not `PsiNamedElement` have a target renderer installed; presentation claims that must hold on `Navigate | Related Symbol` also carry a `GotoRelatedItem`-side mechanism.
- [ ] `create(icon, navigationGroup)` is used with a bundle key, not the single-argument factory.
- [ ] Module resolution has a containing-file fallback wherever the anchor can come from a library.
- [ ] The widened scope applies only to library-anchored anchors; the in-project path is unchanged and cached separately.
- [ ] No fix relies on library **sources** being present or on widening `moduleWithDependenciesAndLibrariesScope`.
- [ ] The marker is anchored on a leaf element (no `Performance warning: LineMarker is supposed to be registered for leaf elements only`).
- [ ] `addActionUsage` sits inside the lazy target supplier, not at marker creation.
- [ ] Empty-state text states a fact about the search, not about the world, and is asserted via `getGutterEmptyText`.
- [ ] Tests assert through the installed gutter (no renderer constructed in the test), assert exact target counts, and any reflective read fails by name for both absent value and absent field.
- [ ] Library-specific tests assert `isInLibrarySource` and a `null` module for the anchor; cross-module tests derive placement from the module the platform picked.
- [ ] Red phase fails every new test inside its own defect; a two-part change has a per-part red phase.
- [ ] Branch `username/feature-name` pushed to `public`; PR targets `main`; exactly one `[Unreleased]` line citing the issue.
