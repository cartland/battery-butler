# Gemini Contribution Guidelines

This document provides instructions for Gemini when contributing to this repository.

## Project Conventions

Adhere to established project conventions. The `.agent/` directory contains detailed rules and workflows.

## Workflow Instructions

1.  **Branching:** Create new branches with the `gemini/` prefix.
    ```bash
    git checkout -b gemini/feature-name
    ```

2.  **Validation:** Before committing, execute the project's validation script. Refer to `.agent/workflows/validate_changes.md` for details.
    ```bash
    ./scripts/validate.sh
    ```

3.  **Pull Requests:** After successful validation, commit changes and open a pull request to `main`. Direct pushes to `main` are prohibited.
