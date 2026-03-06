#!/bin/bash
# Record iOS SwiftUI snapshot references
# Uses swift-snapshot-testing's SNAPSHOT_TESTING_RECORD env var to
# overwrite all reference PNGs with fresh renders from the current environment.
set -e
cd "$(dirname "$0")/.."

echo "Compiling iOS Kotlin modules..."
./gradlew :compose-app:compileKotlinIosSimulatorArm64 \
    :ios-swift-di:compileKotlinIosSimulatorArm64

echo "Recording iOS snapshots..."
SNAPSHOT_TESTING_RECORD=all xcodebuild test \
    -project ios-app-swift-ui/iosAppSwiftUI.xcodeproj \
    -scheme iosAppSwiftUITests \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -derivedDataPath build/ios-tests \
    CODE_SIGN_IDENTITY="" \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGNING_ALLOWED=NO \
    || true  # Recording mode causes XCTFail — expected

echo "iOS snapshots recorded."
