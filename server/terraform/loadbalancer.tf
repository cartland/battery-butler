resource "aws_security_group" "alb" {
  name        = "battery-butler-alb-sg"
  description = "Allow HTTP/HTTPS traffic"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "Allow HTTP2/gRPC traffic"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # For initial testing without certs, we might need port 80 or 50051 direct
  ingress {
    description = "Allow HTTP traffic"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_lb" "main" {
  name               = "battery-butler-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = [aws_subnet.public_a.id, aws_subnet.public_b.id]
}

resource "aws_lb_target_group" "grpc" {
  name        = "battery-butler-grpc-tg"
  port        = 50051
  protocol    = "HTTP"
  protocol_version = "HTTP2"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  health_check {
    enabled             = true
    path                = "/grpc.health.v1.Health/Check" # Standard gRPC health check path if implemented
    port                = "traffic-port"
    protocol            = "HTTP"
    matcher             = "200" # Success code for HTTP/2 health check
  }
}

# NOTE: For real gRPC over ALB, HTTPS (port 443) is highly recommended/required for many clients.
# We will setup a dummy listener on 80 for now redirecting or simple setup, 
# but mostly we want HTTP/2 support.

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.grpc.arn
  }
}
