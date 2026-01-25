---
description: Initial AWS infrastructure setup for server deployment (one-time setup)
---

// turbo-all

1. Create IAM User (`github-actions-deployer`)
   - Attach permissions (AdministratorAccess or custom policy)
   - Create Access Key (ID and Secret)

2. Create S3 Bucket for Terraform State
   - Name: `battery-butler-tf-state-<unique-suffix>`
   - Region: `us-west-1`
   - Enable Versioning

3. Create DynamoDB Table for State Locking
   - Name: `battery-butler-tf-lock`
   - Partition key: `LockID` (String)

4. Configure GitHub Secrets
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `TF_STATE_BUCKET`
   - `TF_LOCK_TABLE`

5. Verification
   `cd server/terraform`
   `terraform init -backend-config="bucket=YOUR_BUCKET_NAME" -backend-config="dynamodb_table=battery-butler-tf-lock"`
