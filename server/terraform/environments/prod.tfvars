# =============================================================================
# PRODUCTION ENVIRONMENT CONFIGURATION
# =============================================================================
#
# Production environment - manual deployment with approval gate.
# Higher resources and redundancy for reliability.
#
# =============================================================================

environment       = "prod"
ecs_cpu           = 256
ecs_memory        = 512
ecs_desired_count = 1
db_instance_class = "db.t3.micro"
db_multi_az       = false
