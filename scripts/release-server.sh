#!/bin/bash
set -e
cd "$(dirname "$0")/.."

# Releases Server to AWS by creating and pushing a server/vX.Y.Z tag.
# This triggers the release-server.yml GitHub Action workflow.
#
# Usage: ./scripts/release-server.sh [OPTIONS]
#
# Options:
#   --allow-duplicate-tag  Skip prompt when commit already has a server/* tag
#   --confirm-release      Skip final release confirmation prompt
#   -h, --help             Show this help message
#
# The script will:
# 1. Fetch all tags from origin
# 2. Find the highest existing server/vX.Y.Z tag version
# 3. Prompt for version bump type (major/minor/patch)
# 4. Create a new tag with the incremented version
# 5. Push the tag to trigger the release workflow

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse flags
ALLOW_DUPLICATE_TAG=false
CONFIRM_RELEASE=false

show_help() {
    echo "Usage: ./scripts/release-server.sh [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --allow-duplicate-tag  Skip prompt when commit already has a server/* tag"
    echo "  --confirm-release      Skip final release confirmation prompt"
    echo "  -h, --help             Show this help message"
    echo ""
    echo "For fully non-interactive mode, use both flags:"
    echo "  ./scripts/release-server.sh --allow-duplicate-tag --confirm-release"
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

echo "=== Server Release Script ==="
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

# Find the highest existing server/vX.Y.Z tag version
# Tags are in format server/v1.0.0, server/v1.0.1, etc.
HIGHEST_TAG=$(git tag -l 'server/v[0-9]*.[0-9]*.[0-9]*' | \
    sed 's|server/v||' | \
    sort -t. -k1,1n -k2,2n -k3,3n | \
    tail -1)

if [ -z "$HIGHEST_TAG" ]; then
    CURRENT_MAJOR=0
    CURRENT_MINOR=0
    CURRENT_PATCH=0
    echo "No existing server/* tags found. Starting at version 1.0.0."
else
    CURRENT_MAJOR=$(echo "$HIGHEST_TAG" | cut -d. -f1)
    CURRENT_MINOR=$(echo "$HIGHEST_TAG" | cut -d. -f2)
    CURRENT_PATCH=$(echo "$HIGHEST_TAG" | cut -d. -f3)
    echo "Current version: server/v$HIGHEST_TAG"
fi

# Calculate next versions
NEXT_PATCH="$CURRENT_MAJOR.$CURRENT_MINOR.$((CURRENT_PATCH + 1))"
NEXT_MINOR="$CURRENT_MAJOR.$((CURRENT_MINOR + 1)).0"
NEXT_MAJOR="$((CURRENT_MAJOR + 1)).0.0"

echo ""
echo "Select version bump type:"
echo "  1) Patch ($NEXT_PATCH) - Bug fixes, minor changes"
echo "  2) Minor ($NEXT_MINOR) - New features, backwards compatible"
echo "  3) Major ($NEXT_MAJOR) - Breaking changes"
echo ""

read -p "Enter choice (1/2/3): " -n 1 -r VERSION_CHOICE
echo ""

case $VERSION_CHOICE in
    1) NEW_VERSION="$NEXT_PATCH" ;;
    2) NEW_VERSION="$NEXT_MINOR" ;;
    3) NEW_VERSION="$NEXT_MAJOR" ;;
    *)
        echo -e "${RED}Invalid choice. Aborting.${NC}"
        exit 1
        ;;
esac

NEW_TAG="server/v$NEW_VERSION"
CURRENT_COMMIT=$(git rev-parse HEAD)
CURRENT_COMMIT_SHORT=$(git rev-parse --short HEAD)

echo ""
echo "=== Release Details ==="
echo "  New tag:    $NEW_TAG"
echo "  Commit:     $CURRENT_COMMIT_SHORT"
echo "  Full SHA:   $CURRENT_COMMIT"
echo ""

# Check if this commit already has a server/* tag
EXISTING_TAGS=$(git tag --points-at HEAD | grep -E '^server/v[0-9]+\.[0-9]+\.[0-9]+$' || true)
if [ -n "$EXISTING_TAGS" ]; then
    echo -e "${YELLOW}Warning: This commit already has server tag(s):${NC}"
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
echo "This triggers the AWS server deployment workflow."
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
    echo "  https://github.com/cartland/battery-butler/actions/workflows/release-server.yml"
    echo ""
else
    echo -e "${RED}Failed to push tag. Cleaning up local tag...${NC}"
    git tag -d "$NEW_TAG"
    exit 1
fi
