---
description: List open CI-failure issues on main so they can be prioritized before new work.
allowed-tools: Bash(*)
user-invocable: true
---

# Check CI Issues

Lists open issues labeled `ci-failure`. These are filed automatically by `.github/workflows/ci-post-merge-issue.yml` when CI fails on a push to `main`. While any is also tagged `blocking`, the `report_blocking_issues` job in `ci.yml` puts a "main is red" warning on every PR. It never blocks a merge.

## When to Use

- **At session start**, before picking up new feature work. If main is broken, fixing that is priority zero.
- **When a PR shows a "main is red" warning** — to find the tracking issue and fix forward before merging anything else.
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

### 4. After the fix lands

Nothing is parked by a red `main` any more. If a PR merged while the warning was up, check that `main` is green afterwards; a PR that only looked green because the break hid behind it files its own issue on the next push.

## Notes

- The label `ci-failure` is created on demand by `scripts/file-ci-failure-issue.sh`. If you don't see it in the labels list yet, that means no failures have been filed.
- The dedup contract is "one issue per failing job name" — repeated failures of the same job add comments to the existing issue, not duplicate issues.
- Run with `--state all --label ci-failure` to also see closed (historic) failures, useful for spotting flakes.
