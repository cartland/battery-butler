# AWS Configuration Guide for Battery Butler Server

Since we deploy via **GitHub Actions**, Terraform cannot store its state locally on the runner. We use a **Remote Backend** with AWS S3 and DynamoDB.

## Step 1: Create IAM User for GitHub Actions

1. Log in to the **AWS Console**.
2. Go to **IAM** > **Users** > **Create user**.
3. Name: `github-actions-deployer` (or similar).
4. **Permissions**:
   - Create a custom policy using the JSON from `server/iam_policy.json`.
   - This policy covers: EC2/VPC, ECS/Fargate, ECR, RDS, Secrets Manager, IAM roles, CloudWatch Logs, S3/DynamoDB (Terraform state).
   - **Important**: When updating `iam_policy.json`, you must manually update the policy in AWS Console.
5. Create an **Access Key**:
   - Go to the user > **Security credentials**.
   - **Create access key** > **CLI**.
   - **Save** the `Access Key ID` and `Secret Access Key`.

## Step 2: Create S3 Bucket for Terraform State

1. Go to **S3** > **Create bucket**.
2. Name: `battery-butler-tf-state-<unique-suffix>` (e.g., `battery-butler-tf-state-cartland-dev`).
   - *Note: Bucket names must be globally unique.*
3. Region: `us-west-1` (Must match your Terraform region).
4. **Bucket Versioning**: Enable (Important for state recovery).
5. **Create bucket**.

## Step 3: Create DynamoDB Table for State Locking

1. Go to **DynamoDB** > **Tables** > **Create table**.
2. Name: `battery-butler-tf-lock`.
3. Partition key: `LockID` (String). *Case sensitive, exact match required.*
4. **Create table**.

**Lock key format**: `{bucket-name}/{state-key}` (e.g., `battery-butler-tf-state-cartland-dev/server/dev/terraform.tfstate`).

## Step 4: Configure GitHub Secrets

1. Go to your GitHub Repository settings.
2. **Settings** > **Secrets and variables** > **Actions**.
3. Add the following **Repository secrets**:

| Secret | Description |
|:---|:---|
| `AWS_ACCESS_KEY_ID` | IAM User Access Key (from Step 1) |
| `AWS_SECRET_ACCESS_KEY` | IAM User Secret Key (from Step 1) |
| `TF_STATE_BUCKET` | S3 bucket name (from Step 2) |
| `TF_LOCK_TABLE` | DynamoDB table name (from Step 3) |

*Optional*: `AWS_DEPLOY_ROLE_ARN` for OIDC authentication (more secure than access keys).

## Step 5: Configure GitHub Environments

Go to **Settings** > **Environments** and create:

| Environment | Required Reviewers | Wait Timer | Branch Policy |
|-------------|-------------------|------------|---------------|
| `dev` | None | 0 min | All branches |
| `staging` | None | 0 min | main only |
| `production` | 1+ required | 5 min | main only |

## Step 6: Create ECR Repository (First Time Only)

The ECR repository is created automatically by the build workflow if it doesn't exist. No manual setup needed.

If you want to create it manually:
```bash
aws ecr create-repository \
  --repository-name battery-butler-server \
  --image-tag-mutability MUTABLE \
  --region us-west-1
```

## Troubleshooting

### Terraform State Lock Stuck

If a CI run is cancelled or fails mid-apply, the DynamoDB lock may not be released:

```bash
# Find the lock (key format: {bucket}/{state-key})
aws dynamodb delete-item \
  --table-name battery-butler-tf-lock \
  --key '{"LockID":{"S":"battery-butler-tf-state-cartland-dev/server/dev/terraform.tfstate"}}'
```

### Free-Tier Limitations

- Only `db.t3.micro` RDS instances are allowed
- Maximum 2 RDS instances total
- Cannot run dev + staging + prod simultaneously
- Use `server-destroy.yml` to tear down unused environments

### IAM Permission Errors

If Terraform fails with an access denied error:
1. Check which permission is missing in the error message
2. Add it to `server/iam_policy.json`
3. Update the policy in AWS Console manually
4. Re-run the workflow
