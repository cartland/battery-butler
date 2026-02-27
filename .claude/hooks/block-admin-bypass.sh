#!/bin/bash
# Block or warn on gh commands that use --admin to bypass branch protection rulesets.
# Behavior depends on CI mode:
#   - development: warn only (allows merging past flaky infrastructure failures)
#   - release: deny (strict enforcement)
#
# Only checks unquoted arguments — text inside quotes (e.g. PR body) is ignored.

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# Read CI mode from config file
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)"
CI_MODE=$(cat "$REPO_ROOT/.github/ci-mode.txt" 2>/dev/null | tr -d '[:space:]')
CI_MODE="${CI_MODE:-release}"

# Strip single-quoted strings, double-quoted strings, and heredoc bodies
# so we only inspect actual command flags, not prose in PR descriptions.
STRIPPED=$(echo "$COMMAND" | sed -E "s/'[^']*'//g; s/\"[^\"]*\"//g" | sed '/<<.*EOF/,/^EOF/d')

if echo "$STRIPPED" | grep -qE '\bgh\b.*--admin'; then
  if [ "$CI_MODE" = "development" ]; then
    echo "WARNING: --admin flag bypasses branch protection. In development mode this is allowed, but use with caution." >&2
  else
    jq -n '{
      "hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "deny",
        "permissionDecisionReason": "BLOCKED: --admin flag bypasses branch protection. Remove --admin and wait for CI to pass."
      }
    }'
    exit 0
  fi
fi

exit 0
