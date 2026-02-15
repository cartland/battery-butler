# Generate Mobile Release Notes

Analyze commits between mobile release tags and produce structured release notes in `MOBILE_RELEASE_NOTES.md`.

## Overview

This workflow examines git history between Android release tags (`android/N`), filters for mobile-relevant changes, eliminates reverted or superseded work, and produces both public-facing and detailed release notes.

## Step 1: Identify Commit Range

### Find tags

```bash
git fetch origin --tags
git tag -l 'android/*' --format='%(refname:short) %(objectname:short) %(creatordate:short)' | sort -t/ -k2 -n
```

### Determine range

- **HEAD has a tag** (e.g., `android/3`): Range is `android/2..android/3` (previous tag to current tag).
- **HEAD has no tag**: Range is `android/N..HEAD` where `android/N` is the latest tag.
- **First release** (only one tag at HEAD): Range is from the repo root to that tag. Use `git log android/1 --oneline`.

### Check for existing release notes

Read `MOBILE_RELEASE_NOTES.md` if it exists. Check what commit ranges are already documented. The new range should start where the last documented range ends.

- If the proposed range overlaps with existing notes, ask the user whether to **replace** the overlapping section or **append** only the non-overlapping portion.
- If there is a gap between the last documented range and the proposed range, warn the user.

### Confirm with user

Present the proposed range and ask the user to confirm before proceeding:
```
Proposed commit range: android/2 (fc2dede, 2026-01-31) .. HEAD (abc1234)
This covers N commits total.
Existing release notes cover up to: android/2
```

## Step 2: Identify Mobile-Relevant Commits

### Mobile path patterns

A commit is mobile-relevant if it modifies files in any of these directories:

- `compose-app/` — Compose Multiplatform UI (Android + Desktop + iOS Compose)
- `compose-resources/` — Shared Compose resources (strings, images)
- `presentation-core/` — Shared presentation layer
- `presentation-feature/` — Feature-level presentation
- `presentation-model/` — Presentation models
- `viewmodel/` — ViewModels
- `domain/` — Domain layer (business logic)
- `usecase/` — Use cases
- `data/` — Data layer (repositories)
- `data-local/` — Local storage (Room)
- `data-network/` — Network layer (gRPC client)
- `ai/` — AI features
- `ios-app-swift-ui/` — iOS native UI
- `ios-app-compose-ui/` — iOS Compose UI
- `ios-swift-di/` — iOS dependency injection
- `protos/` — Protocol buffer definitions (affects client and server)
- `android-screenshot-tests/` — Screenshot tests (indicates UI changes)
- `build.gradle.kts`, `gradle/` — Build config changes that affect mobile builds
- `fixtures/` — Test fixtures used by mobile tests

A commit is **not** mobile-relevant if it **only** touches:
- `server/` — Server-only changes
- `e2e-tests/` — Server E2E tests
- `.agent/`, `.beads/`, `.claude/`, `.github/` — Agent/CI/docs only
- `docs/`, `*.md` (root level) — Documentation only
- `scripts/` — Build/deploy scripts (unless it's a mobile build script)

### List mobile commits

```bash
git log <start>..<end> --oneline --no-merges
```

For each commit, check which files were modified:

```bash
git diff-tree --no-commit-id --name-only -r <sha>
```

For each mobile-relevant commit, read the full diff to understand the change:

```bash
git show <sha> --stat
git show <sha>  # Read the actual diff for context
```

Create a short summary for each mobile-relevant commit (one line).

## Step 3: Filter to Final State

Not every mobile-relevant commit belongs in the release notes. Eliminate:

1. **Reverted commits** — If commit A is reverted by commit B, exclude both.
2. **Superseded commits** — If commit A adds a feature and commit C rewrites it entirely, only mention the final state.
3. **Fix-up chains** — If commits A, B, C are "add feature", "fix feature typo", "fix feature edge case", collapse into one entry describing the final feature.
4. **Refactors with no user-visible effect** — Internal code moves or renames that don't change behavior.

To verify, check the final diff for the entire range:

```bash
git diff <start>..<end> -- <mobile-paths>
```

If a commit's changes don't appear in the final diff, it was reverted or superseded.

## Step 4: Generate Release Notes

### File format

Write to `MOBILE_RELEASE_NOTES.md` at the repo root. If the file doesn't exist, create it with the header. If it exists, prepend the new release section after the header.

Most recent release notes go at the top (reverse chronological).

### Structure per release

```markdown
## [android/N] — YYYY-MM-DD

Range: `previous-tag..android/N` (X mobile-relevant commits out of Y total)

### What's New

- Brief, user-friendly description of visible changes
- Written for end users (no PR numbers, no technical jargon)
- Group by theme: new features, improvements, bug fixes

### Detailed Changes

| PR | Description |
|----|-------------|
| [#123](https://github.com/cartland/battery-butler/pull/123) | Add AI chat screen for conversational device management |
| [#124](https://github.com/cartland/battery-butler/pull/124) | Fix crash when navigating back from settings |
```

### Extracting PR numbers

Most commits from squash merges include the PR number in the message: `feat: Add thing (#123)`. Extract with:

```bash
git log <start>..<end> --oneline | grep -oP '#\d+'
```

If a commit has no PR number, link to the commit SHA instead.

## Tips

- When in doubt about whether a commit is mobile-relevant, include it — the user can trim.
- Proto changes (`protos/`) affect both client and server. Include them if they add new RPCs or fields that the mobile client uses.
- Gradle dependency bumps (e.g., dependabot) are mobile-relevant if they update libraries used by mobile modules.
- The "What's New" section should be concise enough to paste into a Play Store release description.
