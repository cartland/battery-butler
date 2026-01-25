---
description: Deploy the gRPC server to AWS using Terraform and Jib
---

// turbo-all

1. Navigate to Terraform directory
   `cd server/terraform`

2. Initialize Terraform (first time only)
   `terraform init`

3. Review planned changes
   `terraform plan`

4. Apply infrastructure changes
   `terraform apply`

   > [!WARNING]
   > **Production Safety:**
   > Ensure you are on the `main` branch or a verified release branch before applying changes to production.

5. Build and push container to ECR
   `./gradlew :server:app:jib`

6. Force ECS to deploy new version
   `aws ecs update-service --cluster battery-butler-cluster --service battery-butler-service --force-new-deployment`

> [!NOTE]
> The server is automatically deployed via GitHub Actions when changes are pushed to main. Use this workflow for manual deployments or testing in dev environments.
