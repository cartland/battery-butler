#!/bin/bash
set -e
if [ -n "$BUILD_WORKSPACE_DIRECTORY" ]; then
  cd "$BUILD_WORKSPACE_DIRECTORY"
else
  cd "$(dirname "$0")/.."
fi

echo "Cleaning old screenshots..."
rm -rf android-app/src/screenshotTestDebug/reference

echo "Updating screenshots for :android-app..."
./gradlew :android-app:updateDebugScreenshotTest
echo "Screenshots updated."
