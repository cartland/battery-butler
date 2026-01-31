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

### Task Management
```bash
bd ready           # Show tasks ready to work on
bd close <id>      # Mark task complete
bd show <id>       # View task details
bd create ...      # Create new task
```
