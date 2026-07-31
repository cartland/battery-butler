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

  # Sentinel-set check — the same ground truth the release gate uses.
  #
  # real_success_count above is necessary but NOT sufficient: a development-mode
  # run skips every sentinel while spotless/lint/detekt/test still succeed, so the
  # count is non-zero and the run concludes success even though nothing that
  # actually breaks main was exercised. That matters now that workflow_dispatch
  # runs can resolve issues (see ci-post-merge-issue.yml) — a bare dispatch
  # defaults to development mode. Without this, `gh workflow run` with no
  # `-f ci_mode=release` would silently close a real regression.
  #
  # On push runs the sentinels always run, so this is a no-op for the normal path.
  SENTINELS="validation_ios_ui validation_instrumented build_android build_ios_compose build_ios_native build_server"
  missing=""
  for job in $SENTINELS; do
    conclusion=$(
      gh api "repos/$GITHUB_REPOSITORY/actions/runs/$RUN_ID/jobs?per_page=100" \
        --paginate \
        --jq "[.jobs[] | select(.name == \"$job\") | .conclusion] | last // \"absent\""
    )
    if [[ "$conclusion" != "success" ]]; then
      missing="$missing $job($conclusion)"
    fi
  done

  if [[ -n "$missing" ]]; then
    echo "Run succeeded but these sentinel jobs are not success:$missing"
    echo "Skipping auto-close: this run did not exercise what breaks main"
    echo "(most likely a development-mode run). Dispatch with"
    echo "  gh workflow run \"Battery Butler CI\" --ref main -f ci_mode=release"
    echo "to produce a run that can resolve open ci-failure issues."
    exit 0
  fi

  echo "All sentinels green (real validation jobs: $real_success_count)."
  echo "Proceeding with close-on-success."

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
