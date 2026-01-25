# Update Screenshots

Update reference screenshots for regression testing.

## Steps

1. Run the Gradle task to clean references and record new screenshots:
   ```bash
   ./gradlew :android-screenshot-tests:updateDebugScreenshotTest
   ```

2. Verify the changes:
   Check the `android-screenshot-tests/src/screenshotTestDebug/reference` directory for updates.
