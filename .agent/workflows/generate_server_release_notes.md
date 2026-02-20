# Generate Server Release Notes

Analyze commits between server release tags and produce structured release notes in `SERVER_RELEASE_NOTES.md`.

## Overview

This workflow examines git history between server release tags (`server/vX.Y.Z`), filters for server-relevant changes, eliminates reverted or superseded work, and produces both summary and detailed release notes.

## Step 1: Identify Commit Range

### Find tags

```bash
git fetch origin --tags
git tag -l 'server/v*' --format='%(refname:short) %(objectname:short) %(creatordate:short)' | sort -t. -k1,1n -k2,2n -k3,3n
```

### Determine range

- **HEAD has a tag** (e.g., `server/v1.2.0`): Range is `server/v1.1.0..server/v1.2.0` (previous tag to current tag).
- **HEAD has no tag**: Range is `server/vX.Y.Z..HEAD` where `server/vX.Y.Z` is the latest tag.
- **First release** (only one tag at HEAD): Range is from the repo root to that tag. Use `git log server/v1.0.0 --oneline`.

### Check for existing release notes

Read `SERVER_RELEASE_NOTES.md` if it exists. Check what commit ranges are already documented. The new range should start where the last documented range ends.

- If the proposed range overlaps with existing notes, ask the user whether to **replace** the overlapping section or **append** only the non-overlapping portion.
- If there is a gap between the last documented range and the proposed range, warn the user.

### Confirm with user

Present the proposed range and ask the user to confirm before proceeding:
```
Proposed commit range: server/v1.1.0 (fc2dede, 2026-01-31) .. HEAD (abc1234)
This covers N commits total.
Existing release notes cover up to: server/v1.1.0
```

## Step 2: Identify Server-Relevant Commits

### Server path patterns

A commit is server-relevant if it modifies files in any of these directories:

- `server/` — Server application code (Ktor, gRPC services, data layer)
- `server/app/` — Server application entry point, routes, services
- `server/data/` — Server data layer (Exposed ORM, repositories)
- `server/domain/` — Server domain models and interfaces
- `protos/` — Protocol buffer definitions (affects both client and server)
- `e2e-tests/` — End-to-end tests (gRPC client tests against server)
- `server/terraform/` — Infrastructure-as-code (Terraform) for server deployment
- `build.gradle.kts`, `gradle/` — Build config changes that affect server builds
- `scripts/e2e-tests.sh`, `scripts/*server*` — Server-related scripts

A commit is **not** server-relevant if it **only** touches:
- `compose-app/`, `viewmodel/`, `domain/` (without server/ changes) — Mobile-only
- `data/`, `data-local/`, `data-network/` — Client data layer only
- `presentation-core/`, `presentation-feature/`, `presentation-model/` — UI only
- `ios-app-swift-ui/`, `ios-app-compose-ui/`, `ios-swift-di/` — iOS only
- `ai/` — AI features (client-side only)
- `android-screenshot-tests/` — Mobile screenshot tests
- `.agent/`, `.beads/`, `.claude/`, `.github/` — Agent/CI/docs only
- `docs/`, `*.md` (root level) — Documentation only
- `scripts/` — Non-server scripts (unless server-related)

### List server commits

```bash
git log <start>..<end> --oneline --no-merges
```

For each commit, check which files were modified:

```bash
git diff-tree --no-commit-id --name-only -r <sha>
```

For each server-relevant commit, read the full diff to understand the change:

```bash
git show <sha> --stat
git show <sha>  # Read the actual diff for context
```

Create a short summary for each server-relevant commit (one line).

## Step 3: Filter to Final State

Not every server-relevant commit belongs in the release notes. Eliminate:

1. **Reverted commits** — If commit A is reverted by commit B, exclude both.
2. **Superseded commits** — If commit A adds a feature and commit C rewrites it entirely, only mention the final state.
3. **Fix-up chains** — If commits A, B, C are "add endpoint", "fix endpoint bug", "fix endpoint edge case", collapse into one entry describing the final endpoint.
4. **Refactors with no operational effect** — Internal code moves or renames that don't change behavior or API surface.

To verify, check the final diff for the entire range:

```bash
git diff <start>..<end> -- server/ protos/ e2e-tests/
```

If a commit's changes don't appear in the final diff, it was reverted or superseded.

## Step 4: Generate Release Notes

### File format

Write to `SERVER_RELEASE_NOTES.md` at the repo root. If the file doesn't exist, create it with the header. If it exists, prepend the new release section after the header.

Most recent release notes go at the top (reverse chronological).

### Structure per release

```markdown
## [server/vX.Y.Z] — YYYY-MM-DD

From tag `previous-tag` to tag `server/vX.Y.Z` (X server-relevant commits out of Y total)

### What's New

- Summary of API changes, new gRPC endpoints or fields
- Database schema changes or migrations
- Infrastructure/deployment changes
- Bug fixes and operational improvements

### Detailed Changes

| PR | Description |
|----|-------------|
| [#123](https://github.com/cartland/battery-butler/pull/123) | Add SyncService Subscribe endpoint for bidirectional sync |
| [#124](https://github.com/cartland/battery-butler/pull/124) | Fix connection pool exhaustion under load |
```

### Extracting PR numbers

Most commits from squash merges include the PR number in the message: `feat: Add thing (#123)`. Extract with:

```bash
git log <start>..<end> --oneline | grep -oP '#\d+'
```

If a commit has no PR number, link to the commit SHA instead.

## Step 5: Update PR Description

After committing the release notes, update the current PR description to list every PR mentioned in the release notes. This creates bidirectional links in GitHub — each referenced PR will show a backlink to this PR.

**Each PR link must be on its own line.** GitHub expands PR links inline, so putting multiple on one line creates a mess. A brief description on the same line is fine.

```bash
gh pr edit <number> --body "$(cat <<'EOF'
## Summary
Server release notes for [server/vX.Y.Z].

## Referenced PRs
- https://github.com/cartland/battery-butler/pull/123 — Description
- https://github.com/cartland/battery-butler/pull/124 — Description
- https://github.com/cartland/battery-butler/pull/125 — Description

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

List every PR from the "Detailed Changes" table, one per line as a full URL so GitHub creates the cross-reference.

## Tips

- When in doubt about whether a commit is server-relevant, include it — the user can trim.
- Proto changes (`protos/`) affect both client and server. Include them if they add new RPCs or fields that the server implements.
- Terraform/infrastructure changes are server-relevant — deployments, scaling, networking changes affect server operations.
- The "What's New" section should be useful for ops/devops teams reviewing what changed in a deployment.
- E2E test changes indicate new server behavior being validated — usually worth mentioning.
