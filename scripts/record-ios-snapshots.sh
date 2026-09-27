#!/bin/bash
# Record iOS SwiftUI snapshot references
# Uses swift-snapshot-testing's SNAPSHOT_TESTING_RECORD env var to
# overwrite all reference PNGs with fresh renders from the current environment.
#
# In recording mode, every assertSnapshot call intentionally fails (XCTFail)
# so that xcodebuild exits non-zero. This script distinguishes expected
# recording failures from real build/crash errors.
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

# --- Compile KMP modules ---
echo "Compiling iOS Kotlin modules..."
./gradlew :compose-app:compileKotlinIosSimulatorArm64 \
    :ios-swift-di:compileKotlinIosSimulatorArm64

# --- Record snapshots ---
echo "Recording iOS snapshots..."
XCODEBUILD_LOG=$(mktemp)
trap 'rm -f "$XCODEBUILD_LOG"' EXIT

set +e
SNAPSHOT_TESTING_RECORD=all xcodebuild test \
    -project ios-app-swift-ui/iosAppSwiftUI.xcodeproj \
    -scheme iosAppSwiftUITests \
    -destination "$DESTINATION" \
    -derivedDataPath build/ios-tests \
    CODE_SIGN_IDENTITY="" \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGNING_ALLOWED=NO \
    2>&1 | tee "$XCODEBUILD_LOG"
XCODE_EXIT=${PIPESTATUS[0]}
set -e

# --- Analyze results ---
# In recording mode, "TEST FAILED" is expected (every snapshot records then XCTFails).
# A real problem is when xcodebuild can't even build or a test crashes.
if grep -q "BUILD FAILED" "$XCODEBUILD_LOG"; then
    echo ""
    echo "ERROR: xcodebuild build failed (not a recording failure)." >&2
    exit 1
fi

# Count test results from the final summary line
TESTS_EXECUTED=$(grep -oE 'Executed [0-9]+ tests?' "$XCODEBUILD_LOG" | tail -1 | grep -oE '[0-9]+' || echo "0")
UNEXPECTED=$(grep -oE '[0-9]+ unexpected' "$XCODEBUILD_LOG" | tail -1 | grep -oE '[0-9]+' || echo "0")

if [ "$TESTS_EXECUTED" = "0" ]; then
    echo ""
    echo "ERROR: No tests were executed. Check simulator availability." >&2
    exit 1
fi

if [ "$UNEXPECTED" != "0" ]; then
    echo ""
    echo "WARNING: $UNEXPECTED unexpected failure(s) — a test may have crashed." >&2
    echo "Review the xcodebuild output above for details."
    exit 1
fi

# Count recorded PNGs
SNAPSHOT_DIR="ios-app-swift-ui/iosAppSwiftUITests/__Snapshots__"
PNG_COUNT=$(find "$SNAPSHOT_DIR" -name '*.png' | wc -l | tr -d ' ')

echo ""
echo "iOS snapshots recorded successfully."
echo "  Tests executed: $TESTS_EXECUTED"
echo "  PNG files in $SNAPSHOT_DIR: $PNG_COUNT"
