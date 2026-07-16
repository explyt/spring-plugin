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

- Work only with the GitHub repository `explyt/spring-plugin` unless the user explicitly names another repository.
- Never create a new issue when an open (or relevant closed) issue with the same core outcome already exists.
- Before creating an issue, always do light codebase research and a similar-issue search.
- Do not implement, deep-review, or refactor code: research only enough for a good title, body, labels, and template choice.
- Pick labels from the live repository label list (`gh label list --repo explyt/spring-plugin`); never invent label names.
- Template labels are applied automatically only via the web forms; when creating through `gh` CLI, add the matching template label explicitly (`plugin-bug`, `compatibility`, `feature-request`, `question`).
- If issue creation is blocked (permissions, auth, missing tooling), return the ready-to-create title/body/labels and the exact blocker instead of retrying.
- When creating or proposing a new issue, always append as the final body line: `_Created automatically by github-issue-manager skill._`
- Never include secrets, tokens, private customer data, or local absolute paths in an issue body.
- For delegated codebase research use `call_ask_agent`; the subagent must return a complete summary inline (no artifact files needed for this light research).

## Inputs

- The user's description of a problem, idea, or work item.
- An explicit category if the user names one; otherwise infer it from the description.
- Repository: `explyt/spring-plugin` by default.
- GitHub access via `gh` CLI (must be authenticated) or GitHub MCP tools if available in the session.
- For bugs: plugin version, IDE name/build, and build system if the user provided them. Plugin version and IDE build are **required** fields of the bug form — if missing, ask the user once before creating.

## Classification rules

Map the request to one of the repository issue forms (in `.github/ISSUE_TEMPLATE/`):

| Category | Form | Title prefix | Auto label | When |
|---|---|---|---|---|
| Bug | `1-plugin-bug-report.yml` | `[BUG] ` | `plugin-bug` | Unexpected behavior, exception, red balloon / IDE internal error, false-positive inspection, broken navigation/completion, freeze, stack trace |
| Compatibility | `2-compatibility.yml` | `[COMPATIBILITY]` | `compatibility` | Problem specific to an IDE version, OS, JDK, or other-plugin combination |
| Feature | `3-feature-request.yml` | `[FEATURE]` | `feature-request` | New user-facing capability or enhancement of an existing one |
| Question | `4-ask_question.yml` | `[QUESTION] ` | `question` | Usage question; prefer redirecting to GitHub Discussions or Telegram (see `config.yml` contact links) before filing |
| Task | no form | none | none | Refactoring, docs, infrastructure, cleanup, engineering work; create as a plain issue with Description / Additional context sections |

- If the user explicitly set a category, use it unless it is clearly wrong; if you correct it, explain why.
- The bug and feature forms include a **Component** dropdown; pick one of: `Inspection`, `Navigation gutter`, `References`, `Autocompletion`, `Usages`, `Properties`, `OpenAPI`, `Slow operation on EDT`, `Other`.

## Two-lane label policy (from CONTRIBUTING.md §2)

- **Lane A — roadmap items**: implemented by the Explyt team; no extra labels needed.
- **Lane B — contributor-friendly**: if the issue is genuinely optional, well-scoped, and a longer wait costs nothing (small UX, docs, messages, one focused inspection), suggest the `good first issue` or `help wanted` label and say so in the report. Only suggest these labels if they exist in the live label list.

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

### Task body

```md
### Description

...

### Additional context

...

_Created automatically by github-issue-manager skill._
```

Fill unknown optional fields with `_No response_`.

## Algorithm

1. **Normalize the request**
   - Execution: single; reason: input normalization belongs to the orchestrator.
   - Extract title idea, category, affected component, symptoms, stack traces, versions, links, and acceptance criteria.
   - If the category is absent, infer it with `Classification rules`.
   - If a bug report lacks the plugin version or IDE build, ask the user one concise clarification before continuing to creation (searching may proceed).

2. **Lightly research the codebase**
   - Execution: parallelizable with step 3; axis: code area vs GitHub metadata; role: `call_ask_agent` codebase researcher.
   - Ask the subagent to identify the relevant module under `modules/` (e.g. `spring-core`, `spring-web`, `spring-data`), classes, inspection/provider names, and good search keywords; require a complete summary inline as the result format.
   - Keep research shallow: enough for dedupe, component choice, title, and body.

3. **Collect GitHub metadata**
   - Execution: parallelizable with step 2.
   - Fetch live labels: `gh label list --repo explyt/spring-plugin --limit 100`.
   - Keep exact names of applicable labels only.

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
   - Labels: the matching template label plus 0–2 extra labels from the live list; apply the two-lane policy.
   - Body: fill the matching template above; add investigated modules/classes and related issue links to `Additional context`.

7. **Create the issue or produce a dry payload**
   - Execution: single; creation is a side effect and must happen once.
   - If the user asked for a dry run, output the payload without creating.
   - Else create: `gh issue create --repo explyt/spring-plugin --title "..." --body-file <tmpfile> --label <template-label>[,extra]`.
   - On permission/auth failure, retry at most once, then report `blocked` with the ready payload.
   - Never create more than one issue per user request.

8. **Report the result** using the Output format below.

## Output format

Return exactly one of:

### Existing issue

- Outcome: `existing`
- Issue: `#number title`
- URL:
- State:
- Labels:
- Why it matches:

### Created issue

- Outcome: `created`
- Issue: `#number title`
- URL:
- Category: Bug | Compatibility | Feature | Question | Task
- Labels:
- Related issues considered:

### Blocked

- Outcome: `blocked`
- Blocker:
- Proposed title:
- Proposed category:
- Proposed labels:
- Proposed body:
- Related issues considered:

## Acceptance checklist

- [ ] Request classified as Bug, Compatibility, Feature, Question, or Task with a short reason.
- [ ] Relevant codebase context lightly researched before creation.
- [ ] Similar open and closed issues searched with multiple query variants.
- [ ] No new issue created when a substantially matching issue already exists.
- [ ] Created or proposed issue uses the matching form's title prefix, headings, and template label.
- [ ] Bug creation asked for plugin version and IDE build when missing.
- [ ] Labels taken from the live repository label list, never invented; two-lane policy considered.
- [ ] Body ends with `_Created automatically by github-issue-manager skill._`.
- [ ] Final response follows one of the declared output formats.
- [ ] At most one issue created for one user request.
