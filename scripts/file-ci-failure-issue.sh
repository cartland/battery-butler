#!/usr/bin/env bash
#
# File or resolve a GitHub issue for a CI failure on `main`.
#
# Invoked by .github/workflows/ci-post-merge-issue.yml on every
# `workflow_run` completion for a watched workflow on `main`.
#
# Two modes, selected by the caller:
#
#   Blocking (default, Battery Butler CI). Files `ci-failure` + `blocking`
#   issues, which pause PR auto-merges via ci.yml's
#   validation_no_blocking_issues. Verifies sentinels before closing.
#
#   Non-blocking (the automation pipeline: Auto-Generate Content, CI for Auto
#   PRs). Files issues under a DIFFERENT label with no `blocking` tag, so a
#   janitorial failure gets visibility without freezing every merge in the
#   repo. The sentinel check is skipped -- those are Battery Butler CI job
#   names and mean nothing here.
#
# The two modes must never share a label: the success path closes every open
# issue carrying $ISSUE_LABEL, so a green auto-generate run closing real
# `ci-failure` issues would silently disarm the safety net.
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
#
# Optional env (defaults reproduce the original blocking behaviour exactly):
#   ISSUE_LABEL      label to file/close under        (default: ci-failure)
#   BLOCKING         also tag `blocking`              (default: true)
#   TITLE_PREFIX     issue title prefix               (default: "CI failure on main:")
#   CHECK_SENTINELS  verify sentinels before closing  (default: true)

set -euo pipefail

LABEL="${ISSUE_LABEL:-ci-failure}"
BLOCKING_LABEL="blocking"
BLOCKING="${BLOCKING:-true}"
TITLE_PREFIX="${TITLE_PREFIX:-CI failure on main:}"
CHECK_SENTINELS="${CHECK_SENTINELS:-true}"

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

close_open_issues() {
  local open_issues num title
  open_issues=$(gh issue list --label "$LABEL" --state open \
    --json number,title --jq '.[]')
  if [[ -z "$open_issues" ]]; then
    echo "No open $LABEL issues to close."
    return 0
  fi
  # Collect into an array first: `... | while read` runs the body in a
  # subshell, so nothing it does survives the loop.
  local rows=()
  while IFS= read -r row; do
    [[ -n "$row" ]] && rows+=("$row")
  done < <(echo "$open_issues" | jq -c '.')
  for row in "${rows[@]}"; do
    num=$(echo "$row" | jq -r '.number')
    title=$(echo "$row" | jq -r '.title')
    echo "Closing #$num: $title"
    gh issue close "$num" --comment "Resolved: \`$WORKFLOW_NAME\` is green again on \`main\`.

- Run: $RUN_URL
- Commit: \`$HEAD_SHA\`"
  done
}

ensure_label "$LABEL" "B60205" "Failure on main; filed automatically by ci-post-merge-issue.yml"
if [[ "$BLOCKING" == "true" ]]; then
  ensure_label "$BLOCKING_LABEL" "B60205" "Must resolve before new auto-merges proceed"
fi

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
  # The sentinel names below are Battery Butler CI jobs. In non-blocking mode
  # the watched workflow has entirely different jobs, so the check would
  # always conclude "nothing real ran" and never close anything.
  if [[ "$CHECK_SENTINELS" != "true" ]]; then
    echo "Sentinel check skipped (CHECK_SENTINELS=$CHECK_SENTINELS)."
    close_open_issues
    exit 0
  fi

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

  close_open_issues
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
  title="$TITLE_PREFIX $job"

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
    if [[ "$BLOCKING" == "true" ]]; then
      impact="This issue is **blocking**. New PR auto-merges are paused until it is resolved."
    else
      impact="This issue is **not blocking** -- it does not pause merges. It reports a failure in the automation pipeline, which produces generated content and housekeeping PRs rather than anything that ships."
    fi

    body=$(cat <<EOF
\`main\` failed on job: **\`$job\`**.

- Run: $RUN_URL
- Commit: \`$HEAD_SHA\`
- Workflow: $WORKFLOW_NAME

$impact
It will auto-close on the next green run of \`$WORKFLOW_NAME\` on \`main\`.

To investigate:
\`\`\`bash
gh run view $RUN_ID --log-failed
\`\`\`
EOF
)
    labels=(--label "$LABEL")
    if [[ "$BLOCKING" == "true" ]]; then
      labels+=(--label "$BLOCKING_LABEL")
    fi
    gh issue create \
      --title "$title" \
      --body "$body" \
      "${labels[@]}"
  fi
done
