resource "aws_db_subnet_group" "main" {
  name       = "battery-butler-db-subnet-group"
  subnet_ids = [aws_subnet.public_a.id, aws_subnet.public_b.id] # Ideally private, but using public for demo simplicity/fargate-host
  tags = {
    Name = "battery-butler-db-subnet-group"
  }
}

resource "aws_security_group" "db" {
  name        = "battery-butler-db-sg"
  description = "Allow traffic from ECS"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_service.id] # Allow ECS tasks
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Generate a random password for simple setup (in prod, use Secrets Manager to generate/rotate)
resource "random_password" "db_password" {
  length           = 16
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "aws_db_instance" "main" {
  identifier           = "battery-butler-db"
  allocated_storage    = 20
  storage_type         = "gp2"
  engine               = "postgres"
  engine_version       = "16.1"
  instance_class       = "db.t3.micro"
  username             = "postgres"
  password             = random_password.db_password.result
  parameter_group_name = "default.postgres15"
  skip_final_snapshot  = true
  publicly_accessible  = true # For development ease; disable in prod
  db_subnet_group_name = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.db.id]
}

# Generate random suffix for secret name to prevent collision with soft-deleted secrets
resource "random_id" "secret_suffix" {
  byte_length = 4
}

resource "aws_secretsmanager_secret" "db_credentials" {
  name = "battery-butler-db-credentials-${random_id.secret_suffix.hex}"
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
