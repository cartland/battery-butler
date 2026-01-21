# Explicitly create the Service Linked Role for ECS if it doesn't exist
resource "aws_iam_service_linked_role" "ecs" {
  aws_service_name = "ecs.amazonaws.com"
}
