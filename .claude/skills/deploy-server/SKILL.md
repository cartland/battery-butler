---
description: Deploy the gRPC server to AWS using Terraform and Jib, or trigger deployment via release tag.
allowed-tools: Bash(*), Read, Glob, Grep
---

# Deploy Server

Deploy the gRPC server to AWS using Terraform and Jib.

> [!CAUTION]
> **Agent Rule:** NEVER run deployment commands without explicit user permission.
> Always confirm the deployment target and get user approval before proceeding.

## Prerequisites

- AWS CLI configured (`aws configure`)
- Terraform installed
- GitHub secrets configured (see `/setup-aws`)

## Steps

### Option A: Via Release Tag (Recommended)

1. Check current deployment state:
   ```bash
   ./scripts/deploy-status.sh
   ```

2. Run the release script:
   ```bash
   ./scripts/release-server.sh --allow-duplicate-tag --confirm-release
   ```

3. Monitor the release workflow:
   ```bash
   gh run list --workflow=release-server.yml --limit 1
   ```

### Option B: Manual Terraform Deploy

1. Navigate to Terraform directory:
   ```bash
   cd server/terraform
   ```

2. Initialize Terraform (first time only):
   ```bash
   terraform init
   ```

3. Review planned changes:
   ```bash
   terraform plan
   ```

4. Apply infrastructure changes:
   ```bash
   terraform apply
   ```

   > [!WARNING]
   > **Production Safety:**
   > Ensure you are on the `main` branch or a verified release branch before applying changes to production.

5. Build and push container to ECR:
   ```bash
   ./gradlew :server:app:jib
   ```

6. Force ECS to deploy new version:
   ```bash
   aws ecs update-service --cluster battery-butler-cluster --service battery-butler-service --force-new-deployment
   ```

## Automated Deployment

The server is automatically deployed via GitHub Actions when changes are pushed to main (see `.github/workflows/deploy-server.yml`).

## Notes

- The infrastructure includes: VPC, ECS Fargate, RDS PostgreSQL, ALB, ECR
- Terraform state is stored in S3 with DynamoDB locking
- Container is built using Jib (no Docker daemon required)
- Use `./scripts/deploy-status.sh` to check what's deployed across environments
