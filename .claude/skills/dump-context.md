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

### Phase 2: Create Beads (Highest Priority)

For each actionable item identified in Phase 1, create a bead:

```bash
bd create "<title>" \
  --type task|bug|feature|chore \
  --priority P0|P1|P2|P3|P4 \
  --description "<self-contained description>"
```

Rules:
- One `bd create` per actionable item
- Descriptions must be **self-contained** — assume no conversation context
- Include enough detail for a fresh session to pick up the work
- Set appropriate `--type` and `--priority`
- Set up dependencies with `bd dep add` if items are related
- Skip items that already have beads (check with `bd search`)

### Phase 3: Update Documentation

Update these files with knowledge from the conversation:

#### `CLAUDE.md` — Session Resume Points
Replace the "Session Resume Points" section with current state:
- **Ready Tasks**: Top priorities from `bd ready`
- **Recent Completions**: Work completed this session
- **Known Issues**: Bugs, flaky tests, environment issues discovered
- **Context**: Important state that a new session needs to know

#### Other Documentation (if applicable)
- `CHANGELOG.md` — add entries for completed/merged work
- `README.md`, `server/README.md`, etc. — new setup steps, changed behavior
- Remove outdated information discovered during the session

### Phase 4: Update Agent Instructions

Update instruction files with operational knowledge from the conversation:

#### `CLAUDE.md` — Project-Specific Knowledge
Add to the appropriate subsection:
- New commands or workflows discovered
- Mistakes to avoid (add to "Common Mistakes to Avoid" or similar)
- Build system quirks or workarounds
- Testing patterns

#### `.agent/AGENTS.md`
Add if applicable:
- New workflow rules
- Changes to merge/CI process
- New risk categories for PRs

### Phase 5: Commit via PR

1. Stage all changes:
   ```bash
   git add .beads/ CLAUDE.md .agent/AGENTS.md
   # Add any other updated docs
   ```

2. Create branch and commit:
   ```bash
   git checkout -b agent/dump-context-YYYY-MM-DD origin/main
   git commit -m "chore: Dump session context — beads and docs update"
   ```

3. Push and create PR:
   ```bash
   git push -u origin agent/dump-context-YYYY-MM-DD
   gh pr create --title "chore: Dump session context" --body "$(cat <<'EOF'
   ## Summary
   - Created X new beads for tracked tasks
   - Updated session resume points
   - [List other doc updates]

   ## Beads Created
   - `bb-xxx`: <title>
   - `bb-yyy`: <title>

   ## Docs Updated
   - `CLAUDE.md`: Session resume points, [other sections]
   - [Other files]
   EOF
   )"
   ```

This is a P0.5 priority PR (instruction/beads only) — merge immediately when CI passes.

## Tips

- Phase 2 (beads) is the most important — tasks lost in conversation are tasks forgotten
- Be aggressive about creating beads — it's better to have a bead you close as "not needed" than to lose a task
- Session Resume Points should be concise but complete — a fresh session should be able to pick up exactly where you left off
- Don't update docs with speculative information — only document things confirmed during the session
- If the conversation was short or trivial, skip phases that don't apply (not every session needs all 5 phases)
