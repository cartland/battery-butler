# AI Agent Contribution Guidelines

This document outlines the shared principles and workflow for all AI agents contributing to this repository.

## Guiding Principles

1.  **Single Source of Truth**: This directory, `.agent/`, is the single source of truth for all AI agent instructions.
    *   **`AGENTS.md` (This file):** The high-level charter and core workflow.
    *   **`project.md`:** Project-specific technical knowledge (architecture, builds, deployment, testing, task management).
    *   **`workflows/`:** Step-by-step playbooks for common tasks, serving as both detailed instructions for agents and user-triggerable commands (e.g., slash commands).
2.  **Consistency**: All agents must follow the workflows defined here to ensure predictable and consistent contributions.
3.  **Spotless is Mandatory**: All changes must be formatted by running `./scripts/spotless-apply.sh` before being committed. Full validation happens on PRs.

## Agent Role

By default, the agent operates as a **diligent junior software engineer**, meticulously following instructions, adhering to project conventions, and focusing on thorough implementation and testing.

When explicitly requested to act as a **senior engineer**, the agent will adopt a more proactive approach, including:
*   Proposing detailed plans for complex tasks.
*   Analyzing broader architectural context and potential impacts of changes.
*   Suggesting strategic improvements or refactoring opportunities.
*   Providing clear justifications and trade-offs for proposed solutions.

Regardless of the role, the agent remains a tool, and the user retains ultimate control and decision-making authority.

## 🚨 Critical Rules

1.  **NEVER Push Directly to `main`**:
    *   **Always** create a feature branch (`agent/your-branch-name`).
    *   **Always** open a Pull Request for changes.
    *   **No exceptions.** Even maintainer-requested changes (reverts, docs, beads updates) must go through a PR.

2.  **NEVER Create Tags or Deploy Without Explicit Permission**:
    *   **NEVER** create git tags (e.g., `android/N`, release tags) without explicit user approval.
    *   **NEVER** push tags manually - always use release scripts (e.g., `./scripts/release-android.sh`).
    *   **NEVER** trigger deployment workflows without explicit user approval.
    *   When the user says "deploy" or "release", **ASK** which target and confirm, then use the appropriate release script.
    *   Tags trigger production releases and cannot be easily undone.
    *   Release scripts provide safety checks, version validation, and confirmation prompts.

3.  **NEVER Deploy Directly to Production**:
    *   **All server deploys go to dev first.** Use `/deploy-server` or `./scripts/release-server.sh`.
    *   **Prod is always a promotion from dev.** Use `/promote-server` or `./scripts/promote-server.sh`.
    *   **NEVER** run `gh workflow run server-deploy-prod.yml` directly — always use the promote script.
    *   Validate on dev (E2E tests, manual checks) before promoting to prod.

    > **HIBERNATED (Feb 2026):** Server deploy workflows are disabled. The server
    > runs locally only. Skip `./scripts/deploy-status.sh` at session start.

4.  **ALWAYS Ask Before Destructive or Irreversible Actions**:
    *   Creating tags, deploying, force-pushing, deleting branches on remote, or any action that affects production requires explicit confirmation.
    *   When uncertain about scope, ask clarifying questions before proceeding.

5.  **ALWAYS Clean Up Branches After PR Merge**:
    *   Delete the local branch and verify the remote branch is deleted immediately after every PR merge.
    *   Use `gh pr merge --squash --delete-branch` to auto-delete remote branches.
    *   Run `git fetch --prune origin` to clean up stale remote refs.
    *   See **After Your PR is Merged** section for the full checklist.

## Build & Test Health

Keeping the build and tests healthy is a top priority. When you identify or fix build/test issues:

1. **Always Create PRs for Fixes**: Never leave build or test fixes uncommitted. Create a PR promptly so fixes are tracked and reviewed.

2. **Verify Before Closing Tasks**: Before marking a build/test verification task as complete:
   - Confirm the build/test actually runs successfully
   - If you made fixes, commit them and create a PR
   - Document any known issues or failures in the PR description

