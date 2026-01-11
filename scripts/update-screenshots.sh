#!/bin/bash
set -e
cd "$(dirname "$0")/.."

echo "Updating screenshots for :ui-core and :ui-feature..."
./gradlew :ui-core:updateDebugScreenshotTest :ui-feature:updateDebugScreenshotTest
echo "Screenshots updated."
