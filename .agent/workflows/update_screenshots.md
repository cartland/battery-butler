---
description: Update reference screenshots for regression testing
---

// turbo-all

1. Run the Gradle task to clean references and record new screenshots
   `./gradlew :android-screenshot-tests:updateDebugScreenshotTest`

2. Verify the changes
   Check the `android-screenshot-tests/src/screenshotTestDebug/reference` directory for updates.

## Screenshot Test Guidelines

- **All preview composables must be time-deterministic.** Never let `Clock.System.now()` reach a screenshot preview.
- Use the project's standard fixed instant: `Instant.parse("2026-01-18T17:00:00Z")`
- Pass explicit `nowInstant` parameters through the full composable chain — don't rely on defaults.
- If a composable computes dates internally (e.g., pre-filling a date field), add a parameter to override it in previews.
- Comment `// Use fixed dates for stable screenshots` in preview functions for clarity.
