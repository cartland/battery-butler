---
description: Promote server image from dev to production with approval gate.
allowed-tools: Bash(*), Read, Glob, Grep
---

# Promote Server

Promote the server Docker image from dev to production.

> [!CAUTION]
> **Agent Rule:** NEVER promote to production without explicit user permission.
> Always show the current deployment state and confirm before proceeding.

## Steps

1. Check current deployment state:
   ```bash
   ./scripts/deploy-status.sh
   ```

2. Run the promotion script:
   ```bash
   ./scripts/promote-server.sh --confirm
   ```

   The script will:
   - Read the current dev image tag from ECS
   - Trigger `server-deploy-prod.yml` with that image tag
   - Print the approval URL

3. Inform the user they need to **approve the deployment** in the GitHub Actions UI:
   https://github.com/cartland/battery-butler/actions/workflows/server-deploy-prod.yml

4. After approval, monitor the deployment:
   ```bash
   gh run list --workflow=server-deploy-prod.yml --limit 1
   ```

5. Verify the promotion:
   ```bash
   ./scripts/deploy-status.sh
   ```

## Promoting a Specific Image

To promote an image other than the current dev image:
```bash
./scripts/promote-server.sh <image_tag> --confirm
```

## Notes

- The same Docker image SHA is deployed — no rebuild occurs
- Production requires approval via GitHub Environments (configured with required reviewers)
- The workflow auto-syncs `PRODUCTION_SERVER_URL` after successful deployment
- Use `./scripts/deploy-status.sh` to verify dev and prod are running the same image after promotion
