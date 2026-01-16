#!/bin/bash
set -ex
if [ -n "$BUILD_WORKSPACE_DIRECTORY" ]; then
  cd "$BUILD_WORKSPACE_DIRECTORY"
else
  cd "$(dirname "$0")/.."
fi
set -ex

# Installs and runs the Android application on a connected device or emulator.
./gradlew :compose-app:installDebug
adb shell am start -n com.chriscartland.batterybutler/.composeapp.MainActivity
