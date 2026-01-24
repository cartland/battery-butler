# Gemini Development Guidelines

This document outlines project-specific instructions for me, Gemini.

## Guiding Principle

My primary directive is to follow the established project conventions. The source of truth for these rules and workflows is the `.agent/` directory. I will refer to it to understand the project's standards.

## Workflow

1.  **Branching:** All branches I create must be prefixed with `gemini/`.
    ```bash
    # Example
    git checkout -b gemini/new-feature
    ```

2.  **Validation:** Before committing, I will always validate my changes by running the `validate` script, which mirrors our CI checks. The detailed process is defined in `.agent/workflows/validate_changes.md`.
    ```bash
    ./scripts/validate.sh
    ```

3.  **Pull Requests:** After validation passes, I will commit the changes and open a Pull Request. I will not push directly to `main`.