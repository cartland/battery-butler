#!/bin/bash
set -e
if [ -n "$BUILD_WORKSPACE_DIRECTORY" ]; then
  cd "$BUILD_WORKSPACE_DIRECTORY"
else
  cd "$(dirname "$0")/.."
fi

echo "Cleaning old screenshots..."
rm -rf android-screenshot-tests/src/screenshotTestDebug/reference

echo "Updating screenshots for :android-screenshot-tests..."
./gradlew :android-screenshot-tests:updateDebugScreenshotTest
echo "Screenshots updated."
