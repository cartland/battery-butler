# =============================================================================
# ECR REPOSITORY
# =============================================================================
# ECR is shared across all environments - same images, different tags.
# This allows "build once, deploy many" pattern.

resource "aws_ecr_repository" "server" {
  name                 = "battery-butler-server"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
}

# =============================================================================
# ECS CLUSTER & SERVICE (environment-specific)
# =============================================================================

resource "aws_ecs_cluster" "main" {
  name = "battery-butler-${var.environment}-cluster"
}

resource "aws_cloudwatch_log_group" "ecs_log_group" {
  name              = "/ecs/battery-butler-${var.environment}"
  retention_in_days = 30
}

resource "aws_iam_role" "ecs_task_execution_role" {
  name = "battery-butler-${var.environment}-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Allow ECS to read secrets from Secrets Manager
resource "aws_iam_role_policy" "ecs_secrets_policy" {
  name = "battery-butler-${var.environment}-secrets-policy"
  role = aws_iam_role.ecs_task_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = [
          aws_secretsmanager_secret.db_credentials.arn
        ]
      }
    ]
  })
}

resource "aws_security_group" "ecs_service" {
  name        = "battery-butler-${var.environment}-ecs-sg"
  description = "Allow traffic from NLB"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 50051
    to_port         = 50051
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "battery-butler-${var.environment}-ecs-sg"
    Environment = var.environment
  }
}

resource "aws_ecs_task_definition" "server" {
  family                   = "battery-butler-${var.environment}-server"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = tostring(var.ecs_cpu)
  memory                   = tostring(var.ecs_memory)
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    {
      name      = "server"
      image     = "${aws_ecr_repository.server.repository_url}:${var.image_tag}"
      essential = true
      portMappings = [
        {
          containerPort = 50051
          hostPort      = 50051
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/battery-butler-${var.environment}"
          "awslogs-region"        = "us-west-1"
          "awslogs-stream-prefix" = "ecs"
        }
      }
      environment = [
        {
          name  = "SERVER_LABEL"
          value = "AWS Cloud (${var.environment})"
        },
        {
          name  = "ENVIRONMENT"
          value = var.environment
        },
        {
          name  = "DB_SECRET_NAME"
          value = aws_secretsmanager_secret.db_credentials.name
        }
      ]
    }
  ])

  tags = {
    Environment = var.environment
  }
}

resource "aws_ecs_service" "server" {
  name            = "battery-butler-${var.environment}-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.server.arn
  desired_count   = var.ecs_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.public_a.id, aws_subnet.public_b.id]
    security_groups  = [aws_security_group.ecs_service.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.grpc.arn
    container_name   = "server"
    container_port   = 50051
  }

  tags = {
    Environment = var.environment
  }
}
