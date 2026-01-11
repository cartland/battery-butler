#!/bin/bash
set -e
cd "$(dirname "$0")/.."

echo "Validating screenshots for :ui-core and :ui-feature..."
./gradlew :ui-core:validateDebugScreenshotTest :ui-feature:validateDebugScreenshotTest
echo "Screenshots validated."
