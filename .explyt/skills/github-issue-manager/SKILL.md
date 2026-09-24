---
name: "github-issue-manager"
schemaVersion: "v0.1"
description: "Classifies a user request as Bug, Compatibility, Feature, Question, or Task for the explyt/spring-plugin GitHub repo, lightly researches the relevant codebase, searches similar existing issues, and either reports an existing issue or creates a correctly titled and labeled one following the repository issue forms. Use when asked to create, file, triage, or deduplicate a spring-plugin issue from a task description, bug report, compatibility problem, or feature idea."
agent: General
used-by:
  - "General"
---

# GitHub Issue Manager (explyt/spring-plugin)

Create or find an issue in `explyt/spring-plugin` from a user description, following the repository issue forms and label conventions.

## Critical constraints

- Work only with the GitHub repository `explyt/spring-plugin`. If the user names another repository, stop and ask them to use a workflow configured for that repository.
- Never create a new issue when an open (or relevant closed) issue with the same core outcome already exists.
- Before creating an issue, always do light codebase research and a similar-issue search.
- Do not implement, deep-review, or refactor code: research only enough for a good title, body, labels, and template choice.
- Pick labels from the live repository label list (`gh label list --repo explyt/spring-plugin`); never invent label names.
- **Verify every label exists before passing it to `gh`**, including labels named in this skill and labels declared by the issue forms. `gh issue create` fails outright on an unknown label. Check with:
  `gh label list --repo explyt/spring-plugin --limit 200 --json name --jq '.[].name'` and match exactly.
- **Do not trust the `labels:` field in `.github/ISSUE_TEMPLATE/*.yml`.** A form may declare a label that no longer exists in the repository (verified: `plugin-bug` **does** exist in the live label list and is applied to filed bugs, while `feature-request`, declared by the feature form, is **absent**, so that form silently applies nothing). Treat the forms as the source of truth for *body structure only*, and verify each declared label against `gh label list` before relying on it.
- Use the live label catalog and the module/feature mapping below; the static list is a synchronization aid, not a substitute for refreshing GitHub. The repository has `type:internal` for engineering tasks, while form categories use `plugin-bug`, `compatibility`, and `question`. Use a matching `in:<module>` area label when one exists, plus only relevant feature/topic labels.
- Never pass component dropdown values such as `Navigation gutter` or `Autocompletion` as labels: they are form fields, not GitHub labels. Reference examples: #25/#44/#268 (`inspection`, `in:spring-core`), #118 (`inspection`, `in:spring-core`, `properties`), #142 (`inspection`, `in:spring-web`).
- If a needed label genuinely does not exist, file without it and say so in the report; never create labels, invent an `in:<module>` label, or substitute a similar-looking name.
- If issue creation is blocked (permissions, auth, missing tooling), return the ready-to-create title/body/labels and the exact blocker instead of retrying.
- When creating or proposing a new issue, always append as the final body line: `_Created automatically by github-issue-manager skill._`
- Never include secrets, tokens, private customer data, or local absolute paths in an issue body.
- For delegated codebase research use `call_ask_agent`; the subagent must return a complete summary inline (no artifact files needed for this light research).

## Live label catalog and module/feature mapping

The following names were verified against the live `explyt/spring-plugin` label catalog. Refresh them during every run; if GitHub returns a different set, the fetched names win and this section must be updated later.

### Labels

| Group | Existing labels |
|---|---|
| Form/category | `plugin-bug`, `compatibility`, `question`, `type:internal` |
| Module/area | `in:spring-core`, `in:spring-web`, `in:spring-data`, `in:spring-messaging`, `in:spring-initializr`, `in:spring-debugger` |
| Feature/topic | `inspection`, `properties`, `open-api`, `performance`, `native-link` |
| Contributor/workflow | `good first issue`, `state:in-progress`, `state:planned`, `state:review`, `duplicate`, `wontfix` |

`feature-request`, `help wanted`, and area labels for modules not listed below are **not** in the current live catalog. Do not use them unless a fresh label fetch proves that they have been added.

### Module-to-label synchronization

Keep this mapping aligned with the module map in `CONTRIBUTING.md` and the directories under `modules/`:

| Module or project area | Area label |
|---|---|
| `spring-core` | `in:spring-core` |
| `spring-web` | `in:spring-web` |
| `spring-data` | `in:spring-data` |
| `spring-messaging` | `in:spring-messaging` |
| `spring-initializr` | `in:spring-initializr` |
| debugger support | `in:spring-debugger` |
| `spring-security`, `spring-integration`, `spring-cloud`, `spring-aop`, `spring-ai` | no current area label — omit it |
| `quarkus-core`, `jpa`, `spring-gradle`, `base`, `test-framework`, `spring-bootstrap` | no current area label — omit it |

For an unlabelled module, use a feature/topic label when it accurately describes the issue (for example, `inspection` or `performance`) and mention the missing area label in the report. `native-link` is the topic label for Native Context Mode / native project linking, not a module label.

