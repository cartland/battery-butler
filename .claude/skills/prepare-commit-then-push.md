# Prepare Commit Then Push

Run validation scripts, commit changes, and push to remote.

## Steps

1. Fetch latest main:
   ```bash
   git fetch origin main
   ```

2. Prepare for commit:
   ```bash
   ./scripts/prepare-for-commit.sh
   ```

   **Server Development Tips:**
   - If you modified `server/`, run `./gradlew :server:app:jibDockerBuild` to verify the container builds locally.
   - If you modified `server/terraform/`, run `terraform fmt -recursive server/terraform/` to format HCL.
   - Use `./scripts/restart_server.sh` to redeploy/reset the AWS environment if needed.

3. Resolve any issues:
   If the script fails (e.g. lint errors, broken tests), fix them and re-run step 2.

4. Commit changes:
   ```bash
   git add .
   git commit -m "Your commit message"
   ```

5. Resolve commit warnings:
   If git hooks or other checks warn/fail during commit, address them.

6. Push to remote:
   ```bash
   git push
   ```
