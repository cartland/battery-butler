# AI Agent Contribution Guidelines

This document outlines the shared principles and workflow for all AI agents contributing to this repository.

## Guiding Principles

1.  **Single Source of Truth**: This directory, `.agent/`, is the single source of truth for all AI agent instructions.
    *   **`AGENTS.md` (This file):** The high-level charter and core workflow.
    *   **`rules.md`:** An agent-specific entry point for internal technical guidelines.
    *   **`workflows/`:** Step-by-step playbooks for common tasks, serving as both detailed instructions for agents and user-triggerable commands (e.g., slash commands).
2.  **Consistency**: All agents must follow the workflows defined here to ensure predictable and consistent contributions.
3.  **Spotless is Mandatory**: All changes must be formatted by running `./scripts/spotless-apply.sh` before being committed. Full validation happens on PRs.

## 🚨 Critical Rules

1.  **NEVER Push Directly to `main`**:
    *   **Always** create a feature branch (`agent/your-branch-name`).
    *   **Always** open a Pull Request for changes.
    *   **Exception**: Only project maintainers may push to `main` for critical reverts or documentation updates if explicitly authorized.

2.  **NEVER Create Tags or Deploy Without Explicit Permission**:
    *   **NEVER** create git tags (e.g., `android/N`, release tags) without explicit user approval.
    *   **NEVER** push tags manually - always use release scripts (e.g., `./scripts/release-android.sh`).
    *   **NEVER** trigger deployment workflows without explicit user approval.
    *   When the user says "deploy" or "release", **ASK** which target and confirm, then use the appropriate release script.
    *   Tags trigger production releases and cannot be easily undone.
    *   Release scripts provide safety checks, version validation, and confirmation prompts.

3.  **ALWAYS Ask Before Destructive or Irreversible Actions**:
    *   Creating tags, deploying, force-pushing, deleting branches on remote, or any action that affects production requires explicit confirmation.
    *   When uncertain about scope, ask clarifying questions before proceeding.

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
   - **Instrumented tests**: Must run (network failures are acceptable if server isn't available)
   - **Screenshot tests**: Must run (baseline mismatches indicate UI changes, not broken infrastructure)

## Project Technical Rules

- **Configuration**:
  - **Always** check `local.properties` for sensitive or environment-specific configuration (e.g., API Keys, Server URLs).
  - Use `AppConfig` or `BuildConfig` to access these values in code, do NOT hardcode them.

- **Self Improvements**:
  - **Always** update `.agent/` documentation when learning a critical piece of information that will improve future agent performance. This ensures continuous learning and improvement for all agents.
  - **Always** update `.agent/rules.md` when adding new rules or best practices for the project.

- **Bash Commands**:
  - **Always** run bash commands as separate tool calls instead of combining them with `&&`, `||`, or `for` loops.
  - **Never** write shell scripts with control flow (loops, conditionals) in a single bash command.
  - **Example**: Instead of `gh pr merge 1 && gh pr merge 2`, make two separate bash tool calls.
  - This makes command execution more transparent and easier to debug.

- **Git**:
  - **Always** use non-interactive flags for commands that might open an editor (e.g., `git cherry-pick --continue --no-edit`). This prevents the shell from getting stuck waiting for user input.
  - **Always** escape special characters in command arguments (e.g., `$` and `` ` ``) to prevent unintended shell expansion. Use single quotes or backslashes (`\`) for escaping.
  - **Prefer** using `--body-file` with a temporary file for `gh pr create` when the description contains complex Markdown (backticks, quotes) to avoid shell parsing errors.

- **Pull Requests**:
  - **Always** ensure the Pull Request title and description accurately reflect the final changes. If the scope of a branch evolves, update the PR description before merging.
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
  - **Avoid** `clean` steps in scripts and CI if possible, relying on Gradle's incremental build and caching for speed.

- **CI Workflow Synchronization**:
  - **When changing JDK version**: Update ALL workflow files in `.github/workflows/` that use `setup-java`.
  - **When changing Gradle version**: Verify all workflows use compatible settings.
  - **When changing tooling requirements**: Check auxiliary workflows (update-diagrams.yml, update-screenshots.yml) in addition to main CI.
  - **Checklist for tooling changes**:
    1. `ci.yml` - Main CI workflow
    2. `update-diagrams.yml` - Auto-updates architecture diagrams
    3. `update-screenshots.yml` - Auto-updates Android screenshots
    4. Any other workflows in `.github/workflows/`

## Branch Management

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

6.  **Create a Pull Request**: Open a pull request against the `main` branch. Direct pushes to `main` are prohibited.

## After Your PR is Merged

Once your pull request has been approved and merged into `main`, it is important to clean up your branches to keep the repository tidy.

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

4.  **Delete the Remote Branch**:
    ```bash
    git push origin --delete agent/your-branch-name
    ```

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
| **P1** | PRs approved and CI green | Merge sequentially, monitor after each. |
| **P2** | PRs pending CI or review | Wait. Work on other tasks. |
| **P3** | New feature work | Only if P0-P2 queue is empty. |

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
3. **If CI fails**, stop merging and fix immediately
4. **Decide: Rebase or Direct Merge** (see below)

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
| **Low** | Docs-only, README, CLAUDE.md, .beads/* | Batch merge up to 5 at once |
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

**For low-risk PRs (docs-only):**

```bash
# 1. Merge up to 5 docs-only PRs in quick succession
gh pr merge 199 --squash
gh pr merge 209 --squash
gh pr merge 214 --squash
gh pr merge 215 --squash

# 2. Monitor main CI for the batch
gh run list --branch main --limit 1 --watch

# 3. If any failure, identify and revert the culprit
```

**For medium-risk PRs:**

```bash
# 1. Run local validation on each PR branch
git checkout <branch> && ./gradlew spotlessCheck test --quiet

# 2. Merge 2-3 at once if local validation passes
gh pr merge 200 --squash
gh pr merge 201 --squash

# 3. Wait for main CI before next batch
gh run list --branch main --limit 1 --watch
```

#### Parallel Work While CI Runs

While CI is running:
- Rebase remaining PRs onto latest main
- Run local validation on next batch
- Create new PRs for other tasks
- Review and approve pending PRs

```bash
# Rebase multiple branches in parallel
for branch in feat/a feat/b feat/c; do
  git checkout $branch && git rebase origin/main && git push -f &
done
wait
```

#### Failure Recovery

If main breaks after batch merge:

1. **Identify the culprit** - Check which PR likely broke it
2. **Quick fix** - If obvious, fix forward with new PR
3. **Revert** - If unclear, revert the suspect PR
4. **Learn** - Move that PR type to higher risk category

```bash
# Revert last merge if needed
git revert HEAD --no-edit
git push origin main
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