### Main feature/topic labels

- `inspection`: inspections and their quick fixes.
- `properties`: Spring configuration/property metadata, resolution, completion, or navigation.
- `open-api`: OpenAPI/Swagger support.
- `performance`: slow operations, freezes, or other performance work when the issue is not specifically an EDT bug.
- `native-link`: Native Context Mode or native project linking.

Use `compatibility`, `question`, and `type:internal` for issue category, not as feature labels. Apply `good first issue` only under the two-lane policy below; maintainers normally own the state and disposition labels.

## Inputs

- The user's description of a problem, idea, or work item.
- An explicit category if the user names one; otherwise infer it from the description.
- Repository: `explyt/spring-plugin` by default.
- GitHub access via `gh` CLI (must be authenticated) or GitHub MCP tools if available in the session.
- For bugs: plugin version, IDE name/build, and build system if the user provided them. All three are **required** fields of the bug form — if any is missing, ask the user once before creating.

## Classification rules

Map the request to one of the repository issue forms (in `.github/ISSUE_TEMPLATE/`):

The `Labels to apply` column lists only labels from the synchronized catalog above; re-verify against the live list before every `gh` call, because the forms declare labels the repository may no longer have.

| Category | Form | Title prefix | Labels to apply | When |
|---|---|---|---|---|
| Bug | `1-plugin-bug-report.yml` | `[BUG] ` | `plugin-bug` + optional area/topic labels | Unexpected behavior, exception, red balloon / IDE internal error, false-positive inspection, broken navigation/completion, freeze, stack trace |
| Compatibility | `2-compatibility.yml` | `[COMPATIBILITY]` | `compatibility` + optional area/topic labels | Problem specific to an IDE version, OS, JDK, or other-plugin combination |
| Feature | `3-feature-request.yml` | `[FEATURE]` | area and/or topic labels; the form's declared `feature-request` is absent | New user-facing capability or enhancement of an existing one |
| Question | `4-ask_question.yml` | `[QUESTION] ` | `question` + optional area/topic label when useful | Usage question; prefer redirecting to GitHub Discussions or Telegram (see `config.yml` contact links) before filing |
| Task | no form | none | `type:internal` + optional area/topic labels | Refactoring, docs, infrastructure, cleanup, engineering work; create as a plain issue with Description / Additional context sections |

- If the user explicitly set a category, use it unless it is clearly wrong; if you correct it, explain why.
- The bug and feature forms include a **Component** dropdown; pick one of: `Inspection`, `Navigation gutter`, `References`, `Autocompletion`, `Usages`, `Properties`, `OpenAPI`, `Slow operation on EDT`, `Other`.

## Two-lane label policy (from CONTRIBUTING.md §2)

- **Lane A — roadmap items**: implemented by the Explyt team; no extra labels needed.
- **Lane B — contributor-friendly**: if the issue is genuinely optional, well-scoped, and a longer wait costs nothing (small UX, docs, messages, one focused inspection), suggest the `good first issue` label and say so in the report. Only suggest it if it exists in the live label list — as of 2026-08 `good first issue` exists but `help wanted` does **not**. Do not apply it to work needing non-trivial PSI/resolve logic, language-version gating, or multi-namespace handling.

## Body templates

Mirror the form headings so CLI-created issues look identical to form-created ones.

### Bug body

```md
### Component

...

### Problem details

...

### Steps to reproduce

...

### Additional information

...

### Plugin version

...

### IDE name and build

...

### Build system

Gradle | Maven | IntelliJ

### Additional context

...

_Created automatically by github-issue-manager skill._
```

### Compatibility body

```md
### Problem Description

...

### Steps to Reproduce

...

### IntelliJ IDEA Version

...

### Plugin version

...

### Operating System

...

### Other Dependencies

...

_Created automatically by github-issue-manager skill._
```

### Feature body

```md
### Component

...

### Describe the problem

...

### Describe the solution

...

### Additional context

...

_Created automatically by github-issue-manager skill._
```

### Question body

```md
### Your Question

...

_Created automatically by github-issue-manager skill._
```

### Task body

```md
### Description

...

### Additional context

...

_Created automatically by github-issue-manager skill._
```

Fill unknown optional fields with `_No response_`. For a Bug, replace the required Plugin version, IDE name and build, and Build system fields with actual values; never use `_No response_` for them. Build system must be exactly one of `Gradle`, `Maven`, or `IntelliJ`.

## Algorithm

1. **Normalize the request**
   - Execution: single; reason: input normalization belongs to the orchestrator.
   - Extract title idea, category, affected component, symptoms, stack traces, versions, links, and acceptance criteria.
   - If the category is absent, infer it with `Classification rules`.
   - If a bug report lacks the plugin version, IDE build, or build system, ask the user one concise clarification before continuing to creation (searching may proceed).

