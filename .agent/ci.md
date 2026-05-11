# CI Configuration

CI modes, path filtering, auto-generated content, and workflow details.

> **Parent doc:** See `AGENTS.md` for workflow rules and `project.md` for architecture.

## CI Mode (Development vs Release)

CI operates in one of two modes, controlled by `.github/ci-mode.txt`:

- **`development`** (default): Only fast checks (spotless, lint, detekt, unit tests, architecture, theme layer, import boundary, test coverage, naming convention) are required on PRs. Slow jobs (instrumented tests, iOS builds, desktop builds, Android build, server build) are skipped on PRs but always run post-merge on `main`. This speeds up the PR cycle during active development.
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
3. `generateMermaidGraph` embeds `full_system_structure.mmd` into README.md (module dependency graph); `analyzeCode` embeds `code_distribution.mmd` into both README.md and CODE_ANALYSIS.md (sankey chart)
4. Screenshots use `scripts/generate-android-screenshots.sh` to avoid OOM on CI runners
5. Change detection includes `*.mmd`, `*.svg`, `docs/CODE_ANALYSIS.md`, and `README.md` — any of these trigger a follow-up PR
6. Creates follow-up PRs on `auto/update-generated-content`, `auto/update-android-screenshots`, and `auto/update-ios-screenshots`
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

CI uses concurrency groups to prevent parallel runs on the same branch. If a `workflow_dispatch` run starts while a `pull_request` run is in-flight, the `pull_request` run gets cancelled. The `ci` gate treats `cancelled` as failure. **PR status checks only track `pull_request`-event runs**, so a successful `workflow_dispatch` run won't clear the red status. Fix: push a new commit to the PR branch to trigger a fresh `pull_request` CI run, or use `gh run rerun <run-id>` on the original `pull_request`-triggered run (not `gh workflow run`).

## iOS CI — Xcode Version Pinning

iOS CI jobs (`validation_ios_ui`, `build_ios_compose`, `build_ios_native`, `ios-snapshots` in auto-generate) use `maxim-lobanov/setup-xcode@v1` to select an Xcode version.

**Key constraint:** The pinned Xcode version's SDK must have a matching simulator runtime installed on the runner. As of March 2026, `macos-latest` (macos-15-arm64) has:
- **Installed Xcodes:** 16.0–16.4, 26.0.1–26.3
- **Simulator runtimes:** iOS 18.5, 18.6, 26.0, 26.1, 26.2 (no 18.0–18.4)
- **Default:** Xcode 16.4; `latest-stable` resolves to Xcode 26.3

**Xcode 16.2 fails** because it bundles iOS 18.2 SDK but no 18.2 simulator runtime is installed. The fix (PR #965) was to pin to `26.3` which matches the `latest-stable` resolution.

**When updating the pin:** Check the [runner-images readme](https://github.com/actions/runner-images/blob/main/images/macos/macos-15-Readme.md) for available Xcode versions and simulator runtimes. All 4 pin sites must be updated together (3 in `ci.yml`, 1 in `auto-generate.yml`).

## Dependabot PRs

Dependabot is configured (`.github/dependabot.yml`) for weekly updates.

**Merge criteria:**
- Simple updates (patch/minor versions with passing CI) -> merge
- Needs rebase -> use `@dependabot rebase` comment, then merge if CI passes
- Breaking changes -> close PR (large version jumps, CI compilation errors, critical infrastructure changes)
- PRs that modify `.github/workflows/` files cannot be merged via CLI (GitHub security restriction) -> manual merge via web UI

## CI Debugging

- `ci-trigger-auto-prs.yml` fires on ANY `auto-generate.yml` completion (not just success) — individual jobs may succeed independently.
- CI gate checks both `failure` AND `cancelled` statuses — timed-out jobs properly fail CI.
- Path filter negation patterns (`!pattern`) don't work as exclusions in `dorny/paths-filter` — they match everything that ISN'T the pattern. Use explicit subdirectory patterns instead (e.g., `server/app/**` not `server/**` with `!server/*.md`).

## CI Warning Audit

`.github/annotation-ignores.txt` is an allowlist for the `/review-annotations` skill. One `grep -F` substring per line, `#` for comments. Each ignore entry should have a comment explaining the reason and (when applicable) a tracking link. Pre-populated with:

- `expect/actual classes ... are in Beta` — KT-61573 will resolve
- `org.jetbrains.kotlin.multiplatform plugin deprecated compatibility with Android Gradle plugin` — KMP-AGP separation work
- `Deprecated Gradle features were used in this build` — Gradle 9 umbrella warning
- `LocalClipboardManager` — intentionally suppressed at the call site in PR #1105

Run `/review-annotations` periodically (per release branch is typical) to audit new warnings against this list.

## Draining a Backlog of PRs

When >5 PRs queue up on auto-merge and `validation_ios_ui` (16.5 min) is the bottleneck, the supported recipe is to **temporarily flip `.github/ci-mode.txt` from `release` to `development`** in its own one-line PR. The aggregator at `ci.yml:530` skips slow jobs on PRs in development mode (push-to-main still runs the full suite, so this is *not* a relaxation of what hits main — only of what gates a PR before merge). Open a follow-up PR to flip back to `release` once the queue is drained.

For PRs that share files (e.g. `strings.xml`, list-screen content): disable auto-merge on the cluster duplicates via the GraphQL `disablePullRequestAutoMerge` mutation (the local `gh pr merge --disable-auto` is blocked by `.claude/hooks/git-guardrails.sh`), let the leader merge, then rebase the duplicates and re-enable. This prevents cascading rebases. Recipe details: see `workflow_pr_drain_strategy.md` in agent memory.
