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
DEVICE_NAME="iPhone 16"

IOS_VERSION=""
while IFS= read -r line; do
    if [[ "$line" =~ ^--\ iOS\ ([0-9]+\.[0-9]+)\ -- ]]; then
        current_version="${BASH_REMATCH[1]}"
    elif [[ "$line" == *"$DEVICE_NAME"* ]] && [[ "$line" == *"Shutdown"* || "$line" == *"Booted"* ]]; then
        device=$(echo "$line" | sed -E 's/^[[:space:]]+//' | sed -E 's/ \([A-F0-9-]+\).*//')
        if [ "$device" = "$DEVICE_NAME" ]; then
            IOS_VERSION="$current_version"
        fi
    fi
done < <(xcrun simctl list devices available)

if [ -z "$IOS_VERSION" ]; then
    echo "ERROR: No '$DEVICE_NAME' simulator found." >&2
    xcrun simctl list devices available >&2
    exit 1
fi

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
