# Update Project Documentation

Update agent files, READMEs, changelog, and module docs to reflect recent code changes.

## Overview

After making code changes (features, fixes, CI/CD, infrastructure), project documentation must be updated to stay in sync. This workflow identifies what changed, determines which docs are affected, and makes targeted updates.

## Step 1: Identify What Changed

Determine the scope of recent changes using whichever approach fits:

**On a feature branch with unmerged work:**
```bash
git diff origin/main --stat
git diff origin/main --name-only
```

**Changes already merged (branch is up-to-date with main):**
```bash
git log origin/main --oneline -20
git show <merge-commit> --stat
git diff <before-sha>..<after-sha> --stat
```

**No specific diff target:** Review the conversation context for what changed this session, then read affected files directly.

Categorize into: code changes, CI/CD changes, infrastructure changes, build changes.

Read commit messages and diffs to understand intent — *why* things changed, not just *what*.

## Step 2: Determine Affected Docs

| File | Update when... |
|------|---------------|
| `.agent/project.md` | Architecture, build, deployment, testing, or server URL patterns change |
| `.agent/AGENTS.md` | New workflow rules, config patterns, or mistakes-to-avoid |
| `README.md` | User-facing setup, build, or config instructions change |
| `server/README.md` | Server deployment, secrets, or infrastructure changes |
| `CHANGELOG.md` | Any meaningful code change |
| Module `README.md` files | New models, interfaces, or components in a module |

Skip files not affected by the changes.

## Step 3: Update Agent Files

### `.agent/project.md`

Update the appropriate section:
- **Architecture** — New modules, dependencies, patterns
- **Build System** — New build flags, tasks, quirks
- **Server URL Management** — URL flow changes (terraform → secrets → BuildConfig → code)
- **Server Deployment** — Workflow changes, new secrets, observability
- **Testing** — New test categories or patterns
- **CI** — Path filtering, new workflows, auto-generation

Rules:
- Be factual, not speculative
- Include code references (file paths, class names)
- Keep entries concise

### `.agent/AGENTS.md`

Only update for new rules agents must follow:
- Configuration patterns (e.g., "never hardcode X")
- Validation steps
- CI synchronization requirements
- Safety constraints

## Step 4: Update READMEs

### `README.md`
Update for: build instruction changes, new prerequisites, config option changes, new features.

### `server/README.md`
Update for: new GitHub secrets (add to prerequisites table), workflow behavior changes, infrastructure patterns.

### Module READMEs
Update for: new public types, expanded responsibilities, new architecture table entries.

## Step 5: Update CHANGELOG

Add entry under current date. Format:

```markdown
## YYYY-MM-DD

### Features
- **Name**: What and why

### Fixes
- **Name**: What was broken and how fixed

### CI/CD Improvements
- **Name**: Description

### Documentation
- **Name**: What was updated and why
```

Rules: most recent first, explain *why* not just *what*, include PR links when available.

## Step 6: Review and Commit

```bash
# Format if needed
./scripts/spotless-apply.sh

# Review
git diff --stat
git diff

# Commit
git add .agent/ README.md server/README.md CHANGELOG.md
git commit -m "docs: Update project documentation for [description]"
```

## Tips

- Read existing content before editing to avoid duplication
- Don't add speculative information
- Keep agent files concise — read at session start
- Don't remove docs unless demonstrably wrong
- Place new sections logically near related content
