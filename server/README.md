# Battery Butler Server (AWS Deployment)

This directory contains the gRPC Server application and the Infrastructure-as-Code (Terraform) to deploy it to AWS.

## 🏗 Architecture

The server is deployed as a containerized microservice on **AWS Fargate** (Serverless Compute).

- **Registry**: Amazon ECR (stores Docker images built by Jib).
- **Compute**: AWS ECS Cluster + Fargate Service (runs the `vince/battery-butler-server` container).
- **Network**: VPC with Public Subnets (for Fargate/ALB) across 2 AZs.
- **Load Balancer**: Application Load Balancer (ALB) handling HTTP/2 and gRPC traffic on Port 80/443.
- **Database**: Amazon RDS (PostgreSQL) - `db.t3.micro`.
- **Secrets**: Credentials managed via AWS Secrets Manager.

## 🚀 Deployment

Deployment is fully automated via **GitHub Actions** (`.github/workflows/deploy-server.yml`).

### Prerequisites (GitHub Secrets)

To enable deployment, set the following Secrets in your GitHub Repository settings:

| Secret Name | Description |
| :--- | :--- |
| `AWS_ACCESS_KEY_ID` | IAM User Access Key with permissions for ECS, ECR, RDS, VPC. |
| `AWS_SECRET_ACCESS_KEY` | IAM User Secret Key. |

### Manual Deployment (Local)

You can also run the deployment manually from your machine if you have:
1.  [Terraform](https://developer.hashicorp.com/terraform/downloads) installed.
2.  AWS CLI configured (`aws configure`).

#### 1. Provision Infrastructure
```bash
cd server/terraform
terraform init
terraform apply
```

#### 2. Build & Deploy Container
```bash
# Push container to ECR
./gradlew :server:app:jib

# Force ECS to pick up new image
aws ecs update-service --cluster battery-butler-cluster --service battery-butler-service --force-new-deployment
```

## 🛠 Docker (Jib)

This project uses [Jib](https://github.com/GoogleContainerTools/jib) to build optimized Docker images without a Docker daemon.

- **Check Configuration**: `server/app/build.gradle.kts`
- **Build to Daemon (Local)**: `./gradlew :server:app:jibDockerBuild`
- **Build to Registry (Remote)**: `./gradlew :server:app:jib`

## 📚 AWS Glossary

Here is a quick reference for the AWS resources created by Terraform:

- **VPC (Virtual Private Cloud)**: A private network for your resources, isolated from the rest of the internet.
- **ECS (Elastic Container Service)**: The orchestrator that runs your Docker containers.
- **Fargate**: A serverless compute engine for ECS (runs containers without managing servers/EC2).
- **ECR (Elastic Container Registry)**: Where your Docker images are stored (like GitHub for Docker).
- **RDS (Relational Database Service)**: Managed SQL database service (hosting our PostgreSQL DB).
- **ALB (Application Load Balancer)**: Distributes incoming network traffic across multiple targets (our ECS tasks). It handles the HTTP/2 and gRPC connections.
- **IAM (Identity and Access Management)**: Manages permissions (roles, users) securely.
- **AZ (Availability Zone)**: Distinct data centers within an AWS Region (e.g., `us-west-1a`) for redundancy.
- **S3 (Simple Storage Service)**: Object storage (used here to store the "State" of Terraform infrastructure).
