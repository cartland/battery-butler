# AI Agent Contribution Guidelines

This document outlines the shared principles and workflow for all AI agents contributing to this repository.

## Guiding Principles

1.  **Single Source of Truth**: This directory, `.agent/`, is the single source of truth for all AI agent instructions.
    *   **`AGENTS.md` (This file):** The high-level charter and core workflow.
    *   **`rules.md`:** An agent-specific entry point for internal technical guidelines.
    *   **`workflows/`:** Step-by-step playbooks for common tasks, serving as both detailed instructions for agents and user-triggerable commands (e.g., slash commands).
2.  **Consistency**: All agents must follow the workflows defined here to ensure predictable and consistent contributions.
3.  **Spotless is Mandatory**: All changes must be formatted by running `./scripts/spotless-apply.sh` before being committed. Full validation happens on PRs.

## 🚨 Critical Rules

1.  **NEVER Push Directly to `main`**:
    *   **Always** create a feature branch (`agent/your-branch-name`).
    *   **Always** open a Pull Request for changes.
    *   **Exception**: Only project maintainers may push to `main` for critical reverts or documentation updates if explicitly authorized.

2.  **NEVER Create Tags or Deploy Without Explicit Permission**:
    *   **NEVER** create git tags (e.g., `android/N`, release tags) without explicit user approval.
    *   **NEVER** trigger deployment workflows without explicit user approval.
    *   When the user says "deploy", **ASK** which target (server, Android, etc.) and confirm before taking action.
    *   Tags trigger production releases and cannot be easily undone.

3.  **ALWAYS Ask Before Destructive or Irreversible Actions**:
    *   Creating tags, deploying, force-pushing, deleting branches on remote, or any action that affects production requires explicit confirmation.
    *   When uncertain about scope, ask clarifying questions before proceeding.

## Build & Test Health

Keeping the build and tests healthy is a top priority. When you identify or fix build/test issues:

1. **Always Create PRs for Fixes**: Never leave build or test fixes uncommitted. Create a PR promptly so fixes are tracked and reviewed.

2. **Verify Before Closing Tasks**: Before marking a build/test verification task as complete:
   - Confirm the build/test actually runs successfully
   - If you made fixes, commit them and create a PR
   - Document any known issues or failures in the PR description

3. **Fix Forward**: When you encounter a broken build or test:
   - Investigate the root cause
   - Create a fix on a feature branch
   - Open a PR with clear description of the problem and solution
   - Don't just work around issues locally