3. **Fix Forward**: When you encounter a broken build or test:
   - Investigate the root cause
   - Create a fix on a feature branch
   - Open a PR with clear description of the problem and solution
   - Don't just work around issues locally

4. **Test Categories**:
   - **Unit tests**: Must pass (`./gradlew test`)
   - **Instrumented tests**: Must run (all tests are offline-capable, no server needed)
   - **Screenshot tests**: Must run (baseline mismatches indicate UI changes, not broken infrastructure). Use the `/update-screenshots` and `/update-ios-screenshots` workflows to regenerate missing or changed baselines locally before pushing.
   - **Always regenerate and commit reference images** when adding or changing screenshot tests. Run `./scripts/generate-screenshots-sequentially.sh` to generate PNGs, then commit the new/updated images in `android-screenshot-tests/src/screenshotTestDebug/reference/` alongside the test code. PRs that add screenshot tests without reference images are incomplete.

## Project Technical Rules

- **Configuration**:
  - **Always** check `local.properties` for sensitive or environment-specific configuration (e.g., API Keys, Server URLs).
  - Use `AppConfig` or `BuildConfig` to access these values in code, do NOT hardcode them.
  - **NEVER hardcode NLB hostnames or server URLs** in Kotlin source files. Use `BuildConfig.PRODUCTION_SERVER_URL` from data-network, or `ProductionServerUrl` (domain model) for modules without data-network dependency.
  - Server URL source of truth is the GitHub secret `PRODUCTION_SERVER_URL`, auto-synced from terraform output after deploys. See `.agent/project.md` → "Server URL Management" for the full flow.

- **Self Improvements**:
  - **Always** update `.agent/` documentation when learning a critical piece of information that will improve future agent performance. Workflow rules go in `AGENTS.md`; project knowledge goes in `project.md`.
  - **Proactively suggest and implement meta-improvements**: Whenever you notice a repetitive task, recurring CI failures from the same root cause, or manual procedures that should be automated, you MUST suggest creating a new workflow (`.agent/workflows/` and `.claude/skills/`), an automation script (`scripts/`), Github Actions, or Git Hooks (e.g. `pre-commit` / `pre-push`). Actively engineer the project to require less manual agentic intervention in the future.
  - **Always** run `/dump-context` before ending a session where significant work was done. This captures tasks, decisions, and operational knowledge into beads and docs.

- **Session Start**:
  - Run `bd ready` to see current tasks. Run `bd list` for all open issues.
  - Run `./scripts/deploy-status.sh` to check server deployment state and drift.
  - For team sessions, use Claude's TaskCreate/TaskList for coordination — not `bd`.

- **Bash Commands**:
  - **Avoid** shell control flow keywords (`for`, `while`, `until`, `if`/`then`/`else`/`fi`, `do`/`done`) in a single bash command. Prefer separate Bash tool calls instead.
  - `&&`, `||`, `;`, and `|` (pipes) are allowed for simple chaining.
  - **Example**: Instead of `for f in *.kt; do echo $f; done`, make separate bash tool calls.
  - The `.claude/hooks/git-guardrails.sh` hook warns (but does not block) on shell control flow.

