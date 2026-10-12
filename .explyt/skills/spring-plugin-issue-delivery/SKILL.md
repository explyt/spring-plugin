---
name: "spring-plugin-issue-delivery"
schemaVersion: "v0.1"
description: "Orchestrates one explyt/spring-plugin GitHub issue end to end by routing work to Test, Code, Review and Ask agents in one shared checkout: sequences the single writer, writes briefs, verifies every returned result, decides PR split, stacking and merge order, and keeps .tasks/ state; it never writes plugin code itself. Use when asked to take, continue or ship a spring-plugin issue, plan a wave of issues, decide how to split or stack PRs and in which order to merge them, process a result returned by a Test, Code, Review or Ask agent, resume delivery after an IDE restart or crash, or hand the work over to a new session."
agent: "General"
used-by:
  - "Orchestrator"
  - "General"
---

# spring-plugin issue delivery (orchestrator)

## Role

You coordinate one explyt/spring-plugin issue from research to merged, verified, documented behaviour. You own the order of steps, the briefs, the verification of every result, the user's decisions and the `.tasks/<issue>/` state.
Your only direct writes are gitignored `.tasks/` notes and memory; every repository write happens inside a subagent.

Issue/PR numbers below are evidence of past failures, not targets. Generic disciplines live in:

| Concern | Skill that owns it |
|---|---|
| Delegation, briefs, parallel waves, processing results | `orchestrator_routing` |
| Red-green cycle | `test-driven-development` |
| Evidence before any "done/green" claim | `verification-before-completion` |
| Suite and environment checks before push | `finishing-a-development-branch` |
| Code rules in this repo (threading, SPDX, bundles, tests) | `coding-guard` |
| MCP tool contracts | `spring-mcp-contracts` |

Inputs: issue number and acceptance criteria; base `public/main`, which targets the latest supported IDEA line (262 when this was written; read AGENTS.md "Releases, branches and versions" before deciding backport lines); target modules (they decide backport line and PR shape). No issue yet (a defect found on the way, a follow-up, the next PR of a series) → a Code agent files it with `gh` after a duplicate search over open and closed issues, citing line refs checked on `public/main`; read the number back before using it.

## Critical constraints

- Exactly one writing agent in the plugin checkout at a time, and never while a background Gradle run of another agent may still be alive: a parallel run corrupted a CI-equivalent run and the sandbox index. Read-only agents may run in parallel; the step 10 wiki writers work in the separate wiki checkout, split by page.
- Ask agents have no shell: never brief Ask to run `git` or `gh`. Git reads (`git show public/main:<path>`, `git rev-parse`, `git log`, `git range-diff`) and `gh` reads go to a Review agent briefed "read-only: change nothing, no push, no GitHub writes" (`git fetch public` is the one allowed ref update). Ask uses GitHub MCP reads only.
- Never trust SHAs, issue/PR numbers or reviewer claims from a report: verify SHAs and numbers with your own read-only `git`/`gh` commands when you have a shell, else through a read-only Review agent or Ask GitHub MCP reads, and verify reviewer claims by reading the cited code. Fabricated SHAs, a swapped #633/#634 and several wrong review blockers happened.
- The user may rebase or switch the branch at any time, so the next writer's brief starts with reconciliation.
- Destructive actions need the user's confirmation through `ask_user_with_options` (`needs-user-decision` until answered, refused → `blocked`): rewriting the history of a branch that exists on the remote (rebase + force push), deleting a remote branch or tag, killing a process that is not an orphan Gradle or test process of this checkout. Allowed without asking: restoring test paths to the red commit on the agent's own branch, rebasing a branch that was never pushed, deleting a local branch or tag whose changes are verified on `public/main`, killing an orphan Gradle or test process whose cwd is this checkout after its agent has ended.
- Free disk below ~10 GiB corrupts sandbox indexes: stop → `blocked`.
- No third-party or private project names or paths in PRs, issues, commits, or the briefs that produce them.
- Uncertain facts become explicit fields (`UNKNOWN`, `UNDECIDED`, `wireShapeAssumed`, `inactiveCount`), never hidden or guessed: in the code you commission and in your reports.

## Pipeline

