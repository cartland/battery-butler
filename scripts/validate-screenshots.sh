#!/bin/bash
set -e
if [ -n "$BUILD_WORKSPACE_DIRECTORY" ]; then
  cd "$BUILD_WORKSPACE_DIRECTORY"
else
  cd "$(dirname "$0")/.."
fi

echo "Validating screenshots for :android-screenshot-tests..."
./gradlew :android-screenshot-tests:validateDebugScreenshotTest
echo "Screenshots validated."
