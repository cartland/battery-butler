#!/bin/bash
# Show current deployment status for all server environments.
#
# Usage:
#   ./scripts/deploy-status.sh
#
# Requires: aws CLI configured, git repo with commit history.

set -e
cd "$(dirname "$0")/.."

REGION="us-west-1"
ENVS=("dev" "prod")

# Column widths
printf "%-6s  %-9s  %-7s  %-12s  %s\n" "ENV" "IMAGE" "STATUS" "DEPLOYED" "COMMIT"
printf "%-6s  %-9s  %-7s  %-12s  %s\n" "------" "---------" "-------" "------------" "------"

DEV_TAG=""

for ENV in "${ENVS[@]}"; do
    CLUSTER="battery-butler-${ENV}-cluster"
    SERVICE="battery-butler-${ENV}-service"

    # Get task definition ARN from the service
    TASK_DEF=$(aws ecs describe-services \
        --cluster "$CLUSTER" \
        --services "$SERVICE" \
        --region "$REGION" \
        --query 'services[0].taskDefinition' \
        --output text 2>/dev/null) || true

    if [ -z "$TASK_DEF" ] || [ "$TASK_DEF" = "None" ]; then
        printf "%-6s  %-9s  %-7s  %-12s  %s\n" "$ENV" "-" "DOWN" "-" "(no service found)"
        continue
    fi

    # Get running/desired counts
    COUNTS=$(aws ecs describe-services \
        --cluster "$CLUSTER" \
        --services "$SERVICE" \
        --region "$REGION" \
        --query 'services[0].[runningCount,desiredCount]' \
        --output text 2>/dev/null) || true
    RUNNING=$(echo "$COUNTS" | awk '{print $1}')
    DESIRED=$(echo "$COUNTS" | awk '{print $2}')

    if [ "$RUNNING" = "0" ]; then
        STATUS="DOWN"
    elif [ "$RUNNING" = "$DESIRED" ]; then
        STATUS="OK"
    else
        STATUS="${RUNNING}/${DESIRED}"
    fi

    # Get image tag from task definition
    IMAGE=$(aws ecs describe-task-definition \
        --task-definition "$TASK_DEF" \
        --region "$REGION" \
        --query 'taskDefinition.containerDefinitions[0].image' \
        --output text 2>/dev/null) || true
    TAG=$(echo "$IMAGE" | grep -o ':[^:]*$' | tr -d ':')

    if [ "$ENV" = "dev" ]; then
        DEV_TAG="$TAG"
    fi

    # Map tag to git commit message and date
    COMMIT_MSG=""
    COMMIT_DATE=""
    if [ -n "$TAG" ] && git rev-parse --verify "$TAG" &>/dev/null; then
        COMMIT_MSG=$(git log -1 --format='%s' "$TAG" 2>/dev/null | head -c 60)
        COMMIT_DATE=$(git log -1 --format='%cd' --date=short "$TAG" 2>/dev/null)
    else
        COMMIT_MSG="(commit not in local history)"
        COMMIT_DATE="-"
    fi

    printf "%-6s  %-9s  %-7s  %-12s  %s\n" "$ENV" "$TAG" "$STATUS" "$COMMIT_DATE" "$COMMIT_MSG"
done

# Compare dev vs prod
if [ -n "$DEV_TAG" ]; then
    echo ""
    for ENV in "${ENVS[@]}"; do
        [ "$ENV" = "dev" ] && continue

        CLUSTER="battery-butler-${ENV}-cluster"
        SERVICE="battery-butler-${ENV}-service"
        TASK_DEF=$(aws ecs describe-services \
            --cluster "$CLUSTER" \
            --services "$SERVICE" \
            --region "$REGION" \
            --query 'services[0].taskDefinition' \
            --output text 2>/dev/null) || true
        [ -z "$TASK_DEF" ] || [ "$TASK_DEF" = "None" ] && continue

        IMAGE=$(aws ecs describe-task-definition \
            --task-definition "$TASK_DEF" \
            --region "$REGION" \
            --query 'taskDefinition.containerDefinitions[0].image' \
            --output text 2>/dev/null) || true
        ENV_TAG=$(echo "$IMAGE" | grep -o ':[^:]*$' | tr -d ':')

        if [ "$ENV_TAG" = "$DEV_TAG" ]; then
            echo "$ENV is up to date with dev ($DEV_TAG)"
        elif git rev-parse --verify "$ENV_TAG" &>/dev/null && git rev-parse --verify "$DEV_TAG" &>/dev/null; then
            BEHIND=$(git rev-list --count "$ENV_TAG".."$DEV_TAG" -- server/ 2>/dev/null || echo "?")
            if [ "$BEHIND" != "0" ] && [ "$BEHIND" != "?" ]; then
                echo "WARNING: $ENV ($ENV_TAG) is $BEHIND server commit(s) behind dev ($DEV_TAG)"
            elif [ "$BEHIND" = "0" ]; then
                echo "$ENV ($ENV_TAG) has no server changes vs dev ($DEV_TAG)"
            fi
        fi
    done
fi
