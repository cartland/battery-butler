# =============================================================================
# STAGING ENVIRONMENT CONFIGURATION
# =============================================================================
#
# Staging environment - manual deployment for pre-production testing.
# Mid-tier resources to approximate production behavior.
#
# Tip: Set ecs_desired_count = 0 when not in use to save costs.
#
# =============================================================================

environment       = "staging"
ecs_cpu           = 512
ecs_memory        = 1024
ecs_desired_count = 1
db_instance_class = "db.t3.small"
db_multi_az       = false
