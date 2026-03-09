#!/bin/bash
# Build the iOS SwiftUI app for simulator.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Building iOS SwiftUI app..."
xcodebuild -project ios-app-swift-ui/iosAppSwiftUI.xcodeproj \
    -scheme iosAppSwiftUI \
    -destination 'generic/platform=iOS Simulator' \
    build \
    CODE_SIGN_IDENTITY="" \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGNING_ALLOWED=NO \
    -derivedDataPath ios-app-swift-ui/build/ios-build
