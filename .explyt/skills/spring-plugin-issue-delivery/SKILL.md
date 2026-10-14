---
name: "spring-plugin-issue-delivery"
schemaVersion: "v0.1"
description: "Orchestrates explyt/spring-plugin GitHub issues end to end by routing work to Test, Code, Review and Ask agents and forks: one writer per checkout, parallel lanes in sibling git worktrees, stacked PRs for dependent work, briefs, verification of every returned result, PR split and merge order, refreshing open PRs after main moves, and .tasks/ state; it never writes plugin code itself. Use when asked to take, continue or ship a spring-plugin issue, run a wave of issues in parallel, decide how to split or stack PRs and in which order to merge them, rebase open PRs after merges or fix a broken stack, process a result returned by a Test, Code, Review or Ask agent, resume delivery after an IDE restart or crash, or hand the work over to a new session."
agent: "General"
used-by:
  - "Orchestrator"
  - "General"
---

# spring-plugin issue delivery (orchestrator)

## Role

You coordinate explyt/spring-plugin issues from research to merged, verified, documented behaviour. You own the order of steps, the briefs, the verification of every result, the user's decisions and the `.tasks/<issue>/` state.
Your direct actions are limited to: gitignored `.tasks/` notes and memory; read-only `git`/`gh`; starting, waiting for and counting Gradle test runs; rebasing open PRs, resolving CHANGELOG-only conflicts and force-pushing the rebased commits (see Refreshing open PRs). Every code or test change happens inside a subagent or fork.

Issue/PR numbers below are evidence of past failures, not targets. Generic disciplines live in:

| Concern | Skill that owns it |
|---|---|
| Delegation, briefs, parallel waves, processing results | `orchestrator_routing` |
| Red-green cycle | `test-driven-development` |
| Evidence before any "done/green" claim | `verification-before-completion` |
| Suite and environment checks before push | `finishing-a-development-branch` |
| Code rules in this repo (threading, SPDX, bundles, tests) | `coding-guard` |
| MCP tool contracts | `spring-mcp-contracts` |
| IDE `Java heap space`, OOM, growth until restart, UI freezes | `jvm-heap-retention-attribution`, `intellij-freeze-attribution` |

Inputs: issue numbers and acceptance criteria; base `public/main`, which targets the latest supported IDEA line (262 when this was written; read AGENTS.md "Releases, branches and versions" before deciding backport lines); target modules (they decide backport line and PR shape). No issue yet (a defect found on the way, a follow-up, the next PR of a series) → a Code agent or fork files it with `gh` after a duplicate search over open and closed issues, citing line refs checked on `public/main`; read the number back before using it.

## Critical constraints

