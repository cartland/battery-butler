#!/bin/bash
# Git workflow guardrails for Claude Code agents.
# Blocks dangerous operations and enforces repo conventions.

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')
[ -z "$COMMAND" ] && exit 0

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)"

deny() {
  jq -n --arg reason "$1" '{
    "hookSpecificOutput": {
      "hookEventName": "PreToolUse",
      "permissionDecision": "deny",
      "permissionDecisionReason": $reason
    }
  }'
  exit 0
}

# Strip heredoc bodies first (before quote stripping removes the EOF marker),
# then strip quoted strings to avoid false positives on prose content.
STRIPPED=$(echo "$COMMAND" | sed '/<<.*EOF/,/^EOF/d' | sed -E "s/'[^']*'//g; s/\"[^\"]*\"//g")

# --- 8. Block shell control flow (check first, applies to all commands) ---
# Note: && and || are allowed (simple chaining). Only block loop/conditional keywords.
if echo "$STRIPPED" | grep -qE '\b(for|while|until|if|do|done|then|fi|elif|else)\b'; then
  deny "BLOCKED: Don't use shell control flow (for/while/if). Run each command as a separate Bash tool call."
fi

# --- 7. Block git -C ---
if echo "$STRIPPED" | grep -qE '\bgit\s+-C\b'; then
  deny "BLOCKED: Don't use git -C. Run git from the repository root."
fi

# --- git push checks ---
if echo "$STRIPPED" | grep -qE '\bgit\s+push\b'; then

  # 2. Block push to main
  if echo "$STRIPPED" | grep -qE '\bgit\s+push\b.*\b(main|master)\b'; then
    deny "BLOCKED: Never push directly to main. Create a branch and open a PR."
  fi
  if [ -n "$REPO_ROOT" ]; then
    CURRENT_BRANCH=$(git branch --show-current 2>/dev/null)
    if [ "$CURRENT_BRANCH" = "main" ] || [ "$CURRENT_BRANCH" = "master" ]; then
      deny "BLOCKED: You are on '$CURRENT_BRANCH'. Switch to a feature branch before pushing."
    fi
  fi

  # 3. Block force push
  if echo "$STRIPPED" | grep -qE '\bgit\s+push\b.*(-f\b|--force\b|--force-with-lease\b)'; then
    deny "BLOCKED: Force push is destructive. Use normal push."
  fi

  # 1. Block push without validation (skip for docs/beads-only changes)
  if [ -n "$REPO_ROOT" ]; then
    MARKER="$REPO_ROOT/.claude/.validation-passed"
    VALIDATED_HASH=$(cat "$MARKER" 2>/dev/null)
    CURRENT_HEAD=$(git rev-parse HEAD 2>/dev/null)
    if [ "$VALIDATED_HASH" != "$CURRENT_HEAD" ]; then
      # Check if all changed files (vs origin/main) are docs/beads-only
      CHANGED_FILES=$(git diff --name-only origin/main...HEAD 2>/dev/null)
      DOCS_ONLY=true
      for f in $CHANGED_FILES; do
        case "$f" in
          .beads/*|.agent/*|CLAUDE.md|GEMINI.md|ANTIGRAVITY.md|AGENTS.md|*.md|.claude/*) ;;
          *) DOCS_ONLY=false; break ;;
        esac
      done
      if [ "$DOCS_ONLY" = "false" ]; then
        if [ ! -f "$MARKER" ]; then
          deny "BLOCKED: Run ./scripts/validate.sh before pushing. No validation record found."
        fi
        deny "BLOCKED: Code changed since last validation. Re-run ./scripts/validate.sh."
      fi
    fi
  fi
fi

# --- 4. Enforce squash merge ---
if echo "$STRIPPED" | grep -qE '\bgh\s+pr\s+merge\b'; then
  if ! echo "$STRIPPED" | grep -qF -- '--squash'; then
    deny "BLOCKED: Always use --squash --delete-branch when merging PRs."
  fi
fi

# --- 5. Block tag creation/modification (only listing allowed) ---
if echo "$STRIPPED" | grep -qE '\bgit\s+tag\b'; then
  if ! echo "$STRIPPED" | grep -qE '\bgit\s+tag\b.*(-l\b|--list\b)'; then
    deny "BLOCKED: Never create or modify tags directly. Use release scripts (/release-android, /promote-server). Use git tag -l to list."
  fi
fi

# --- 6. Block destructive git commands ---
if echo "$STRIPPED" | grep -qE '\bgit\s+reset\s+--hard\b'; then
  deny "BLOCKED: git reset --hard discards commits. Use git stash or a backup branch."
fi
if echo "$STRIPPED" | grep -qE '\bgit\s+clean\b.*-[a-zA-Z]*f'; then
  deny "BLOCKED: git clean -f permanently deletes untracked files."
fi
if echo "$STRIPPED" | grep -qE '\bgit\s+(checkout|restore)\s+\.\s*$'; then
  deny "BLOCKED: Discarding all changes is destructive. Use git stash or target specific files."
fi

exit 0
