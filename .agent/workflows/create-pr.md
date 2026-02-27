---
description: Create a standardized Pull Request with gathered context and proper naming.
allowed-tools: Bash(*), Read, Glob, Grep
---

# Create Pull Request

Gather information about the current branch and create a well-formatted Pull Request using the GitHub CLI.

## Steps

1. **Review Changes:**
   Review the git log and diff to understand what was changed since `origin/main`. This will help inform the PR title and description.
   ```bash
   git log origin/main..HEAD --stat
   git diff origin/main..HEAD
   ```

2. **Determine PR Title:**
   Use the Conventional Commits format for the PR title:
   - `feat(...)`: A new feature
   - `fix(...)`: A bug fix
   - `test(...)`: Adding missing tests or correcting existing tests
   - `chore(...)`: Other changes that don't modify src or test files (e.g., updating dependencies or `.agent` files)
   - `refactor(...)`: A code change that neither fixes a bug nor adds a feature
   
   *Example: `feat(ios): Add DeviceDetailScreen UI`*

3. **Determine PR Body:**
   Create a comprehensive PR body covering:
   - **Overview:** Why is this PR necessary? What problem does it solve?
   - **Changes:** Bullet points detailing the specific technical changes made.
   - **Verification:** How was this tested? (e.g., CI, local tests, screenshots updated).

4. **Create the PR:**
   Use the `gh pr create` command to push the branch (if not already pushed) and open the PR.
   ```bash
   # Make sure the branch is pushed to origin first
   git push -u origin HEAD
   
   # Create the pull request
   gh pr create --title "<type>(<scope>): <subject>" --body "## Overview
   <Description of the problem solved>
   
   ## Changes
   - <Change 1>
   - <Change 2>
   
   ## Verification
   - <Verification steps performed>"
   ```

## Notes

- Before creating a PR, always ensure tests and linters have been run locally if applicable.
- If the PR is a `chore` for dumping context, refer to the `/dump-context` workflow for specific PR body requirements and automatically merge it when CI passes.
