# AWS Cost Estimate (Development Environment)

This document provides a monthly cost estimate for running the **Battery Butler** server on AWS using the provided Terraform configuration.
Estimates are based on **us-east-1** (N. Virginia) on-demand pricing as of Jan 2026.

### Summary
**Total Estimated Monthly Cost: ~$41.00**

> **Note**: If your AWS account is less than 12 months old, you may be eligible for the **AWS Free Tier**, significantly reducing this cost (specifically for RDS).

---

### Detailed Breakdown

#### 1. Application Load Balancer (ALB)
The entry point for gRPC traffic.
*   **Hourly Charge**: $0.0225 per hour
*   **Monthly Cost**: $0.0225 * 730 hours = **$16.43**
*   *Note*: LCU (Load Balancer Capacity Unity) charges are negligible for development traffic.

#### 2. Compute (AWS Fargate)
Serverless compute for the container.
*   **Configuration**: 0.25 vCPU, 0.5 GB Memory
*   **vCPU Cost**: 0.25 * $0.04048/hour = $0.01012/hour
*   **Memory Cost**: 0.5 * $0.004445/hour = $0.00222/hour
*   **Total Hourly**: ~$0.0123/hour
*   **Monthly Cost**: $0.0123 * 730 hours = **$9.01**

#### 3. Database (Amazon RDS)
Managed PostgreSQL instance.
*   **Instance**: `db.t3.micro` (Single AZ)
*   **Compute Cost**: $0.017/hour * 730 hours = **$12.41**
*   **Storage Cost**: 20 GB (gp2) * $0.115/GB = **$2.30**
*   **Total Monthly**: **$14.71**
*   *Free Tier Strategy*: Eligible for 750 hours/month of `db.t3.micro` and 20GB storage for the first 12 months.

#### 4. Secrets Manager
Stores database credentials.
*   **Cost**: **$0.40** per secret per month.

#### 5. Network & Registry
*   **ECR (Registry)**: ~$0.10/GB month (Negligible for small images).
*   **Data Transfer**: Free inbound. Outbound to internet is ~$0.09/GB (Negligible for dev).
*   **NAT Gateway**: **$0.00** (Architecture uses Public Subnets for Fargate to avoid the ~$32/mo NAT Gateway cost).

### Cost Optimization Tips
1.  **Stop Fargate**: Set `desired_count = 0` in `compute.tf` when not in use to save ~$9/mo.
2.  **Stop RDS**: Stop the RDS instance manually if not working for >7 days (AWS auto-starts it after 7 days) to save ~$12/mo.
3.  **Delete Stack**: Run `terraform destroy` when not actively developing to drop costs to ~$0 (storage only).
