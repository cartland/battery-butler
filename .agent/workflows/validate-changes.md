---
description: Validate changes locally with CI parity before pushing.
allowed-tools: Bash(*), Read, Glob, Grep
---

# Validate Changes

Validate changes locally with CI parity.

## Steps

1. Run the validation script:
   ```bash
   ./scripts/validate.sh
   ```

2. If any step fails, check the specific output section (e.g. "iOS Checks").

3. For quicker iteration, run individual checks:
   ```bash
   # Formatting only
   ./scripts/spotless-apply.sh

   # Unit tests only
   ./gradlew test

   # Detekt only
   ./gradlew detekt
   ```

4. To debug iOS-specific issues:
   Run the xcodebuild command manually with verbose filtering if needed, and ensure `CODE_SIGNING_ALLOWED=NO` is used for simulator builds.

## Notes

- Always run before pushing to ensure CI will pass
- The validate script matches `ci.yml` strictly
- Spotless is the fastest check — run it first to catch formatting issues early
