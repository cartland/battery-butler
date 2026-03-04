#!/bin/bash
set -e
cd "$(dirname "$0")/.."

# Releases Android app to internal testing by creating and pushing an android/N tag.
# This triggers the release-android.yml GitHub Action workflow.
#
# Usage: ./scripts/release-android.sh
#
# The script will:
# 1. Fetch all tags from origin
# 2. Find the highest existing android/N tag number
# 3. Create a new tag with N+1 (or android/1 if no tags exist)
# 4. Push the tag to trigger the release workflow

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse flags
ALLOW_DUPLICATE_TAG=false
CONFIRM_TAG=""
CONFIRM_HASH=""
DRY_RUN=false

show_help() {
    echo "Usage: ./scripts/release-android.sh [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --allow-duplicate-tag      Skip prompt when commit already has an android/* tag"
    echo "  --confirm-tag <tag>        Skip final release confirmation prompt."
    echo "                             The value must exactly match the next computed tag"
    echo "                             (e.g., android/18). This is a safety check — the"
    echo "                             argument cannot change which tag is created."
    echo "  --confirm-hash <sha>       Required when releasing from a non-main branch."
    echo "                             Must match HEAD's full SHA as a safety confirmation."
    echo "  --dry-run                  Show what would happen without creating or pushing tags"
    echo "  -h, --help                 Show this help message"
    echo ""
    echo "For fully non-interactive mode, use both flags:"
    echo "  ./scripts/release-android.sh --allow-duplicate-tag --confirm-tag android/18"
    echo ""
    echo "Releasing from a non-main branch:"
    echo "  ./scripts/release-android.sh --confirm-hash \$(git rev-parse HEAD)"
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --allow-duplicate-tag)
            ALLOW_DUPLICATE_TAG=true
            shift
            ;;
        --confirm-tag)
            CONFIRM_TAG="$2"
            shift 2
            ;;
        --confirm-hash)
            CONFIRM_HASH="$2"
            shift 2
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
            echo -e "${RED}Error: Unknown option: $1${NC}"
            echo ""
            show_help
            exit 1
            ;;
    esac
done

echo "=== Android Release Script ==="
echo ""

# Check CI status on HEAD before proceeding
echo "Checking CI status on HEAD..."
REPO=$(gh repo view --json nameWithOwner --jq '.nameWithOwner' 2>/dev/null || echo "")
if [ -n "$REPO" ]; then
    CI_CONCLUSION=$(gh api "repos/${REPO}/commits/$(git rev-parse HEAD)/check-runs" \
        --jq '.check_runs[] | select(.name == "ci") | .conclusion' 2>/dev/null || echo "")
    if [ -z "$CI_CONCLUSION" ]; then
        echo -e "${YELLOW}Warning: Could not find CI check status for HEAD.${NC}"
        echo "CI may not have run yet on this commit."
        if [ "$DRY_RUN" != true ]; then
            read -p "Continue anyway? (y/N) " -n 1 -r
            echo ""
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                echo "Aborted. Run CI first or wait for it to complete."
                exit 1
            fi
        fi
    elif [ "$CI_CONCLUSION" != "success" ]; then
        echo -e "${RED}Error: CI check on HEAD has conclusion '$CI_CONCLUSION' (expected 'success').${NC}"
        echo "Cannot release from a commit where CI did not pass."
        echo "Wait for CI to pass or fix the failures first."
        exit 1
    else
        echo -e "${GREEN}CI passed on HEAD.${NC}"
    fi
else
    echo -e "${YELLOW}Warning: Could not determine repository. Skipping CI check.${NC}"
fi
echo ""

# Ensure we're on a clean working tree
if ! git diff-index --quiet HEAD --; then
    echo -e "${RED}Error: Working tree has uncommitted changes.${NC}"
    echo "Please commit or stash your changes before releasing."
    exit 1
fi

# Fetch all tags from origin to ensure we have the latest
echo "Fetching tags from origin..."
git fetch origin --tags

# Find the highest existing android/N tag number
# Tags can be in format android/1, android/2, etc.
HIGHEST_VERSION=$(git tag -l 'android/[0-9]*' | \
    sed 's|android/||' | \
    grep -E '^[0-9]+$' | \
    sort -n | \
    tail -1)

if [ -z "$HIGHEST_VERSION" ]; then
    NEXT_VERSION=1
    echo "No existing android/* tags found. Starting at version 1."
