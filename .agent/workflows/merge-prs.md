---
description: Manage and merge a queue of open PRs while keeping main healthy.
allowed-tools: Bash(*), Read, Glob, Grep
---

# Merge PRs

Manage and merge a queue of open PRs while keeping main healthy.

## Steps

### 1. Analyze Open PRs

List all open PRs with their status:

```bash
gh pr list --state open --json number,title,mergeable,statusCheckRollup --limit 50 | jq '[.[] | {
  number,
  title,
  mergeable,
  checks: (if (.statusCheckRollup | length) == 0 then "no_checks"
           elif ([.statusCheckRollup[]? | .conclusion] | all(. == "SUCCESS")) then "passing"
           elif ([.statusCheckRollup[]? | .conclusion] | any(. == "FAILURE")) then "failing"
           else "pending" end)
}] | sort_by(.number)'
```

### 2. Categorize PRs

Group PRs into categories:

- **Ready to Merge**: `mergeable: MERGEABLE` + `checks: passing`
- **Needs Rebase**: `mergeable: CONFLICTING` or checks failing due to stale base
- **Needs Work**: Failing CI due to actual issues in the PR
- **Should Close**: Superseded, downgrade, or no longer needed

### 3. Check Main Health First

Before merging, verify main is healthy:

```bash
git fetch origin main
gh run list --branch main --limit 3 --json conclusion,status,name
```

If main has failures, prioritize PRs that fix main.

### 4. Merge Order Strategy

Merge in this order to minimize conflicts:

1. **Critical fixes** — PRs that fix broken main
2. **Documentation** — Low risk, isolated changes
3. **Tests** — Additive only, rarely cause conflicts
4. **Infrastructure/CI** — Build configs, linting rules
5. **Bug fixes** — Security, memory leaks, crashes
6. **Refactoring** — May cause conflicts with feature PRs
7. **Features** — Most likely to have conflicts

### 5. Merge Passing PRs

```bash
gh pr merge <number> --squash --delete-branch
```

### 6. Rebase Conflicting PRs

```bash
git checkout <branch-name>
git fetch origin main
git rebase origin/main
# Resolve any conflicts, then:
git add <resolved-files>
GIT_EDITOR=true git rebase --continue
git push --force-with-lease origin <branch-name>
```

### 7. Close PRs with Rationale

```bash
gh pr close <number> --comment "Closing: <rationale>"
```

### 8. Monitor CI

After merging, verify main CI passes:

```bash
gh run list --branch main --limit 1 --json conclusion,status,name
```

## Tips

- Always merge critical fixes before other PRs
- If many PRs fail the same check, fix the root cause in main first
- PRs touching the same files should be merged carefully — rebase between each
- Use `gh pr view <number> --json files` to see which files a PR touches
