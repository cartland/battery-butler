#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# Builds, installs, and runs the iOS application (SwiftUI) on a simulator.

# Set the bundle identifier for the app
BUNDLE_IDENTIFIER="com.chriscartland.batterybutler.BatteryButler"

# Find an available, booted iPhone simulator
SIMULATOR_UDID=$(xcrun simctl list devices 'iPhone' --json | jq -r '.devices | .[] | .[] | select(.state == "Booted") | .udid' | head -n 1)

# If no simulator is booted, find the latest available iPhone and boot it
if [ -z "$SIMULATOR_UDID" ]; then
  echo "No booted iPhone simulator found. Booting the latest available one..."
  SIMULATOR_UDID=$(xcrun simctl list devices 'iPhone' --json | jq -r '.devices | .[] | .[] | select(.isAvailable == true) | .udid' | tail -n 1)
  
  if [ -z "$SIMULATOR_UDID" ]; then
    echo "No available iPhone simulators found. Please create one in Xcode."
    exit 1
  fi
  
  xcrun simctl boot "$SIMULATOR_UDID"
  # Wait for the simulator to be fully booted
  xcrun simctl wait "$SIMULATOR_UDID" 'booted'
fi

echo "Using simulator: $SIMULATOR_UDID"

# Build the iOS application (SwiftUI scheme)
echo "Building iOS application (SwiftUI)..."
xcodebuild -project ios-app-swift-ui/iosAppSwiftUI.xcodeproj -configuration Debug -scheme iosAppSwiftUI -sdk iphonesimulator build -derivedDataPath ios-app-swift-ui/build

# Uninstall the old application (if it exists)
echo "Uninstalling old application..."
xcrun simctl uninstall "$SIMULATOR_UDID" "$BUNDLE_IDENTIFIER" || true

# Install the application
echo "Installing application..."
# Note: Path depends on Xcode build output structure
xcrun simctl install "$SIMULATOR_UDID" "ios-app-swift-ui/build/Build/Products/Debug-iphonesimulator/BatteryButler.app"

# Launch the application
echo "Launching application..."
xcrun simctl launch "$SIMULATOR_UDID" "$BUNDLE_IDENTIFIER"
