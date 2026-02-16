# Project Knowledge

Shared project-specific knowledge for all AI agents. This supplements the workflow rules in `AGENTS.md` with technical context about the codebase.

## Architecture

**Battery Butler** is a Kotlin Multiplatform (KMP) app with a Ktor gRPC server.

- **Platforms**: Android, iOS (SwiftUI + Compose), Desktop
- **Server**: Ktor + gRPC on AWS ECS Fargate
- **Database**: Room (local), RDS PostgreSQL (server)
- **Build**: Gradle (app), Bazel (iOS protos), Terraform (infrastructure)
- **CI**: GitHub Actions
- **Task tracking**: `bd` (Beads CLI)

### Offline-First Sync

The app works entirely offline and syncs bidirectionally when online:
- Local changes persist immediately to Room database
- Changes sync to server when connectivity is available
- Server changes sync back to local on reconnect

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

## Common Commands

```bash
# Format code
./scripts/spotless-apply.sh

# Full validation (matches CI)
./scripts/validate.sh

# Build platforms
./gradlew :compose-app:assembleDebug          # Android
./gradlew :compose-app:desktopJar             # Desktop
./gradlew :server:app:build                   # Server
xcodebuild -project ios-app-swift-ui/...      # iOS
```

## Testing

- **Unit tests**: `./gradlew test` - must pass
- **Screenshot tests**: `./gradlew :android-screenshot-tests:validateDebugScreenshotTest` - failures indicate UI changes, not broken infrastructure
- **Instrumented tests**: Require running emulator, network failures are expected if server isn't running
- **E2E tests** (`e2e-tests/`): Wire gRPC client tests against a real server. NOT included in CI or `validate.sh`.
  ```bash
  ./scripts/e2e-tests.sh                    # Auto-starts local server
  ./scripts/e2e-tests.sh --remote           # Uses E2E_SERVER_URL env var
  E2E_SERVER_URL=http://<nlb>:80 ./scripts/e2e-tests.sh --remote  # Against cloud
  ./gradlew :e2e-tests:test -De2e.server.url=http://localhost:50051  # Direct
  ```

## Build System

- **Bazel disk cache issue**: When running `bazel build` in scripts called from Xcode, use `--disk_cache=""` to ensure outputs are materialized locally. The disk cache can return metadata without creating actual files.
- **iOS protos**: Run `./scripts/generate-protos.sh` before iOS builds if proto files changed. The script generates Swift protobuf files from Bazel.

## Server URL Management

The production server URL flows through the system as follows:

**Source of truth:** GitHub secret `PRODUCTION_SERVER_URL`, auto-synced from terraform output after each deploy.

**How it propagates:**
1. Terraform creates NLB → deploy workflows capture `nlb_dns_name` → `gh secret set PRODUCTION_SERVER_URL`
2. CI workflows set `ORG_GRADLE_PROJECT_PRODUCTION_SERVER_URL` env var from the secret
3. Gradle reads it as a project property → `data-network/build.gradle.kts` generates `BuildConfig.kt`
4. Code accesses via `com.chriscartland.batterybutler.datanetwork.BuildConfig.PRODUCTION_SERVER_URL`

**DI pattern for modules without data-network dependency:**
- `ProductionServerUrl` data class (in `domain/model/`) wraps the URL for type-safe injection
- `AppComponent` (Android/Desktop) and `NativeComponent` (iOS) provide it from `BuildConfig.PRODUCTION_SERVER_URL`
- ViewModels and other components receive it via constructor injection

**Key rules:**
- **NEVER hardcode NLB hostnames** in Kotlin source — use `BuildConfig.PRODUCTION_SERVER_URL` or `ProductionServerUrl`
- `gradle.properties` has a fallback value for local dev only; CI always overrides it
- `release-android.yml` validates server connectivity before uploading to Play Store

## Releases

**NEVER push git tags manually. Always use the release scripts.**

```bash
# Android release
./scripts/release-android.sh

# Server release (future)
./scripts/release-server.sh
```

Release scripts check for existing tags, increment correctly, provide confirmation prompts, and ensure you're on the right commit.

## Task Management

### Two Task Systems

- **`bd` (beads)** — Cross-session project tracking. Persists in git. Use for epics, bugs, features that span multiple sessions.
- **Claude's TaskCreate/TaskList** — Within-session team coordination. Ephemeral. Use for breaking work into subtasks during a team session.

**Rules:**
- Teammates in a Claude Code team use TaskCreate/TaskList for coordination (never `bd`)
- Use `bd` at session start (`bd ready`) and session end (`bd close`) for project-level items
- Don't duplicate: if it's a single-session task, it doesn't need a bead

### `bd` Quick Reference

Use `bd` CLI for all task/issue management. **Never modify `.beads/issues.jsonl` directly.** Run `bd help` for full command list.

```bash
# Session workflow
bd list              # List all open issues
bd ready             # Show tasks ready to work on (no blockers)
bd show <id>         # View full task details
bd create "Title" --type task --priority P2  # Create a task
bd close <id> --reason "Fixed in PR #123"    # Mark complete
bd search "login"    # Search by text
```

