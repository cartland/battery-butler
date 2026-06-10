---
description: Capture session knowledge (tasks, decisions, workarounds) into TODO.md and docs before session ends.
---

# Dump Context

Capture conversation knowledge into durable artifacts before a session ends or context is compacted. This prevents loss of decisions, tasks, workarounds, and architecture knowledge.

## When to Use

- Before ending a session where significant work was done
- When context window is getting large and compaction is likely
- After completing a complex investigation or multi-step task
- When switching to a very different area of work

## Steps

### Phase 1: Scan Conversation

Review the entire conversation and categorize knowledge into four buckets:

1. **Actionable items** — tasks, bugs, follow-ups, TODOs mentioned but not yet tracked
2. **Decisions and architecture** — design choices, trade-offs evaluated, patterns established
3. **Operational knowledge** — workarounds, commands discovered, mistakes made and corrected
4. **Status and context** — what was completed, what's in progress, what's blocked and why

Write a brief summary of findings before proceeding.

### Phase 2: Update TODO.md (Highest Priority)

For each actionable item identified in Phase 1, add a task to `TODO.md` (repo root)
under the appropriate priority heading (`## P2` / `## P3` / `## P4`):

```markdown
### bb-xxxx — <title>

<self-contained description>
```

Rules:
- One `### ` task subsection per actionable item
- Mint a new short unique `bb-xxxx` id (any unused slug). IDs are stable anchors
  other docs/workflow comments may reference
- Descriptions must be **self-contained** — assume no conversation context
- Include enough detail for a fresh session to pick up the work
- Place under the right `## P2`/`## P3`/`## P4` heading by priority
- Skip items that already have a task (search `TODO.md` first)
- If an existing task gained new evidence/status, append it to that task in place
  rather than duplicating

### Phase 3: Update Documentation

Update these files with knowledge from the conversation:

#### `.agent/project.md` — Project Knowledge
Add to the appropriate section:
- New commands or workflows discovered
- Build system quirks or workarounds
- Testing patterns
- Deployment changes

#### `.agent/AGENTS.md` — Workflow Rules
Add if applicable:
- New workflow rules
- Changes to merge/CI process
- Mistakes to avoid — **especially cases where the agent reported incorrect state to the user** (e.g., wrong PR merge status, assumed auto-merge was set, missed merge conflicts). These must become explicit verification rules so the mistake cannot recur.

#### Other Documentation (if applicable)
- `CHANGELOG.md` — add entries for completed/merged work
- `README.md`, `server/README.md`, etc. — new setup steps, changed behavior
- Remove outdated information discovered during the session

### Phase 5: Commit via PR

1. Stage all changes:
   ```bash
   git add TODO.md .agent/project.md .agent/AGENTS.md
   # Add any other updated docs
   ```

2. Create branch and commit:
   ```bash
   git checkout -b agent/dump-context-YYYY-MM-DD origin/main
   git commit -m "chore: Dump session context — TODO.md and docs update"
   ```

3. Push and create PR:
   ```bash
   git push -u origin agent/dump-context-YYYY-MM-DD
   gh pr create --title "chore: Dump session context" --body "$(cat <<'EOF'
   ## Summary
   - Added X new tasks to TODO.md
   - Updated session resume points
   - [List other doc updates]

   ## Tasks Added
   - `bb-xxx`: <title>
   - `bb-yyy`: <title>

   ## Docs Updated
   - `.agent/project.md`: [sections updated]
   - [Other files]
   EOF
   )"
   ```

This is a P0.5 priority PR (instruction/docs only) — merge immediately when CI passes.

## Tips

- Phase 2 (TODO.md) is the most important — tasks lost in conversation are tasks forgotten
- Be aggressive about adding tasks — it's better to have a task you later delete as "not needed" than to lose one
- Don't put session-specific state in docs — use `TODO.md` for tasks and let it be the resume point
- Don't update docs with speculative information — only document things confirmed during the session
- If the conversation was short or trivial, skip phases that don't apply (not every session needs all 5 phases)
