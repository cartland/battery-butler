---
description: Review GitHub Actions annotations from recent CI runs. Surfaces new warnings and suggests fixes or suppressions.
allowed-tools: Bash(*), Read, Write, Edit, Glob, Grep
user-invocable: true
---

# Review CI Annotations

Audit GitHub Actions annotations from recent CI runs against an allowlist, so deprecation warnings and tooling issues don't pile up unnoticed between failures.

## Steps

### 1. Fetch annotations from the latest main run

```bash
LATEST_SHA=$(gh api repos/cartland/battery-butler/branches/main --jq '.commit.sha')

# Check runs that have any annotations
gh api "repos/cartland/battery-butler/commits/$LATEST_SHA/check-runs" --paginate \
  --jq '.check_runs[] | select(.output.annotations_count > 0) | {id, name, count: .output.annotations_count}'

# For each interesting check run, fetch its annotations
gh api "repos/cartland/battery-butler/check-runs/<CHECK_RUN_ID>/annotations" \
  --jq '.[] | {level: .annotation_level, message: .message, path, line: .start_line}'
```

### 2. Filter against the ignore list

`.github/annotation-ignores.txt` is a plain-text allowlist (one pattern per line, `#` for comments). Match with `grep -F`. Anything not matching is "new".

### 3. For each new annotation, recommend one of

- **Fix** — actionable now (e.g., bump a deprecated dep, migrate a deprecated API). When a deferred fix already has a corresponding PR or `TODO.md` task, link it instead of re-opening the discussion.
- **Ignore** — add a one-line pattern to `.github/annotation-ignores.txt` with a `#` comment explaining the reason (incident link, upstream tracking issue, deliberate trade-off).
- **Defer** — note it but don't act (e.g., waiting for upstream fix or a planned migration).

### 4. Report summary

Group annotations as:
- **New** — needs human decision
- **Ignored** — matched a line in the ignore file
- **Resolved-since-last-review** — present in the ignore file but no longer appearing in CI (candidate to remove from the ignore list)

## Notes

- Run periodically, not on every CI run — once a week or per release branch is typical.
- The ignore list uses plain string matching (no regex). Pattern strings should be the **minimum unique substring** of the annotation message — short enough to match across CI environment differences, long enough not to over-match.
- Focus on `warning` annotations. `failure`-level annotations should already be visible through the normal CI failure track.
- Repo-level deprecation context for this project lives in `.agent/project.md` and `.agent/ci.md` — check those before adding a new ignore.
