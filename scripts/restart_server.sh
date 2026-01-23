#!/bin/bash
set -e

CLUSTER_NAME="battery-butler-cluster"
SERVICE_NAME="battery-butler-service"
REGION="us-west-1"

echo "Restarting AWS Server..."
echo "Cluster: $CLUSTER_NAME"
echo "Service: $SERVICE_NAME"
echo "Region:  $REGION"
echo ""

# Force a new deployment, which spins up a new task and drains the old one
aws ecs update-service \
    --cluster "$CLUSTER_NAME" \
    --service "$SERVICE_NAME" \
    --force-new-deployment \
    --region "$REGION" \
    --output json | grep "serviceArn"

echo ""
echo "Restart initiated successfully."
echo "Note: It may take 1-2 minutes for the new task to start and the old one to stop."
