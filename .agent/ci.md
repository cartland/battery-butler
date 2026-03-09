# CI Configuration

CI modes, path filtering, auto-generated content, and workflow details.

> **Parent doc:** See `AGENTS.md` for workflow rules and `project.md` for architecture.

## CI Mode (Development vs Release)

CI operates in one of two modes, controlled by `.github/ci-mode.txt`:

- **`development`** (default): Only fast checks (spotless, lint, detekt, unit tests, architecture, theme layer) are required on PRs. Slow jobs (instrumented tests, iOS builds, desktop builds, Android build, server build) are skipped on PRs but always run post-merge on `main`. This speeds up the PR cycle during active development.
- **`release`**: All jobs are required on PRs. Use this before cutting a release to ensure full coverage.

**Switching modes:**
```bash
# Switch to release mode
echo "release" > .github/ci-mode.txt
git add .github/ci-mode.txt
git commit -m "chore: Switch CI to release mode"

# Switch back to development mode
echo "development" > .github/ci-mode.txt
git add .github/ci-mode.txt
git commit -m "chore: Switch CI to development mode"
```

**How it works:**
- The `changes` job reads `.github/ci-mode.txt` and outputs `ci_mode`
- Slow jobs have an additional condition: `github.event_name == 'push' || ci_mode != 'development'`
- The `ci` gate job is mode-aware: in dev mode on PRs, it only checks fast job results
- On push to `main`, ALL jobs run regardless of mode (post-merge safety net)
- Issues caught post-merge in dev mode get fixed in follow-up PRs

**Hook behavior by mode:**
- Shell control flow (`for`/`while`/`if`): Always warning (never blocks)
- `--admin` bypass: Warning in development mode, blocked in release mode
- Validation-before-push: Always warning

## Path Filtering

CI uses `dorny/paths-filter` to skip expensive builds for non-code changes:
- **Beads-only changes** (`.beads/**`): Skip all builds, only run `ci` gate
- **Docs-only changes** (`*.md`, `.agent/**`): Skip all builds
- **Non-code server files** (`server/*.json`, `server/*.md`): Skip all builds
- **Code changes**: Run full build matrix (Android, iOS, Desktop, Server)

## Auto-Generated Content (Diagrams, Screenshots)

**Workflows NEVER push commits to PR branches.** Generated content is updated post-merge on `main` via follow-up PRs.

**How it works:**
1. Code merges to `main` -> `auto-generate.yml` runs
2. Generates diagrams + analysis (Job 1), screenshots sequentially (Job 2), and iOS snapshots (Job 3)
3. Screenshots use `scripts/generate-android-screenshots.sh` to avoid OOM on CI runners
4. Creates follow-up PRs on `auto/update-generated-content`, `auto/update-screenshots`, and `auto/update-ios-screenshots`
5. Uses `GITHUB_TOKEN` (not `BOT_PAT`) for PR creation -- loop-proof by design
6. **Inline CI trigger** (PR #928): Each job closes/reopens its PR with `BOT_PAT` immediately after creation, triggering CI within seconds
7. `ci-trigger-auto-prs.yml` remains as a fallback (fires on workflow completion)

**Why inline trigger was needed:** `cancel-in-progress: true` could cancel an auto-generate run after it created a PR but before the workflow completed. Since `ci-trigger-auto-prs.yml` only fires on completion, the PR would sit with no CI checks for ~16 minutes until the next run completed.

## CI Concurrency on Main (PR #856)

Push-to-main CI runs use SHA-based concurrency groups so rapid merges don't cancel each other. PR runs still cancel stale runs on the same branch. This ensures every main commit gets a complete CI result.

## Release Build Verification (PR #857)

`release-build-on-green.yml` builds a signed release AAB after every green CI on main. This proves the release pipeline (signing, bundling, Gradle config) works without deploying. Uses `VERSION_CODE=1` (must be >= 1 for Android Gradle Plugin). Artifacts uploaded for 30 days. Skips docs-only changes.

## Pre-Release CI Gate (PR #854)

`release-android.yml` has a `verify-ci` job that checks CI passed on the tagged commit before building. Uses `[.check_runs[] | select(.name == "ci")] | last | .conclusion` to handle multiple check-runs from CI re-runs. `release-android.sh` also checks CI status locally before creating tags.

## GitHub CLI Workflow Scope

Merging PRs that modify `.github/workflows/` files requires the `workflow` OAuth scope. If `gh pr merge` fails with "base branch policy prohibits the merge" and the PR touches workflow files, run `gh auth refresh -s workflow` to add the scope (requires browser-based device code flow).

## Concurrency Group Gotcha

CI uses concurrency groups to prevent parallel runs on the same branch. If a `workflow_dispatch` run starts while a `pull_request` run is in-flight, the `pull_request` run gets cancelled. The `ci` gate treats `cancelled` as failure. **PR status checks only track `pull_request`-event runs**, so a successful `workflow_dispatch` run won't clear the red status. Fix: push a new commit to the PR branch to trigger a fresh `pull_request` CI run.

## Dependabot PRs

Dependabot is configured (`.github/dependabot.yml`) for weekly updates.

**Merge criteria:**
- Simple updates (patch/minor versions with passing CI) -> merge
- Needs rebase -> use `@dependabot rebase` comment, then merge if CI passes
- Breaking changes -> close PR (large version jumps, CI compilation errors, critical infrastructure changes)
- PRs that modify `.github/workflows/` files cannot be merged via CLI (GitHub security restriction) -> manual merge via web UI
