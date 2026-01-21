# AWS Configuration Guide for Battery Butler Server

Since we are deploying via **GitHub Actions**, Terraform cannot store its "state" (the record of what it created) locally on the runner, because the runner is destroyed after each job. We must configure a **Remote Backend** using AWS S3 and DynamoDB.

## Step 1: Create IAM User for GitHub Actions
1. Log in to the **AWS Console**.
2. Go to **IAM** > **Users** > **Create user**.
3. Name: `github-actions-deployer`.
4. **Permissions**:
   - **Option A (Easiest)**: Attach `AdministratorAccess` (Recommended for initial setup/learning).
   - **Option B (Least Privilege)**: Click **Create policy**, select the **JSON** tab.
   - **Copy and paste the content** from the file included in this repository: `server/iam_policy.json`.
   - This policy allows Terraform to manage specific resources (VPC, ECS, RDS, etc.) without full admin rights.
5. Create an **Access Key**:
   - Go to the user > **Security credentials**.
   - **Create access key** > **CLI**.
   - **Save** the `Access Key ID` and `Secret Access Key`.

## Step 2: Create S3 Bucket for Terraform State
1. Go to **S3** > **Create bucket**.
2. Name: `battery-butler-tf-state-<unique-suffix>` (e.g., `battery-butler-tf-state-cartland-dev`).
   - *Note: Bucket names must be globally unique.*
3. Region: `us-west-1` (Must match your `main.tf` region).
4. **Bucket Versioning**: Enable (Important for state recovery).
5. **Create bucket**.

## Step 3: Create DynamoDB Table for State Locking
1. Go to **DynamoDB** > **Tables** > **Create table**.
2. Name: `battery-butler-tf-lock`.
3. Partition key: `LockID` (String). *Case sensitive, exact match required.*
4. **Create table**.

## Step 4: Configure GitHub Secrets
1. Go to your GitHub Repository: `cartland/battery-butler`.
2. **Settings** > **Secrets and variables** > **Actions**.
3. Add the following **Repository secrets**:
   - `AWS_ACCESS_KEY_ID`: (From Step 1)
   - `AWS_SECRET_ACCESS_KEY`: (From Step 1)

## Step 5: Configure Backend Secrets
Since this repository is public, we will **NOT** put the bucket name in the code. Instead, we will store it as a GitHub Secret and inject it during deployment.

1. Go to **Settings** > **Secrets and variables** > **Actions**.
2. Add the following **Repository secrets**:
   - `TF_STATE_BUCKET`: `YOUR_BUCKET_NAME` (e.g., `battery-butler-tf-state-cartland-dev`)
   - `TF_LOCK_TABLE`: `battery-butler-tf-lock`

*Note: The `server/terraform/main.tf` file will now have an empty `backend "s3" {}` block. This is intentional.*
