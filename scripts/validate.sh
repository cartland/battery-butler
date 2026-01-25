#!/bin/bash
set -ex
if [ -n "$BUILD_WORKSPACE_DIRECTORY" ]; then
  cd "$BUILD_WORKSPACE_DIRECTORY"
else
  cd "$(dirname "$0")/.."
fi

# CI Parity Validation Script
# This script mimics the steps defined in .github/workflows/ci.yml
# Use this to verify that your changes will likely pass CI.

echo "--- 1. Clean Project ---"
echo "--- 1. Clean Project - SKIPPED (Relying on Incremental Build) ---"
# ./gradlew clean

echo "--- 2. Formatting (Spotless) ---"
./gradlew spotlessCheck --stacktrace

echo "--- 3. Lint ---"
./gradlew lint --stacktrace

echo "--- 3b. Detekt ---"
./gradlew detekt --stacktrace

echo "--- 4. Tests (Unit & Instrumented) ---"
./scripts/test.sh

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
        echo "Compiling iOS Swift DI module..."
        ./gradlew :ios-swift-di:compileKotlinIosSimulatorArm64

        echo "Strict Linkage Verification..."
        ./gradlew :compose-app:verifyIosFrameworkLinkage -Pkotlin.native.binary.partialLinkage=disable

        echo "Building iOS App (Compose UI)..."
        xcodebuild -project ios-app-compose-ui/iosAppComposeUI.xcodeproj -configuration Debug -scheme iosAppComposeUI -destination 'generic/platform=iOS Simulator' clean build CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO CONFIGURATION_BUILD_DIR=build/ios-compose/
        
        echo "Building iOS App (SwiftUI)..."
        # Using -scheme because the scheme is shared
        # Disabling code signing to avoid 'requires a development team' error during local validation
        xcodebuild -project ios-app-swift-ui/iosAppSwiftUI.xcodeproj -configuration Debug -scheme iosAppSwiftUI -destination 'generic/platform=iOS Simulator' clean build CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO CONFIGURATION_BUILD_DIR=build/ios-swiftui/
    else
        echo "Warning: xcodebuild not found. Skipping iOS build checks."
    fi
else
    echo "Skipping iOS checks (not on macOS)."
fi

echo "--- Validation Complete! ---"
