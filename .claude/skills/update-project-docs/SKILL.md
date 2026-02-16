---
description: Update agent files, READMEs, and changelog after code changes to keep documentation in sync.
allowed-tools: Bash(git *), Read, Write, Edit, Glob, Grep
---

# Update Project Documentation

Analyze recent code changes and update all project documentation to reflect them. This keeps agent instructions, READMEs, and the changelog in sync with the codebase.

## When to Use

- After implementing a feature, fix, or architectural change
- After modifying CI/CD workflows, deploy pipelines, or build configuration
- After adding new modules, models, or DI patterns
- Before ending a session where code changes were made but docs weren't updated

## Steps

### Phase 1: Identify What Changed

Determine the scope of recent changes. Use whichever approach fits the current situation:

**If on a feature branch with uncommitted or unmerged work:**
```bash
git diff origin/main --stat
git diff origin/main --name-only
```

**If changes were already merged (branch is up-to-date with main):**
```bash
# Look at recent commits on main to find what was merged this session
git log origin/main --oneline -20
# Examine specific merged PRs
git show <merge-commit> --stat
# Or diff a range of recent commits
git diff <before-sha>..<after-sha> --stat
```

**If invoked without a specific diff target**, review the conversation context to identify what code changes were made this session, then read the affected files directly.

Categorize changes into:
1. **Code changes** — new files, modified modules, new types/patterns
2. **CI/CD changes** — workflow files, deploy pipelines, secrets usage
3. **Infrastructure changes** — terraform, server config, environment setup
4. **Build changes** — gradle config, dependencies, build flags

For each category, understand the *intent* behind the changes — read commit messages and the actual diffs to understand why things changed, not just what changed.

### Phase 2: Determine Which Docs Need Updates

Read each documentation file and check if it's affected by the changes:

| File | Update when... |
|------|---------------|
| `.agent/project.md` | Architecture, build system, deployment, testing, or server URL patterns change |
| `.agent/AGENTS.md` | New workflow rules, configuration patterns, or mistakes-to-avoid are discovered |
| `README.md` | User-facing setup, build, or configuration instructions change |
| `server/README.md` | Server deployment, secrets, or infrastructure changes |
| `CHANGELOG.md` | Any meaningful code change (features, fixes, CI/CD improvements) |
| Module `README.md` files | New models, interfaces, or components added to a module |
| `data-network/README.md` | Network layer, BuildConfig, or gRPC changes |
| `domain/README.md` | New domain models, repository interfaces, or error types |

Skip files that aren't affected by the changes. Don't update documentation speculatively.

### Phase 3: Update Agent Files

#### `.agent/project.md` — Project Knowledge

This is the shared knowledge base for all AI agents. Update the appropriate section:

- **Architecture** — New modules, dependencies, or patterns
- **Build System** — New build flags, tasks, or quirks
- **Server URL Management** — Changes to how URLs flow from terraform to code
- **Server Deployment** — Workflow changes, new secrets, observability updates
- **Testing** — New test categories or patterns
- **CI** — Path filtering changes, new workflows, auto-generation updates

Rules:
- Be factual, not speculative — only document confirmed behavior
- Include code references where helpful (file paths, class names)
- Keep entries concise — this file is read at session start

#### `.agent/AGENTS.md` — Workflow Rules

Only update if changes introduce new rules that agents must follow:

- **Configuration** — New "never hardcode X" rules, new config access patterns
- **Validation** — New validation steps or scripts
- **CI Workflow Synchronization** — New files to keep in sync
- **Critical Rules** — New safety constraints

### Phase 4: Update READMEs

#### `README.md` — Root project README

Update if:
- Build/run instructions changed
- New prerequisites added
- Configuration options changed (e.g., `local.properties` or `gradle.properties`)
- New optional features added

Keep the tone user-friendly — this is for contributors, not agents.

#### `server/README.md` — Server deployment README

Update if:
- GitHub secrets added or changed (update the prerequisites table)
- Deploy workflow behavior changed
- New infrastructure patterns introduced
- Terraform configuration changed

#### Module READMEs (e.g., `domain/README.md`, `data-network/README.md`)

Update if:
- New public types, interfaces, or components added
- Module responsibilities expanded
- Architecture tables need new entries

### Phase 5: Update CHANGELOG

Add an entry under the current date. If an entry for today already exists, append to it.

Format:
```markdown
## YYYY-MM-DD

### Features
- **Feature name**: Description of what and why

### Fixes
- **Fix name**: Description of what was broken and how it was fixed

### CI/CD Improvements
- **Improvement name**: Description of the improvement

### Documentation
- **Doc update name**: What was updated and why
```

Rules:
- Most recent changes at the top
- Explain the *why*, not just the *what*
- Include PR links when available: `[#N](https://github.com/cartland/battery-butler/pull/N)`
- Group related changes under appropriate categories

### Phase 6: Review and Commit

1. Run spotless to format any Kotlin files touched:
   ```bash
   ./scripts/spotless-apply.sh
   ```

2. Review all changes:
   ```bash
   git diff --stat
   git diff
   ```

3. Verify no code behavior was changed — only documentation files should be modified.

4. Stage and commit:
   ```bash
   git add .agent/ README.md server/README.md domain/README.md data-network/README.md CHANGELOG.md
   # Add any other updated docs
   git commit -m "docs: Update project documentation for [brief description of code changes]"
   ```

## Tips

- Read the existing content before editing — understand what's already documented to avoid duplication
- Don't add speculative information — only document things confirmed by the code changes
- Keep agent files (`project.md`, `AGENTS.md`) concise — agents read these at session start
- Don't remove existing documentation unless it's demonstrably wrong or outdated
- When adding new sections, place them logically near related content
- If code comments in the changed files are outdated, note that but don't update them here (that's a code change, not a docs change)
