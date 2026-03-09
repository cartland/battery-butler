#!/bin/bash
# Protect .beads/ files from direct edits by Claude Code agents.
# All .beads/ modifications should go through the 'bd' CLI.

INPUT=$(cat)
TOOL_NAME=$(echo "$INPUT" | jq -r '.tool_name // empty')
[ -z "$TOOL_NAME" ] && exit 0

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

DENY_MSG="BLOCKED: Direct edits to .beads/ files are not allowed. Use the 'bd' CLI instead (e.g., bd new, bd start, bd close, bd list)."

case "$TOOL_NAME" in
  Edit|Write)
    FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')
    if echo "$FILE_PATH" | grep -qE '(^|/)\.beads/'; then
      deny "$DENY_MSG"
    fi
    ;;
  Bash)
    COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')
    [ -z "$COMMAND" ] && exit 0

    # Allow bd commands — that's the correct interface
    if echo "$COMMAND" | grep -qE '^\s*bd\s'; then
      exit 0
    fi

    # Check for write operations targeting .beads/
    # Block: echo/cat/tee/cp/mv/rm/sed -i/awk redirecting or targeting .beads/
    if echo "$COMMAND" | grep -qE '>\s*\.beads/'; then
      deny "$DENY_MSG"
    fi
    if echo "$COMMAND" | grep -qE '>\s*[^ ]*\.beads/'; then
      deny "$DENY_MSG"
    fi
    if echo "$COMMAND" | grep -qE '\b(rm|mv|cp)\b.*\.beads/'; then
      deny "$DENY_MSG"
    fi
    if echo "$COMMAND" | grep -qE '\bsed\b.*-i.*\.beads/'; then
      deny "$DENY_MSG"
    fi
    if echo "$COMMAND" | grep -qE '\btee\b.*\.beads/'; then
      deny "$DENY_MSG"
    fi
    ;;
esac

exit 0
