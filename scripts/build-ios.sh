#!/bin/bash
set -ex

# Builds the iOS application.
# This script builds the iOS application for the simulator.
xcodebuild -scheme iosApp -project ios-app-compose-ui/iosApp.xcodeproj -configuration Debug -sdk iphonesimulator build -derivedDataPath ios-app-compose-ui/build