### Committing Beads Changes

Beads files should be committed to git like any other code:

```bash
# Include with code changes (recommended)
git add src/... .beads/issues.jsonl
git commit -m "feat: Add feature X (closes bb-123)"

# Standalone beads update (when no code changes)
git add .beads/
git commit -m "chore(beads): Update task tracking"
```

**What gets committed:** `.beads/issues.jsonl`, `.beads/interactions.jsonl`, `.beads/config.yaml`, `.beads/metadata.json`

**What stays local (gitignored):** `*.db*`, `daemon.*`, `bd.sock`

## Server Deployment

Multi-environment deployment pipeline: dev -> staging -> prod. Same Docker image SHA promoted through environments.

**Workflows:**
- `server-build.yml` -- Auto-deploys to dev on push to main (server changes), syncs `DEV_SERVER_URL` secret
- `server-deploy-staging.yml` -- Manual trigger with `image_tag` input
- `server-deploy-prod.yml` -- Manual trigger with approval gate, syncs `PRODUCTION_SERVER_URL` secret
- `server-destroy.yml` -- Tear down staging/dev infrastructure
- `server-rollback.yml` -- Emergency rollback

**Deploy commands:**
```bash
# Check what's deployed right now
./scripts/deploy-status.sh

# Promote to staging
gh workflow run server-deploy-staging.yml -f image_tag=<sha>

# Promote to prod (requires approval)
gh workflow run server-deploy-prod.yml -f image_tag=<sha>

# Test endpoints
grpcurl -plaintext -proto protos/com/chriscartland/batterybutler/protos/battery_service.proto \
  <nlb-dns>:80 com.chriscartland.batterybutler.proto.BatteryService/GetServerStatus

# Run E2E tests against a live environment
E2E_SERVER_URL=http://<nlb-dns>:80 ./scripts/e2e-tests.sh --remote
```

**Deployment observability:**
- `./scripts/deploy-status.sh` -- Shows image tag, status, commit, and drift warnings for each environment
- GitHub commit statuses (`deploy/dev`, `deploy/staging`, `deploy/prod`) -- Annotated on each commit after deploy, visible on GitHub commit pages
- Deploy workflows always check prod vs dev drift (run `deploy-status.sh` at session start)

**Key architecture decisions:**
- ECR is managed outside terraform (data source, not resource) to avoid state lock issues
- Each environment has separate terraform state (`server/{env}/terraform.tfstate`)
- Concurrency groups prevent parallel deploys to same environment
- IAM permissions documented in `server/iam_policy.json` -- update AWS Console manually when changed
- Deploy workflows auto-sync NLB hostname to GitHub secrets (`PRODUCTION_SERVER_URL`, `DEV_SERVER_URL`)
- `release-android.yml` validates server connectivity before Play Store upload

**AWS free-tier limitations:**
- Only `db.t3.micro` RDS instances allowed
- Max 2 RDS instances -- can't run dev + staging + prod simultaneously
- Use `server-destroy.yml` to tear down unused environments

## CI

### Path Filtering

CI uses `dorny/paths-filter` to skip expensive builds for non-code changes:
- **Beads-only changes** (`.beads/**`): Skip all builds, only run `ci` gate
- **Docs-only changes** (`*.md`, `.agent/**`): Skip all builds
- **Non-code server files** (`server/*.json`, `server/*.md`): Skip all builds
- **Code changes**: Run full build matrix (Android, iOS, Desktop, Server)

### Auto-Generated Content (Diagrams, Screenshots)

**Workflows NEVER push commits to PR branches.** Generated content is updated post-merge on `main` via follow-up PRs.

**How it works:**
1. Code merges to `main` -> `auto-generate.yml` runs
2. Generates diagrams + analysis + screenshots
3. Creates follow-up PRs on `auto/update-generated-content` and `auto/update-screenshots`
4. Uses `GITHUB_TOKEN` (not `BOT_PAT`) -- loop-proof by design
5. `ci-trigger-auto-prs.yml` dispatches CI on the auto PRs

### Dependabot PRs

Dependabot is configured (`.github/dependabot.yml`) for weekly updates.

**Merge criteria:**
- Simple updates (patch/minor versions with passing CI) -> merge
- Needs rebase -> use `@dependabot rebase` comment, then merge if CI passes
- Breaking changes -> close PR (large version jumps, CI compilation errors, critical infrastructure changes)
- PRs that modify `.github/workflows/` files cannot be merged via CLI (GitHub security restriction) -> manual merge via web UI

## Efficiency Rules

- **NEVER use `sleep` commands** - Don't wait for CI. Find productive work instead.
- **Always iterate locally** - Run local validation while CI runs remotely.
- **Check CI status without waiting** - Use `gh pr view` or `gh run list` without `--watch`.
- **Work in parallel** - While one PR's CI runs, work on other tasks from `bd ready`.
