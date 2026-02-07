# =============================================================================
# DEV ENVIRONMENT CONFIGURATION
# =============================================================================
#
# Development environment - auto-deployed on push to main.
# Minimal resources for cost efficiency.
#
# =============================================================================

environment       = "dev"
ecs_cpu           = 256
ecs_memory        = 512
ecs_desired_count = 1
db_instance_class = "db.t3.micro"
db_multi_az       = false