2. **Lightly research the codebase**
   - Execution: parallelizable with step 3; axis: code area vs GitHub metadata; role: `call_ask_agent` codebase researcher.
   - Ask the subagent to identify the relevant module under `modules/` (e.g. `spring-core`, `spring-web`, `spring-data`), classes, inspection/provider names, and good search keywords; require a complete summary inline as the result format.
   - Keep research shallow: enough for dedupe, component choice, title, and body.

3. **Collect GitHub metadata**
   - Execution: parallelizable with step 2.
   - Fetch live labels and their descriptions: `gh label list --repo explyt/spring-plugin --limit 200 --json name,description`.
   - Compare the fetched names with the module map in `CONTRIBUTING.md` and the synchronization table above. Keep exact names of applicable labels only, and confirm each intended label appears verbatim in the fetched output before using it.
   - Treat labels missing from the live output as unavailable, even if they appear in this skill or an issue form. In particular, do not use `feature-request`, `help wanted`, or an invented `in:<module>` label.
   - Also inspect 2-3 existing issues of the same category (`gh issue view <n> --repo explyt/spring-plugin --json labels`) to copy the actual labeling convention rather than relying on the forms.

4. **Search for duplicates and related issues**
   - Execution: single, after steps 2–3.
   - Run up to 4 query variants against `repo:explyt/spring-plugin`: exact title terms, component terms, code identifiers from research, user-visible error/feature text. Example: `gh search issues --repo explyt/spring-plugin "OpenAPI completion" --state open`.
   - Search open issues first, then closed ones for already-fixed or superseded work.
   - For bugs include exception class names and message text; for features include capability and component names.

5. **Decide: existing vs new**
   - Treat an issue as existing when the core requested outcome is the same, even if the wording, labels, or form differ.
   - If a match exists, stop and return its number, title, URL, state, labels, and a short match explanation.
   - If only related issues exist, keep them for `Additional context` and continue.

6. **Prepare title, labels, and body**
   - Title: form prefix + concise problem/outcome statement (e.g. `[BUG] False positive SpringKotlinObjectInspection on @Component object`).
   - Labels: retain the matching category label (`plugin-bug`, `compatibility`, or `question`) when applicable; add at most one matching area label and at most two relevant topic labels from the live list. For tasks, use `type:internal` instead of a form label. Add `good first issue` only when the two-lane policy permits it. For a Feature, never use the absent `feature-request` label; it may have only area/topic labels.
   - Body: fill the matching template above; add investigated modules/classes and related issue links to `Additional context`.

7. **Create the issue or produce a dry payload**
   - Execution: single; creation is a side effect and must happen once.
   - If the user asked for a dry run, output the payload without creating.
   - Else create with `gh issue create --repo explyt/spring-plugin --title "..." --body-file <tmpfile>`. Add `--label <comma-separated verified labels>` only when the prepared label set is non-empty; never pass a form label that was absent from the live catalog.
   - Do not retry `gh issue create`: an ambiguous failure may have created the issue already. Report the exact failure and the ready payload as `blocked`.
   - Never create more than one issue per user request.

8. **Report the result** using the Output format below. Include the selected category and a short reason in the required `Classification` line, including when reporting an existing issue or a blocked creation.

## Output format

Return exactly one of:

### Existing issue

- Outcome: `existing`
- Classification: Bug | Compatibility | Feature | Question | Task — short reason
- Issue: `#number title`
- URL:
- State:
- Labels:
- Why it matches:

### Created issue

- Outcome: `created`
- Classification: Bug | Compatibility | Feature | Question | Task — short reason
- Issue: `#number title`
- URL:
- Category: Bug | Compatibility | Feature | Question | Task
- Labels:
- Unavailable labels omitted:
- Related issues considered:

### Blocked

- Outcome: `blocked`
- Classification: Bug | Compatibility | Feature | Question | Task — short reason
- Blocker:
- Proposed title:
- Proposed category:
- Proposed labels:
- Unavailable labels omitted:
- Proposed body:
- Related issues considered:

## Acceptance checklist

- [ ] Request classified as Bug, Compatibility, Feature, Question, or Task with a short reason.
- [ ] Relevant codebase context lightly researched before creation.
- [ ] Similar open and closed issues searched with multiple query variants.
- [ ] No new issue created when a substantially matching issue already exists.
- [ ] Created or proposed issue uses the matching form's title prefix, headings, and only verified applicable labels; Feature issues do not use the absent `feature-request` label.
- [ ] Bug creation asked for plugin version, IDE build, and build system when any is missing, and the body contains one selected build system.
- [ ] Every applied label verified verbatim against the live label list and mapped to a current module/feature/category (not taken blindly from the issue form's `labels:` field); absent module labels were omitted and reported in `Unavailable labels omitted`; two-lane policy considered.
- [ ] Body ends with `_Created automatically by github-issue-manager skill._`.
- [ ] Final response follows one of the declared output formats.
- [ ] At most one issue created for one user request.
