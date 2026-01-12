#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# CI Parity Validation Script
# This script mimics the steps defined in .github/workflows/ci.yml
# Use this to verify that your changes will likely pass CI.

echo "--- 1. Clean Project ---"
./gradlew clean

echo "--- 2. Formatting (Spotless) ---"
./gradlew spotlessCheck --stacktrace

echo "--- 3. Lint ---"
./gradlew lint --stacktrace

echo "--- 4. Unit Tests ---"
./gradlew test --stacktrace

echo "--- 5. Build Android App ---"
./gradlew :compose-app:assembleDebug

echo "--- 6. Build Desktop App ---"
./gradlew :compose-app:packageDistributionForCurrentOS

echo "--- 7. Build Server ---"
./gradlew :server:app:build

# iOS checks - Only run on macOS
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "--- 8. iOS Checks (macOS detected) ---"
    
    # Check for Xcode and xcodebuild
    if command -v xcodebuild >/dev/null 2>&1; then
        echo "Building iOS App (Compose UI)..."
        xcodebuild -scheme iosApp -project ios-app-compose-ui/iosApp.xcodeproj -configuration Debug -destination 'generic/platform=iOS Simulator' build CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO -derivedDataPath build
        
        echo "Building iOS App (SwiftUI)..."
        # Using -target because the scheme might not be shared in the .xcodeproj
        # Disabling code signing to avoid 'requires a development team' error during local validation
        xcodebuild -project ios-app-swift-ui/iosAppSwiftUI.xcodeproj -configuration Debug -target iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO -derivedDataPath build
    else
        echo "Warning: xcodebuild not found. Skipping iOS build checks."
    fi
else
    echo "Skipping iOS checks (not on macOS)."
fi

echo "--- Validation Complete! ---"
