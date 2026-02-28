---
description: Deploy the gRPC server to the dev environment. Use /promote-server for production.
allowed-tools: Bash(*), Read, Glob, Grep
---

# Deploy Server (Dev Only)

Deploy the gRPC server to the **dev** environment.

> [!CAUTION]
> **Agent Rule:** NEVER run deployment commands without explicit user permission.
> Always confirm the deployment target and get user approval before proceeding.
>
> **All deploys go to dev first. NEVER deploy directly to prod.**
> To promote to prod, use `/promote-server` after validating on dev.

> **HIBERNATED (Feb 2026):** AWS infrastructure is not running. All deploy
> workflows are disabled (`if: false`). The server runs locally only.
> To re-enable, see the checklist in `server/README.md`.

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

### Option B: Push to Main (Auto-Deploy)

Server changes pushed to `main` are automatically deployed to dev via `server-build.yml`.

### Option C: Manual Terraform Deploy

1. Initialize Terraform (first time only):
   ```bash
   cd server/terraform && terraform init
   ```

2. Review and apply:
   ```bash
   terraform plan && terraform apply
   ```

3. Build and push container to ECR:
   ```bash
   ./gradlew :server:app:jib
   ```

4. Force ECS to deploy new version:
   ```bash
   aws ecs update-service --cluster battery-butler-dev-cluster --service battery-butler-dev-service --force-new-deployment --region us-west-1
   ```

## After Deploying to Dev

1. Verify dev is healthy:
   ```bash
   ./scripts/deploy-status.sh
   ```

2. Run E2E tests against dev:
   ```bash
   E2E_SERVER_URL=http://<dev-nlb>:80 E2E_AUTH_TOKEN=<token> ./scripts/e2e-tests.sh --remote
   ```

3. When ready for prod, use `/promote-server`.

## Notes

- The infrastructure includes: VPC, ECS Fargate, RDS PostgreSQL, NLB, ECR
- Terraform state is stored in S3 with DynamoDB locking
- Container is built using Jib (no Docker daemon required)
- Use `./scripts/deploy-status.sh` to check what's deployed across environments
