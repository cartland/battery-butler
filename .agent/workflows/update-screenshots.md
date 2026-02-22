---
description: Update reference screenshots for visual regression testing.
allowed-tools: Bash(*), Read, Glob, Grep
---

# Update Screenshots

Update reference screenshots for regression testing.

## Steps

1. Run the Gradle task to clean references and record new screenshots:
   ```bash
   ./gradlew :android-screenshot-tests:updateDebugScreenshotTest
   ```

2. Verify the changes:
   Check the `android-screenshot-tests/src/screenshotTestDebug/reference` directory for updates.

3. Validate the new screenshots match expectations:
   ```bash
   ./gradlew :android-screenshot-tests:validateDebugScreenshotTest
   ```

## Notes

- `updateDebugScreenshotTest` and `validateDebugScreenshotTest` can't run in the same Gradle invocation (the update task's clean step deletes references mid-build)
- All preview composables must be time-deterministic — never let `Clock.System.now()` reach a screenshot preview
- Use `Instant.parse("2026-01-18T17:00:00Z")` as the standard fixed instant in previews
- On CI, screenshots are generated via `scripts/generate-screenshots-sequentially.sh` to avoid OOM