- One writer per checkout. Parallel writers each get their own sibling git worktree of the plugin checkout (`git worktree add <plugin checkout>/../wt-<issue> -b <branch> public/main`), never `/tmp` (`VfsRootAccessNotAllowedError`). Read-only agents and forks may run in parallel with anything. Never start Gradle in a checkout where another agent's Gradle run may still be alive: a parallel run in one checkout corrupted a CI-equivalent run and the sandbox index. At most one all-module run (`:spring-bootstrap:test`) at a time across all checkouts; module runs may overlap.
- Do not wait for merges (user rule): independent issues run in parallel lanes; work that needs an unmerged PR stacks on that PR's branch (PR shapes).
- Worktree lanes have no IDE index of their own: IDE tools given a relative path (`read_file`, `edit_file`, inspections) read the plugin checkout, not the worktree. Worktree briefs use absolute file paths and Gradle from the terminal. IDE MCP tools need `projectPath=<plugin checkout>` while several projects are open; an agent that omitted it died on "Unable to determine the target project".
- Ask agents have no shell: never brief Ask to run `git` or `gh`. Git reads (`git show public/main:<path>`, `git rev-parse`, `git log`, `git range-diff`) and `gh` reads go to a Review agent or a fork briefed "read-only: change nothing, no push, no GitHub writes" (`git fetch public` is the one allowed ref update). Ask uses GitHub MCP reads only.
- Never trust SHAs, issue/PR numbers, counts or reviewer claims from a report: verify SHAs, numbers and counts with your own read-only `git`/`gh`, and verify reviewer claims by reading the cited code. Fabricated SHAs, a swapped #633/#634 and several wrong review blockers happened; wrong counts in your own briefs stopped publish agents.
- The user may rebase, merge or switch branches at any time, so the next writer's brief starts with reconciliation.
- Destructive actions need the user's confirmation through `ask_user_with_options` (`needs-user-decision` until answered, refused → `blocked`): rewriting the history of a branch that exists on the remote (rebase + force push; one confirmation may name a list of PRs), deleting a remote branch or tag, removing a worktree with unpushed commits or whose `git status --porcelain` lists anything other than build output, killing any process the allowed list below does not cover, deleting a test sandbox index that shows no `CorruptedException` (e.g. to free disk). Allowed without asking: restoring test paths to the red commit on the agent's own branch, rebasing a branch that was never pushed, deleting a local branch or tag whose changes are verified on `public/main` or pushed, removing a worktree whose branch is pushed and whose `git status --porcelain` is empty or lists only build output (`build/`, `.kotlin/`), with `git worktree remove --force` only after that check, killing a Gradle/test process whose cwd is one of your checkouts after checking its cwd and start time when it is orphaned, hung, or running on a head a review blocker has superseded (whoever started it), deleting the `system-test/index` of a module test sandbox inside one of your checkouts after its `log-test/idea.log` shows `CorruptedException` and every Gradle/test process of that checkout has stopped (never an IDE's own index).
- Free disk below ~15 GiB with parallel lanes (~10 GiB with one) corrupts sandbox indexes: stop before Gradle → remove worktrees of published PRs; still below → `blocked`.
- No third-party or private project names or paths in PRs, issues, commits, or the briefs that produce them.
- Uncertain facts become explicit fields (`UNKNOWN`, `UNDECIDED`, `wireShapeAssumed`, `inactiveCount`), never hidden or guessed; hide only what the framework never serves: in the code you commission and in your reports.

## Pipeline

| # | Step | Agent | The brief MUST contain | Check in the result | Next / outcome |
|---|---|---|---|---|---|
| 1 | Readiness (optional) | Review or a fork, read-only, for `git show public/main:<path>`; Ask for issue text via GitHub MCP, or for the working tree when it equals the base | Issue, acceptance criteria, files to read, design forks with line refs, findings returned in the report | Cited line refs resolve on `public/main`; you write `.tasks/<issue>/research.md` (or `ready*.md`) from the report | Skipped or no fork → 2; design fork → `ask_user_with_options`, `needs-user-decision` until answered. Skip the question only when the user said explicitly in this session that you decide such forks for the current work (e.g. "decide yourself", "use common sense"); silence, an unanswered question, an earlier answer to another fork or a delegation of other work does not count; then decide by the recorded preferences and write the entry as `delegated` with the user's words. Record every answer in `.tasks/queue-research/decisions.md` and memory → 2 |
| 2 | Red tests | Test | On first entry a fresh branch `<username>/<topic>` (AGENTS.md convention) from fetched `public/main`, or from the base PR's head when stacking; every return to step 2 (from steps 3–8, including step 5 patches and step 6 blockers) adds the red commit on top of the current verified issue head, named in the brief; test files only; precondition asserts; negative controls assert that the code under test ran (inspection available for the file, model not empty); a test of a lock-releasing or async helper reaches its precondition before calling it; Java/Kotlin twins; RED/GREEN table | Commit is test-only (`git show --stat`); each red fails on its assertion, not on setup or compilation; no control passes vacuously | Unintended red → 2 again; behaviour already correct on `main` → `needs-user-decision`; framework-dependent expectation → 3; else → 4 |
| 3 | Framework probe | Test or a fork, writes only in `/tmp` | The exact expectation; real jar versions (both majors when behaviour may differ, e.g. Spring 6.2 and 7.0, Boot 3.5 and 4.1); `ApplicationContextRunner` / `MergedAnnotations` / `ObjectMapper` / a real context; observed output verbatim; for a version- or setting-dependent verdict a table of input kind × framework version × every state of each input (e.g. a property missing, `false`, `true`, unresolvable), one probe per row | Jar versions match the project; output confirms or refutes the expectation; a row that contradicts the framework source is re-run with a checked fixture before use | Refuted (also the issue's premise) → 2 with the probe output, tests pin the framework; confirmed → 4; probe impossible → 4, expectation reported as unprobed |
| 4 | Fix | Code | Red commit SHA and failing test names; one mechanism per run; tests untouched; reuse helpers; follow `coding-guard` and the user rules (this user wants no comments); SPDX year = creation year | Reds green in fresh XML; `git diff <red commit>..HEAD -- <test paths>` is empty; nothing unrelated changed; no shared helper changed its contract beyond the brief (grep the tests that pin it) | Green → 5. Still red → 4 with a narrower brief. A shared helper changed its contract → 4 with a brief that keeps the contract. A test touched → the next Code brief first restores the test paths from the red commit and shows that diff empty, then → 4; an old test that pins the behaviour the issue changes → a Test agent updates it in its own test-only commit, then → 4; the agent reports a red test itself is wrong → 2. Turns exhausted → the agent commits what is green; you write branch, last commit, remaining reds and the next brief to `.tasks/<issue>/status.md` and resume it by `resume_id` → 4 |
| 5 | Coverage | Test | IDE coverage through the Gradle run configuration; 0-byte `.ic` → retry once, then map scenario → branch by hand; commit gap tests only when green; a defect-exposing gap test becomes a patch in `.tasks/<issue>/`, not a commit | Uncovered branches with reasons; patch paths | Defect patch → 2 (applied as a red commit failing on its assertion) → 4; else → 6 |
| 6 | Review | Review, read-only | Exact range `<base>..<head>` read with `git show`; blockers only: confidently wrong answer, data loss, regression against `main`, or a CHANGELOG line or PR text that claims more than the code does; then non-blocking notes in a separate list, each naming the acceptance criterion it touches; after fixes, the delta range only | Each blocker verified against the cited code before routing; a non-blocking note that makes another acceptance criterion of the issue worse is promoted to a blocker | Confirmed blocker → 2 for a red, then 4, then a delta review in 6; wording-only blocker → 4; none → 7 |
| 7 | CI-equivalent run | Test as the sole Gradle user of that checkout, or you with a background `run_command` | `./gradlew --no-daemon --offline :spring-bootstrap:test --rerun-tasks`, sequential, all modules; per-module counts from fresh XML | Counts per module; failing classes; flake confirmed by an isolated rerun with `--rerun` | Required when `spring-core`, `spring-web`, `base`, `test-framework` or Gradle build files changed; otherwise run each changed module with `:<module>:test --rerun`. A delta after a green all-module run that touches only modules outside that list → those modules' runs. A run whose head a review then blocks → stop it and run again on the new head. Red → 4; green → 8 |
| 8 | Publish | Code or a fork, the only step that pushes new commits to plugin branches | Push; `gh pr create` with the body from a file; `Closes` vs `Refs`; CHANGELOG line per the rules in PR shapes, edited with `edit_file`/`apply_patch`, never shell `printf`; PR shape from below; expected commit and file counts computed by you with `git` | `git diff public/main..HEAD -- CHANGELOG.md` adds one line per issue inside `## [Unreleased]`; PR number, base, head, published body (`gh pr view N --json body --jq .body`) and CI checks read back by you or Review read-only | `pr-open`, tell the user the merge order. CI red: known flake (Exodus `ThreadLeakTracker` or `Finish all transactions before closing database environment`, timeout with a green JUnit report) → rerun the job; real failure → 4 → 6 delta → 7 as required → 8; outcome stays `pr-open`. Main moved and the PR conflicts → Refreshing open PRs |
| 9 | After merge | You or Review read-only for `git`/`gh` reads; Ask for GitHub MCP reads; Code or a fork for GitHub writes, no checkout edits | Grep key symbols on `public/main` via `git show`, or `git diff <PR head> public/main -- modules` empty (for a wave of squash merges that touch the same files: per PR `git diff <PR head> <squash> -- <module files of the squash>` empty, or, when the PR was not rebased on an earlier squash of the wave, the same changed lines in the PR range and in the squash: `patch-id` hashes context lines too, so a mismatch alone is no proof); close issues that were only `Refs`'d, with a summary; progress on umbrella issues (e.g. #496) | Every key symbol landed; issue states read back | Not merged yet → stays `pr-open`; lost code (#519/#522) → Code re-applies the missing hunks on a fresh branch from `public/main` → 7 → 8, `pr-open`; all landed → `merged-verified` → 10 |
| 10 | Docs | Code or a fork ×2 in the wiki checkout split by page owner (`MCP-Tools.md` vs the rest), run one after the other (one writer active in the wiki checkout at a time), never committing; then one Code agent or fork as verifier that commits and pushes (one writer may do a small pass alone, then the verifier); Code in the plugin-checkout writer slot for `COMPATIBILITY.md` | Merged behaviour only, read via `git show public/main`; re-audit claims of earlier passes in the touched sections; `COMPATIBILITY.md` row: landed commit(s) on `public/main` (squash SHA or rebased range), lines, backport prerequisites; never stage `COMPATIBILITY.md`: it stays local and uncommitted | Verifier checked JSON blocks, `<details>`, anchors, escapes; wiki push read back from the remote | Defects → 10 again: the owning page writer fixes them, then the verifier re-checks before any push; a defect nobody can fix from merged code → `blocked`. No user-visible change → no wiki writers, reason stated. Wiki pushed (or no user-visible change with the reason stated) and `COMPATIBILITY.md` row appended → `documented` |

Ground truth needs step 3 because reading sources was wrong in #557 (WebFlux + `@EnableSpringDataWebSupport` fails startup; `PageImpl` subclasses are wrapped), #592 (alias precedence; convention only before Spring 7), #639 (composed alias default applies), #651 (`@ConditionalOnClass` is read without merged attributes) and #689 (each of three review loops found a row the table lacked: alias pairs, Spring 5.3 registration order, an unresolvable property).

Loop bound: every backward route in the table (`2→2`, `3→2`, `4→4`, `4→2`, `5→2`, `6→2`, `6→4`, `7→4`, `8→4`, `10→10`) runs at most 3 times per issue. A fourth one ends the issue as `blocked` with the reason and the step, and the user decides.

Exits outside the table: a step that cannot finish honestly (constraint violated, foreign tracked diff, unverifiable claim, an agent stop condition that cannot be cleared) → `blocked` with reason and step. A deliberate stop (the user asked for part of the pipeline, or the session hands over) reports the outcome reached: before step 8 → `in-progress` with the last completed step and the branch; after 8 → `pr-open`; after 9 → `merged-verified`. Write the outcome, step, branch and verified SHAs to `.tasks/<issue>/status.md` before replying.

## What every brief contains

- [ ] Absolute checkout path: the plugin checkout, a sibling worktree of it, or the wiki checkout (step 10); never `/tmp` for plugin tests; only step 3 probes use `/tmp`. In a worktree: absolute file paths for IDE file tools.
- [ ] The agent's role: writer, or read-only with the exact sentence from Critical constraints.
- [ ] Branch and base; reconcile first with `git reflog` and `git range-diff`; stop on tracked diffs the agent did not make.
- [ ] Exact files with line refs on the base, and the acceptance criteria the step serves; expected behaviour quoted from its `decisions.md` entry when one exists, else from the issue's acceptance criteria, never paraphrased (a paraphrase that contradicted the entry cost #689 a review loop).
- [ ] Tests: precondition asserts first (annotation resolves to the Spring class, the verdict exists in the model, module layout as intended); Java and Kotlin twins; spring-ai test bodies use `runBlockingReleasingWriteIntent` (test-framework, #679), never a plain `runBlocking` on the EDT: it deadlocks with indexing (#616).
- [ ] "Build and test through `run_command` are allowed by the user rules", because the tool description forbids them; the shell is fish: no heredocs, use a script or `bash`.
- [ ] IDE MCP tools: `projectPath=<plugin checkout>`.
- [ ] Never `./gradlew --stop` (kills other projects' daemons); kill orphans by PID after checking their cwd.
- [ ] After an IDE restart or crash, Gradle runs with `--no-daemon`: older daemons kill test workers with SIGABRT in AppKit.
- [ ] Counts and RED/GREEN only from fresh JUnit XML; IDE summaries merge twins and undercount.
- [ ] Commit only explicitly staged paths: `git commit -m "<subject>" -- <paths>` (`-m` before `--`). Step 10 wiki writers never commit; the verifier commits and pushes. Never stage `COMPATIBILITY.md`: it stays local and uncommitted. New commits are pushed only in step 8 and by the step 10 verifier.
- [ ] Commit subjects follow Conventional Commits (AGENTS.md): `feat:`, `fix:`, `docs:`, `chore:`, `ci:`, plus `test:` for red commits and `style:` for formatting; a capital letter after the colon; the issue as `(#N)`.
- [ ] Traps: `NoSuchMethodError` after a constructor change = stale compiled tests → `--rerun-tasks -Pkotlin.incremental=false` (CI fixed by #637); Exodus `ThreadLeakTracker` or `Finish all transactions before closing database environment` flake → rerun the class alone with `--rerun` (without it Gradle restores the cached XML); at ≥ 97 % disk a native `FileDescriptor.close0` hang in a VFS gist write followed by a leaked project in the next class is the environment: free disk and rerun, not step 4; a run without new test output for 15 minutes → read the module sandbox `log-test/idea.log` for `CorruptedException` (corrupt index → stop the run's processes, then delete that sandbox's `system-test/index` per Critical constraints and rerun the module), else `jstack` the worker of that checkout, stop, report; a CI step timeout with a green JUnit report means modules that never wrote XML are missing, not passing; `edit_file`/`write_file` may report failure after applying or refuse a mid-line edit → re-read before retrying, `apply_patch` or a script file as fallback, never inline `python3 -c` with backticks (bash executes them, fish escapes them).
- [ ] Never wait for a long Gradle run inside the brief: start it in the background and report, or stop before it (agents ran out of turns while waiting four times).
- [ ] Stop conditions: foreign tracked diff, disk below the threshold, failed precondition, turn budget nearly spent (commit what is green, report the rest).
- [ ] Report format: verified SHAs, changed files, RED/GREEN or per-module table with the XML path, what was not done, open questions.

## Processing results

- A red counts only when it fails for the intended reason; a green only with XML written after the last commit.
- An agent that hit a stop condition → clear the cause and re-brief the step, or report `blocked`.
- A Test agent that ran out of turns before committing its tests → read its XML yourself (newer than the test files' mtime) and apply step 2's Check and Next columns; when they pass, record `git hash-object` of each test file and brief the next agent to start with a test-only commit of exactly those blobs (hashes compared) before any production edit; that commit is the red commit of step 4. The uncommitted files stay in that checkout for the next agent, which is its only writer. A failed check follows step 2's Next column (counts as `2→2` when it routes back to step 2).
- A long run an agent left running → wait for it yourself with one background command, count fresh XML, then brief the next step; never poll.
- A lost result (restart, crash, truncation) → resume the agent by `resume_id` (it is valid only in the chat that started the agent; never carry one across a handover); without one, reconcile from git and rerun the same brief.
- After each step, update `.tasks/<issue>/` and the lane log (`.tasks/parallel-lanes-*.md`: lane, worktree, branch, step, agent) with verified SHAs and open questions.

## PR shapes and merge order

You decide the shape and the merge order and tell the user that order explicitly.

- Split by backport line: `spring-core`/`spring-web` → all lines 242–262; `spring-ai` (MCP) → 252+. A branch that uses main-only code (router walker, recent helpers) is main-only; say so in the body.
- A change that needs an unmerged PR ships as a stacked PR targeting that PR's branch (a web change that changes MCP expectations → the MCP PR targets the web branch). Merge order: the stacked PR into the base branch first, then the base into `main`. Verify the stacked PR's files actually landed (#519/#522 lost code once).
- If the user merges the base first (squash), GitHub retargets the stacked PR to `main` and shows it conflicting: the base commits are still on the stacked branch → Refreshing open PRs with `--onto`.
- `Closes` only when the PR finishes the issue. Partial work uses `Refs` plus a follow-up issue listing the leftovers with code line references.
- CHANGELOG (AGENTS.md): one line per issue inside `## [Unreleased]`, in the existing feature-area subsection (`### Spring Web`, `### Spring MCP`, ...; grep before adding a heading). `[Unreleased]` holds only entries of the generation in `gradle.properties` `pluginVersion`; never edit a released section; only `[Unreleased]` and the current release carry brackets. The line comes with the step 4 fix commit or at step 8; it names only behaviour the code delivers.
- Backport prerequisites go into the PR body (e.g. #625 needs #567 → #309, and #590).

## Refreshing open PRs after main moves

You run the rebase and resolve CHANGELOG-only conflicts yourself; every other conflict resolution, including mechanical renames and keeping tests from both sides, goes to a Code agent or fork in that worktree.

1. `git fetch public`; for each open PR `git merge-tree --write-tree --name-only public/main <head>` lists the conflicting files.
2. Ask once for the force pushes (name the PRs). Per PR: a sibling worktree on the branch; tag the old head `tmp/<PR>-old`; `git rebase public/main` (`git rebase --onto public/main <old base head>` when its base was squash-merged; check equal `git patch-id --stable` of the old base range and the squash first).
3. You resolve only `CHANGELOG.md`: main's lines plus the branch's own final line, intermediate versions of the own line dropped. Any other conflicting file → hand the paused rebase to a Code agent or fork with these rules: a main-side mechanical rename (e.g. `runBlocking` → `runBlockingReleasingWriteIntent`) → take the branch's version and replay the rename; tests added on both sides → keep both, no duplicate names; anything else → `git rebase --abort` and a normal step 4 brief.
4. Verify: the own CHANGELOG line equals the old one and `git diff public/main..HEAD -- CHANGELOG.md` only adds; production changes equal by `git patch-id --stable` over `git diff <old base> tmp/<PR>-old -- modules ':!**/src/test/**'` vs the new range, or the difference is explained; `git range-diff`; no conflict markers; `git diff --check`.
5. Test per step 7 rules on the new head, then `git push --force-with-lease=refs/heads/<branch>:<old head> public <branch>`; read back the head and `mergeable` = `MERGEABLE`; if the PR body names a merged base, a Code agent or fork edits it (reading the live body first) and you read the published body back; remove the tag and the worktree.
- `gh stack` (v0.2.1) answered "not part of a stack" for a stack the REST API shows (`gh api repos/<owner>/<repo>/pulls/<n> --jq .stack`); plain git works.

## Session hygiene

- A tool call fails with "Fields ... are required, but they were missing" → shorten or split the call; never resend the identical call.
- Long context → write a handover in `.tasks/` (per issue: outcome, step, branch, verified SHAs, open PR table with merge order, next queue, traps) and continue in a new session; unfinished issues report the outcome reached.
- IDE restart → resume agents by `resume_id`, have the next writer reconcile from git, and brief Gradle runs with `--no-daemon`.
- After a PR is published and its worktree is clean, remove the worktree; branches stay (this freed ~20 GiB once).
- IDE `Java heap space`, OOM or freezes are not blamed on parallel agents without evidence: diagnose with the skills in the table above.

## User interaction and output format

Reply in the user's language. Ask only for genuine design forks and the destructive-action confirmations required by Critical constraints, through `ask_user_with_options`. Report exactly one outcome per issue: `needs-user-decision`, `in-progress` (last completed step, branch), `pr-open`, `merged-verified`, `documented` or `blocked` (reason, step). Order:

1. Verdict with the outcome per issue.
2. What is not done.
3. What is done.
4. PRs waiting for merge: number, branch, base, backport line, prerequisites, merge order.
5. Lanes running: lane, checkout, step, agent; free disk when below 30 GiB.
6. Why tracked files show as modified in the checkout, if they do.

## Acceptance checklist

- [ ] No two writers were active in the same checkout at the same time, no writer started in a checkout while another agent's Gradle run there may have been alive, and no two all-module runs overlapped.
- [ ] No Ask agent was briefed to run `git` or `gh`; every read-only brief carried the read-only sentence.
- [ ] Every brief carried the brief checklist items that apply to its step, and no brief asked an agent to wait for a long Gradle run.
- [ ] If step 2 ran: the red commit is test-only, each red failed for the intended reason, no control passed vacuously, twins exist where both languages apply.
- [ ] Every framework-dependent expectation is backed by a step 3 probe, or the report says it is not.
- [ ] Test counts and RED/GREEN in the report come from fresh JUnit XML.
- [ ] If step 6 ran: the review was blocking-only on an exact range, every claim acted on was verified, and non-blocking notes were checked for acceptance criteria they worsen.
- [ ] Before every push the step 7 rule held: the all-module run when `spring-core`, `spring-web`, `base`, `test-framework` or Gradle build files changed, else the changed modules' runs.
- [ ] If a PR was opened: its shape matches the backport line, `Closes`/`Refs` matches completeness, the published body and CI check state were read back, and the user was told the merge order; if CI went red, it was rerun as a known flake or routed to step 4.
- [ ] If PRs were refreshed: the force pushes had the user's confirmation and used `--force-with-lease` on the old head, each own CHANGELOG line survived unchanged, production patch-ids matched or the difference was explained, and each PR read back as `MERGEABLE`.
- [ ] If step 9 ran: landed code was verified on `public/main`, or the loss was routed back through step 7 and step 8.
- [ ] If step 10 ran: either the report states why no wiki change was needed, or the wiki text describes merged behaviour only, the wiki writers did not commit, and the verifier committed and pushed after its check with the push read back; in both cases the `COMPATIBILITY.md` row names the landed commit(s) on `public/main` (squash SHA or rebased range) and the file was not staged.
- [ ] Every commit staged explicit paths only, and new commits were pushed only in step 8 and by the step 10 verifier; refresh pushes carried rebased commits only.
- [ ] No backward route ran more than 3 times per issue; a fourth ended as `blocked`.
- [ ] Every commit subject follows Conventional Commits with the issue reference. Every destructive action that Critical constraints gate had the user's confirmation; every action taken without asking met the stated condition.
- [ ] Every CHANGELOG change is one line per issue inside `## [Unreleased]`, in an existing feature-area subsection; no released section changed.
- [ ] Every SHA, issue/PR number and count in the report was verified by your own read-only `git`/`gh`, a read-only Review agent or Ask GitHub MCP reads.
- [ ] The report declares exactly one outcome per issue and follows the output order.
