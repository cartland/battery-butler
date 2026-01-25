# Setup AWS

Initial AWS infrastructure setup for server deployment (one-time setup).

## Step 1: Create IAM User

1. Log in to AWS Console
2. Navigate to IAM > Users > Create user
3. Name: `github-actions-deployer`
4. Attach permissions:
   - Option A (easier): `AdministratorAccess`
   - Option B (least privilege): Use policy from `server/iam_policy.json`
5. Create Access Key:
   - Go to Security credentials
   - Create access key > CLI
   - Save the Access Key ID and Secret Access Key

## Step 2: Create S3 Bucket for Terraform State

1. Navigate to S3 > Create bucket
2. Name: `battery-butler-tf-state-<unique-suffix>` (e.g., `battery-butler-tf-state-yourname-dev`)
3. Region: `us-west-1` (must match Terraform region)
4. Enable Bucket Versioning (important for state recovery)
5. Create bucket

## Step 3: Create DynamoDB Table for State Locking

1. Navigate to DynamoDB > Tables > Create table
2. Name: `battery-butler-tf-lock`
3. Partition key: `LockID` (String) - case sensitive, exact match required
4. Create table

## Step 4: Configure GitHub Secrets

Navigate to GitHub repo: Settings > Secrets and variables > Actions

Add these secrets:
- `AWS_ACCESS_KEY_ID` - From Step 1
- `AWS_SECRET_ACCESS_KEY` - From Step 1
- `TF_STATE_BUCKET` - Your bucket name from Step 2
- `TF_LOCK_TABLE` - `battery-butler-tf-lock`

## Verification

After setup, test by running:
```bash
cd server/terraform
terraform init -backend-config="bucket=YOUR_BUCKET_NAME" -backend-config="dynamodb_table=battery-butler-tf-lock"
```

## Notes

- This is a one-time setup per AWS account
- The S3 bucket name must be globally unique
- Terraform state is shared between local and CI environments
- See `server/AWS_SETUP.md` for detailed instructions
