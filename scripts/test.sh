#!/bin/bash
set -ex
if [ -n "$BUILD_WORKSPACE_DIRECTORY" ]; then
  cd "$BUILD_WORKSPACE_DIRECTORY"
else
  cd "$(dirname "$0")/.."
fi

echo "Running unit tests..."
./gradlew test

echo "Running instrumented tests (compose-app)..."
./gradlew :compose-app:pixel5api34DebugAndroidTest

echo "Running instrumented tests (data)..."
./gradlew :data:connectedAndroidDeviceTest

echo "Cleaning up managed devices..."
./gradlew :compose-app:cleanManagedDevices
