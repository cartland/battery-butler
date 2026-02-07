# =============================================================================
# PRODUCTION ENVIRONMENT CONFIGURATION
# =============================================================================
#
# Production environment - manual deployment with approval gate.
# Higher resources and redundancy for reliability.
#
# =============================================================================

environment       = "prod"
ecs_cpu           = 1024
ecs_memory        = 2048
ecs_desired_count = 2
db_instance_class = "db.t3.medium"
db_multi_az       = true
