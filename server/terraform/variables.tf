# =============================================================================
# TERRAFORM VARIABLES
# =============================================================================
#
# This file defines variables for multi-environment deployments.
# Use with -var-file=environments/<env>.tfvars
#
# Example:
#   terraform plan -var-file=environments/dev.tfvars -var="image_tag=abc1234"
#
# =============================================================================

variable "environment" {
  type        = string
  description = "Environment name (dev, staging, prod)"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be one of: dev, staging, prod"
  }
}

variable "ecs_cpu" {
  type        = number
  description = "CPU units for ECS task (256, 512, 1024, 2048, 4096)"
  default     = 256
}

variable "ecs_memory" {
  type        = number
  description = "Memory (MB) for ECS task"
  default     = 512
}

variable "ecs_desired_count" {
  type        = number
  description = "Number of ECS task instances to run"
  default     = 1
}

variable "db_instance_class" {
  type        = string
  description = "RDS instance class"
  default     = "db.t3.micro"
}

variable "db_multi_az" {
  type        = bool
  description = "Enable Multi-AZ deployment for RDS (recommended for prod)"
  default     = false
}

variable "image_tag" {
  type        = string
  description = "Docker image tag to deploy (typically a git SHA)"
}
