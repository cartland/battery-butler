# PR Merge Strategy

Detailed PR merge workflow, batch merging, integration branches, and merge sequencing.

> **Parent doc:** See `AGENTS.md` for core rules and workflow.

## PR Merge Workflow

### Priority Levels

| Priority | Condition | Action |
|----------|-----------|--------|
| **P0** | `main` is broken (CI failing) | Stop everything. Fix immediately. |
| **P0.5** | Instruction PRs (see below) | Auto-merge (`--auto --squash`). Shared context for all agents. |
| **P1** | PRs approved and CI green | Merge sequentially, monitor after each. |
| **P2** | PRs pending CI or review | Wait. Work on other tasks. |
| **P3** | New feature work | Only if P0-P2 queue is empty. |

### Instruction PRs (P0.5 Priority)

PRs that **only** modify the following files are **highest priority after broken builds**:

- `.agent/` - Agent instructions and workflows
- `CLAUDE.md`, `GEMINI.md`, `ANTIGRAVITY.md`, `AGENTS.md` - Agent entry points
- `TODO.md` - Project task tracking

**Why P0.5?** These PRs establish shared understanding across all agents:
- Task assignments and status changes
- Workflow improvements and rules
- Session resume points and context

**Fast-path rules:**
1. These PRs pass CI immediately (path filtering skips expensive builds)
2. Use `gh pr merge <N> --auto --squash --delete-branch` right after pushing — no need to poll CI
3. Pull latest main after merge completes to get updated instructions
4. All agents benefit from latest decisions and tasks immediately

**Quick merge:**
```bash
# Check if PR only modifies instruction files
gh pr view <number> --json files | jq '.files[].path'

# If only .agent/, CLAUDE.md, or TODO.md → auto-merge when CI passes
gh pr merge <number> --auto --squash --delete-branch
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
3. **Check remaining open PRs for merge conflicts** after each merge (`gh pr view <N> --json mergeable`). PRs touching shared files (`TODO.md`, `.agent/project.md`, `CHANGELOG.md`) are especially prone to conflicts. Rebase immediately if conflicting.
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
2. **Add a P0 fix task** to the `## P0`/top of `TODO.md`: `Fix broken main: <failure description>`
3. **Identify the breaking commit**:
   ```bash
   gh run list --branch main --limit 5
   git log --oneline origin/main -10
   ```
4. **Fix options** (in order of preference):
   - Quick fix forward (new PR to fix the issue)
   - Revert the breaking commit if fix is complex

### Task Tracking

Track multi-session work in `TODO.md` (repo root). Within-session PR juggling
(which PR is pending CI, which is ready, which is merged-but-monitoring) is
ephemeral state — keep it in your head or in Claude's TaskCreate/TaskList during a
team session, not in `TODO.md`. Only durable, cross-session items (bugs, follow-ups,
a broken-main fix that outlives the session) belong in `TODO.md`, under the
appropriate priority heading.

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
| **P0.5 (Immediate)** | `.agent/`, `CLAUDE.md`, `TODO.md` | Auto-merge (`--auto --squash --delete-branch`) |
| **Low** | Docs-only, README, comments | Auto-merge, batch up to 5 at once |
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

**For P0.5 PRs (instruction - highest priority):**

```bash
# These establish shared context - auto-merge when CI passes
# Do NOT batch these - each provides immediate value to all agents

# 1. Identify instruction PRs
gh pr list --json number,title,files | jq '.[] | select(.files | all(.path | test("^(\\.agent/|CLAUDE\\.md|TODO\\.md)"))'

# 2. Set auto-merge (merges automatically when CI passes)
gh pr merge <number> --auto --squash --delete-branch

# 3. Pull to get latest instructions after merge completes
git pull origin main
```

**For low-risk PRs (docs-only, excluding instructions):**

```bash
# 1. Set auto-merge on docs-only PRs (no need to wait/poll)
gh pr merge 199 --auto --squash --delete-branch
gh pr merge 209 --auto --squash --delete-branch
gh pr merge 214 --auto --squash --delete-branch

# 2. After merges complete, check main CI for the batch
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
