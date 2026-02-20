#!/bin/bash
# Promote server image from dev to prod.
#
# ENFORCEMENT: Production deployments MUST go through this script.
# This script guarantees:
#   - The image being promoted is actually running on dev
#   - Dev is healthy (running count == desired count)
#   - No-op promotions are flagged (dev == prod already)
#
# Usage:
#   ./scripts/promote-server.sh              # Promotes current dev image to prod
#   ./scripts/promote-server.sh <image_tag>  # Promotes a specific image tag to prod
#   ./scripts/promote-server.sh --confirm    # Skip confirmation prompt
#
# The script will:
# 1. Verify dev is healthy
# 2. Identify the dev image tag (or validate the provided one is on dev)
# 3. Check if promotion is needed (dev != prod)
# 4. Trigger the server-deploy-prod.yml workflow
# 5. Print the approval URL

set -e
cd "$(dirname "$0")/.."

REGION="us-west-1"
DEV_CLUSTER="battery-butler-dev-cluster"
DEV_SERVICE="battery-butler-dev-service"
PROD_CLUSTER="battery-butler-prod-cluster"
PROD_SERVICE="battery-butler-prod-service"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
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
            echo "Promotes the dev server image to production."
            echo "This is the ONLY supported path to production."
            echo ""
            echo "Options:"
            echo "  --confirm   Skip confirmation prompt"
            echo "  -h, --help  Show this help message"
            echo ""
            echo "If no image_tag is provided, the current dev image tag is used."
            echo "If an image_tag is provided, it must match what's running on dev."
            exit 0
            ;;
        *)
            IMAGE_TAG="$1"
            shift
            ;;
    esac
done

echo "================================================"
echo "  SERVER PROMOTION — DEV → PROD"
echo "  This is the only path to production."
echo "================================================"
echo ""

# --- Step 1: Verify dev is healthy ---

echo -e "${CYAN}Checking dev environment health...${NC}"

DEV_TASK_DEF=$(aws ecs describe-services \
    --cluster "$DEV_CLUSTER" \
    --services "$DEV_SERVICE" \
    --region "$REGION" \
    --query 'services[0].taskDefinition' \
    --output text 2>/dev/null) || true

if [ -z "$DEV_TASK_DEF" ] || [ "$DEV_TASK_DEF" = "None" ]; then
    echo -e "${RED}BLOCKED: Dev service not found.${NC}"
    echo "Cannot promote — dev must be running. Deploy to dev first:"
    echo "  ./scripts/release-server.sh"
    exit 1
fi

DEV_COUNTS=$(aws ecs describe-services \
    --cluster "$DEV_CLUSTER" \
    --services "$DEV_SERVICE" \
    --region "$REGION" \
    --query 'services[0].[runningCount,desiredCount]' \
    --output text 2>/dev/null) || true
DEV_RUNNING=$(echo "$DEV_COUNTS" | awk '{print $1}')
DEV_DESIRED=$(echo "$DEV_COUNTS" | awk '{print $2}')

if [ "$DEV_RUNNING" = "0" ]; then
    echo -e "${RED}BLOCKED: Dev service is DOWN (0 running tasks).${NC}"
    echo "Cannot promote an unhealthy dev environment."
    echo "Check dev status: ./scripts/deploy-status.sh"
    exit 1
fi

if [ "$DEV_RUNNING" != "$DEV_DESIRED" ]; then
    echo -e "${YELLOW}WARNING: Dev is unstable ($DEV_RUNNING/$DEV_DESIRED tasks running).${NC}"
    echo "Dev may be in the middle of a deployment."
    if [ "$CONFIRM" != true ]; then
        read -p "Continue anyway? (y/N) " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Aborted. Wait for dev to stabilize."
            exit 0
        fi
    else
        echo "--confirm specified, continuing despite unstable dev..."
    fi
fi

echo -e "${GREEN}Dev is healthy ($DEV_RUNNING/$DEV_DESIRED tasks running).${NC}"
echo ""

# --- Step 2: Get dev image tag ---

DEV_IMAGE=$(aws ecs describe-task-definition \
    --task-definition "$DEV_TASK_DEF" \
    --region "$REGION" \
    --query 'taskDefinition.containerDefinitions[0].image' \
    --output text 2>/dev/null) || true
DEV_IMAGE_TAG=$(echo "$DEV_IMAGE" | grep -o ':[^:]*$' | tr -d ':')

if [ -z "$DEV_IMAGE_TAG" ]; then
    echo -e "${RED}BLOCKED: Could not determine dev image tag.${NC}"
    exit 1
fi

# --- Step 3: Validate provided image tag against dev ---

if [ -n "$IMAGE_TAG" ]; then
    if [ "$IMAGE_TAG" != "$DEV_IMAGE_TAG" ]; then
        echo -e "${RED}BLOCKED: Provided image tag does not match dev.${NC}"
        echo ""
        echo "  Provided tag:  $IMAGE_TAG"
        echo "  Dev is running: $DEV_IMAGE_TAG"
        echo ""
        echo "Production must run an image that was validated on dev."
        echo "Either:"
        echo "  1. Deploy $IMAGE_TAG to dev first: ./scripts/release-server.sh"
        echo "  2. Promote the current dev image: ./scripts/promote-server.sh (no tag argument)"
        exit 1
    fi
else
    IMAGE_TAG="$DEV_IMAGE_TAG"
fi

# --- Step 4: Check if promotion is needed ---

echo -e "${CYAN}Checking current production state...${NC}"

PROD_TASK_DEF=$(aws ecs describe-services \
    --cluster "$PROD_CLUSTER" \
    --services "$PROD_SERVICE" \
    --region "$REGION" \
    --query 'services[0].taskDefinition' \
    --output text 2>/dev/null) || true

PROD_IMAGE_TAG=""
if [ -n "$PROD_TASK_DEF" ] && [ "$PROD_TASK_DEF" != "None" ]; then
    PROD_IMAGE=$(aws ecs describe-task-definition \
        --task-definition "$PROD_TASK_DEF" \
        --region "$REGION" \
        --query 'taskDefinition.containerDefinitions[0].image' \
        --output text 2>/dev/null) || true
    PROD_IMAGE_TAG=$(echo "$PROD_IMAGE" | grep -o ':[^:]*$' | tr -d ':')
fi

if [ "$PROD_IMAGE_TAG" = "$IMAGE_TAG" ]; then
    echo -e "${GREEN}Production is already running $IMAGE_TAG — no promotion needed.${NC}"
    echo ""
    echo "Dev and prod are in sync."
    exit 0
fi

# --- Step 5: Show promotion details ---

echo ""
echo "=== Promotion Details ==="
echo "  Image tag:    $IMAGE_TAG"

COMMIT_MSG=""
if git rev-parse --verify "$IMAGE_TAG" &>/dev/null; then
    COMMIT_MSG=$(git log -1 --format='%s' "$IMAGE_TAG" 2>/dev/null | head -c 70)
fi
[ -n "$COMMIT_MSG" ] && echo "  Commit:       $COMMIT_MSG"

echo "  Dev status:   running ($DEV_RUNNING/$DEV_DESIRED)"
if [ -n "$PROD_IMAGE_TAG" ]; then
    echo "  Current prod: $PROD_IMAGE_TAG"
else
    echo "  Current prod: (not running or not found)"
fi
echo "  Target:       production"
echo ""

# --- Step 6: Confirm ---

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

# --- Step 7: Trigger the workflow ---

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
