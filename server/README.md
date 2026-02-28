# Battery Butler Server (AWS Deployment)

> **STATUS: HIBERNATED (Feb 2026)**
> AWS infrastructure is not running. The server runs locally only.
> All infrastructure code is preserved. See "Re-enabling AWS" below.

This directory contains the gRPC Server application and the Infrastructure-as-Code (Terraform) to deploy it to AWS.

## Architecture

The server is deployed as a containerized microservice on **AWS Fargate** (Serverless Compute).

- **Registry**: Amazon ECR (stores Docker images built by Jib, managed outside Terraform).
- **Compute**: AWS ECS Cluster + Fargate Service (runs the container).
- **Network**: VPC with Public Subnets across 2 AZs.
- **Load Balancer**: Network Load Balancer (NLB) forwarding TCP port 80 to gRPC port 50051.
- **Database**: Amazon RDS (PostgreSQL) - `db.t3.micro`.
- **Secrets**: Database credentials managed via AWS Secrets Manager.

## Multi-Environment Deployment

The server uses a **build once, deploy many** strategy. The same Docker image SHA is promoted through environments:

```
Push to main -> Build Image -> Tag with SHA -> Push to ECR -> Auto-deploy to Dev
                                                                     |
                                              Manual trigger -> Deploy to Staging
                                                                     |
                                       Manual trigger + approval -> Deploy to Prod
```

### Environments

| Environment | Trigger | State Key | GitHub Environment |
|-------------|---------|-----------|-------------------|
| **Dev** | Auto on push to main | `server/dev/terraform.tfstate` | `dev` |
| **Staging** | Manual `workflow_dispatch` | `server/staging/terraform.tfstate` | `staging` |
| **Production** | Manual + reviewer approval | `server/prod/terraform.tfstate` | `production` |

Each environment has its own Terraform state and configuration in `terraform/environments/{env}.tfvars`.

### Workflows

| Workflow | File | Purpose |
|----------|------|---------|
| Build & Deploy Dev | `server-build.yml` | Build container, push to ECR, deploy to dev |
| Deploy Staging | `server-deploy-staging.yml` | Promote image to staging |
| Deploy Production | `server-deploy-prod.yml` | Promote image to prod (with approval) |
| Destroy | `server-destroy.yml` | Tear down staging or dev infrastructure |
| Rollback | `server-rollback.yml` | Emergency rollback to previous image |

### Deploy Commands

```bash
# Check latest dev image tag from build logs
gh run list --workflow=server-build.yml --limit 1

# Promote to staging
gh workflow run server-deploy-staging.yml -f image_tag=<sha>

# Promote to production (requires approval on GitHub)
gh workflow run server-deploy-prod.yml -f image_tag=<sha>

# Destroy an environment
gh workflow run server-destroy.yml -f environment=staging
```

### Testing Endpoints

The NLB exposes port 80 (TCP), forwarding to gRPC on port 50051:

```bash
# Install grpcurl if needed: brew install grpcurl

# Test an endpoint
grpcurl -plaintext -proto protos/com/chriscartland/batterybutler/protos/battery_service.proto \
  <nlb-dns>:80 com.chriscartland.batterybutler.proto.BatteryService/GetServerStatus
```

## Prerequisites (GitHub Secrets)

| Secret Name | Description |
|:---|:---|
| `AWS_ACCESS_KEY_ID` | IAM User Access Key |
| `AWS_SECRET_ACCESS_KEY` | IAM User Secret Key |
| `AWS_DEPLOY_ROLE_ARN` | (Optional) OIDC role ARN for keyless auth |
| `TF_STATE_BUCKET` | S3 bucket for Terraform state |
| `TF_LOCK_TABLE` | DynamoDB table for state locking |
| `BOT_PAT` | GitHub PAT with `repo` scope (used to auto-sync server URL secrets) |
| `PRODUCTION_SERVER_URL` | Production NLB URL (auto-synced by prod deploy workflow) |
| `DEV_SERVER_URL` | Dev NLB URL (auto-synced by dev deploy workflow) |

See `AWS_SETUP.md` for detailed setup instructions.

### Auto-Synced Secrets

Deploy workflows automatically update server URL secrets after each `terraform apply`:

- `server-build.yml` (dev deploy) → syncs `DEV_SERVER_URL`
- `server-deploy-prod.yml` (prod deploy) → syncs `PRODUCTION_SERVER_URL`

This ensures the Android release workflow (`release-android.yml`) always builds against the correct NLB hostname, even after infrastructure recreates. The sync uses `BOT_PAT` with `continue-on-error` so a failed sync doesn't fail the deploy.

## IAM Permissions

The IAM policy is documented in `server/iam_policy.json`. This file is the source of truth but must be **manually synced to AWS Console** when updated. Key notes:

- ECR is managed outside Terraform (created by AWS CLI in build job, read via `data` source)
- Terraform tag operations require both `TagResource` and `UntagResource` permissions
- Inline IAM policies require `iam:PutRolePolicy`, `iam:GetRolePolicy`, `iam:DeleteRolePolicy`

## Terraform

### Configuration

Environment-specific settings are in `terraform/environments/`:
- `dev.tfvars` - Minimal resources for development
- `staging.tfvars` - Mid-tier for pre-production testing
- `prod.tfvars` - Production configuration

All currently use `db.t3.micro` for AWS free-tier compatibility.

### Manual Deployment (Local)

Requires [Terraform](https://developer.hashicorp.com/terraform/downloads) and AWS CLI (`aws configure`).

```bash
cd server/terraform
terraform init \
  -backend-config="bucket=YOUR_BUCKET" \
  -backend-config="key=server/dev/terraform.tfstate" \
  -backend-config="region=us-west-1" \
  -backend-config="dynamodb_table=battery-butler-tf-lock" \
  -backend-config="encrypt=true"

terraform apply -var-file=environments/dev.tfvars -var="image_tag=latest-dev"
```

## Docker (Jib)

This project uses [Jib](https://github.com/GoogleContainerTools/jib) to build optimized Docker images without a Docker daemon.

- **Check Configuration**: `server/app/build.gradle.kts`
- **Build to Daemon (Local)**: `./gradlew :server:app:jibDockerBuild`
- **Build to Registry (Remote)**: `./gradlew :server:app:jib`
- **Build to Tarball (Local Debug)**: `./gradlew :server:app:jibBuildTar`

## AWS Glossary

- **VPC (Virtual Private Cloud)**: A private network for your resources.
- **ECS (Elastic Container Service)**: Orchestrator that runs Docker containers.
- **Fargate**: Serverless compute engine for ECS.
- **ECR (Elastic Container Registry)**: Docker image storage.
- **RDS (Relational Database Service)**: Managed PostgreSQL database.
- **NLB (Network Load Balancer)**: Distributes TCP traffic to ECS tasks.
- **IAM (Identity and Access Management)**: Manages permissions.
- **AZ (Availability Zone)**: Distinct data centers within an AWS Region.
- **S3 (Simple Storage Service)**: Object storage for Terraform state.

## Re-enabling AWS

When ready to restore AWS infrastructure:

1. Rotate AWS IAM credentials, update GitHub secrets
2. Remove `if: false` from all 6 server workflow files
3. Run `server-build.yml` to rebuild and deploy to dev
4. Update `gradle.properties` comment (remove hibernation note)
5. Update `strings.xml` — remove "(disabled)" from server labels
6. Reorder `SettingsViewModel.availableNetworkModes` — AWS options first
7. Update this README — remove hibernation notice
8. Promote to prod via `/promote-server`
