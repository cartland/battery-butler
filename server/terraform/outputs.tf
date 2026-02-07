# =============================================================================
# TERRAFORM OUTPUTS
# =============================================================================

output "environment" {
  description = "The environment name"
  value       = var.environment
}

output "nlb_dns_name" {
  description = "The DNS name of the network load balancer"
  value       = aws_lb.main.dns_name
}

output "ecr_repository_url" {
  description = "The URL of the ECR repository"
  value       = data.aws_ecr_repository.server.repository_url
}

output "ecs_cluster_name" {
  description = "The name of the ECS cluster"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "The name of the ECS service"
  value       = aws_ecs_service.server.name
}

output "db_endpoint" {
  description = "The RDS database endpoint"
  value       = aws_db_instance.main.endpoint
  sensitive   = true
}

output "image_tag" {
  description = "The deployed image tag"
  value       = var.image_tag
}
