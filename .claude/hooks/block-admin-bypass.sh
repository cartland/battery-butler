#!/bin/bash
# Block gh commands that use --admin to bypass branch protection rulesets.
# This prevents accidental merges with failing CI.
#
# Only checks unquoted arguments — text inside quotes (e.g. PR body) is ignored.

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# Strip single-quoted strings, double-quoted strings, and heredoc bodies
# so we only inspect actual command flags, not prose in PR descriptions.
STRIPPED=$(echo "$COMMAND" | sed -E "s/'[^']*'//g; s/\"[^\"]*\"//g" | sed '/<<.*EOF/,/^EOF/d')

if echo "$STRIPPED" | grep -qE '\bgh\b.*--admin'; then
  jq -n '{
    "hookSpecificOutput": {
      "hookEventName": "PreToolUse",
      "permissionDecision": "deny",
      "permissionDecisionReason": "BLOCKED: --admin flag bypasses branch protection. Remove --admin and wait for CI to pass."
    }
  }'
  exit 0
fi

exit 0