4. **Test Categories**:
   - **Unit tests**: Must pass (`./gradlew test`)
   - **Instrumented tests**: Must run (network failures are acceptable if server isn't available)
   - **Screenshot tests**: Must run (baseline mismatches indicate UI changes, not broken infrastructure)

## Project Technical Rules

- **Configuration**:
  - **Always** check `local.properties` for sensitive or environment-specific configuration (e.g., API Keys, Server URLs).
  - Use `AppConfig` or `BuildConfig` to access these values in code, do NOT hardcode them.

- **Self Improvements**:
  - **Always** update `.agent/` documentation when learning a critical piece of information that will improve future agent performance. This ensures continuous learning and improvement for all agents.
  - **Always** update `.agent/rules.md` when adding new rules or best practices for the project.

- **Bash Commands**:
  - **Always** run bash commands as separate tool calls instead of combining them with `&&`, `||`, or `for` loops.
  - **Never** write shell scripts with control flow (loops, conditionals) in a single bash command.
  - **Example**: Instead of `gh pr merge 1 && gh pr merge 2`, make two separate bash tool calls.
  - This makes command execution more transparent and easier to debug.

- **Git**:
  - **Always** use non-interactive flags for commands that might open an editor (e.g., `git cherry-pick --continue --no-edit`). This prevents the shell from getting stuck waiting for user input.
  - **Always** escape special characters in command arguments (e.g., `$` and `` ` ``) to prevent unintended shell expansion. Use single quotes or backslashes (`\`) for escaping.
  - **Prefer** using `--body-file` with a temporary file for `gh pr create` when the description contains complex Markdown (backticks, quotes) to avoid shell parsing errors.

- **Pull Requests**:
  - **Always** ensure the Pull Request title and description accurately reflect the final changes. If the scope of a branch evolves, update the PR description before merging.

- **iOS Builds**:
  - **Always** use `-derivedDataPath build/<target_name>` (e.g., `build/ios_compose`) when running multiple `xcodebuild` commands in a single script. This ensures **artifact isolation** between steps, mimicking CI parity, and prevents accidental cross-linking of frameworks.
  - **Always** use `-derivedDataPath build/...` generally to keep artifacts out of system locations.
  - **Always** use `-target` instead of `-scheme` if the scheme file is not shared (checked into git).
  - **Always** disable code signing for local simulator builds or CI builds without certificates using `CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO`.

- **Bazel**:
  - **Bazel Outputs:** All Bazel outputs are consolidated in `.bazel/` (e.g. `.bazel/bin`) via `.bazelrc`. This directory is gitignored and excluded from Spotless.
  - **Spotless vs Bazel:** Spotless exclusions are configured to ignore `.bazel/`. Running `spotlessApply` works safely even with Bazel symlinks present.

- **Validation**:
  - **Always** run `./scripts/validate.sh` before pushing to main. This script is maintained to match `ci.yml` strictly.
  - **Always** run `./scripts/spotless-apply.sh` and fix errors before pushing to main.
  - **Avoid** `clean` steps in scripts and CI if possible, relying on Gradle's incremental build and caching for speed.

- **CI Workflow Synchronization**:
  - **When changing JDK version**: Update ALL workflow files in `.github/workflows/` that use `setup-java`.
  - **When changing Gradle version**: Verify all workflows use compatible settings.
  - **When changing tooling requirements**: Check auxiliary workflows (update-diagrams.yml, update-screenshots.yml) in addition to main CI.
  - **Checklist for tooling changes**:
    1. `ci.yml` - Main CI workflow
    2. `update-diagrams.yml` - Auto-updates architecture diagrams
    3. `update-screenshots.yml` - Auto-updates Android screenshots
    4. Any other workflows in `.github/workflows/`

## Branch Management

**Before starting new work, decide if you need a new branch:**
- Check if there's an existing PR for the current branch.
- If new work is unrelated to the current branch/PR, create a new branch from `origin/main`.
- When uncertain, ask the user or default to creating a new branch.

## Core Git Workflow

1.  **Sync with `main`**: Before starting new work, fetch the latest `main` branch.
    ```bash
    git fetch origin main
    ```

2.  **Create a Branch**: Create a new branch from `origin/main`. The branch MUST be prefixed with `agent/`.
    ```bash
    git checkout -b agent/your-branch-name origin/main
    ```

3.  **Implement Changes**: Make all code modifications according to the project's established conventions, as detailed in `rules.md`.

4.  **Format Code**: Run Spotless to ensure code style compliance.
    ```bash
    ./scripts/spotless-apply.sh
    ```

5.  **Commit and Push**: Once validation passes, commit the changes with a clear message and push the branch.
    ```bash
    git add .
    git commit -m "feat: Describe the feature or fix"
    git push origin agent/your-branch-name
    ```

    > [!IMPORTANT]
    > **Spotless Verification**: After running `git commit`, check the output. If the post-commit hook reports `spotlessCheck FAILED`, you MUST run `./gradlew spotlessApply`, amend your commit (or create a fix commit), and verify again. Do not ignore this failure.

6.  **Create a Pull Request**: Open a pull request against the `main` branch. Direct pushes to `main` are prohibited.

## After Your PR is Merged

Once your pull request has been approved and merged into `main`, it is important to clean up your branches to keep the repository tidy.

1.  **Switch to the `main` Branch**:
    ```bash
    git checkout main
    ```

2.  **Pull the Latest Changes**: Ensure your local `main` branch is up-to-date.
    ```bash
    git pull origin main
    ```

3.  **Delete the Local Branch**:
    ```bash
    git branch -d agent/your-branch-name
    ```

4.  **Delete the Remote Branch**:
    ```bash
    git push origin --delete agent/your-branch-name
    ```

**Example for `AGENT_NAME.md`:**
```markdown
# AGENT_NAME Instructions

This file provides the initial instructions for AGENT_NAME.

**First Action:** Immediately read the main contribution guidelines located at `.agent/AGENTS.md`. This file contains the required workflow and rules for all AI agents in this project. Do not proceed with any other actions until you have read and understood it.
```