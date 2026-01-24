---
description: Run validation scripts, commit changes, and push to remote
---

1. Fetch latest main
   `git fetch origin main`

2. Prepare for commit
   // turbo
   `./scripts/prepare-for-commit.sh`

   > [!TIP]
   > **Server Development Tips:**
   > - If you modified `server/`, run `./gradlew :server:app:jibDockerBuild` to verify the container builds locally.
   > - If you modified `server/terraform/`, run `terraform fmt -recursive server/terraform/` to format HCL.
   > - Use `./scripts/restart_server.sh` to redeploy/reset the AWS environment if needed.

2. Resolve any issues
   If the script fails (e.g. lint errors, broken tests), fix them and re-run step 1.

3. Commit changes
   `git add .`
   `git commit -m "Your commit message"`

4. Resolve commit warnings
   If git hooks or other checks warn / fail during commit, address them.

5. Push to remote
   `git push`
