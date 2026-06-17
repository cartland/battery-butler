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
- **Docs-only changes** (`*.md`, `.agent/**`): Skip all builds (includes `TODO.md`)
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

## Pre-Release CI Gate (PRs #854, #1197, #1200)

`release-android.yml`'s `verify-ci` job AND the local `scripts/release-android.sh` both check that CI passed on the target commit before allowing a release. They use the **sentinel-set** pattern — observed-job-outcomes is the only correct ground truth, because the `ci` aggregator alone can be a false-green.

**Why the aggregator was insufficient (the false-green path)**: A successful `ci` aggregator means nothing when:
- The commit was docs-only / config-only and dorny/paths-filter skipped every real `validation_*` and `build_*` job (`ci` aggregates skipped+skipped+... = success).
- The commit's CI ran in `development` mode on a `workflow_dispatch` / `pull_request` event — slow jobs skip via `ci.yml`'s `github.event_name == 'push' || ci_mode != 'development'` condition.

Both let a tag pass the old gate even though the commit was never validated.

**The fix (sentinel-set check)**: Both gates now require every job in this set to have a completed `success` conclusion on the target commit:

```
validation_ios_ui
validation_instrumented
build_android
build_ios_compose
build_ios_native
build_server
```

If any sentinel is not `success`, the gate refuses. This catches path-filter skip AND dev-mode skip AND any future skip condition without modeling them explicitly. Both gates use the same set — if you want to add or remove a job, update **both files together** to keep them in sync.

**Bonus jq fix**: The original `[.check_runs[] | select(.name == "ci")] | last | .conclusion` was buggy for re-runs. GitHub's `check_runs` API returns runs in DESCENDING `started_at`, so `| last` picked the OLDEST run (often a path-filtered skipped run, on commits that had both a skipped push + a real workflow_dispatch rerun). New pattern is in both files:

```jq
[.[] | select(.name == $job) | select(.status == "completed")]
    | sort_by(.completed_at) | last | .conclusion // "no_completed_run"
```

