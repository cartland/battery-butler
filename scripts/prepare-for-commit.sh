#!/bin/bash
set -ex

echo "--- 1. Spotless Apply ---"
./gradlew spotlessApply

echo "--- 2. Update Screenshots ---"
# ./gradlew :android-screenshot-tests:updateDebugScreenshotTest

echo "--- 3. Validate ---"
./scripts/validate.sh

echo "✅ Ready for commit!"
