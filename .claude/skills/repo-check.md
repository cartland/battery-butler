---
description: Check repo status, origin sync, open PRs, and recent GitHub Actions runs.
allowed-tools: Bash(git *), Bash(gh *)
---

# Repo Check

Comprehensive repo status check covering local state, remote sync, PRs, and CI.

## Steps

1. **Local status** — show current branch, working tree status (staged, unstaged, untracked).

2. **Origin sync** — fetch origin, then compare:
   - Commits ahead of `origin/main` (`git log --oneline origin/main..HEAD`)
   - Commits behind `origin/main` (`git log --oneline HEAD..origin/main`)
   - Whether the branch has been pushed to origin
   - Recent commits on `origin/main` (`git log --oneline origin/main -10`) — shows what's new on main

3. **Pull requests** — use `gh` to check:
   - Open PRs from the current branch (`gh pr list --state open --head <branch>`)
   - Merged/closed PRs from the current branch (`gh pr list --state all --head <branch>`)
   - Any open PRs assigned to the user (`gh pr list --state open --author @me`)

4. **GitHub Actions** — show recent runs:
   - Last 10 workflow runs (`gh run list --limit 10`)
   - If there are any failures, show details for the most recent failed run

## Output Format

Present results in a clear summary with tables where appropriate. Flag anything that needs attention:
- Uncommitted changes
- Branch behind origin/main
- Failed CI runs
- Stale branches (merged PRs but branch not deleted)
