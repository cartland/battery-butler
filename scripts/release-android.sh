#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Release Android app by creating and pushing an android/N tag.
# This triggers release-android.yml to build and deploy to Play Store INTERNAL track.
#
# IMPORTANT: We only publish to internal track, never production.
# Users promote from internal to production manually in Play Console.
#
# USAGE
#
# Start with --check. It prints the exact command(s) to run next, with real
# values (SHAs, tag names) already filled in. Copy-paste, don't re-type from
# memory — seeing concrete values is the whole point.
#
#   ./scripts/release-android.sh --check
#
# Normal release (CI passed on HEAD with real sentinel jobs):
#   ./scripts/release-android.sh --confirm-tag android/N
#
# Emergency release (CI on HEAD ran in dev mode or was path-filtered):
#   ./scripts/release-android.sh \
#       --confirm-tag android/N \
#       --confirm-skipped-jobs <40-char-sha>
#
# Rollback release (detached HEAD on an older tag):
#   git checkout android/M             # older tag you want to re-release
#   ./scripts/release-android.sh --check  # prints rollback command
#   ./scripts/release-android.sh \
#       --confirm-tag android/N \
#       --confirm-hash <40-char-sha-of-target> \
#       --confirm-rollback-from <40-char-sha-of-previous-latest>
#
# DESIGN PRINCIPLE
#
# Every override asks you to state a value from reality (a SHA, a tag).
# The script fails if the value does not match. This makes correct usage
# easy (read it from --check) and accidental usage hard (you would have
# to type the right value for the wrong situation).

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
RESET='\033[0m'

# Sentinel set: the real validation/build jobs that must have succeeded on
# the target commit. Kept in sync with .github/workflows/release-android.yml
# `verify-ci` step. See bb-nmqn / bb-k1ui.
SENTINEL_JOBS=(
    "validation_ios_ui"
    "validation_instrumented"
    "build_android"
    "build_ios_compose"
    "build_ios_native"
    "build_server"
)

# Parse flags
MODE="interactive"
ALLOW_TAGGED_COMMIT=false
CONFIRM_TAG=""
CONFIRM_HASH=""
CONFIRM_ROLLBACK_FROM=""
CONFIRM_SKIPPED_JOBS=""
DRY_RUN=false

