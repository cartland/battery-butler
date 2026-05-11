---
description: List open CI-failure issues on main so they can be prioritized before new work.
allowed-tools: Bash(*)
user-invocable: true
---

# Check CI Issues

Lists open issues labeled `ci-failure`. These are filed automatically by `.github/workflows/ci-post-merge-issue.yml` when CI fails on a push to `main`. While any `ci-failure` issue is also tagged `blocking`, PR auto-merges are paused (enforced by the `validation_no_blocking_issues` job in `ci.yml`).

## When to Use

- **At session start**, before picking up new feature work. If main is broken, fixing that is priority zero.
- **When a PR's auto-merge appears stuck** — the `validation_no_blocking_issues` job will fail until the blocking issue is closed.
- **After noticing red CI on main** — to find the canonical tracking issue and avoid filing a duplicate.

## Steps

### 1. List open ci-failure issues

```bash
gh issue list --label ci-failure --state open
```

### 2. If any are open, triage

- Read the issue body to find the failing run URL and commit SHA.
- Look at the run logs:
  ```bash
  gh run view <run-id> --log-failed
  ```
- Decide: flake (re-run), real regression (fix on a branch), or environmental (e.g., runner issue → comment and wait).

### 3. Fix forward

CI failures on `main` are fixed in a new PR off `origin/main`. Once that PR merges and the next push-to-main CI is green, the issue auto-closes via the same workflow that opened it.

### 4. Re-enable auto-merge on parked PRs (if any)

PRs that hit `validation_no_blocking_issues` before the fix landed need a fresh run after the issue closes — easiest path is to re-trigger CI on each (push an empty commit, or use the GraphQL `enablePullRequestAutoMerge` after the next CI run goes green).

## Notes

- The label `ci-failure` is created on demand by `scripts/file-ci-failure-issue.sh`. If you don't see it in the labels list yet, that means no failures have been filed.
- The dedup contract is "one issue per failing job name" — repeated failures of the same job add comments to the existing issue, not duplicate issues.
- Run with `--state all --label ci-failure` to also see closed (historic) failures, useful for spotting flakes.
