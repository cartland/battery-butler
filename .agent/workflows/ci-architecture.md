# CI Architecture

This document explains the CI workflows and their design decisions.

## Workflow Overview

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `ci.yml` | PR, main push | Build validation (Android, iOS, Desktop, Server) |
| `auto-generate.yml` | main push | Generate diagrams, analysis, screenshots post-merge |
| `ci-trigger-auto-prs.yml` | workflow dispatch | Dispatch CI on auto-generated PRs |
| `server-build.yml` | main push (server paths) | Build + deploy to dev |
| `server-deploy-staging.yml` | manual | Deploy to staging |
| `server-deploy-prod.yml` | manual + approval | Deploy to production |
| `server-destroy.yml` | manual | Tear down staging/dev infrastructure |
| `server-rollback.yml` | manual | Emergency rollback |
| `release-android.yml` | Tag push (`android/N`) | Release to Play Store |

## Core Design Rule

**Workflows NEVER push commits to PR branches.** This prevents:
- Infinite workflow loops (commit triggers CI triggers commit...)
- Permanently blocked PRs
- Unexpected changes to developer branches

## How Generated Content Works

Generated content (architecture diagrams, code analysis, screenshots) is updated **post-merge** on `main` via follow-up PRs:

```
Developer merges PR to main
    |
    v
auto-generate.yml runs on main
    |
    v
Generates diagrams + analysis + screenshots
    |
    v
Creates follow-up PRs:
  - auto/update-generated-content
  - auto/update-android-screenshots
  - auto/update-ios-screenshots
    |
    v
ci-trigger-auto-prs.yml dispatches CI on auto PRs
    |
    v
Auto PRs merged (safe, loop-proof)
```

**Loop prevention:**
- `auto-generate.yml` uses `GITHUB_TOKEN` (not `BOT_PAT`) -- commits from GITHUB_TOKEN don't trigger workflows
- `BOT_PAT` is only used for dispatching CI workflows on auto PRs (never for pushing commits)

## CI Path Filtering

CI uses `dorny/paths-filter` to skip expensive builds for non-code changes:
- **Beads-only changes** (`.beads/**`): Skip all builds, only run `ci` gate
- **Docs-only changes** (`*.md`, `.agent/**`): Skip all builds
- **Non-code server files** (`server/*.json`, `server/*.md`): Skip all builds
- **Code changes**: Run full build matrix

## PR Drift Check

The `check_generated_content` job in `ci.yml` is a **non-blocking** drift warning. It checks if diagrams or analysis would change and reports (informational only, does not block merge). Actual updates happen post-merge.
