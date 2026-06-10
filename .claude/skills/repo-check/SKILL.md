---
description: Quick health check of the repo — open PRs, CI status, recent releases, open issues, TODO.md tasks, and local state.
allowed-tools: Bash(*), Read, Glob, Grep
user-invocable: true
---

# Repo Check

Quick health check of the repo state. Intended for session start and as a one-shot status snapshot.

## Steps

### 1. Open PRs

```bash
gh pr list --state open --json number,title,mergeStateStatus,autoMergeRequest \
  --jq '.[] | {number, title, status: .mergeStateStatus, autoMerge: (.autoMergeRequest != null)}'
```

Then check **failing checks per PR** — `BLOCKED` is GitHub's catch-all status that covers everything from "waiting on a reviewer" to "CI red"; the status-check rollup is what tells you which PRs actually have failures:

```bash
gh pr list --state open --json number,title,statusCheckRollup --jq '
  .[] | {
    number,
    title,
    failed: [.statusCheckRollup[]?
      | select(.conclusion == "FAILURE"
            or .conclusion == "CANCELLED"
            or .conclusion == "TIMED_OUT")
      | .name]
  } | select(.failed | length > 0)'
```

If a PR has failing checks, drill in to identify the cause:

```bash
gh run view <RUN_ID> --log-failed 2>&1 \
  | grep -E "FAILED|Error:|Exception|##\[error\]" | head -25
```

Common bb failure patterns:
- **`String(ByteArray)` on iOS Kotlin/Native** — works on JVM, fails on K/N. Use `byteArray.decodeToString()`.
- **`Error parsing stability configuration file on line 0`** — `compose_compiler_config.conf` cannot contain `#` comments. Plain patterns only.
- **`URI is not hierarchical`** in convention tests — JAR URI; `UseCaseConventionTest` needs `url.protocol != "file"` guard.
- **Gradle Metaspace OOM in KSP** (`:experimental:compose-app:kspDebugKotlinAndroid` etc.) — bump heap / metaspace in `gradle.properties`.
- **Transient runner failure** (HTTP 502 fetching gradle distribution, simulator boot timeout) — rerun with `gh run rerun <RUN_ID> --failed`.

Report: count, any conflicts (DIRTY), any without auto-merge, any with failing checks (name them).

### 2. CI Health

```bash
# Latest push-to-main CI run
gh run list --branch main --workflow "Battery Butler CI" --limit 1 \
  --json conclusion,headSha,createdAt \
  --jq '.[0] | {conclusion, sha: .headSha[:7], date: .createdAt[:10]}'

# Any open ci-failure issues (filed by ci-post-merge-issue.yml)
gh issue list --label ci-failure --state open --json number,title --jq '.[]'
```

If any `ci-failure` issue is open, pull the body to find the failing run and commit:

```bash
for n in $(gh issue list --label ci-failure --state open --json number --jq '.[].number'); do
  echo "Issue #$n:"
  gh issue view "$n" --json body --jq '.body' \
    | grep -E '^- (Run|Commit|Workflow):'
  echo
done
```

Why this matters: while any `ci-failure` + `blocking` issue is open, the `validation_no_blocking_issues` job in `ci.yml` fails every new PR — auto-merges are paused. Fixing `main` is priority zero. See `.agent/ci.md` → "Post-Merge Auto-Issue Safety Net".

Report: last push-to-main result, any open `ci-failure` issues with their commit SHA.

### 3. Recent Releases

```bash
# Latest Android release tag
git tag -l 'android/*' --sort=-version:refname | head -3

# Latest server release tag (if any)
git tag -l 'server/*' --sort=-version:refname | head -3
```

### 4. Open Issues

```bash
gh issue list --state open --json number,title,labels \
  --jq '.[] | {number, title, labels: [.labels[].name]}'
```

Flag any with the `ci-failure` or `blocking` labels — those gate the queue.

### 5. TODO Tasks

Read `TODO.md` (repo root) and report the count of open tasks per priority heading
(`## P2`, `## P3`, `## P4`). Surface anything marked ⚠️ / USER ACTION REQUIRED.

### 6. Local State

```bash
git status --short
git branch --show-current
git log --oneline @{u}..HEAD 2>/dev/null | head -5   # unpushed commits
```

Report: current branch, clean/dirty, any unpushed commits.

## Output Format

Compact status report:

```
PRs:        N open (N conflicts, N auto-merge, N failing checks)
            [name each PR with failing checks: "#NNN fail: <job-name>"]
CI (main):  passing/failing (last run: YYYY-MM-DD)
Issues:     N open [if any ci-failure, name the commit SHA per issue]
TODO:       N open (P2:N P3:N P4:N) [flag any USER ACTION REQUIRED]
Releases:   android/N, server/N
Local:      <branch>, clean | <branch>, dirty (list changed files)
```

Expand anything that needs attention: conflicts, failing checks, blocking issues, uncommitted changes. A dirty working tree always needs attention.

For open `ci-failure` issues, always state the commit SHA from the issue body — that tells the reader whether a fix is already in flight in another PR.
