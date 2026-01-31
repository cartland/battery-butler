# Claude Code Instructions

This file provides the initial instructions for Claude Code.

**First Action:** Immediately read the main contribution guidelines located at `.agent/AGENTS.md`. This file contains the required workflow and rules for all AI agents in this project. Do not proceed with any other actions until you have read and understood it.

For additional context on your role, also read the prompt in `.claude/PROMPT.md`.

## Self-Improvement

**You MUST update this file and `.claude/` settings when you learn project-specific best practices.**

When you discover something that would help future sessions:
1. Add it to the appropriate section below
2. Create a PR for the update (don't leave it uncommitted)
3. Be specific and actionable

This ensures continuous learning across sessions.

## Project-Specific Knowledge

### Build System
- **Bazel disk cache issue**: When running `bazel build` in scripts called from Xcode, use `--disk_cache=""` to ensure outputs are materialized locally. The disk cache can return metadata without creating actual files.
- **iOS protos**: Run `./scripts/generate-protos.sh` before iOS builds if proto files changed. The script generates Swift protobuf files from Bazel.

### Testing
- **Unit tests**: `./gradlew test` - must pass
- **Screenshot tests**: `./gradlew :android-screenshot-tests:validateDebugScreenshotTest` - failures indicate UI changes, not broken infrastructure
- **Instrumented tests**: Require running emulator, network failures are expected if server isn't running

### Common Tasks
- **Format code**: `./scripts/spotless-apply.sh`
- **Full validation**: `./scripts/validate.sh`
- **Build Android**: `./gradlew :compose-app:assembleDebug`
- **Build iOS**: `xcodebuild -project ios-app-swift-ui/iosAppSwiftUI.xcodeproj -scheme iosAppSwiftUI ...`
- **Build Desktop**: `./gradlew :compose-app:desktopJar`
- **Build Server**: `./gradlew :server:app:build`

### Task Management
- Use `bd` (Beads) for task tracking
- `bd ready` - show tasks ready to work on
- `bd close <id>` - mark task complete
- `bd show <id>` - view task details