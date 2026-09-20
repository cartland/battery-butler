#!/bin/bash
# =============================================================================
# VALIDATE GITHUB ACTIONS WORKFLOWS
# =============================================================================
#
# PURPOSE:
#   Checks the workflow files themselves, on the PR that changes them.
#
#   Most workflows here only ever trigger on push to main (auto-generate.yml)
#   or on a tag (release-android.yml). Nothing in CI reads those files on a
#   PR, so a mistake in one is invisible until after merge -- at which point
#   it either silently does not run or fails where nobody is watching. These
#   checks run pre-submit instead.
#
# CHECKS:
#   1. No scheduled (cron) triggers. Workflows run pre-submit or post-submit.
#   2. Every ./scripts/*.sh a workflow invokes exists and is executable.
#   3. Every workflow parses as YAML (best effort -- needs ruby or python-yaml).
#
# USAGE:
#   ./scripts/validate-workflows.sh
# =============================================================================

set -uo pipefail

WORKFLOW_DIR=".github/workflows"
FAILED=0

fail() {
  echo "::error::$1"
  echo "  FAIL: $1"
  FAILED=1
}

if [ ! -d "$WORKFLOW_DIR" ]; then
  echo "No $WORKFLOW_DIR directory; nothing to validate."
  exit 0
fi

WORKFLOWS=()
while IFS= read -r wf; do
  [ -n "$wf" ] && WORKFLOWS+=("$wf")
done < <(find "$WORKFLOW_DIR" -maxdepth 1 \( -name '*.yml' -o -name '*.yaml' \) | sort)

if [ ${#WORKFLOWS[@]} -eq 0 ]; then
  echo "No workflow files found; nothing to validate."
  exit 0
fi

# ---------------------------------------------------------------------------
# 1. No scheduled triggers.
# ---------------------------------------------------------------------------
# Actions here run pre-submit (pull_request) or post-submit (push to main /
# workflow_run / tag). A cron adds a third category that fires when nobody is
# watching, races the post-merge run for the same branches, and burns minutes
# re-deriving output that is a pure function of a commit that has not changed.
# Anything a nightly run would catch, the push that caused it already catches.
echo "=== Checking for scheduled (cron) triggers ==="
for wf in "${WORKFLOWS[@]}"; do
  if grep -qE '^[[:space:]]*-[[:space:]]*cron:' "$wf" || grep -qE '^[[:space:]]{2}schedule:[[:space:]]*$' "$wf"; then
    fail "$wf declares a scheduled trigger. Workflows must run pre-submit (pull_request) or post-submit (push / workflow_run / tag). See the TRIGGERS note in auto-generate.yml."
  fi
done
[ "$FAILED" -eq 0 ] && echo "  OK: no cron triggers"

# ---------------------------------------------------------------------------
# 2. Referenced scripts exist and are executable.
# ---------------------------------------------------------------------------
# A workflow that shells out to a script deleted or renamed in the same PR
# fails at run time, which for a push-to-main-only workflow means after merge.
echo ""
echo "=== Checking referenced ./scripts/*.sh ==="
REFS=$(grep -ohE '\./scripts/[A-Za-z0-9_.-]+\.sh' "${WORKFLOWS[@]}" | sort -u)

if [ -z "$REFS" ]; then
  echo "  (no script references found)"
else
  for ref in $REFS; do
    path="${ref#./}"
    if [ ! -f "$path" ]; then
      fail "$ref is referenced by a workflow but does not exist"
    elif [ ! -x "$path" ]; then
      fail "$ref is referenced by a workflow but is not executable (chmod +x)"
    else
      echo "  OK: $ref"
    fi
  done
fi

# ---------------------------------------------------------------------------
# 3. YAML parses.
# ---------------------------------------------------------------------------
# Best effort: a malformed workflow is rejected by GitHub at dispatch time,
# which again is post-merge for the push-only workflows.
echo ""
echo "=== Checking YAML syntax ==="
if command -v ruby >/dev/null 2>&1; then
  for wf in "${WORKFLOWS[@]}"; do
    if ! ruby -ryaml -e 'YAML.load_file(ARGV[0])' "$wf" 2>/dev/null; then
      fail "$wf is not valid YAML"
    fi
  done
  echo "  Parsed with ruby"
elif python3 -c 'import yaml' 2>/dev/null; then
  for wf in "${WORKFLOWS[@]}"; do
    if ! python3 -c 'import sys,yaml; yaml.safe_load(open(sys.argv[1]))' "$wf" 2>/dev/null; then
      fail "$wf is not valid YAML"
    fi
  done
  echo "  Parsed with python yaml"
else
  echo "  SKIPPED: no ruby and no python yaml module available"
fi

echo ""
if [ "$FAILED" -ne 0 ]; then
  echo "=== Workflow validation FAILED ==="
  exit 1
fi

echo "=== Workflow validation passed ==="
