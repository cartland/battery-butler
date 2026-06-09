#!/usr/bin/env bash
#
# File or resolve a GitHub issue for a CI failure on `main`.
#
# Invoked by .github/workflows/ci-post-merge-issue.yml on every
# `workflow_run` completion for Battery Butler CI on `main`.
#
# Failure path:
#   - List failing jobs from the run via the Actions API.
#   - For each failing job, find an open issue titled "CI failure on main: <job>"
#     with the ci-failure label. Comment on it if present, open a new one if not.
#   - New issues are also tagged `blocking`, which gates new PR auto-merges via
#     ci.yml's validation_no_blocking_issues job.
#
# Success path:
#   - Close every open ci-failure issue with a resolution comment.
#
# Required env:
#   GH_TOKEN, GITHUB_REPOSITORY, RUN_ID, RUN_URL, RUN_CONCLUSION,
#   HEAD_SHA, WORKFLOW_NAME

set -euo pipefail

LABEL="ci-failure"
BLOCKING_LABEL="blocking"

echo "Run:        $RUN_URL"
echo "Conclusion: $RUN_CONCLUSION"
echo "Commit:     $HEAD_SHA"
echo "Workflow:   $WORKFLOW_NAME"
echo

ensure_label() {
  local name="$1" color="$2" description="$3"
  if ! gh label list --search "$name" --json name --jq '.[].name' \
      | grep -qx "$name"; then
    echo "Creating label: $name"
    gh label create "$name" --color "$color" --description "$description" || true
  fi
}

ensure_label "$LABEL" "B60205" "CI regression on main; filed automatically by ci-post-merge-issue.yml"
ensure_label "$BLOCKING_LABEL" "B60205" "Must resolve before new auto-merges proceed"

# -----------------------------------------------------------------------------
# Success path: close all open ci-failure issues.
#
# Caveat: path-filtered runs (docs-only, auto-generate-only) mark
# every real validation job as SKIPPED while the `ci` aggregator still
# returns success because nothing real ran. That false success must NOT
# auto-close a ci-failure issue that was filed by a prior real-code run —
# the underlying break is still on main. See bb-2r4g.
# -----------------------------------------------------------------------------
if [[ "$RUN_CONCLUSION" == "success" ]]; then
  # Count jobs that succeeded and aren't control-flow gates. `changes`,
  # `ci`, and `validation_no_blocking_issues` always run (they're gates,
  # not validations of the code itself), so they don't count as evidence
  # that anything real was checked.
  real_success_count=$(
    gh api "repos/$GITHUB_REPOSITORY/actions/runs/$RUN_ID/jobs?per_page=100" \
      --paginate \
      --jq '[.jobs[]
              | select(.conclusion == "success")
              | select(.name != "changes"
                       and .name != "ci"
                       and .name != "validation_no_blocking_issues")]
            | length'
  )

  if [[ "$real_success_count" -eq 0 ]]; then
    echo "Run succeeded but no real validation jobs ran — every non-gate"
    echo "job was SKIPPED (path-filtered docs/auto-generate run)."
    echo "Skipping auto-close: open ci-failure issues may still reflect a"
    echo "real break on main."
    exit 0
  fi

  echo "Real validation jobs that succeeded: $real_success_count. Proceeding"
  echo "with close-on-success."

  open_issues=$(gh issue list --label "$LABEL" --state open \
    --json number,title --jq '.[]')
  if [[ -z "$open_issues" ]]; then
    echo "No open ci-failure issues to close."
    exit 0
  fi
  echo "$open_issues" | jq -c '.' | while read -r row; do
    num=$(echo "$row" | jq -r '.number')
    title=$(echo "$row" | jq -r '.title')
    echo "Closing #$num: $title"
    gh issue close "$num" --comment "Resolved: \`main\` CI is green again.

- Run: $RUN_URL
- Commit: \`$HEAD_SHA\`"
  done
  exit 0
fi

# -----------------------------------------------------------------------------
# Failure path: open or comment per failing job.
# -----------------------------------------------------------------------------
mapfile -t failed_jobs < <(
  gh api "repos/$GITHUB_REPOSITORY/actions/runs/$RUN_ID/jobs?per_page=100" \
    --paginate \
    --jq '.jobs[] | select(.conclusion == "failure") | .name'
)

if [[ ${#failed_jobs[@]} -eq 0 ]]; then
  echo "No failed jobs found for conclusion=$RUN_CONCLUSION. Nothing to file."
  exit 0
fi

for job in "${failed_jobs[@]}"; do
  title="CI failure on main: $job"

  existing=$(
    gh issue list --label "$LABEL" --state open \
      --json number,title \
      --jq ".[] | select(.title == \"$title\") | .number" \
      | head -n1
  )

  if [[ -n "$existing" ]]; then
    echo "Commenting on existing issue #$existing for job: $job"
    gh issue comment "$existing" --body "Job failed again.

- Run: $RUN_URL
- Commit: \`$HEAD_SHA\`"
  else
    echo "Opening new issue for job: $job"
    body=$(cat <<EOF
\`main\` CI failed on job: **\`$job\`**.

- Run: $RUN_URL
- Commit: \`$HEAD_SHA\`
- Workflow: $WORKFLOW_NAME

This issue is **blocking**. New PR auto-merges are paused until it is resolved.
It will auto-close on the next green push-to-\`main\` CI run.

To investigate:
\`\`\`bash
gh run view $RUN_ID --log-failed
\`\`\`
EOF
)
    gh issue create \
      --title "$title" \
      --body "$body" \
      --label "$LABEL" \
      --label "$BLOCKING_LABEL"
  fi
done
