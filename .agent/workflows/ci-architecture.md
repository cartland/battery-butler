# CI Architecture and Auto-Update Strategy

This document explains the CI workflows, their purposes, and the design decisions behind them.

## Workflow Overview

| Workflow | Trigger | Purpose | Auto-commits? |
|----------|---------|---------|---------------|
| `ci.yml` | PR, main push | Build validation | No |
| `update-diagrams.yml` | main push | Generate architecture diagrams | Creates follow-up PR |
| `update-screenshots.yml` | main push | Generate Android screenshots | Creates follow-up PR |
| `deploy-server.yml` | main push (server paths) | Deploy to production | No (deploys) |
| `release-android.yml` | Tag push (`android/N`) | Release to Play Store | No (releases) |

## Current Limitations

### Problem: Updates happen AFTER merge, not ON the PR

The `update-diagrams.yml` and `update-screenshots.yml` workflows:
1. Only run when code is pushed to `main`
2. Generate updated diagrams/screenshots
3. Create a NEW PR with the updates

**Why this is suboptimal:**
- Contributors see screenshot test failures on their PR
- They must wait for their PR to merge, then merge the auto-generated follow-up PR
- Two PRs for one logical change
- Follow-up PRs can pile up if not merged

### Why it's implemented this way

**GitHub constraints:**
1. **Branch protection** prevents direct push to `main`
2. **GITHUB_TOKEN** cannot push to PR branches from forks (security)
3. **GITHUB_TOKEN** cannot trigger new workflow runs (prevents infinite loops)

**The `peter-evans/create-pull-request` action** is a workaround that:
- Creates a PR from the workflow
- Uses GITHUB_TOKEN which can create PRs but not push to protected branches

## Desired Behavior

```
Developer creates PR
    ↓
CI runs and generates updated diagrams/screenshots
    ↓
Updates are committed back to the PR branch
    ↓
PR now includes the generated files
    ↓
Merge PR (single PR, not two)
```

## Solution Options

### Option 1: Bot pushes to PR branch (Recommended)

**How it works:**
1. Use a Personal Access Token (PAT) instead of GITHUB_TOKEN
2. PAT stored as a secret (e.g., `BOT_PAT`)
3. Workflow runs on `pull_request` trigger
4. Generates updates and commits to the PR branch

**Pros:**
- Single PR workflow
- Updates visible before merge
- Developer doesn't need to do anything

**Cons:**
- Requires PAT with `repo` scope
- Must handle merge conflicts if PR updated while CI runs
- Security: PAT has broader permissions than GITHUB_TOKEN

**Implementation:**
```yaml
on:
  pull_request:
    branches: [main]

jobs:
  update-diagrams:
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v4
        with:
          ref: ${{ github.head_ref }}
          token: ${{ secrets.BOT_PAT }}  # PAT with repo access

      - name: Generate diagrams
        run: ./gradlew generateMermaidGraph

      - name: Commit and push
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add -A
          git diff --staged --quiet || git commit -m "Auto-update diagrams"
          git push
```

### Option 2: Required status check blocks merge until manual update

**How it works:**
1. CI detects if diagrams are outdated
2. Fails the check with instructions
3. Developer must run generation locally and push

**Pros:**
- No special tokens needed
- Developer maintains control

**Cons:**
- Manual step required
- Worse developer experience

### Option 3: Keep current approach (post-merge PRs)

**How it works:**
- Keep the current `update-diagrams.yml` and `update-screenshots.yml`
- Accept that updates come as follow-up PRs

**Pros:**
- Already implemented
- No new secrets needed

**Cons:**
- Two PRs for one change
- Updates visible only after merge

## Recommended Implementation

### Phase 1: Add auto-fix to PR branches (Option 1)

1. Create a GitHub PAT with `repo` scope
2. Add as secret `BOT_PAT`
3. Modify `update-diagrams.yml`:
   - Trigger on `pull_request` instead of `push` to main
   - Checkout with PAT
   - Commit changes back to PR branch
4. Modify `update-screenshots.yml` similarly

### Phase 2: Add Spotless auto-fix

Same pattern as diagrams:
1. Run `./gradlew spotlessApply`
2. Commit formatting fixes to PR branch
3. Developer sees formatted code before merge

### Security Considerations

- **PAT scope**: Use fine-grained PAT with minimum necessary permissions
- **Token rotation**: Rotate PAT periodically
- **Audit**: Log when bot makes commits
- **Fork PRs**: Consider whether to allow on fork PRs (security risk)

## Configuration Checklist

The workflow files have been updated to support auto-commits to PR branches.
To enable this feature, complete the following:

- [ ] Create PAT with fine-grained permissions (see workflow files for details)
- [ ] Add PAT as repository secret `BOT_PAT`
- [ ] Test on a non-fork PR
- [x] Update `update-diagrams.yml` to trigger on PRs ✓
- [x] Update `update-screenshots.yml` to trigger on PRs ✓

### Quick Setup

1. Go to https://github.com/settings/tokens?type=beta
2. Create token with:
   - Repository: This repo only
   - Permissions: Contents (read/write), Pull requests (read/write)
3. Add as secret: Settings → Secrets → Actions → `BOT_PAT`

See the workflow files for detailed setup instructions.

## Troubleshooting

### "refusing to allow a GitHub App to create or update workflow"
- GITHUB_TOKEN cannot modify workflow files
- Use PAT or don't modify `.github/` files in auto-commits

### Infinite workflow loops
- If commit triggers a new workflow run, it can loop
- Use `[skip ci]` in commit message, or
- Check if the committer is the bot and skip

### Merge conflicts
- If PR is updated while CI runs, push may fail
- Options: rebase and retry, or fail gracefully with instructions
