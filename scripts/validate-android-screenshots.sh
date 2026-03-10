#!/bin/bash
set -e

# Validate reference screenshots against current UI (sequential per file to avoid OOM).
# Mirrors generate-android-screenshots.sh but runs validateDebugScreenshotTest.

# Iterate over all screenshot test files
for file in android-screenshot-tests/src/screenshotTest/kotlin/com/chriscartland/batterybutler/androidscreenshottests/*.kt; do
    filename=$(basename "$file" .kt)
    # Append 'Kt' to the class name as required by the AGP screenshot plugin
    classname="com.chriscartland.batterybutler.androidscreenshottests.${filename}Kt"

    echo "----------------------------------------------------------------"
    echo "Validating screenshots for $classname"
    echo "----------------------------------------------------------------"

    ./gradlew :android-screenshot-tests:validateDebugScreenshotTest --tests "$classname" -PretainedReferenceScreenshots
done

echo "All screenshot validation tests passed."
