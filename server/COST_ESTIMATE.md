# AWS Cost Estimate

Monthly cost estimates for running Battery Butler server on AWS.
Based on **us-west-1** (N. California) on-demand pricing as of Feb 2026.

## Per-Environment Cost

| Component | Dev | Staging | Prod |
|-----------|-----|---------|------|
| NLB | $16.43 | $16.43 | $16.43 |
| Fargate (0.25 vCPU, 0.5 GB) | $9.01 | $9.01 | $9.01 |
| RDS (db.t3.micro) | $14.71 | $14.71 | $14.71 |
| Secrets Manager | $0.40 | $0.40 | $0.40 |
| ECR + Data Transfer | ~$0.50 | - | - |
| **Total** | **~$41** | **~$41** | **~$41** |

> **Free Tier**: If your AWS account is less than 12 months old, RDS `db.t3.micro` (750 hours/month) and 20GB storage are free, reducing each environment to ~$26/month.

> **Free Tier Limitation**: Max 2 RDS instances. Cannot run all 3 environments simultaneously on free tier.

## Multi-Environment Total

| Scenario | Monthly Cost |
|----------|-------------|
| Dev only | ~$41 |
| Dev + Prod (staging destroyed) | ~$82 |
| Dev + Staging + Prod | ~$123 |

## Detailed Breakdown

### Network Load Balancer (NLB)
- Hourly: $0.0225/hour
- Monthly: $0.0225 * 730 = **$16.43**

### Compute (AWS Fargate)
- Config: 0.25 vCPU, 0.5 GB Memory, 1 task
- vCPU: 0.25 * $0.04048/hour = $0.01012/hour
- Memory: 0.5 * $0.004445/hour = $0.00222/hour
- Monthly: ~$0.0123 * 730 = **$9.01**

### Database (Amazon RDS)
- Instance: `db.t3.micro` (Single AZ)
- Compute: $0.017/hour * 730 = **$12.41**
- Storage: 20 GB (gp2) * $0.115/GB = **$2.30**
- Total: **$14.71**

### Secrets Manager
- Per secret: **$0.40/month**

## Cost Optimization Tips

1. **Destroy unused environments**: `gh workflow run server-destroy.yml -f environment=staging` — saves ~$41/month per environment.
2. **Stop Fargate**: Set `ecs_desired_count = 0` in tfvars when not in use — saves ~$9/month.
3. **Stop RDS**: Stop the instance manually for up to 7 days (AWS auto-restarts after 7 days) — saves ~$12/month.
4. **Full teardown**: `terraform destroy` when not developing — drops to ~$0.
5. **Share RDS**: Long-term, consider sharing one RDS instance across environments with separate databases (requires Terraform refactoring, tracked in `bb-jj7`).
