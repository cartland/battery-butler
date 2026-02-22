---
description: Analyze commits between server release tags and generate SERVER_RELEASE_NOTES.md with public and detailed sections.
allowed-tools: Bash(*), Read, Write, Edit, Glob, Grep
---

# Generate Server Release Notes

Analyze commits between server release tags and produce structured release notes.

## Steps

### Phase 1: Identify Commit Range

1. Fetch tags and list server release tags:
   ```bash
   git fetch origin --tags
   git tag -l 'server/v*' --format='%(refname:short) %(objectname:short) %(creatordate:short)' | sort -t. -k1,1n -k2,2n -k3,3n
   ```

2. Determine the commit range:
   - If HEAD has a `server/v*` tag: range is **previous tag -> current tag**
   - If HEAD has no tag: range is **latest tag -> HEAD**
   - If only one tag exists at HEAD: range is **repo root -> that tag**

3. Check `SERVER_RELEASE_NOTES.md` for existing coverage:
   - Read the file if it exists
   - Find the most recent range documented
   - Ensure the new range is sequential (starts where the last ends)
   - If there's overlap or a gap, flag it

4. **Ask the user to confirm** the commit range before proceeding. Present:
   - The start and end points (tags and/or HEAD)
   - Total commit count
   - What's already documented
   - Whether this creates new notes or extends existing ones

### Phase 2: Find Server-Relevant Commits

Read `.agent/workflows/generate_server_release_notes.md` for the full list of server path patterns.

For the confirmed range:

1. List all commits:
   ```bash
   git log <start>..<end> --oneline --no-merges
   ```

2. For each commit, check which files it modifies:
   ```bash
   git diff-tree --no-commit-id --name-only -r <sha>
   ```

3. A commit is server-relevant if it touches server paths (server/, protos/, e2e-tests/, etc.) and is **not** exclusively mobile, CI, docs, or agent files.

4. For each server-relevant commit, read the full change to understand it:
   ```bash
   git show <sha>
   ```

5. Write a one-line summary for each server-relevant commit.

### Phase 3: Filter to Final State

Eliminate noise from the release notes:

1. Check for reverted commits (A added then B reverts A -- exclude both)
2. Check for superseded commits (A adds feature, C rewrites it -- only mention final)
3. Collapse fix-up chains into single entries

Verify by comparing the final diff:
```bash
git diff <start>..<end> -- server/ protos/ e2e-tests/
```

If a commit's changes aren't visible in the final diff, it was reverted or superseded -- exclude it.

### Phase 4: Write Release Notes

Write to `SERVER_RELEASE_NOTES.md` at the repo root. If the file doesn't exist, create it with this header:

```markdown
# Server Release Notes

Release notes for Battery Butler server (Ktor gRPC on AWS ECS Fargate). Most recent release first.

Each section covers one release tag range. "What's New" is a summary of user-visible and operational changes. "Detailed Changes" links every included PR.

---
```

Then add a section for the new release (prepended after the header, before older entries):

```markdown
## [server/vX.Y.Z] -- YYYY-MM-DD

From tag `previous-tag` to tag `server/vX.Y.Z` (X server-relevant commits out of Y total)

### What's New

- Summary of API changes, new endpoints, bug fixes
- Operational changes (infrastructure, deployment, monitoring)
- No PR numbers, concise descriptions

### Detailed Changes

| PR | Description |
|----|-------------|
| [#N](https://github.com/cartland/battery-butler/pull/N) | Description |
```

Extract PR numbers from commit messages (squash merge format: `feat: Thing (#123)`). If no PR number, link to the commit SHA.

### Phase 5: Update PR Description

After committing the release notes, update the current PR description to list every PR mentioned in the release notes. This creates bidirectional links in GitHub -- each referenced PR will show a backlink to this PR.

**Each PR link must be on its own line.** GitHub expands PR links inline, so putting multiple on one line creates a mess. A brief description on the same line is fine.

Use `gh pr edit` to update the body:

```bash
gh pr edit <number> --body "$(cat <<'EOF'
## Summary
Server release notes for [server/vX.Y.Z].

## Referenced PRs
- https://github.com/cartland/battery-butler/pull/123 -- Description
- https://github.com/cartland/battery-butler/pull/124 -- Description
- https://github.com/cartland/battery-butler/pull/125 -- Description

Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

List every PR from the "Detailed Changes" table, one per line as a full URL so GitHub creates the cross-reference.
