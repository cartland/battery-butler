#!/bin/bash
set -e
if [ -n "$BUILD_WORKSPACE_DIRECTORY" ]; then
  cd "$BUILD_WORKSPACE_DIRECTORY"
else
  cd "$(dirname "$0")/.."
fi

echo "Cleaning old screenshots..."
rm -rf android-screenshot-library/src/screenshotTestDebug/reference

echo "Updating screenshots for :android-screenshot-library..."
./gradlew :android-screenshot-library:updateDebugScreenshotTest
echo "Screenshots updated."
