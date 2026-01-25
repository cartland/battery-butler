---
description: Run validation scripts, commit changes, and push/PR
---

// turbo-all

1. Fetch latest main
   `git fetch origin main`

2. Prepare for commit
   `./scripts/prepare-for-commit.sh`

   > [!TIP]
   > **Server Development Tips:**
   > - If you modified `server/`, run `./gradlew :server:app:jibDockerBuild` to verify the container builds locally.
   > - If you modified `server/terraform/`, run `terraform fmt -recursive server/terraform/` to format HCL.
   > - Use `./scripts/restart_server.sh` to redeploy/reset the AWS environment if needed.

3. Resolve any issues
   If the script fails (e.g. lint errors, broken tests), fix them and re-run step 2.

4. Check Branch Status
   Check if you are on main or an existing feature branch.
   `git branch --show-current`
   `gh pr view --json url,state || echo "No existing PR"`

5. Choose Branch Strategy
   - **New Feature:** If on `main` (or starting fresh), create a new branch: `git checkout -b feature/your-feature-name`
   - **Update Existing PR:** Stay on current branch.
   - **Stacked PR:** If on a feature branch but want a separate PR, create new branch: `git checkout -b feature/stacked-feature-name`

6. Commit changes
   `git add .`
   `git commit -m "Your commit message"`

7. Push to remote
   `git push -u origin HEAD`

8. Handle Pull Request
   Generate a `gh pr create` command with a title and body summarizing the changes.
   - **New PR Example:** `gh pr create --title "feat: Add new user flow" --body "Description of changes..."`
   - **View Existing:** `gh pr view`
