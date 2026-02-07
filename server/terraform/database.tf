# =============================================================================
# DATABASE CONFIGURATION (environment-specific)
# =============================================================================

resource "aws_db_subnet_group" "main" {
  name       = "battery-butler-${var.environment}-db-subnet-group"
  subnet_ids = [aws_subnet.public_a.id, aws_subnet.public_b.id]

  tags = {
    Name        = "battery-butler-${var.environment}-db-subnet-group"
    Environment = var.environment
  }
}

resource "aws_security_group" "db" {
  name        = "battery-butler-${var.environment}-db-sg"
  description = "Allow traffic from ECS"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_service.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "battery-butler-${var.environment}-db-sg"
    Environment = var.environment
  }
}

# Generate a random password for the database
resource "random_password" "db_password" {
  length           = 16
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "aws_db_instance" "main" {
  identifier           = "battery-butler-${var.environment}-db"
  allocated_storage    = 20
  storage_type         = "gp2"
  engine               = "postgres"
  engine_version       = "15"
  instance_class       = var.db_instance_class
  username             = "postgres"
  password             = random_password.db_password.result
  parameter_group_name = "default.postgres15"
  skip_final_snapshot  = var.environment != "prod"
  publicly_accessible  = var.environment == "dev"
  multi_az             = var.db_multi_az
  db_subnet_group_name = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.db.id]

  # Enable deletion protection for production
  deletion_protection = var.environment == "prod"

  tags = {
    Name        = "battery-butler-${var.environment}-db"
    Environment = var.environment
  }
}

# Generate random suffix for secret name to prevent collision with soft-deleted secrets
resource "random_id" "secret_suffix" {
  byte_length = 4
}

resource "aws_secretsmanager_secret" "db_credentials" {
  name = "battery-butler-${var.environment}-db-credentials-${random_id.secret_suffix.hex}"

  tags = {
    Environment = var.environment
  }
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id     = aws_secretsmanager_secret.db_credentials.id
  secret_string = jsonencode({
    username = aws_db_instance.main.username
    password = random_password.db_password.result
    host     = aws_db_instance.main.address
    port     = aws_db_instance.main.port
    dbname   = aws_db_instance.main.db_name
  })
}