**Local `--check` mode is the canonical entry point** (PR #1200). It prints the per-sentinel-job conclusion plus a copy-paste-ready next command:

```bash
./scripts/release-android.sh --check
```

**Path-filter / dev-mode recovery**. If sentinels are skipped on the commit you want to release, ensure `.github/ci-mode.txt = release` on main, then dispatch CI manually:

```bash
gh workflow run "Battery Butler CI" --ref main
# Watch progress; full release-mode suite takes ~25 min for validation_ios_ui alone
gh run list --workflow "Battery Butler CI" --event workflow_dispatch --limit 1 \
  --json conclusion,status,databaseId,headSha
```

The dispatched run's check_runs appear on the commit. Once all sentinels are `success`, re-run `--check` and the gate will pass. Verified end-to-end on android/30 (`f50cc0d`, dispatched run `25773057059`) and android/31 (`c7419d5`, dispatched run `25859603708`).

**SHA-typed emergency overrides** (PR #1200) — both require a value from `--check` output, so accidental usage is hard:

- `--confirm-skipped-jobs <target-sha>` — release with sentinels not `success` (use only when build is independently validated)
- `--confirm-rollback-from <latest-tag-sha>` — required when target commit is on an older tag
- `--confirm-hash <target-sha>` — release a non-HEAD commit

**After release, flip CI back to development** in a second small PR. Leaving CI in release mode makes every PR run validation_ios_ui (~25 min) which slows the dev loop.

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

**Ignored dependencies** (`.github/dependabot.yml` → `ignore:`):
- `com.google.cloud.tools:jib-gradle-plugin` — pinned to 3.4.1. Jib 3.5.x bytecode calls a `putArchiveEntry(TarArchiveEntry)` overload that only exists in commons-compress 1.26+, but the build forces commons-compress 1.21 for Ktor compat. PR #1133 bumped past this and broke `build_server` on main; reverted in PR #1146 and added to the ignore list. See `build.gradle.kts` lines 7-13 + 17-22 for the resolution-strategy forces and the upstream issue [jib#4235](https://github.com/GoogleContainerTools/jib/issues/4235).
- `org.jetbrains.androidx.lifecycle:*` — pinned to `2.10.0-beta01` (`androidx-lifecycle-alpha` in `libs.versions.toml`). **2.11.0-beta01 dropped the `iosX64` target**, so every lifecycle artifact failed K/N resolution (`Couldn't resolve dependency … Unresolved platforms: [iosX64]`), breaking `build_ios_compose` / `build_ios_native` / `validation_ios_ui`. PR #1225 bumped it and broke `main` (commit `a9924c4`); reverted in PR #1239. **It passed PR CI because dev-mode skips iOS jobs on PRs — the break only surfaced post-merge.** Lesson: KMP/Compose dependency bumps with iOS/native targets must be validated in release-mode CI (or locally via `xcodebuild`/an iOS build) before merge; dev-mode PR CI cannot see iOS breaks.
- `org.jetbrains.kotlin:*` — ignored for versions `>= 2.3.20` (kotlin compiler/plugin artifacts only; the `kotlinx-*` group is left free to update). SKIE 0.10.9 does not support Kotlin 2.4.0 — `validation_compile_tests` fails with `Error: SKIE 0.10.9 does not support Kotlin 2.4.0. Supported versions are: [..., 2.3.0].` Dependabot PR #1222 (kotlin group → 2.4.0) closed; ignore added in PR #1243. Remove when a SKIE release supports the target Kotlin — see TODO `bb-k4sk`.
- `io.grpc:*` (>= 1.79.0) and `com.squareup.wire:*` (>= 6.0.0) — generated gRPC stubs call codegen APIs removed in gRPC 1.79 (`BlockingClientCall`, `blockingV2*Call`), producing 5 compile errors in `server/app`. Dependabot PR #1223 (grpc group → grpcJava 1.81 / wire 6.4) closed; ignore added in PR #1243. Remove after regenerating protos with a matching codegen plugin — see TODO `bb-gr79`.

**General rule for pinned dependencies**: if `build.gradle.kts` or `libs.versions.toml` has a `// Pinned to X` / `// Do not bump` comment, also add that dep to the dependabot ignore list — otherwise dependabot will propose the bump weekly and a slip-through is only a matter of time (especially in dev-mode CI where slow jobs are skipped on PRs).

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

## Post-Merge Auto-Issue Safety Net

`ci-post-merge-issue.yml` listens for every `Battery Butler CI` run that completes on a push to `main`:

- **On failure**: lists failing jobs via the Actions API and, for each, either opens an issue titled `CI failure on main: <job-name>` (labels: `ci-failure`, `blocking`) or comments on the existing open issue with that exact title. One issue per failing job, deduped by title.
- **On success**: closes every open `ci-failure` issue with a "resolved on green" comment.

This is what makes development-mode CI safe as the steady state: PRs only run fast checks, slow checks run post-merge on `main`, and any post-merge regression surfaces as a tracked issue instead of silent red.

**Scope caveat — only watches `Battery Butler CI`**: the workflow's trigger gates on `workflow_run.name == "Battery Butler CI"` AND `event == 'push'` AND `head_branch == 'main'`. Failures in the *automation pipeline* (`Auto-Generate Content`, `CI for Auto PRs`) are NOT covered — they never file a `ci-failure` issue no matter how often they fail, and so they never gate the merge queue. They only surface via a manual `/repo-check`. (Observed 2026-06-08: weekly `CI for Auto PRs` → `trigger-ci` "Bad credentials" failures from the expired BOT_PAT, bb-16u1, were invisible to this net.)

**Verified working (2026-06-08)**: open / comment-dedup / close-on-green have all fired correctly in production history. Concrete examples — open: #1216–1218; comment-dedup: #1180 (3× "Job failed again." on the same `ci` issue instead of duplicates); auto-close-on-green: #1199/#1198/#1192/#1184/#1168/#1164/#1140/#1139. Note the most *recent* closures (the 05-16 batch) were closed *manually* by the developer who fixed `main`, ahead of the auto-close — that's expected, not a regression.

**Companion gate**: `validation_no_blocking_issues` in `ci.yml` runs on every PR and fails if any open `ci-failure` issue carries the `blocking` label. This pauses new auto-merges until the regression is fixed forward.

**Companion ritual**: run `/check-ci-issues` at session start, before picking up new feature work. The skill at `.claude/skills/check-ci-issues/SKILL.md` documents the triage flow. The fix is a normal PR off `origin/main`; the next green push-to-main auto-closes the issue.

**Implementation**: `scripts/file-ci-failure-issue.sh` (called by the workflow). Labels are created on demand the first time a failure occurs.

**Chicken-and-egg unblock**: a fix-forward PR is itself blocked by `validation_no_blocking_issues` while the issue it fixes is open. Manually close the tracking issue(s) with a comment referencing the fix PR (e.g. `gh issue close N --comment "Fix in flight via PR #M"`), then `gh run rerun <run-id> --failed` on the PR's failed `validation_no_blocking_issues` check. Once the fix merges and a green push-to-main runs, the auto-issue workflow re-closes any stragglers. Two examples this session: PR #1137 (closed #1128/#1129), PR #1146 (closed #1144/#1145).

**Former limitation — docs-only success false-close (NOW FIXED, bb-2r4g)**: if a code-level failure files an issue and the next push to `main` is a docs/auto-generate-only commit, that commit's CI returns `ci: success` because the path filter skips every real job — so the success path would close the issue even though the code break still lives in `main`. Originally observed between PR #1133 (jib bump break, filed #1144/#1145) and PR #1143 (docs-only epic close, success). **This is now guarded**: `scripts/file-ci-failure-issue.sh` computes `real_success_count` (jobs that succeeded excluding the control-flow gates `changes`, `ci`, `validation_no_blocking_issues`) and `exit 0`s without closing anything when it's `0` — i.e. a path-filtered run where every real job was SKIPPED no longer auto-closes a real-failure issue. Verified in the script source 2026-06-08.