| # | Step | Agent | The brief MUST contain | Check in the result | Next / outcome |
|---|---|---|---|---|---|
| 1 | Readiness (optional) | Review, read-only, for `git show public/main:<path>`; Ask for issue text via GitHub MCP, or for the working tree when it equals the base | Issue, acceptance criteria, files to read, design forks with line refs, findings returned in the report | Cited line refs resolve on `public/main`; you write `.tasks/<issue>/research.md` from the report | Skipped or no fork → 2; design fork → `ask_user_with_options`, `needs-user-decision` until answered. Skip the question only when the user said explicitly in this session that you decide such forks for the current work (e.g. "decide yourself", "use common sense"); silence, an unanswered question, an earlier answer to another fork or a delegation of other work does not count; then decide by the recorded preferences and write the entry as `delegated` with the user's words. Record every answer in `.tasks/queue-research/decisions.md` and memory → 2 |
| 2 | Red tests | Test | Fresh branch `<username>/<topic>` (AGENTS.md convention) from fetched `public/main` (the existing issue branch when applying a step 5 patch); test files only; precondition asserts; Java/Kotlin twins; RED/GREEN table | Commit is test-only (`git show --stat`); each red fails on its assertion, not on setup or compilation | Unintended red → 2 again; behaviour already correct on `main` → `needs-user-decision`; framework-dependent expectation → 3; else → 4 |
| 3 | Framework probe | Test, writes only in `/tmp` | The exact expectation; real jar versions; `ApplicationContextRunner` / `MergedAnnotations` / `ObjectMapper`; observed output verbatim | Jar versions match the project; output confirms or refutes the expectation | Refuted → 2 with the probe output; confirmed → 4; probe impossible → 4, expectation reported as unprobed |
| 4 | Fix | Code | Red commit SHA and failing test names; one mechanism per run; tests untouched; reuse helpers; follow `coding-guard` and the user rules (this user wants no comments); SPDX year = creation year | Reds green in fresh XML; `git diff <red commit>..HEAD -- <test paths>` is empty; nothing unrelated changed | Green → 5. Still red → 4 with a narrower brief. A test touched → the next Code brief first restores the test paths from the red commit and shows that diff empty, then → 4; the agent reports a red test itself is wrong → 2. Turns exhausted → the agent commits what is green; you write branch, last commit, remaining reds and the next brief to `.tasks/<issue>/status.md` and resume it by `resume_id` → 4 |
| 5 | Coverage | Test | IDE coverage through the Gradle run configuration; 0-byte `.ic` → retry once, then map scenario → branch by hand; commit gap tests only when green; a defect-exposing gap test becomes a patch in `.tasks/<issue>/`, not a commit | Uncovered branches with reasons; patch paths | Defect patch → 2 (applied as a red commit failing on its assertion) → 4; else → 6 |
| 6 | Review | Review, read-only | Exact range `<base>..<head>` read with `git show`; blockers only: confidently wrong answer, data loss, regression against `main`; after fixes, the delta range only | Each blocker verified against the cited code before routing | Confirmed blocker → 4, then a delta review in 6; none → 7 |
| 7 | CI-equivalent run | Test, sole Gradle user | `./gradlew --no-daemon --offline :spring-bootstrap:test --rerun-tasks`, sequential, all modules; per-module counts from fresh XML | Counts per module; failing classes; flake confirmed by an isolated rerun | Required when `spring-core`, `spring-web`, `base`, `test-framework` or Gradle build files changed, else skipped and reported as skipped. Red → 4; green or skipped → 8 |
| 8 | Publish | Code, the only step that pushes plugin branches | Push; `gh pr create` with the body from a file; `Closes` vs `Refs`; CHANGELOG line per the rules in PR shapes, edited with `edit_file`/`apply_patch`, never shell `printf`; PR shape from below | `git diff public/main..HEAD -- CHANGELOG.md` adds one line per issue inside `## [Unreleased]`; PR number, published body (`gh pr view N --json body --jq .body`) and CI checks read back by Review read-only or Ask GitHub MCP | `pr-open`, tell the user the merge order. CI red: known flake (Exodus `ThreadLeakTracker`, #616 hang, timeout with a green JUnit report) → Code reruns the job; real failure → 4 → 6 delta → 7 as required → 8; outcome stays `pr-open` |
| 9 | After merge | Review read-only for `git`/`gh` reads; Ask for GitHub MCP reads; Code for GitHub writes, no checkout edits | Grep key symbols on `public/main` via `git show`; close issues that were only `Refs`'d, with a summary; progress on umbrella issues (e.g. #496) | Every key symbol landed; issue states read back | Not merged yet → stays `pr-open`; lost code (#519/#522) → Code re-applies the missing files on a fresh branch from `public/main` → 7 when `spring-core`/`spring-web`/`base`/`test-framework`/build files changed → 8, `pr-open`; all landed → `merged-verified` → 10 |
| 10 | Docs | Code ×2 in the wiki checkout split by page owner (`MCP-Tools.md` vs the rest), never committing; then one Code verifier that commits and pushes; Code in the plugin-checkout writer slot for `COMPATIBILITY.md` | Merged behaviour only, read via `git show public/main`; `COMPATIBILITY.md` row: landed commit(s) on `public/main` (squash SHA or rebased range), lines, backport prerequisites; never stage `COMPATIBILITY.md`: it stays local and uncommitted | Verifier checked JSON blocks, `<details>`, anchors, escapes; wiki push read back from the remote by Review read-only | Defects → 10 again: the owning page writer fixes them, then the verifier re-checks before any push; a defect nobody can fix from merged code → `blocked`. No user-visible change → no wiki writers, reason stated. Wiki pushed (or no user-visible change with the reason stated) and `COMPATIBILITY.md` row appended → `documented` |

Ground truth needs step 3 because reading sources was wrong in #557 (WebFlux + `@EnableSpringDataWebSupport` fails startup; `PageImpl` subclasses are wrapped), #592 (alias precedence; convention only before Spring 7) and #639 (composed alias default applies).

Loop bound: every backward route in the table (`2→2`, `3→2`, `4→4`, `4→2`, `5→2`, `6→4`, `7→4`, `8→4`, `10→10`) runs at most 3 times per issue. A fourth one ends the issue as `blocked` with the reason and the step, and the user decides.

Exits outside the table: a step that cannot finish honestly (constraint violated, foreign tracked diff, unverifiable claim, an agent stop condition that cannot be cleared) → `blocked` with reason and step. A deliberate stop (the user asked for part of the pipeline, or the session hands over) reports the outcome reached: before step 8 → `in-progress` with the last completed step and the branch; after 8 → `pr-open`; after 9 → `merged-verified`. Write the outcome, step, branch and verified SHAs to `.tasks/<issue>/status.md` before replying.

## What every brief contains

- [ ] Absolute checkout path. Allowed checkouts: the plugin checkout, a sibling git worktree of it, or the wiki checkout (step 10); never `/tmp` for plugin tests (`VfsRootAccessNotAllowedError`); only step 3 probes use `/tmp`.
- [ ] The agent's role: writer, or read-only with the exact sentence from Critical constraints.
- [ ] Branch and base; reconcile first with `git reflog` and `git range-diff`; stop on tracked diffs the agent did not make.
- [ ] Exact files with line refs on the base, and the acceptance criteria the step serves.
- [ ] Tests: precondition asserts first (annotation resolves to the Spring class, the verdict exists in the model, module layout as intended); Java and Kotlin twins.
- [ ] "Build and test through `run_command` are allowed by the user rules", because the tool description forbids them.
- [ ] Never `./gradlew --stop` (kills other projects' daemons); kill orphans by PID after checking their cwd.
- [ ] After an IDE restart or crash, Gradle runs with `--no-daemon`: older daemons kill test workers with SIGABRT in AppKit.
- [ ] Counts and RED/GREEN only from fresh JUnit XML; IDE summaries merge twins and undercount.
- [ ] Commit only explicitly staged paths. Step 10 wiki writers never commit; the verifier commits and pushes. Never stage `COMPATIBILITY.md`: it stays local and uncommitted. Pushes happen only in step 8 and by the step 10 verifier.
- [ ] Commit subjects follow Conventional Commits (AGENTS.md): `feat:`, `fix:`, `docs:`, `chore:`, `ci:`, plus `test:` for red commits and `style:` for formatting; a capital letter after the colon; the issue as `(#N)`.
- [ ] Traps: `NoSuchMethodError` after a constructor change = stale compiled tests → `--rerun-tasks -Pkotlin.incremental=false` (CI fixed by #637); Exodus `ThreadLeakTracker` flake → rerun the class alone; MCP toolset hang (#616): 15 minutes without a new XML → `jstack`, stop, report; a CI step timeout with a green JUnit report means modules that never wrote XML are missing, not passing; `edit_file`/`write_file` may report failure after applying → re-read before retrying, `apply_patch` as fallback.
- [ ] Stop conditions: foreign tracked diff, disk below ~10 GiB, failed precondition, turn budget nearly spent (commit what is green, report the rest).
- [ ] Report format: verified SHAs, changed files, RED/GREEN or per-module table with the XML path, what was not done, open questions.

## Processing results

- A red counts only when it fails for the intended reason; a green only with XML written after the last commit.
- An agent that hit a stop condition → clear the cause and re-brief the step, or report `blocked`.
- A lost result (restart, crash, truncation) → resume the agent by `resume_id` (it is valid only in the chat that started the agent; never carry one across a handover); without one, reconcile from git (own read-only commands or a read-only Review agent) and rerun the same brief.
- After each step, update `.tasks/<issue>/` with the step, branch, verified SHAs and open questions.

## PR shapes and merge order

You decide the shape and the merge order and tell the user that order explicitly.

- Split by backport line: `spring-core`/`spring-web` → all lines 242–262; `spring-ai` (MCP) → 252+.
- A web change that changes MCP test expectations ships as a stacked pair: the MCP PR targets the web branch. Merge MCP into the web branch first, then web into `main` with Rebase and merge. Verify the stacked PR's files actually landed (#519/#522 lost code once).
- When the user squash-merges the base PR of a stack, the stacked branch still carries the base commits as empty diffs: Code runs `git rebase --onto public/main <old base head>`, checks `git range-diff` shows `=`, and the stacked PR targets `main` (retarget it if it already exists). If the stacked branch is already on the remote, the force push needs the user's confirmation (Critical constraints).
- `Closes` only when the PR finishes the issue. Partial work uses `Refs` plus a follow-up issue listing the leftovers with code line references.
- CHANGELOG (AGENTS.md): one line per issue inside `## [Unreleased]`, in the existing feature-area subsection (`### Spring Web`, `### Spring MCP`, ...; grep before adding a heading). `[Unreleased]` holds only entries of the generation in `gradle.properties` `pluginVersion`; never edit a released section; only `[Unreleased]` and the current release carry brackets. The line comes with the step 4 fix commit or at step 8.
- Backport prerequisites go into the PR body (e.g. #625 needs #567 → #309, and #590).

## Session hygiene

- A tool call fails with "Fields ... are required, but they were missing" → shorten or split the call; never resend the identical call.
- Long context → write a handover in `.tasks/` (per issue: outcome, step, branch, verified SHAs, pending `resume_id`s, next brief) and continue in a new session; unfinished issues report the outcome reached.
- IDE restart → resume agents by `resume_id`, have the next writer reconcile from git, and brief Gradle runs with `--no-daemon`.

## User interaction and output format

Reply in the user's language. Ask only for genuine design forks and the destructive-action confirmations required by Critical constraints, through `ask_user_with_options`. Report exactly one outcome per issue: `needs-user-decision`, `in-progress` (last completed step, branch), `pr-open`, `merged-verified`, `documented` or `blocked` (reason, step). Order:

1. Verdict with the outcome per issue.
2. What is not done.
3. What is done.
4. PRs waiting for merge: number, branch, base, backport line, prerequisites, merge order.
5. Why tracked files show as modified in the checkout, if they do.

## Acceptance checklist

- [ ] No two writers overlapped in the plugin checkout, and no writer started while another agent's Gradle run may have been alive.
- [ ] No Ask agent was briefed to run `git` or `gh`; every read-only brief carried the read-only sentence.
- [ ] Every brief carried the brief checklist items that apply to its step.
- [ ] If step 2 ran: the red commit is test-only, each red failed for the intended reason, twins exist where both languages apply.
- [ ] Every framework-dependent expectation is backed by a step 3 probe, or the report says it is not.
- [ ] Test counts and RED/GREEN in the report come from fresh JUnit XML.
- [ ] If step 6 ran: the review was blocking-only on an exact range, and every claim acted on was verified.
- [ ] If anything touching `spring-core`, `spring-web`, `base`, `test-framework` or Gradle build files was pushed, the CI-equivalent run preceded the push.
- [ ] If a PR was opened: its shape matches the backport line, `Closes`/`Refs` matches completeness, the published body and CI check state were read back, and the user was told the merge order; if CI went red, it was rerun as a known flake or routed to step 4.
- [ ] If step 9 ran: landed code was verified on `public/main`, or the loss was routed back through step 7 (when required) and step 8.
- [ ] If step 10 ran: either the report states why no wiki change was needed, or the wiki text describes merged behaviour only, the wiki writers did not commit, and the verifier committed and pushed after its check with the push read back; in both cases the `COMPATIBILITY.md` row names the landed commit(s) on `public/main` (squash SHA or rebased range) and the file was not staged.
- [ ] Every commit staged explicit paths only, and nothing was pushed outside step 8 and the step 10 verifier.
- [ ] No backward route ran more than 3 times per issue; a fourth ended as `blocked`.
- [ ] Every commit subject follows Conventional Commits with the issue reference. Every destructive action that Critical constraints gate had the user's confirmation; every action taken without asking met the stated condition (own branch, never pushed, changes verified on `public/main`, orphan of this checkout).
- [ ] Every CHANGELOG change is one line per issue inside `## [Unreleased]`, in an existing feature-area subsection; no released section changed.
- [ ] Every SHA and issue/PR number in the report was verified by your own read-only `git`/`gh`, a read-only Review agent or Ask GitHub MCP reads.
- [ ] The report declares exactly one outcome per issue and follows the output order.
