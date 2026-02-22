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
CONFIRM_RELEASE=false
CONFIRM_HASH=""
DRY_RUN=false

show_help() {
    echo "Usage: ./scripts/release-android.sh [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --allow-duplicate-tag      Skip prompt when commit already has an android/* tag"
    echo "  --confirm-release          Skip final release confirmation prompt"
    echo "  --confirm-hash <sha>       Required when releasing from a non-main branch."
    echo "                             Must match HEAD's full SHA as a safety confirmation."
    echo "  --dry-run                  Show what would happen without creating or pushing tags"
    echo "  -h, --help                 Show this help message"
    echo ""
    echo "For fully non-interactive mode, use both flags:"
    echo "  ./scripts/release-android.sh --allow-duplicate-tag --confirm-release"
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
        --confirm-release)
            CONFIRM_RELEASE=true
            shift
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
if [ "$CONFIRM_RELEASE" = true ]; then
    echo "--confirm-release specified, proceeding..."
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
