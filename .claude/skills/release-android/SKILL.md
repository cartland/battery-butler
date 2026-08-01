---
description: Release the Android app to Play Store internal track via tag-based deployment.
allowed-tools: Bash(*), Read, Glob, Grep
---

# Release Android

> [!CAUTION]
> **Agent Rule:** NEVER create tags or push them without explicit user permission.
> Tags trigger production releases and cannot be easily undone.
> Always confirm the release details and get user approval before proceeding.

This skill is the agent-facing operational shortcut. `./scripts/release-android.sh --check` is the source of truth for which command to paste next — read its output, copy the recommended command, do not retype flag combinations from memory.

## The flow

```bash
# 1. Clean state
git checkout main && git pull --ff-only && git status   # working tree must be clean

# 2. Read the next-step command from --check
./scripts/release-android.sh --check
```

`--check` prints:
- Latest tag and its commit SHA
- Next tag that will be created
- Target commit (HEAD or --confirm-hash)
- Branch / worktree state
- Per-sentinel-job CI conclusion on the target commit (the 6 jobs that the server-side `verify-ci` also enforces)
- Whether this is a rollback (target commit on an older tag)
- A copy-paste-ready command for the right scenario

```bash
# 3. Paste the command --check printed. The common scenarios:
./scripts/release-android.sh --confirm-tag android/N                  # normal
./scripts/release-android.sh --confirm-tag android/N \
    --confirm-skipped-jobs <target-sha>                                # CI was path-filtered or dev-mode
./scripts/release-android.sh --confirm-tag android/N \
    --confirm-hash <target-sha> \
    --confirm-rollback-from <current-latest-tag-sha>                   # rollback

# 4. Watch the deploy
gh run list --workflow=release-android.yml --limit 1
gh run watch <run-id>
```

## What `--check` will tell you

| Scenario detected | What `--check` does |
|---|---|
| All sentinels green, on main, clean tree | Prints the simple `--confirm-tag` command |
| Dirty working tree | Refuses; asks you to commit/stash and re-run `--check` |
| Not on main | Prints command with `--confirm-hash <HEAD-sha>` |
| No CI on commit yet | Prints `gh workflow run "Battery Butler CI" --ref main -f ci_mode=release` recovery |
| One or more sentinel jobs not success | Prints both the recovery flow (rerun CI in release mode) and the emergency override |
| Target commit on an older tag | Prints the full rollback command with both SHAs filled in |

## Sentinel jobs (must succeed before release)

These are the jobs that BOTH the local script AND server-side `verify-ci` check on the tagged commit. Path-filter false-greens (docs-only changes that skip every real job but produce a `success` `ci` aggregator) and dev-mode false-greens (jobs that only run on `push` events or when `ci-mode = release`) are caught here:

- `validation_ios_ui`
- `validation_instrumented`
- `build_android`
- `build_ios_compose`
- `build_ios_native`
- `build_server`

If any of these is not `success` on the target commit, the gate refuses. The fix is almost always: ensure `.github/ci-mode.txt = release` on `main`, dispatch CI manually (`gh workflow run "Battery Butler CI" --ref main -f ci_mode=release`), wait for all sentinels green, then re-run `--check`.

> **Don't drop `-f ci_mode=release`.** The `ci_mode` workflow_dispatch input defaults to `development` and *overrides* the committed `ci-mode.txt`, so a bare `gh workflow run … --ref main` re-runs only the fast validations and **silently skips every build/instrumented/iOS sentinel** — the run still concludes `success`, and `--check` will still show those sentinels `skipped`. (Bit us on android/35, 2026-07-02.)

> **Check for an in-progress run on the target SHA before dispatching.** A merge that just landed almost always has its own automatic `push`-triggered CI run still going. Dispatching a second (`workflow_dispatch`) run on the *same* SHA races it — GitHub Actions concurrency handling has been observed cancelling `validation_ios_ui` in one of the two runs, leaving neither with a clean `success` for that sentinel (see `bb-w73e` in TODO.md). Run `gh run list --workflow "Battery Butler CI" --limit 3 --json databaseId,status,headSha,event` first; if there's an `in_progress` run on your target SHA, wait for it to finish, *then* dispatch. Worked cleanly for android/46 and android/47 (2026-07-08/09) after being bitten by the collision once.

This is why android/30 and android/31 used a 4-PR pre-flight: flip ci-mode → release, dispatch CI, watch all jobs go green, then push the tag. `--check` makes that flow explicit so you don't have to remember it.

## Rollback (two steps, by design)

```bash
git checkout android/M           # move HEAD to the older commit you want to re-release
./scripts/release-android.sh --check   # prints the rollback command with the right SHAs
```

The rollback command requires both SHAs (target + previous-latest). You can only produce them by running `--check` on the rollback target — this forces deliberate HEAD movement.

## Optional: verify the release after push

Strictly optional. The server-side `verify-ci` gate (release-android.yml) already enforces the same sentinel check on the tagged commit before building, so a failed release is loud. But if you want explicit confirmation the upload succeeded:

```bash
# 1. Watch the workflow finish
gh run list --workflow=release-android.yml --limit 1
gh run watch <run-id>

# 2. Check Play Console internal track
# Open: https://play.google.com/console/u/0/developers/<dev-id>/app/<app-id>/tracks/internal-testing
# The newly uploaded versionCode should appear within a few minutes of the workflow completing.
```

Do not block on this step unless something looked wrong during the run. The workflow itself is the source of truth.

## What you should NOT do

- **Don't retype flag combinations from memory.** `--check` prints the right command with the right SHAs filled in. Copy-paste prevents wrong-SHA accidents.
- **Don't `git tag` directly.** The script enforces gates that `git tag` would bypass.
- **Don't deploy to production.** This script only deploys to the Play Store internal track. Promotion is manual in Play Console.
- **Don't reach for `--confirm-skipped-jobs` reflexively.** If `--check` flags skipped sentinels, the right move is almost always to re-run CI in `release` mode on the commit. Only override when you have manually verified the build (e.g., ran the relevant CI jobs locally) AND there's a real reason CI couldn't pass cleanly.
- **Don't reach for `--allow-tagged-commit` to re-tag an *older* commit.** That's the rollback case; `--confirm-rollback-from` is the right flag there. `--allow-tagged-commit` is for a different, legitimate case: releasing the *current* HEAD again, unchanged, because a build-time-only input changed (a GitHub Actions variable/secret, not code) and you need a fresh build that bakes in the new value — same tree, new tag, `--check` will show the commit already has a tag. Either way, never add this flag on your own initiative (see `.agent/AGENTS.md` § Critical Rules) — confirm the specific flag with the user first, since it bypasses a safety prompt on a production release.

## See also

- `scripts/release-android.sh` — the truth of which flags do what (run with `--help`)
- `.github/workflows/release-android.yml` — the CI that the tag triggers, including the server-side `verify-ci` sentinel gate (bb-nmqn)
- `.agent/ci.md` § Pre-Release CI Gate — the path-filter + dev-mode false-green pattern this gate exists to prevent
