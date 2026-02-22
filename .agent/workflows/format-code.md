# Format Code

Run Spotless to format all code according to project style guidelines.

## Steps

1. Apply formatting to all files:
   ```bash
   ./scripts/format.sh
   ```

   Or directly:
   ```bash
   ./gradlew spotlessApply
   ```

2. Verify formatting is correct:
   ```bash
   ./gradlew spotlessCheck
   ```

## Notes

- Spotless formats Kotlin (.kt, .kts) files according to ktlint rules
- The format script is automatically run as part of `/prepare-commit-then-push`
- Post-commit hooks will warn if formatting issues are detected
- Always run formatting before committing code
