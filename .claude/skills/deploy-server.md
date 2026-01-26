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