else
    NEXT_VERSION=$((HIGHEST_VERSION + 1))
    echo "Highest existing version: android/$HIGHEST_VERSION"
fi

NEW_TAG="android/$NEXT_VERSION"
CURRENT_COMMIT=$(git rev-parse HEAD)
CURRENT_COMMIT_SHORT=$(git rev-parse --short HEAD)

# Detect current branch
CURRENT_BRANCH=$(git branch --show-current)
if [ -z "$CURRENT_BRANCH" ]; then
    CURRENT_BRANCH="(detached HEAD)"
fi

# Branch safety gate
if [ "$CURRENT_BRANCH" != "main" ]; then
    if [ -z "$CONFIRM_HASH" ]; then
        echo -e "${RED}Error: Not on main branch (currently on '$CURRENT_BRANCH').${NC}"
        echo ""
        echo "Releasing from a non-main branch requires explicit confirmation."
        echo "If you intended this, re-run with:"
        echo ""
        echo "  ./scripts/release-android.sh --confirm-hash $(git rev-parse HEAD)"
        echo ""
        exit 1
    elif [ "$CONFIRM_HASH" != "$CURRENT_COMMIT" ]; then
        echo -e "${RED}Error: --confirm-hash does not match HEAD.${NC}"
        echo "  Provided: $CONFIRM_HASH"
        echo "  HEAD:     $CURRENT_COMMIT"
        exit 1
    else
        echo -e "${YELLOW}Warning: Releasing from non-main branch '$CURRENT_BRANCH'.${NC}"
        echo "  --confirm-hash matches HEAD, proceeding."
        echo ""
    fi
fi

echo ""
echo "=== Release Details ==="
echo "  Branch:     $CURRENT_BRANCH"
echo "  New tag:    $NEW_TAG"
echo "  Commit:     $CURRENT_COMMIT_SHORT"
echo "  Full SHA:   $CURRENT_COMMIT"
echo ""

# Check if this commit already has an android/* tag
EXISTING_TAGS=$(git tag --points-at HEAD | grep -E '^android/[0-9]+$' || true)
if [ -n "$EXISTING_TAGS" ]; then
    echo -e "${YELLOW}Warning: This commit already has android tag(s):${NC}"
    echo "$EXISTING_TAGS"
    echo ""
    if [ "$ALLOW_DUPLICATE_TAG" = true ]; then
        echo "--allow-duplicate-tag specified, continuing..."
    else
        read -p "Do you want to create another tag anyway? (y/N) " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Aborted."
            exit 0
        fi
    fi
fi

# Confirm before proceeding
echo -e "${YELLOW}This will create tag '$NEW_TAG' and push it to origin.${NC}"
echo "This triggers the Play Store internal release workflow."
echo ""
if [ -n "$CONFIRM_TAG" ]; then
    # --confirm-tag is a safety check only — it cannot change which tag is created.
    # The script always computes the next tag independently. The argument must match.
    if [ "$CONFIRM_TAG" != "$NEW_TAG" ]; then
        echo -e "${RED}Error: --confirm-tag does not match the next computed tag.${NC}"
        echo "  Provided: $CONFIRM_TAG"
        echo "  Expected: $NEW_TAG"
        echo ""
        echo "The --confirm-tag argument is a safety confirmation, not an override."
        echo "The next tag is always computed as the highest existing tag + 1."
        exit 1
    fi
    echo "--confirm-tag $CONFIRM_TAG matches computed tag, proceeding..."
else
    read -p "Proceed with release? (y/N) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi
fi

if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}=== Dry Run ===${NC}"
    echo "Would create tag $NEW_TAG on commit $CURRENT_COMMIT_SHORT"
    echo "Would push tag to origin"
    echo ""
    echo "No tags were created or pushed."
    exit 0
fi

# Create the tag
echo ""
echo "Creating tag $NEW_TAG..."
git tag "$NEW_TAG"

# Push the tag
echo "Pushing tag to origin..."
if git push origin "$NEW_TAG"; then
    echo ""
    echo -e "${GREEN}=== Release Initiated ===${NC}"
    echo ""
    echo "Tag $NEW_TAG has been pushed."
    echo ""
    echo "Monitor the release workflow at:"
    echo "  https://github.com/cartland/battery-butler/actions/workflows/release-android.yml"
    echo ""
else
    echo -e "${RED}Failed to push tag. Cleaning up local tag...${NC}"
    git tag -d "$NEW_TAG"
    exit 1
fi
