# Claude Code Instructions

**First Action:** Read `.agent/AGENTS.md` (workflow rules) and `.agent/project.md` (project knowledge).

## Claude-Specific

- **NEVER push directly to `main`.** Always create a branch and open a PR, even for trivial changes like beads updates or docs.
- Run `/dump-context` before ending any session where significant work was done.

## Merge Rules

- **Never merge a PR with failing CI.** Wait for the `ci` check to pass.
- **Never push directly to main.** Always use a branch + PR.
- Branch protection enforces the `ci` status check. Admin bypass is for emergencies only.
