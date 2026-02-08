# Claude Code Instructions

This file provides the initial instructions for Claude Code.

**First Action:** Immediately read the main contribution guidelines located at `.agent/AGENTS.md`. This file contains the required workflow and rules for all AI agents in this project. Do not proceed with any other actions until you have read and understood it.

For additional context on your role, also read the prompt in `.claude/PROMPT.md`.

## Self-Improvement

**You MUST update this file and `.agent/AGENTS.md` when you learn project-specific best practices.**

When you discover something that would help future sessions:
1. Add it to the appropriate section below
2. Create a PR for the update (don't leave it uncommitted)
3. Be specific and actionable

This ensures continuous learning across sessions.

## PR Merge Workflow (Quick Reference)

See `.agent/AGENTS.md` for full details. Key rules:

### Priorities
- **P0**: Fix broken main (drop everything)
- **P1**: Merge approved green PRs
- **P2**: Wait for pending PRs
- **P3**: New work (only if queue empty)

### Before Merging
```bash
# Always check main CI first
gh run list --branch main --limit 3
# If red, STOP and fix main first
```

### After Merging
```bash
# Monitor main CI after each merge
gh run list --branch main --limit 1
# Don't merge next PR until main is green
```

### When Main Breaks
```bash
# 1. Stop all merges
# 2. Create P0 task
bd create --type task --priority P0 --title "BROKEN BUILD: <description>"
# 3. Fix immediately
```

### Accelerated Development (Batch Merging)

See `.agent/AGENTS.md` for full "Accelerated Development Strategy" section.

**Quick Reference:**
- **Low-risk PRs** (docs-only): Batch merge up to 5 at once
- **Medium-risk PRs** (single-file changes): Merge 2-3, wait for CI
- **High-risk PRs** (multi-file, CI changes): Serial merge, wait for CI

```bash
# Batch merge docs-only PRs (fast)
gh pr merge 214 --squash
gh pr merge 209 --squash
gh pr merge 222 --squash
# Then monitor main CI for the batch
gh run list --branch main --limit 1 --watch
```

### Dependabot PRs
Dependabot is configured (`.github/dependabot.yml`) for weekly updates.

**Merge criteria:**
- ✅ **Simple updates**: Patch/minor versions with passing CI → merge
- ✅ **Needs rebase**: Use `@dependabot rebase` comment, then merge if CI passes
- ❌ **Breaking changes**: Close PR if:
  - Large version jumps (e.g., 15+ minor versions)
  - CI fails with compilation errors (not just needing rebase)
  - Changes to critical infrastructure (gRPC, database, sync)
  - No urgent security CVEs requiring immediate action

**When closing breaking Dependabot PRs:**
- Leave a comment explaining the rationale
- If upgrade is needed later, do it incrementally with proper testing

**Important:** PRs that modify `.github/workflows/` files (GitHub Actions updates) cannot be merged via CLI due to GitHub security restrictions. These require manual merge via web UI.

## Architecture Principles

### Offline-First Sync
The app should work entirely offline and sync bidirectionally when online:
- Local changes persist immediately to Room database
- Changes sync to server when connectivity is available
- Server changes sync back to local on reconnect
- Deletes should eventually sync (see `bb-d0t` for remote delete support)

### Error Handling
**Project code NEVER throws exceptions except `CancellationException`.**

Use sealed class hierarchies for exhaustive `when` expressions:
```kotlin
// GOOD: Required sealed type - compiler enforces handling all cases
data class Failed(val error: DataError) : SyncStatus

when (status) {
    is SyncStatus.Failed -> when (status.error) {
        is DataError.Network.ConnectionFailed -> // handle
        is DataError.Network.Timeout -> // handle
        // ... compiler error if cases missing
    }
}

// BAD: Optional field - callers can ignore typed error
data class Failed(val message: String, val error: DataError? = null)
```

Key types (see `domain/model/DataResult.kt`):
- `DataResult<T>` - Success/Error wrapper for operations
- `DataError` - Sealed hierarchy: Network, Database, Ai, Unknown
- Catch library exceptions at data layer boundaries, return typed errors

## Project-Specific Knowledge

### Efficiency Rules
- **NEVER use `sleep` commands** - Don't wait for CI. Find productive work instead.
- **Always iterate locally** - Run local validation while CI runs remotely
- **Check CI status without waiting** - Use `gh pr view` or `gh run list` without `--watch`
- **Work in parallel** - While one PR's CI runs, work on other tasks from `bd ready`

### Build System
- **Bazel disk cache issue**: When running `bazel build` in scripts called from Xcode, use `--disk_cache=""` to ensure outputs are materialized locally. The disk cache can return metadata without creating actual files.
- **iOS protos**: Run `./scripts/generate-protos.sh` before iOS builds if proto files changed. The script generates Swift protobuf files from Bazel.

### Testing
- **Unit tests**: `./gradlew test` - must pass
- **Screenshot tests**: `./gradlew :android-screenshot-tests:validateDebugScreenshotTest` - failures indicate UI changes, not broken infrastructure
- **Instrumented tests**: Require running emulator, network failures are expected if server isn't running

### Common Commands
```bash
# Format code
./scripts/spotless-apply.sh

# Full validation
./scripts/validate.sh

# Build platforms
./gradlew :compose-app:assembleDebug          # Android
./gradlew :compose-app:desktopJar             # Desktop
./gradlew :server:app:build                   # Server
xcodebuild -project ios-app-swift-ui/...      # iOS
```

### Releases

**NEVER push git tags manually. Always use the release scripts.**

```bash
# Android release
./scripts/release-android.sh

# Server release (future)
./scripts/release-server.sh
```

#### Why This Matters
1. Release scripts check for existing tags and increment correctly
2. Manual tag pushes can conflict with Play Store versionCode history
3. Scripts provide confirmation prompts and safety checks
4. Scripts ensure you're on the right commit

### Task Management with `bd` (Beads)

**IMPORTANT: Beads is the primary task tracking system for this project.**

Use `bd` CLI for all task/issue management. **Never modify `.beads/issues.jsonl` directly.**

Beads is an AI-native issue tracker that lives in the repository. Issues are stored in `.beads/issues.jsonl` (JSONL format for easy git merging) while SQLite is used as a local cache.

#### How It Works
- **JSONL is the source of truth** - Portable, git-friendly, mergeable
- **SQLite is a local cache** - Fast queries, rebuilt from JSONL automatically
- **Parallel machines** - Each machine has its own SQLite cache; JSONL merges via git
- **Merge driver** - `.gitattributes` configures `merge=beads` for smart conflict resolution

#### Rules for Task Tracking
1. **Always create beads for new tasks** - Don't just discuss tasks, create beads for them
2. **Check `bd list` at session start** - See what work is pending
3. **Close beads when done** - Use `bd close <id>` with a reason
4. **Never lose tasks in conversation** - If something needs doing, create a bead

#### Session Workflow
```bash
# Start of session
bd list                    # See pending tasks
bd ready                   # See tasks ready to work on

# When you identify new work
bd create "Task title" --type task --priority P2

# When you complete work
bd close <id> --reason "Fixed in PR #123"
```

#### Quick Reference
```bash
bd list              # List all open issues
bd ready             # Show tasks ready to work on (no blockers)
bd show <id>         # View full task details
bd close <id>        # Mark task complete
```

#### Creating Issues
```bash
# Create a task
bd create "Fix the login button" --type task --priority P2

# Create an epic
bd create "User Authentication" --type epic --priority P2 \
  --description "Implement login flow with Google Sign-In..."

# Create a bug
bd create "App crashes on startup" --type bug --priority P1

# With full options
bd create "Title" \
  --type task|bug|feature|epic|chore \
  --priority P0|P1|P2|P3|P4 \
  --description "Detailed description..." \
  --labels "ui,android"
```

#### Issue Types
- `task` - General work item (default)
- `bug` - Something broken that needs fixing
- `feature` - New functionality
- `epic` - Large initiative with multiple tasks
- `chore` - Maintenance work

#### Priority Levels
- **P0** - Critical/blocking (fix immediately)
- **P1** - High priority
- **P2** - Normal priority (default)
- **P3** - Low priority
- **P4** - Nice to have

#### Filtering & Searching
```bash
bd list --type epic           # List only epics
bd list --priority P1         # List high priority items
bd search "login"             # Search by text
bd list --labels ui           # Filter by label
```

#### Closing Issues
```bash
bd close <id>                              # Simple close
bd close <id> --reason "Fixed in PR #123"  # With reason
```

#### Committing Beads Changes
Beads files should be committed to git like any other code:

```bash
# After creating/closing issues, commit the changes
git add .beads/issues.jsonl
git commit -m "chore: Update issue tracking"
```

**What gets committed:**
- `.beads/issues.jsonl` - Issue data (JSONL format)
- `.beads/interactions.jsonl` - Interaction logs
- `.beads/config.yaml` - Configuration
- `.beads/metadata.json` - Metadata

**What stays local (gitignored):**
- `*.db*` - SQLite database (local cache)
- `daemon.*` - Runtime files
- `bd.sock` - Socket file

#### Parallel Machine Workflow
When multiple machines edit issues:
1. Each machine modifies its local SQLite + JSONL
2. Commit and push `.beads/issues.jsonl` via PR
3. Git merges JSONL using the beads merge driver
4. After pull, SQLite cache rebuilds from merged JSONL

The JSONL format (one JSON object per line) makes merges simple - each issue is a separate line, so conflicts only occur when the same issue is edited on multiple machines.

#### Beads Commit Strategy

**When to commit beads:**

1. **With every PR** - If your PR references or closes a bead, include `.beads/issues.jsonl` in the same commit or PR
2. **At end of session** - Always commit beads before ending a work session, even if no code changes were made
3. **After creating tasks** - Commit immediately after `bd create` to avoid losing task definitions
4. **After significant updates** - Closing multiple tasks, updating priorities, or adding detailed descriptions

**How to commit:**
```bash
# Include with code changes (recommended)
git add src/... .beads/issues.jsonl
git commit -m "feat: Add feature X (closes bb-123)"

# Standalone beads update (when no code changes)
git add .beads/
git commit -m "chore(beads): Update task tracking"
```

**Best practices:**
- Don't let beads changes accumulate - commit early and often
- If working on a feature branch, include beads updates in that branch
- Push beads commits promptly so other collaborators see task updates
- Use descriptive commit messages that reference affected task IDs

### Server Deployment

Multi-environment deployment pipeline: dev → staging → prod. Same Docker image SHA promoted through environments.

**Workflows:**
- `server-build.yml` — Auto-deploys to dev on push to main (server changes)
- `server-deploy-staging.yml` — Manual trigger with `image_tag` input
- `server-deploy-prod.yml` — Manual trigger with approval gate (GitHub Environment)
- `server-destroy.yml` — Tear down staging/dev infrastructure
- `server-rollback.yml` — Emergency rollback

**Deploy commands:**
```bash
# Check latest dev image tag
gh run list --workflow=server-build.yml --limit 1

# Promote to staging
gh workflow run server-deploy-staging.yml -f image_tag=<sha>

# Promote to prod (requires approval)
gh workflow run server-deploy-prod.yml -f image_tag=<sha>

# Test endpoints
grpcurl -plaintext -proto protos/com/chriscartland/batterybutler/protos/battery_service.proto \
  <nlb-dns>:80 com.chriscartland.batterybutler.proto.BatteryService/GetServerStatus
```

**Key architecture decisions:**
- ECR is managed outside terraform (data source, not resource) to avoid state lock issues
- Each environment has separate terraform state (`server/{env}/terraform.tfstate`)
- Concurrency groups prevent parallel deploys to same environment
- IAM permissions documented in `server/iam_policy.json` — update AWS Console manually when changed

**AWS free-tier limitations:**
- Only `db.t3.micro` RDS instances allowed
- Max 2 RDS instances — can't run dev + staging + prod simultaneously
- Use `server-destroy.yml` to tear down unused environments

### CI Path Filtering

CI uses `dorny/paths-filter` to skip expensive builds for non-code changes:
- **Beads-only changes** (`.beads/**`): Skip all builds, only run `ci` gate
- **Docs-only changes** (`*.md`, `.agent/**`): Skip all builds
- **Non-code server files** (`server/*.json`, `server/*.md`): Skip all builds
- **Code changes**: Run full build matrix (Android, iOS, Desktop, Server)

## Session Resume Points

**Last Updated: 2026-02-07**

### Ready Tasks
Run `bd ready` to see current tasks. Top priorities:
1. `bb-jj7` (P3 epic) — Split terraform into shared and per-environment configs
2. `bb-sys` (P3 epic) — Login: Google Sign-In with ID Token Verification

### Recent Completions
- Multi-environment server deployment (PRs #405-#411)
- Comprehensive IAM permissions audit and fix (PR #408)
- Server deployed to dev and prod (image `eed5b36`)
- Staging destroyed to free RDS quota (free-tier limit)

### Known Issues
- **Screenshot tests with relative time** are flaky — components showing "X days ago" change based on test run date (tracked in `bb-9k1`)
- **Dependabot workflow PRs** need manual merge via web UI
- **Staging unavailable** — destroyed to stay within free-tier RDS instance quota; re-deploy after AWS account upgrade

### Context
- CI uses unified "ci" job for required checks
- Docs-only PRs skip expensive builds via path filtering
- Accelerated development strategy in `.agent/AGENTS.md`
- **Never use sleep** — find productive work while CI runs
- Server NLB endpoints expose port 80 (TCP → gRPC 50051); no HTTP health endpoint exposed externally
