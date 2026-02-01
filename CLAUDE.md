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

## Project-Specific Knowledge

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
