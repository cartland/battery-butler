#!/bin/bash
set -e

# Clean artifacts once at the beginning
echo "Cleaning reference screenshots..."
./gradlew :android-screenshot-tests:cleanReferenceScreenshots

# Iterate over all screenshot test files
for file in android-screenshot-tests/src/screenshotTest/kotlin/com/chriscartland/batterybutler/androidscreenshottests/*.kt; do
    filename=$(basename "$file" .kt)
    # Append 'Kt' to the class name as required by the AGP screenshot plugin
    classname="com.chriscartland.batterybutler.androidscreenshottests.${filename}Kt"
    
    echo "----------------------------------------------------------------"
    echo "Running screenshot generation for $classname"
    echo "----------------------------------------------------------------"
    
    ./gradlew :android-screenshot-tests:updateDebugScreenshotTest --tests "$classname" -PretainedReferenceScreenshots
done

echo "All screenshot tests completed."
