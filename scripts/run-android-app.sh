#!/bin/bash
set -ex
cd "$(dirname "$0")/.."
set -ex

# Installs and runs the Android application on a connected device or emulator.
./gradlew :compose-app:installDebug
adb shell am start -n com.chriscartland.batterybutler/.MainActivity
