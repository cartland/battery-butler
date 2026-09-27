#!/bin/bash
# Run iOS SwiftUI tests on simulator.
#
# Usage:
#   ./scripts/test-ios.sh                          # Run all tests
#   ./scripts/test-ios.sh BatteryAgeHelperTests     # Run one test class
#   ./scripts/test-ios.sh Foo Bar                   # Run multiple test classes
set -euo pipefail
cd "$(dirname "$0")/.."

# --- Resolve simulator destination ---
# shellcheck source=scripts/lib/resolve-ios-simulator.sh
source "$(dirname "$0")/lib/resolve-ios-simulator.sh"
resolve_ios_simulator || exit 1
DEVICE_NAME="$IOS_DEVICE_NAME"
IOS_VERSION="$IOS_RUNTIME_VERSION"

DESTINATION="platform=iOS Simulator,name=${DEVICE_NAME},OS=${IOS_VERSION}"
echo "Using simulator: ${DEVICE_NAME}, iOS ${IOS_VERSION}"

# --- Build test filter args ---
FILTER_ARGS=()
for class in "$@"; do
    FILTER_ARGS+=("-only-testing:iosAppSwiftUITests/${class}")
done

# --- Run tests ---
echo "Running iOS tests..."
xcodebuild test \
    -project ios-app-swift-ui/iosAppSwiftUI.xcodeproj \
    -scheme iosAppSwiftUITests \
    -destination "$DESTINATION" \
    -derivedDataPath ios-app-swift-ui/build/ios-tests \
    CODE_SIGN_IDENTITY="" \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGNING_ALLOWED=NO \
    ${FILTER_ARGS[@]+"${FILTER_ARGS[@]}"}
