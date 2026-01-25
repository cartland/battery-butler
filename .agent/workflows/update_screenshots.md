---
description: Update reference screenshots for regression testing
---

// turbo-all

1. Run the Gradle task to clean references and record new screenshots
   `./gradlew :android-screenshot-tests:updateDebugScreenshotTest`

2. Verify the changes
   Check the `android-screenshot-tests/src/screenshotTestDebug/reference` directory for updates.