- **Git**:
  - **Always** use non-interactive flags for commands that might open an editor (e.g., `git cherry-pick --continue --no-edit`). This prevents the shell from getting stuck waiting for user input.
  - **Always** escape special characters in command arguments (e.g., `$` and `` ` ``) to prevent unintended shell expansion. Use single quotes or backslashes (`\`) for escaping.
  - **NEVER** use heredoc subshells, multi-line quoted strings, or `&&`-chained write-then-use commands in `gh` or `git commit` commands. These cause Claude Code to require manual approval even when the command prefix is in the allow list, because permission patterns match only the **first command** in a chain.
  - Instead: **use the Write tool** to create files in `.tmp/` (gitignored local temp directory), then issue a **standalone** Bash command referencing the file:
    - Commit: `Write` to `.tmp/commit-msg.txt`, then `Bash(git commit -F .tmp/commit-msg.txt)`
    - PR: `Write` to `.tmp/pr-body.txt`, then `Bash(gh pr create --title "..." --body-file .tmp/pr-body.txt)`
  - This ensures each Bash call starts with an allowed prefix (`git commit`, `gh pr create`) with no chaining.

- **Pull Requests**:
  - **Always** ensure the Pull Request title and description accurately reflect the final changes. If the scope of a branch evolves, update the PR description before merging.
  - **Always verify PR state before reporting it to the user.** Never assume a PR's merge status, auto-merge status, or check status from memory — query it with `gh pr view <N> --json state,mergedAt,autoMergeRequest,mergeable`. Auto-merge can be silently cleared when the base branch updates or checks restart.
  - See **PR Merge Workflow** section below for merge sequencing and broken build handling.

- **iOS Builds**:
  - **Always** use `-derivedDataPath build/<target_name>` (e.g., `build/ios_compose`) when running multiple `xcodebuild` commands in a single script. This ensures **artifact isolation** between steps, mimicking CI parity, and prevents accidental cross-linking of frameworks.
  - **Always** use `-derivedDataPath build/...` generally to keep artifacts out of system locations.
  - **Always** use `-target` instead of `-scheme` if the scheme file is not shared (checked into git).
  - **Always** disable code signing for local simulator builds or CI builds without certificates using `CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO`.

- **Bazel**:
  - **Bazel Outputs:** All Bazel outputs are consolidated in `.bazel/` (e.g. `.bazel/bin`) via `.bazelrc`. This directory is gitignored and excluded from Spotless.
  - **Spotless vs Bazel:** Spotless exclusions are configured to ignore `.bazel/`. Running `spotlessApply` works safely even with Bazel symlinks present.

- **Validation**:
  - **Always** run `./scripts/validate.sh` before pushing to main. This script is maintained to match `ci.yml` strictly.
  - **Always** run `./scripts/spotless-apply.sh` and fix errors before pushing to main.
  - **Every plan must include `./scripts/validate.sh` as a verification step.** If a plan lists abbreviated checks (e.g. `compileDebugSources + spotless + test`), replace or append `./scripts/validate.sh` — it covers all of those and more (detekt, lint, architecture). Never let a plan leave out the full validation step.
  - **Avoid** `clean` steps in scripts and CI if possible, relying on Gradle's incremental build and caching for speed.

- **Linter & Architecture Enforcement**:
  - **Prefer automated enforcement over prose rules.** When you discover a rule that should always hold (naming convention, dependency direction, forbidden API usage), first check if an existing linter can enforce it. If not, propose adding a new check. A lint that fails the build is worth more than a paragraph in docs.
  - **Existing enforcement mechanisms** (choose the right one for each rule):

    | Mechanism | Location | Best For | Error Reporting |
    |-----------|----------|----------|-----------------|
    | **Detekt rules** (`detekt.yml`) | Config-only | Kotlin code patterns, naming, complexity, forbidden methods | Built-in detekt format |
    | **Detekt Compose plugin** | `detekt.yml` Compose section | Compose-specific: modifier naming, parameter order, CompositionLocal allowlist | Built-in detekt format |
    | **Custom Gradle tasks** (`buildSrc/`) | `buildSrc/src/main/kotlin/` | Cross-file rules, dependency graphs, theme layer boundaries, coverage gaps | Custom `GradleException` |
    | **Convention tests** (test source sets) | `*Test.kt` in `jvmTest`/`desktopTest` | Runtime reflection checks: "every X has a Y", API shape enforcement | JUnit assertion messages |
    | **Shell-based analysis** (`scripts/`) | `scripts/analyze-architecture.sh` | Multi-tool pipelines, graph analysis, report generation | Script exit codes + stdout |
    | **Spotless / ktlint** | `build.gradle.kts` | Formatting, import ordering, whitespace | Built-in ktlint format |
    | **Android Lint** | `lint.xml` / Gradle lint config | Android-specific: resource issues, API level compat, accessibility | Built-in Android Lint format |
    | **Claude Code hooks** (`.claude/hooks/`) | `.claude/hooks/*.sh` | Agent-specific guardrails: git safety, workflow enforcement | Hook stdout (warn or block) |

  - **Decision guide — which mechanism to use:**
    1. Can an existing detekt rule cover it? → Enable/configure it in `detekt.yml`
    2. Is it a file-scanning pattern match (regex on source)? → Custom Gradle task (follow `ThemeLayerCheckTask` pattern)
    3. Is it a structural rule about module dependencies? → Extend `ArchitectureCheckTask` or `analyze-architecture.sh`
    4. Does it require runtime reflection (class/method existence)? → Convention test (follow `UseCaseConventionTest` pattern)
    5. Is it agent-specific (git safety, workflow)? → Claude Code hook in `.claude/hooks/`

  - **Error message standards** (critical for AI tooling):
    - Every violation message **must** include: file path, line number (when applicable), rule ID, what's wrong, and how to fix it.
    - Format: `file:line [rule-id] Description of violation\n  Fix: Concrete action to take`
    - Bad: `"Architecture violation"` — Good: `"Module ':viewmodel' depends on forbidden module ':data'. Allowed: [:usecase, :domain, :presentation-model]. Move data access into a UseCase."`
    - Bad: `"Test missing"` — Good: `"ViewModel classes missing a corresponding '*ViewModelTest' class:\n  - com.example.FooViewModel\n\nCreate a test file for each ViewModel listed above."`

  - **Adding a new Gradle check** (template):
    1. Create `buildSrc/src/main/kotlin/<package>/<CheckName>CheckTask.kt` extending `DefaultTask`
    2. Create `buildSrc/src/main/kotlin/<package>/<CheckName>Plugin.kt` registering the task in group `"verification"`
    3. Register the plugin ID in `buildSrc/build.gradle.kts` under `gradlePlugin { plugins { ... } }`
    4. Apply the plugin in root `build.gradle.kts`: `id("<plugin-id>")`
    5. Add the task to `scripts/validate.sh` and the appropriate CI job in `.github/workflows/ci.yml`
    6. Follow the `ThemeLayerCheckTask` pattern: define rules as data, scan files, collect violations, throw `GradleException` with actionable messages

  - **Adding a new convention test** (template):
    1. Create `<module>/src/<testSourceSet>/kotlin/.../ConventionTestName.kt`
    2. Use classpath scanning to discover classes matching a pattern (see `UseCaseConventionTest`)
    3. Assert the convention with a clear failure message listing every violator
    4. The test runs automatically with `./gradlew test` — no extra CI config needed

  - **When to propose a new check** (agents should proactively suggest these):
    - A code review catches the same mistake twice → it should be a lint
    - A rule exists only in docs/comments → it should be enforced automatically
    - A PR introduces a new convention (e.g., "all screens must have X") → add a convention test
    - An agent makes a mistake that a linter could have caught → propose adding that linter rule

- **Compose-Specific Detekt Rules** (enforced by `detekt-compose` plugin, checked in `validate.sh`):
  - Modifier parameters in `@Composable` functions **must** be named `modifier` (not `contentModifier`, `shellModifier`, etc.).
  - Parameter order in `@Composable` functions: **non-default params first**, then `modifier: Modifier = Modifier`, then other params with defaults, then trailing lambdas.
  - When extracting or creating new composables that accept a `Modifier`, verify both rules before committing.

- **CI Workflow Synchronization**:
  - **When changing JDK version**: Update ALL workflow files in `.github/workflows/` that use `setup-java`.
  - **When changing Gradle version**: Verify all workflows use compatible settings.
  - **When changing tooling requirements**: Check `auto-generate.yml` in addition to main CI.
  - **Checklist for tooling changes**:
    1. `ci.yml` - Main CI workflow (includes `check_generated_content` drift check)
    2. `auto-generate.yml` - Post-merge generation of diagrams, analysis, screenshots
    3. Any other workflows in `.github/workflows/`
  - **Generated content architecture**: Workflows NEVER push commits to PR branches. Generated content (diagrams, analysis, screenshots) is updated post-merge on `main` via follow-up PRs created by `auto-generate.yml`.

## Branch Management

**Default: Every task should result in a PR.** When the user asks you to do any work, the default assumption is that you will create a branch and open a PR for it. Each PR should typically be based on `origin/main` (independent PRs). Stacked PRs (one branch based on another) are rarely needed and should only be used when changes genuinely depend on each other.

**Before starting new work, decide if you need a new branch:**
- Check if there's an existing PR for the current branch.
- If new work is unrelated to the current branch/PR, create a new branch from `origin/main`.
- When uncertain, ask the user or default to creating a new branch.

## Core Git Workflow

1.  **Sync with `main`**: Before starting new work, fetch the latest `main` branch.
    ```bash
    git fetch origin main
    ```

2.  **Create a Branch**: Create a new branch from `origin/main`. The branch MUST be prefixed with `agent/`.
    ```bash
    git checkout -b agent/your-branch-name origin/main
    ```

3.  **Implement Changes**: Make all code modifications according to the project's established conventions, as detailed in `rules.md`.

4.  **Format Code**: Run Spotless to ensure code style compliance.
    ```bash
    ./scripts/spotless-apply.sh
    ```

5.  **Commit and Push**: Once validation passes, commit the changes with a clear message and push the branch.
    ```bash
    git add .
    git commit -m "feat: Describe the feature or fix"
    git push origin agent/your-branch-name
    ```

    > [!IMPORTANT]
    > **Spotless Verification**: After running `git commit`, check the output. If the post-commit hook reports `spotlessCheck FAILED`, you MUST run `./gradlew spotlessApply`, amend your commit (or create a fix commit), and verify again. Do not ignore this failure.

6.  **Create a Pull Request**: Open a pull request against the `main` branch. Direct pushes to `main` are prohibited. Use the `/create-pr` workflow for standardized title and body formatting.

## After Your PR is Merged

**MANDATORY: Always clean up branches immediately after a PR is merged.** Stale branches accumulate quickly and clutter the repository.

1.  **Switch to the `main` Branch**:
    ```bash
    git checkout main
    ```

2.  **Pull the Latest Changes**: Ensure your local `main` branch is up-to-date.
    ```bash
    git pull origin main
    ```

3.  **Delete the Local Branch**:
    ```bash
    git branch -d agent/your-branch-name
    ```

4.  **Delete the Remote Branch** (if `--delete-branch` was not used during merge):
    ```bash
    git push origin --delete agent/your-branch-name
    ```

5.  **Prune stale remote refs**:
    ```bash
    git fetch --prune origin
    ```

> **Tip:** Use `gh pr merge --squash --delete-branch` to auto-delete the remote branch on merge. You still must delete the local branch manually.

**Example for `AGENT_NAME.md`:**
```markdown
# AGENT_NAME Instructions

This file provides the initial instructions for AGENT_NAME.

**First Action:** Immediately read the main contribution guidelines located at `.agent/AGENTS.md`. This file contains the required workflow and rules for all AI agents in this project. Do not proceed with any other actions until you have read and understood it.
```

## PR Merge Workflow

### Priority Levels

| Priority | Condition | Action |
|----------|-----------|--------|
| **P0** | `main` is broken (CI failing) | Stop everything. Fix immediately. |
| **P0.5** | Instruction/Beads PRs (see below) | Merge immediately. Shared context for all agents. |
| **P1** | PRs approved and CI green | Merge sequentially, monitor after each. |
| **P2** | PRs pending CI or review | Wait. Work on other tasks. |
| **P3** | New feature work | Only if P0-P2 queue is empty. |

### Instruction/Beads PRs (P0.5 Priority)

PRs that **only** modify the following files are **highest priority after broken builds**:

- `.agent/` - Agent instructions and workflows
- `CLAUDE.md`, `GEMINI.md`, `ANTIGRAVITY.md`, `AGENTS.md` - Agent entry points
- `.beads/` - Task tracking data

**Why P0.5?** These PRs establish shared understanding across all agents:
- Task assignments and status changes
- Workflow improvements and rules
- Session resume points and context

**Fast-path rules:**
1. These PRs pass CI immediately (path filtering skips expensive builds)
2. Merge as soon as CI is green (no batching needed, each is self-contained)
3. Pull latest main after merging to get updated instructions
4. All agents benefit from latest decisions and tasks immediately

**Quick merge:**
```bash
# Check if PR only modifies instruction/beads files
gh pr view <number> --json files | jq '.files[].path'

# If only .agent/, CLAUDE.md, or .beads/ → merge immediately when green
gh pr merge <number> --squash --delete-branch
```

### The Golden Rule

> **A broken `main` blocks everything.** No PR merges until `main` is green.

### Pre-Merge Checklist

Before merging ANY PR:

```bash
# 1. Check main branch CI status
gh run list --branch main --limit 3

# 2. If main is red, STOP. Fix main first.
# 3. If main is green, proceed with merge.
```

### Merge Sequencing

When multiple PRs are ready to merge:

1. **Merge ONE at a time**
2. **Wait for main CI** after each merge (check with `gh run list --branch main`)
3. **Check remaining open PRs for merge conflicts** after each merge (`gh pr view <N> --json mergeable`). PRs touching shared files (`.beads/issues.jsonl`, `.agent/project.md`, `CHANGELOG.md`) are especially prone to conflicts. Rebase immediately if conflicting.
4. **If CI fails**, stop merging and fix immediately
5. **Decide: Rebase or Direct Merge** (see below)

### Rebase vs Direct Merge Decision

**Tradeoff:** Rebasing resets the CI clock (slower but safer). Direct merge is faster but may fail after merge.

| PR Type | Strategy | Rationale |
|---------|----------|-----------|
| Docs-only changes | Direct merge | No code interaction risk |
| Single-file fixes | Direct merge if no conflicts | Low risk |
| Multi-file code changes | Rebase first | May interact with merged changes |
| Changes to shared code (build, CI, core) | Always rebase | High interaction risk |

**Decision tree:**
```
Is the PR docs-only or trivial?
  └─ Yes → Direct merge (use --auto if CI was green)
  └─ No → Does it touch files changed by recently merged PRs?
           └─ Yes → Rebase first
           └─ No → Direct merge is acceptable
```

**Direct merge (faster):**
```bash
# CI already passed, no rebase needed
gh pr merge <number> --squash --delete-branch
```

**Rebase first (safer):**
```bash
git fetch origin main
git checkout agent/pr-branch
git rebase origin/main
git push --force-with-lease origin agent/pr-branch
# Wait for PR CI to pass again, then merge
```

### Tracking Merge Success Rate

If post-merge failures become frequent (>10% of merges break main):
1. Switch to "always rebase" strategy
2. Consider enabling GitHub merge queue
3. Review which PR types are causing failures

```bash
# Check for failures in recent merges:
gh run list --branch main --limit 10 --json conclusion | grep -c failure
```

### When Main Breaks

**Immediate actions:**

1. **Stop all PR merges** - Do not merge anything else
2. **Create P0 fix task**:
   ```bash
   bd create --type task --priority P0 --title "Fix broken main: <failure description>"
   ```
3. **Identify the breaking commit**:
   ```bash
   gh run list --branch main --limit 5
   git log --oneline origin/main -10
   ```
4. **Fix options** (in order of preference):
   - Quick fix forward (new PR to fix the issue)
   - Revert the breaking commit if fix is complex

### Task Tracking with bd

#### PR Lifecycle Tasks

When creating PRs, track them:

```bash
# Create task for PR
bd create --type task --title "PR #123: <title>" --label pr-pending

# When CI passes and approved
bd update <id> --label pr-ready

# After merge, monitor main CI
bd update <id> --label pr-merged-monitoring

# After main CI passes
bd close <id>
```

#### Broken Build Tasks

```bash
# Create P0 task immediately
bd create --type task --priority P0 --title "BROKEN BUILD: <description>"

# This automatically blocks other work via priority
```

#### Recommended Labels

| Label | Meaning |
|-------|---------|
| `pr-pending` | PR created, waiting for CI/review |
| `pr-ready` | CI green, approved, ready to merge |
| `pr-merged-monitoring` | Merged, watching main CI |
| `build-broken` | Main CI is failing |
| `blocked-by-build` | Waiting for main to be green |

### Parallel PR Strategy

To maximize velocity while staying safe:

1. **Submit PRs in parallel** - Don't wait for one to merge before creating another
2. **Merge serially** - One at a time, with CI checks between
3. **Rebase before merge** - Ensure each PR is on latest main
4. **Monitor after merge** - Don't walk away until main CI passes

```
Good:
  Submit PR #1 ──────────────────────────────────┐
  Submit PR #2 ──────────────────────────────────┤ (parallel submission)
  Submit PR #3 ──────────────────────────────────┘

  Merge PR #1 → Wait for main CI → Green ✓
  Rebase PR #2 → Wait for PR CI → Merge PR #2 → Wait for main CI → Green ✓
  Rebase PR #3 → Wait for PR CI → Merge PR #3 → Wait for main CI → Green ✓

Bad:
  Merge PR #1 → Merge PR #2 → Merge PR #3 → Main CI fails → Which one broke it?
```

### Quick Reference Commands

```bash
# Check main CI status
gh run list --branch main --limit 3

# Check PR CI status
gh pr checks <pr-number>

# Merge with auto-wait for CI (if enabled)
gh pr merge <pr-number> --auto --squash

# List open PRs ready to merge
gh pr list --state open --json number,title,reviewDecision,statusCheckRollup

# Rebase PR on latest main
git fetch origin main
git rebase origin/main
git push --force-with-lease
```

## Accelerated Development Strategy

### The Problem with Serial Merging

Waiting for full CI (15-20 min) after each PR merge creates a bottleneck. With 10 PRs, that's 2.5+ hours of waiting.

### Accelerated Approach: Batch + Monitor

**Core principle:** Use local validation for high confidence, then batch merges while monitoring.

#### Risk Categories

| Risk Level | PR Type | Strategy |
|------------|---------|----------|
| **P0.5 (Immediate)** | `.agent/`, `CLAUDE.md`, `.beads/*` | Merge immediately when green (priority) |
| **Low** | Docs-only, README, comments | Batch merge up to 5 at once |
| **Medium** | Single-file code changes, test fixes | Merge 2-3, wait for CI |
| **High** | Multi-file refactors, CI changes, shared code | Serial merge, wait for CI |

#### Local Validation Before Merge

Before merging any code PR, run local validation:

```bash
# Quick validation (< 2 min)
./gradlew spotlessCheck --quiet

# Medium validation (< 5 min)
./gradlew spotlessCheck test --quiet

# Full validation (< 10 min) - for high-risk PRs
./scripts/validate.sh
```

#### Batch Merge Protocol

**For P0.5 PRs (instruction/beads - highest priority):**

```bash
# These establish shared context - merge immediately when CI passes
# Do NOT batch these - each provides immediate value to all agents

# 1. Identify instruction/beads PRs
gh pr list --json number,title,files | jq '.[] | select(.files | all(.path | test("^(\\.agent/|CLAUDE\\.md|\\.beads/)"))'

# 2. Merge each immediately when green (CI passes fast due to path filtering)
gh pr merge <number> --squash --delete-branch

# 3. Pull to get latest instructions before continuing other work
git pull origin main
```

**For low-risk PRs (docs-only, excluding instructions):**

```bash
# 1. Merge up to 5 docs-only PRs in quick succession
gh pr merge 199 --squash
gh pr merge 209 --squash
gh pr merge 214 --squash
gh pr merge 215 --squash

# 2. Check main CI for the batch (don't use --watch)
gh run list --branch main --limit 1

# 3. If any failure, identify and revert the culprit
```

**For medium-risk PRs:**

```bash
# 1. Run local validation on each PR branch
git checkout <branch>
./gradlew spotlessCheck test --quiet

# 2. Merge 2-3 at once if local validation passes
gh pr merge 200 --squash
gh pr merge 201 --squash

# 3. Check main CI before next batch (don't use --watch)
gh run list --branch main --limit 1
```

#### Parallel Work While CI Runs

While CI is running:
- Rebase remaining PRs onto latest main
- Run local validation on next batch
- Create new PRs for other tasks
- Review and approve pending PRs

#### Failure Recovery

If main breaks after batch merge:

1. **Identify the culprit** - Check which PR likely broke it
2. **Quick fix** - If obvious, fix forward with new PR
3. **Revert** - If unclear, revert the suspect PR
4. **Learn** - Move that PR type to higher risk category

```bash
# Revert last merge if needed (create PR, don't push directly to main)
git checkout -b agent/revert-broken-merge origin/main
git revert HEAD --no-edit
git push -u origin agent/revert-broken-merge
gh pr create --title "revert: Fix broken main" --body "Reverting last merge"
```

#### Velocity Metrics

Track to improve:
- PRs merged per hour
- Post-merge failure rate
- Time spent waiting vs working

**Target:** <10% post-merge failure rate while maximizing throughput.

### When to Use Each Strategy

| Situation | Strategy |
|-----------|----------|
| Catching up on PR backlog | Batch low-risk, parallel rebase |
| Active development | Serial merge with local validation |
| Broken main | Stop all merges, P0 fix |
| End of session | Ensure main is green before stopping |
| Rapid iteration (many changes) | Integration branch |

## Integration Branch Strategy (Rapid Development)

When making many related changes, use an integration branch to iterate quickly without waiting for main CI on every change.

### Concept

```
main ─────────────────────────────────────── ← Protected, requires CI
    \                                     /
     └── agent/integration ──●──●──●──●──● ← Fast iteration
                             │  │  │  │  │
                          (commits, no CI wait)
```

### When to Use

- Making 5+ related changes in a session
- Exploring/prototyping before final implementation
- Batch updates (docs, configs, tests)
- When CI latency is blocking productivity

### Workflow

1. **Create integration branch from main**
   ```bash
   git checkout -b agent/integration-<topic> origin/main
   ```

2. **Make rapid changes with local validation only**
   ```bash
   # Each change: validate locally, commit, continue
   ./gradlew spotlessCheck test --quiet
   git add . && git commit -m "feat: Change X"
   # No need to push or wait for CI
   ```

3. **Periodically sync with main** (if main changes)
   ```bash
   git fetch origin main
   git rebase origin/main
   ```

4. **When ready, create single PR to main**
   ```bash
   git push -u origin agent/integration-<topic>
   gh pr create --title "feat: <topic> - batch update"
   ```

5. **CI runs once on the batch**, not on each commit

### Trade-offs

| Aspect | Integration Branch | Direct to Main PRs |
|--------|-------------------|-------------------|
| Speed | Fast (no CI wait per commit) | Slow (CI after each) |
| Risk | Higher (batch testing) | Lower (incremental) |
| Atomicity | Single large PR | Multiple small PRs |
| Rollback | All or nothing | Granular |

### Rules

1. **Local validation is mandatory** - Always run `./gradlew spotlessCheck test` before committing
2. **Keep integration branches short-lived** - Merge to main within 1-2 sessions
3. **Don't let integration branches diverge too far** - Rebase on main regularly
4. **Final PR must pass full CI** - No shortcuts for the merge to main

### Example Session

```bash
# Morning: Start integration branch
git checkout -b agent/integration-docs origin/main

# Rapid changes (no CI wait)
vim docs/FEATURES.md && ./gradlew spotlessCheck && git add . && git commit -m "docs: Update features"
vim CLAUDE.md && ./gradlew spotlessCheck && git add . && git commit -m "docs: Update instructions"
vim README.md && ./gradlew spotlessCheck && git add . && git commit -m "docs: Update readme"
# ... more changes ...

# End of session: Create PR for the batch
git push -u origin agent/integration-docs
gh pr create --title "docs: Batch documentation update"
# CI runs once on all changes combined
```