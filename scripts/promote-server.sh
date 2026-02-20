#!/bin/bash
# Promote server image from dev to prod.
#
# Usage:
#   ./scripts/promote-server.sh              # Promotes current dev image to prod
#   ./scripts/promote-server.sh <image_tag>  # Promotes a specific image tag to prod
#   ./scripts/promote-server.sh --confirm    # Skip confirmation prompt
#
# The script will:
# 1. Show current deployment status
# 2. Identify the dev image tag (or use the provided one)
# 3. Trigger the server-deploy-prod.yml workflow
# 4. Open the approval URL in the browser (if available)

set -e
cd "$(dirname "$0")/.."

REGION="us-west-1"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Parse flags
CONFIRM=false
IMAGE_TAG=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --confirm)
            CONFIRM=true
            shift
            ;;
        -h|--help)
            echo "Usage: ./scripts/promote-server.sh [OPTIONS] [image_tag]"
            echo ""
            echo "Options:"
            echo "  --confirm   Skip confirmation prompt"
            echo "  -h, --help  Show this help message"
            echo ""
            echo "If no image_tag is provided, the current dev image tag is used."
            exit 0
            ;;
        *)
            IMAGE_TAG="$1"
            shift
            ;;
    esac
done

echo "=== Server Promotion: Dev → Prod ==="
echo ""

# Show current status
./scripts/deploy-status.sh
echo ""

# Get dev image tag if not provided
if [ -z "$IMAGE_TAG" ]; then
    CLUSTER="battery-butler-dev-cluster"
    SERVICE="battery-butler-dev-service"

    TASK_DEF=$(aws ecs describe-services \
        --cluster "$CLUSTER" \
        --services "$SERVICE" \
        --region "$REGION" \
        --query 'services[0].taskDefinition' \
        --output text 2>/dev/null) || true

    if [ -z "$TASK_DEF" ] || [ "$TASK_DEF" = "None" ]; then
        echo -e "${RED}Error: Could not find dev service.${NC}"
        exit 1
    fi

    IMAGE=$(aws ecs describe-task-definition \
        --task-definition "$TASK_DEF" \
        --region "$REGION" \
        --query 'taskDefinition.containerDefinitions[0].image' \
        --output text 2>/dev/null) || true
    IMAGE_TAG=$(echo "$IMAGE" | grep -o ':[^:]*$' | tr -d ':')

    if [ -z "$IMAGE_TAG" ]; then
        echo -e "${RED}Error: Could not determine dev image tag.${NC}"
        exit 1
    fi
fi

# Show commit info for the image tag
COMMIT_MSG=""
if git rev-parse --verify "$IMAGE_TAG" &>/dev/null; then
    COMMIT_MSG=$(git log -1 --format='%s' "$IMAGE_TAG" 2>/dev/null | head -c 70)
fi

echo "=== Promotion Details ==="
echo "  Image tag:  $IMAGE_TAG"
[ -n "$COMMIT_MSG" ] && echo "  Commit:     $COMMIT_MSG"
echo "  Target:     production"
echo ""

# Confirm
if [ "$CONFIRM" = true ]; then
    echo "--confirm specified, proceeding..."
else
    echo -e "${YELLOW}This will deploy image $IMAGE_TAG to production.${NC}"
    echo "You will need to approve the deployment in GitHub Actions."
    echo ""
    read -p "Trigger production deployment? (y/N) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi
fi

# Trigger the workflow
echo ""
echo "Triggering production deployment workflow..."
if gh workflow run server-deploy-prod.yml -f image_tag="$IMAGE_TAG"; then
    echo ""
    echo -e "${GREEN}=== Deployment Triggered ===${NC}"
    echo ""
    echo "Image $IMAGE_TAG will be deployed to production after approval."
    echo ""
    echo "Approve the deployment at:"
    echo "  https://github.com/cartland/battery-butler/actions/workflows/server-deploy-prod.yml"
    echo ""
else
    echo -e "${RED}Failed to trigger deployment workflow.${NC}"
    exit 1
fi
