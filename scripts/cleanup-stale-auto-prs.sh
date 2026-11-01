#!/bin/bash
# =============================================================================
# CLEANUP STALE AUTO-GENERATED PRs
# =============================================================================
#
# PURPOSE:
#   Closes auto-generated PRs that have outlived their usefulness and deletes
#   the branches left behind, so the auto/* namespace holds at most one live
#   PR per managed branch.
#
#   An auto/* PR is closed when it is either older than MAX_AGE_HOURS or
#   already conflicting with main. Nothing is lost by closing one: the next
#   Auto-Generate Content run recreates it if the content still differs.
#
# USAGE:
#   ./scripts/cleanup-stale-auto-prs.sh
#
#   Requires GH_TOKEN with contents:write and pull-requests:write.
#
# WHY A SCRIPT AND NOT INLINE YAML:
#   This runs as a job step inside auto-generate.yml (post-merge). It used to
#   live inline in a cron-triggered workflow, where it could not be run
#   locally, checked by shellcheck, or read without scrolling through YAML.
# =============================================================================

set -uo pipefail

MAX_AGE_HOURS="${MAX_AGE_HOURS:-24}"
MAX_AGE_SECONDS=$((MAX_AGE_HOURS * 3600))
CURRENT_TIME=$(date +%s)

MANAGED_BRANCHES=(
  auto/update-generated-content
  auto/update-android-screenshots
  auto/update-ios-screenshots
)

# GNU date and BSD date disagree on parsing an ISO-8601 timestamp. This runs on
# ubuntu in CI but may be run by hand on macOS, so try both spellings.
to_epoch() {
  date -d "$1" +%s 2>/dev/null ||
    date -j -f "%Y-%m-%dT%H:%M:%SZ" "$1" +%s 2>/dev/null ||
    echo 0
}

echo "=== Cleaning up stale auto-generated PRs ==="
echo ""

for BRANCH in "${MANAGED_BRANCHES[@]}"; do
  echo "Checking branch: $BRANCH"

  # Collect first, then loop. Piping into `while` runs the body in a subshell,
  # so anything it reports back is silently discarded.
  PRS=$(gh pr list --head "$BRANCH" --state open --json number,createdAt,mergeable --jq '.[] | "\(.number) \(.createdAt) \(.mergeable)"')

  if [ -z "$PRS" ]; then
    echo "  No open PRs"
    continue
  fi

  while read -r PR_NUM CREATED MERGEABLE; do
    [ -z "$PR_NUM" ] && continue

    CREATED_TIME=$(to_epoch "$CREATED")

    if [ "$CREATED_TIME" -eq 0 ]; then
      # An unparseable timestamp must not read as "infinitely old" and take a
      # healthy PR with it.
      echo "  PR #$PR_NUM -> Keeping: could not parse createdAt '$CREATED'"
      continue
    fi

    AGE_SECONDS=$((CURRENT_TIME - CREATED_TIME))
    AGE_HOURS=$((AGE_SECONDS / 3600))

    echo "  PR #$PR_NUM - Age: ${AGE_HOURS}h, Mergeable: $MERGEABLE"

    REASON=""
    if [ "$AGE_SECONDS" -gt "$MAX_AGE_SECONDS" ]; then
      REASON="Stale: PR is ${AGE_HOURS} hours old (max: ${MAX_AGE_HOURS} hours)"
    elif [ "$MERGEABLE" = "CONFLICTING" ]; then
      REASON="Conflicts: PR has merge conflicts with main branch"
    fi

    if [ -n "$REASON" ]; then
      echo "  -> Closing: $REASON"
      gh pr close "$PR_NUM" --comment "Auto-closing: $REASON

A new PR will be created by the next Auto-Generate Content workflow run if changes are still needed." || echo "  -> Close failed, continuing"
    fi
  done <<<"$PRS"
done

echo ""
echo "=== Cleaning up orphaned auto/* branches ==="

for BRANCH in $(git ls-remote --heads origin 'refs/heads/auto/*' | awk '{print $2}' | sed 's|refs/heads/||'); do
  OPEN_PR=$(gh pr list --head "$BRANCH" --state open --json number --jq 'length')

  if [ "$OPEN_PR" = "0" ]; then
    echo "Deleting orphaned branch: $BRANCH"
    git push origin --delete "$BRANCH" || true
  else
    echo "Keeping branch with open PR: $BRANCH"
  fi
done

echo ""
echo "=== Cleanup complete ==="
