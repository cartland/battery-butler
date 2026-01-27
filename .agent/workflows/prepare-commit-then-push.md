---
description: Run formatter (Spotless), commit changes, and push/PR
---

// turbo-all

1. Fetch latest main
   `git fetch origin main`

2. Format Code (Spotless)
   `./scripts/spotless-apply.sh`

   > [!TIP]
   > We prioritize meaningful PRs over local validation. Tests and deeper checks will run in CI.

3. Resolve any Formatting issues
   If the script fails, fix the format errors.

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
   - **New PR Example:**
     ```bash
     gh pr create --title "feat: Add new user flow" --body "Description of changes..."
     ```
     > [!TIP]
     > For multi-line bodies, use a temporary file to avoid formatting issues:
     > ```bash
     > printf "Line 1\nLine 2" > pr_body.txt
     > gh pr create --title "feat: ..." --body-file pr_body.txt
     > rm pr_body.txt
     > ```
   - **View Existing:** `gh pr view`