show_help() {
    cat <<'EOF'
Usage: ./scripts/release-android.sh [OPTIONS]

Recommended workflow: run with --check first. It prints the exact command
to run next, with SHAs filled in. Copy-paste that command.

Modes:
  (no args)                 Interactive mode — prompts for confirmation
  --check                   Print state + recommended next command, then exit
  --confirm-tag <tag>       Non-interactive release. Tag must match computed next tag.
  --dry-run                 Show what would happen without creating tags

Override flags (all require a specific value from --check output):
  --confirm-hash <sha>              Release a specific commit (e.g. for rollback).
                                    Must match the resolved commit's full 40-char SHA.
  --confirm-rollback-from <sha>     Required for rollback releases. Must match the
                                    SHA the latest tag currently points to.
  --confirm-skipped-jobs <sha>      Release even though one or more sentinel CI jobs
                                    are not 'success' on the target commit. SHA must
                                    equal the target commit. Use this only when CI
                                    ran in development mode or was path-filtered AND
                                    you have separately validated the build.
  --allow-tagged-commit             Skip prompt when commit already has an android/* tag.

Help:
  -h, --help                Show this help

Examples:
  ./scripts/release-android.sh --check
  ./scripts/release-android.sh --confirm-tag android/32
EOF
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --check)
            MODE="check"
            shift
            ;;
        --confirm-tag)
            MODE="confirm"
            CONFIRM_TAG="$2"
            shift 2
            ;;
        --confirm-hash)
            CONFIRM_HASH="$2"
            shift 2
            ;;
        --confirm-rollback-from)
            CONFIRM_ROLLBACK_FROM="$2"
            shift 2
            ;;
        --confirm-skipped-jobs)
            CONFIRM_SKIPPED_JOBS="$2"
            shift 2
            ;;
        --allow-tagged-commit)
            ALLOW_TAGGED_COMMIT=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo -e "${RED}Error: Unknown option: $1${RESET}"
            show_help
            exit 1
            ;;
    esac
done

# ============================================================
# Compute state (used by both --check and release modes)
# ============================================================

REPO=$(gh repo view --json nameWithOwner --jq '.nameWithOwner' 2>/dev/null || echo "")

# Fetch tags
git fetch origin --tags --quiet 2>/dev/null || git fetch origin --tags

# Highest existing version
HIGHEST_VERSION=$(git tag -l 'android/[0-9]*' | \
    sed 's|android/||' | \
    grep -E '^[0-9]+$' | \
    sort -n | \
    tail -1 || true)

if [ -z "$HIGHEST_VERSION" ]; then
    NEXT_VERSION=1
    LATEST_TAG="(none)"
    LATEST_TAG_SHA="(none)"
else
    NEXT_VERSION=$((HIGHEST_VERSION + 1))
    LATEST_TAG="android/$HIGHEST_VERSION"
    LATEST_TAG_SHA=$(git rev-parse "$LATEST_TAG" 2>/dev/null || echo "(missing)")
fi

NEW_TAG="android/$NEXT_VERSION"

# Resolve target commit
if [ -n "$CONFIRM_HASH" ]; then
    TARGET_COMMIT=$(git rev-parse --verify "$CONFIRM_HASH" 2>/dev/null) || {
        echo -e "${RED}Error: --confirm-hash '$CONFIRM_HASH' is not a valid git ref.${RESET}"
        exit 1
    }
    TARGET_COMMIT_SHORT=$(git rev-parse --short "$TARGET_COMMIT")
    TARGET_SOURCE="--confirm-hash $CONFIRM_HASH -> $TARGET_COMMIT_SHORT"
else
    TARGET_COMMIT=$(git rev-parse HEAD)
    TARGET_COMMIT_SHORT=$(git rev-parse --short HEAD)
    TARGET_SOURCE="HEAD ($TARGET_COMMIT_SHORT)"
fi

# Current branch
CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || echo "")
if [ -z "$CURRENT_BRANCH" ]; then
    IS_DETACHED="true"
    CURRENT_BRANCH="(detached)"
else
    IS_DETACHED="false"
fi

# CI state on target commit. Fetch all check_runs once; query each sentinel
# from the result. Uses the same jq pattern as release-android.yml verify-ci
# (latest COMPLETED run per name, by completed_at) — see bb-nmqn.
CI_AGGREGATOR_STATE="unknown"
SENTINEL_RESULTS=()    # parallel array of "JOB:CONCLUSION"
SENTINELS_ALL_SUCCESS="false"
CI_FETCH_OK="false"

if [ -n "$REPO" ]; then
    CHECK_RUNS_RAW=$(gh api --paginate \
        "repos/${REPO}/commits/${TARGET_COMMIT}/check-runs?per_page=100" \
        --jq '.check_runs[]' 2>/dev/null | jq -s '.' 2>/dev/null || echo "")

    if [ -n "$CHECK_RUNS_RAW" ] && [ "$CHECK_RUNS_RAW" != "[]" ]; then
        CI_FETCH_OK="true"

        CI_AGGREGATOR_STATE=$(echo "$CHECK_RUNS_RAW" | jq -r '
            [.[] | select(.name == "ci") | select(.status == "completed")]
                | sort_by(.completed_at) | last | .conclusion // "no_completed_run"
        ')

        ANY_NON_SUCCESS=false
        for JOB in "${SENTINEL_JOBS[@]}"; do
            CONCLUSION=$(echo "$CHECK_RUNS_RAW" | jq -r --arg job "$JOB" '
                [.[] | select(.name == $job) | select(.status == "completed")]
                    | sort_by(.completed_at) | last | .conclusion // "no_completed_run"
            ')
            SENTINEL_RESULTS+=("${JOB}:${CONCLUSION}")
            if [ "$CONCLUSION" != "success" ]; then
                ANY_NON_SUCCESS=true
            fi
        done
        if [ "$ANY_NON_SUCCESS" = "false" ]; then
            SENTINELS_ALL_SUCCESS="true"
        fi
    fi
fi

# Rollback detection: target commit is already tagged (not the latest),
# usually because the user ran `git checkout android/M` on an older tag.
EXISTING_TAGS_ON_TARGET=$(git tag -l 'android/[0-9]*' --points-at "$TARGET_COMMIT" 2>/dev/null | sort -t/ -k2 -n || true)
IS_ROLLBACK="false"
NEWEST_TAG_ON_TARGET=""
if [ -n "$EXISTING_TAGS_ON_TARGET" ]; then
    NEWEST_TAG_ON_TARGET=$(echo "$EXISTING_TAGS_ON_TARGET" | tail -1)
    if [ -n "$LATEST_TAG_SHA" ] && [ "$LATEST_TAG_SHA" != "(none)" ] && [ "$LATEST_TAG_SHA" != "(missing)" ] && [ "$TARGET_COMMIT" != "$LATEST_TAG_SHA" ]; then
        IS_ROLLBACK="true"
    fi
fi

# Working tree state (only meaningful when tagging HEAD)
WORKING_TREE_CLEAN="true"
if ! git diff-index --quiet HEAD --; then
    WORKING_TREE_CLEAN="false"
fi

# ============================================================
# --check mode: print state + next command, exit
# ============================================================
if [ "$MODE" = "check" ]; then
    echo "Latest tag:   $LATEST_TAG (sha: $LATEST_TAG_SHA)"
    echo "Next tag:     $NEW_TAG"
    echo "Target:       $TARGET_SOURCE"
    echo "Full SHA:     $TARGET_COMMIT"
    echo "Branch:       $CURRENT_BRANCH"
    if [ "$WORKING_TREE_CLEAN" = "false" ] && [ -z "$CONFIRM_HASH" ]; then
        echo -e "Worktree:     ${YELLOW}DIRTY${RESET} (uncommitted changes — commit/stash first)"
    fi

    echo ""
    echo "CI on target commit:"
    if [ "$CI_FETCH_OK" != "true" ]; then
        if [ -z "$REPO" ]; then
            echo -e "  ${YELLOW}Repository not detected (gh not authenticated?). Skipping CI check.${RESET}"
        else
            echo -e "  ${YELLOW}No check_runs found for $TARGET_COMMIT_SHORT — CI may not have run yet.${RESET}"
        fi
    else
        case "$CI_AGGREGATOR_STATE" in
            success) echo -e "  ci aggregator:           ${GREEN}success${RESET}" ;;
            *)       echo -e "  ci aggregator:           ${YELLOW}${CI_AGGREGATOR_STATE}${RESET}" ;;
        esac
        for ENTRY in "${SENTINEL_RESULTS[@]}"; do
            JOB="${ENTRY%%:*}"
            CONC="${ENTRY##*:}"
            PADDED=$(printf "%-24s" "$JOB")
            if [ "$CONC" = "success" ]; then
                echo -e "  ${PADDED} ${GREEN}${CONC}${RESET}"
            else
                echo -e "  ${PADDED} ${YELLOW}${CONC}${RESET}"
            fi
        done
    fi

    if [ -n "$EXISTING_TAGS_ON_TARGET" ]; then
        echo ""
        echo "Target tags:  $(echo "$EXISTING_TAGS_ON_TARGET" | tr '\n' ' ')"
    fi

    echo ""

    # === Decide next command ===
    if [ "$IS_ROLLBACK" = "true" ]; then
        echo -e "${BOLD}Detected ROLLBACK${RESET} (target is on $NEWEST_TAG_ON_TARGET, older than $LATEST_TAG)."
        echo ""
        echo "Copy and run this command to proceed:"
        echo ""
        echo "  ./scripts/release-android.sh \\"
        echo "      --confirm-tag $NEW_TAG \\"
        echo "      --confirm-hash $TARGET_COMMIT \\"
        echo "      --confirm-rollback-from $LATEST_TAG_SHA"
        if [ "$CI_FETCH_OK" = "true" ] && [ "$SENTINELS_ALL_SUCCESS" != "true" ]; then
            echo "      --confirm-skipped-jobs $TARGET_COMMIT"
        fi
        echo ""
    elif [ "$WORKING_TREE_CLEAN" = "false" ] && [ -z "$CONFIRM_HASH" ]; then
        echo -e "${RED}Cannot release: working tree has uncommitted changes.${RESET}"
        echo "Commit or stash, then re-run --check."
        echo ""
    elif [ "$IS_DETACHED" = "true" ] && [ "$IS_ROLLBACK" != "true" ]; then
        echo -e "${YELLOW}Detached HEAD without a matching prior tag.${RESET}"
        echo "Either checkout main, or pass --confirm-hash explicitly:"
        echo ""
        echo "  ./scripts/release-android.sh \\"
        echo "      --confirm-tag $NEW_TAG \\"
        echo "      --confirm-hash $TARGET_COMMIT"
        echo ""
    elif [ "$CURRENT_BRANCH" != "main" ] && [ "$IS_DETACHED" = "false" ]; then
        echo -e "${YELLOW}Not on main (currently '$CURRENT_BRANCH').${RESET}"
        echo "If this is intentional:"
        echo ""
        echo "  ./scripts/release-android.sh --confirm-hash $TARGET_COMMIT"
        echo ""
    elif [ "$CI_FETCH_OK" != "true" ]; then
        echo -e "${YELLOW}CI has not produced check_runs for this commit yet.${RESET}"
        echo "Wait for CI to finish, or trigger it explicitly:"
        echo ""
        echo "  gh workflow run \"Battery Butler CI\" --ref main -f ci_mode=release"
        echo "  ./scripts/release-android.sh --check"
        echo ""
    elif [ "$SENTINELS_ALL_SUCCESS" != "true" ]; then
        echo -e "${YELLOW}One or more sentinel CI jobs did not succeed on this commit.${RESET}"
        echo "This usually means CI was path-filtered or ran in development mode."
        echo ""
        echo "Recommended: re-run CI in release mode on this commit:"
        echo ""
        echo "  # Confirm .github/ci-mode.txt = 'release' on main first."
        echo "  gh workflow run \"Battery Butler CI\" --ref main -f ci_mode=release"
        echo "  ./scripts/release-android.sh --check"
        echo ""
        echo "Emergency override (use only if you have manually verified the build):"
        echo ""
        echo "  ./scripts/release-android.sh \\"
        echo "      --confirm-tag $NEW_TAG \\"
        echo "      --confirm-skipped-jobs $TARGET_COMMIT"
        echo ""
    else
        echo "Copy and run this command to release:"
        echo ""
        echo "  ./scripts/release-android.sh --confirm-tag $NEW_TAG"
        echo ""
    fi

    exit 0
fi

# ============================================================
# Release mode (interactive or confirm)
# ============================================================

# Interactive mode requires a TTY — otherwise direct the user to --check.
if [ "$MODE" = "interactive" ]; then
    if [ ! -t 0 ] || [ ! -t 1 ]; then
        echo -e "${RED}Error: Interactive mode requires a TTY.${RESET}"
        echo "Run --check to see the exact command for this state:"
        echo "  ./scripts/release-android.sh --check"
        exit 1
    fi
fi

echo -e "${BOLD}=== Android Release ===${RESET}"
echo ""

# === Gate: Clean working tree (only when tagging HEAD) ===
if [ -z "$CONFIRM_HASH" ] && [ "$WORKING_TREE_CLEAN" = "false" ]; then
    echo -e "${RED}Error: Working tree has uncommitted changes.${RESET}"
    echo "Commit or stash changes before releasing."
    exit 1
fi

# === Gate: Must be on main (when tagging HEAD) — override via --confirm-hash ===
if [ -z "$CONFIRM_HASH" ] && [ "$CURRENT_BRANCH" != "main" ]; then
    echo -e "${RED}Error: Not on main branch (on '$CURRENT_BRANCH').${RESET}"
    echo ""
    echo "Run --check to see the exact command for this state:"
    echo "  ./scripts/release-android.sh --check"
    exit 1
fi

if [ -n "$CONFIRM_HASH" ]; then
    echo -e "${YELLOW}Releasing specific commit: $TARGET_COMMIT_SHORT${RESET}"
    echo "  Resolved from: $CONFIRM_HASH"
    echo "  Full SHA:      $TARGET_COMMIT"
    echo ""
fi

# === Gate: Rollback confirmation ===
# If --confirm-rollback-from is provided, validate it matches the latest tag's
# current commit. If the target looks like a rollback but this flag is not
# provided, require it (prevents accidental "rollback" by forgetting to
# re-checkout main).
if [ -n "$CONFIRM_ROLLBACK_FROM" ]; then
    if [ "$CONFIRM_ROLLBACK_FROM" != "$LATEST_TAG_SHA" ]; then
        echo -e "${RED}Error: --confirm-rollback-from does not match the latest tag's commit.${RESET}"
        echo "  Expected (sha of $LATEST_TAG): $LATEST_TAG_SHA"
        echo "  Got:                           $CONFIRM_ROLLBACK_FROM"
        echo ""
        echo "Run --check to see the correct value."
        exit 1
    fi
    echo -e "${YELLOW}Rollback confirmed: $LATEST_TAG -> $NEW_TAG at $TARGET_COMMIT_SHORT${RESET}"
    echo ""
elif [ "$IS_ROLLBACK" = "true" ]; then
    echo -e "${RED}Error: Target commit is on tag $NEWEST_TAG_ON_TARGET, older than $LATEST_TAG.${RESET}"
    echo "  This looks like a rollback but --confirm-rollback-from was not provided."
    echo ""
    echo "Run --check to see the correct command."
    exit 1
fi

# === Gate: Sentinel CI jobs ===
# Two acceptable paths:
#   A. All sentinel jobs succeeded on target commit (normal)
#   B. --confirm-skipped-jobs matches target commit (emergency)
if [ "$CI_FETCH_OK" != "true" ]; then
    if [ -n "$CONFIRM_SKIPPED_JOBS" ]; then
        if [ "$CONFIRM_SKIPPED_JOBS" != "$TARGET_COMMIT" ]; then
            echo -e "${RED}Error: --confirm-skipped-jobs SHA does not match target commit.${RESET}"
            echo "  Expected (target): $TARGET_COMMIT"
            echo "  Got:               $CONFIRM_SKIPPED_JOBS"
            exit 1
        fi
        echo -e "${YELLOW}WARNING: No CI check_runs for target — overriding via --confirm-skipped-jobs.${RESET}"
    else
        echo -e "${RED}Error: No CI check_runs found for target commit.${RESET}"
        echo "Wait for CI to finish, or trigger it explicitly:"
        echo "  gh workflow run \"Battery Butler CI\" --ref main -f ci_mode=release"
        echo "Run --check to see the next step."
        exit 1
    fi
elif [ "$SENTINELS_ALL_SUCCESS" = "true" ]; then
    echo -e "${GREEN}All sentinel CI jobs passed on $TARGET_COMMIT_SHORT.${RESET}"
    for ENTRY in "${SENTINEL_RESULTS[@]}"; do
        echo "  ✓ ${ENTRY%%:*}"
    done
elif [ -n "$CONFIRM_SKIPPED_JOBS" ]; then
    if [ "$CONFIRM_SKIPPED_JOBS" != "$TARGET_COMMIT" ]; then
        echo -e "${RED}Error: --confirm-skipped-jobs SHA does not match target commit.${RESET}"
        echo "  Expected (target): $TARGET_COMMIT"
        echo "  Got:               $CONFIRM_SKIPPED_JOBS"
        exit 1
    fi
    echo -e "${YELLOW}WARNING: Releasing with some sentinel jobs not 'success' (--confirm-skipped-jobs).${RESET}"
    for ENTRY in "${SENTINEL_RESULTS[@]}"; do
        JOB="${ENTRY%%:*}"
        CONC="${ENTRY##*:}"
        if [ "$CONC" = "success" ]; then
            echo "  ✓ $JOB: $CONC"
        else
            echo -e "  ${YELLOW}!${RESET} $JOB: $CONC"
        fi
    done
    echo "  Server-side verify-ci will also enforce this — make sure CI in 'release' mode passes before push."
    echo ""
else
    echo -e "${RED}Error: One or more sentinel CI jobs did not succeed on target commit.${RESET}"
    for ENTRY in "${SENTINEL_RESULTS[@]}"; do
        JOB="${ENTRY%%:*}"
        CONC="${ENTRY##*:}"
        if [ "$CONC" = "success" ]; then
            echo "  ✓ $JOB: $CONC"
        else
            echo -e "  ${RED}✗${RESET} $JOB: $CONC"
        fi
    done
    echo ""
    echo "This usually means CI was path-filtered or ran in development mode."
    echo "Re-run CI in release mode and try again:"
    echo "  gh workflow run \"Battery Butler CI\" --ref main"
    echo "Run --check for the full recovery flow."
    exit 1
fi

# === Existing tags warning ===
if [ -n "$EXISTING_TAGS_ON_TARGET" ]; then
    echo ""
    echo -e "${YELLOW}Warning: This commit already has android tag(s):${RESET}"
    echo "$EXISTING_TAGS_ON_TARGET"
    echo ""
    if [ "$ALLOW_TAGGED_COMMIT" = true ] || [ -n "$CONFIRM_ROLLBACK_FROM" ]; then
        echo "Continuing (override active)..."
    elif [ "$MODE" = "interactive" ]; then
        read -p "Create another tag anyway? (y/N) " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Aborted."
            exit 0
        fi
    else
        echo -e "${RED}Error: Pass --allow-tagged-commit or --confirm-rollback-from to proceed.${RESET}"
        exit 1
    fi
fi

# === Release Details ===
echo ""
echo "=== Release Details ==="
echo "  Target:     $TARGET_SOURCE"
echo "  Branch:     $CURRENT_BRANCH"
echo "  Latest tag: $LATEST_TAG ($LATEST_TAG_SHA)"
echo "  New tag:    $NEW_TAG"
echo "  Commit:     $TARGET_COMMIT_SHORT ($TARGET_COMMIT)"
echo ""

# === Confirmation ===
if [ "$MODE" = "confirm" ]; then
    if [ "$CONFIRM_TAG" != "$NEW_TAG" ]; then
        echo -e "${RED}Error: --confirm-tag does not match computed next tag.${RESET}"
        echo "  Provided: $CONFIRM_TAG"
        echo "  Expected: $NEW_TAG"
        echo ""
        echo "--confirm-tag is a safety check, not an override."
        echo "The next tag is always computed as highest existing + 1."
        exit 1
    fi
    echo "--confirm-tag matches, proceeding..."
else
    echo -e "${YELLOW}This will create tag '$NEW_TAG' and push it to origin.${RESET}"
    read -p "Proceed with release? (y/N) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi
fi

# === Dry run ===
if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}=== Dry Run ===${RESET}"
    echo "Would create tag $NEW_TAG on commit $TARGET_COMMIT_SHORT"
    echo "Would push tag to origin"
    echo "No tags were created or pushed."
    exit 0
fi

# === Create and push tag ===
echo ""
echo "Creating tag $NEW_TAG on $TARGET_COMMIT_SHORT..."
git tag "$NEW_TAG" "$TARGET_COMMIT"

echo "Pushing tag to origin..."
if git push origin "$NEW_TAG"; then
    echo ""
    echo -e "${GREEN}=== Release Initiated ===${RESET}"
    echo ""
    echo "Tag $NEW_TAG pushed to origin."
    echo "Monitor: https://github.com/cartland/battery-butler/actions/workflows/release-android.yml"
else
    echo -e "${RED}Error: Failed to push tag.${RESET}"
    echo "Removing local tag..."
    git tag -d "$NEW_TAG"
    exit 1
fi
