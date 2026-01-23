#!/bin/bash
set -e

echo "Retrieving Database Credentials from AWS Secrets Manager..."

# 1. Find the Secret ARN
# We search for a secret name starting with 'battery-butler-db-credentials'
SECRET_ARN=$(aws secretsmanager list-secrets \
    --query "SecretList[?starts_with(Name, 'battery-butler-db-credentials')].ARN | [0]" \
    --output text \
    --region us-west-1)

if [ "$SECRET_ARN" == "None" ]; then
    echo "Error: Could not find a secret starting with 'battery-butler-db-credentials' in us-west-1."
    echo "Make sure you are logged in (aws sso login) and have the correct permissions."
    exit 1
fi

echo "Found Secret: $SECRET_ARN"

# 2. Get the Secret Value
SECRET_JSON=$(aws secretsmanager get-secret-value \
    --secret-id "$SECRET_ARN" \
    --query "SecretString" \
    --output text \
    --region us-west-1)

# 3. Parse and Display (using python for robust JSON parsing without jq dependency)
echo ""
echo "=========================================="
echo "          DATABASE CONNECTION INFO        "
echo "=========================================="
echo "$SECRET_JSON" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f'Host:     {data.get(\"host\")}')
print(f'Port:     {data.get(\"port\")}')
print(f'Database: {data.get(\"dbname\")}')
print(f'Username: {data.get(\"username\")}')
print(f'Password: {data.get(\"password\")}')
"
echo "=========================================="
echo ""

# 4. Security Group Info
SG_ID=$(aws ec2 describe-security-groups \
    --filters Name=group-name,Values=battery-butler-db-sg \
    --query "SecurityGroups[0].GroupId" \
    --output text \
    --region us-west-1)

echo "IMPORTANT: To connect, your IP must be whitelisted."
echo "Security Group ID: $SG_ID"
echo ""
echo "To whitelist your current IP, run:"
echo "  MY_IP=\$(curl -s http://checkip.amazonaws.com)"
echo "  aws ec2 authorize-security-group-ingress --group-id $SG_ID --protocol tcp --port 5432 --cidr \${MY_IP}/32 --region us-west-1"
echo ""
