---
description: Analyze commits between mobile release tags and generate MOBILE_RELEASE_NOTES.md with public and detailed sections.
allowed-tools: Bash(*), Read, Write, Edit, Glob, Grep
---

# Generate Mobile Release Notes

Analyze commits between mobile release tags and produce structured release notes.

## Steps

### Phase 1: Identify Commit Range

1. Fetch tags and list Android release tags:
   ```bash
   git fetch origin --tags
   git tag -l 'android/*' --format='%(refname:short) %(objectname:short) %(creatordate:short)' | sort -t/ -k2 -n
   ```

2. Determine the commit range:
   - If HEAD has an `android/*` tag: range is **previous tag → current tag**
   - If HEAD has no tag: range is **latest tag → HEAD**
   - If only one tag exists at HEAD: range is **repo root → that tag**

3. Check `MOBILE_RELEASE_NOTES.md` for existing coverage:
   - Read the file if it exists
   - Find the most recent range documented
   - Ensure the new range is sequential (starts where the last ends)
   - If there's overlap or a gap, flag it

4. **Ask the user to confirm** the commit range before proceeding. Present:
   - The start and end points (tags and/or HEAD)
   - Total commit count
   - What's already documented
   - Whether this creates new notes or extends existing ones

### Phase 2: Find Mobile-Relevant Commits

Read `.agent/workflows/generate_mobile_release_notes.md` for the full list of mobile path patterns.

For the confirmed range:

1. List all commits:
   ```bash
   git log <start>..<end> --oneline --no-merges
   ```

2. For each commit, check which files it modifies:
   ```bash
   git diff-tree --no-commit-id --name-only -r <sha>
   ```

3. A commit is mobile-relevant if it touches mobile paths (compose-app, domain, viewmodel, data, protos, ios-app, etc.) and is **not** exclusively server, CI, docs, or agent files.

4. For each mobile-relevant commit, read the full change to understand it:
   ```bash
   git show <sha>
   ```

5. Write a one-line summary for each mobile-relevant commit.

### Phase 3: Filter to Final State

Eliminate noise from the release notes:

1. Check for reverted commits (A added then B reverts A — exclude both)
2. Check for superseded commits (A adds feature, C rewrites it — only mention final)
3. Collapse fix-up chains into single entries

Verify by comparing the final diff:
```bash
git diff <start>..<end> -- compose-app/ domain/ viewmodel/ data/ data-local/ data-network/ presentation-core/ presentation-feature/ presentation-model/ usecase/ ai/ ios-app-swift-ui/ ios-app-compose-ui/ ios-swift-di/ protos/ compose-resources/ fixtures/ android-screenshot-tests/
```

If a commit's changes aren't visible in the final diff, it was reverted or superseded — exclude it.

### Phase 4: Write Release Notes

Write to `MOBILE_RELEASE_NOTES.md` at the repo root. If the file doesn't exist, create it with this header:

```markdown
# Mobile Release Notes

Release notes for Battery Butler mobile app (Android & iOS). Most recent release first.

Each section covers one release tag range. "What's New" is user-facing language suitable for app store descriptions. "Detailed Changes" links every included PR.

---
```

Then add a section for the new release (prepended after the header, before older entries):

```markdown
## [android/N] — YYYY-MM-DD

Range: `previous-tag..android/N` (X mobile-relevant commits out of Y total)

### What's New

- User-friendly descriptions grouped by theme
- No PR numbers, no jargon
- Suitable for Play Store / App Store release notes

### Detailed Changes

| PR | Description |
|----|-------------|
| [#N](https://github.com/cartland/battery-butler/pull/N) | Description |
```

Extract PR numbers from commit messages (squash merge format: `feat: Thing (#123)`). If no PR number, link to the commit SHA.
