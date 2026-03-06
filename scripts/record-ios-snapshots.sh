#!/bin/bash
# Record iOS snapshot test references.
# Uses SNAPSHOT_TESTING_RECORD=all to overwrite existing PNGs.
set -e
cd "$(dirname "$0")/.."

echo "Recording iOS snapshots..."
SNAPSHOT_TESTING_RECORD=all xcodebuild test \
  -project ios-app-swift-ui/iosAppSwiftUI.xcodeproj \
  -scheme iosAppSwiftUITests \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=latest' \
  -derivedDataPath ios-app-swift-ui/build/ios-tests \
  CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO

echo "Snapshots recorded."
