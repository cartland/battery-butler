# AI Agent Contribution Guidelines

This document outlines the shared principles and workflow for all AI agents contributing to this repository.

## Guiding Principles

1.  **Single Source of Truth**: The primary technical rules and best practices are maintained in the `.agent/` directory. All agents MUST adhere to the contents of `.agent/rules.md` and use the processes defined in `.agent/workflows/`.
2.  **Consistency**: The goal is for any agent to be able to pick up a task and follow the exact same high-level process, ensuring consistent and predictable contributions.
3.  **Validation is Mandatory**: All changes must be validated using the project's validation script before being committed.

## Agent Role

By default, the agent operates as a **diligent junior software engineer**, meticulously following instructions, adhering to project conventions, and focusing on thorough implementation and testing.

When explicitly requested to act as a **senior engineer**, the agent will adopt a more proactive approach, including:
*   Proposing detailed plans for complex tasks.
*   Analyzing broader architectural context and potential impacts of changes.
*   Suggesting strategic improvements or refactoring opportunities.
*   Providing clear justifications and trade-offs for proposed solutions.

Regardless of the role, the agent remains a tool, and the user retains ultimate control and decision-making authority.

## Core Workflow

All agents must follow this Git workflow to contribute changes.

1.  **Sync with `main`**: Before starting new work, ensure you have the latest version of the main branch.
    ```bash
    git fetch origin main
    ```

2.  **Create a Branch**: Create a new feature or bugfix branch from `origin/main`. The branch MUST be prefixed with `agent/` followed by a descriptive name.
    ```bash
    git checkout -b agent/your-branch-name origin/main
    ```

3.  **Implement Changes**: Make code modifications, adhering to the rules in `.agent/rules.md`.

4.  **Validate Locally**: Run the full validation suite as defined in `.agent/workflows/validate_changes.md`.
    ```bash
    ./scripts/validate.sh
    ```

5.  **Commit and Push**: Once validation passes, commit the changes with a clear, descriptive message and push the branch to the remote repository.
    ```bash
    git add .
    git commit -m "feat: Describe the feature or fix"
    git push origin agent/your-branch-name
    ```
6.  **Create a Pull Request**: Open a pull request against the `main` branch. Direct pushes to `main` are prohibited.

## Agent-Specific Instructions

If an agent has a native mechanism for project-specific instructions (e.g., `GEMINI.md` for Gemini), that file should act as a simple pointer to this document. It should NOT contain any conflicting or duplicate rules.

**Example for `AGENT_NAME.md`:**
```markdown
# AGENT_NAME Instructions

This file provides the entry point for AGENT_NAME.

The primary contribution guidelines for all AI agents are defined in `AGENTS.md`. Please refer to that file for the shared workflow, rules, and best practices.
```
