#!/bin/bash
set -e
if [ -n "$BUILD_WORKSPACE_DIRECTORY" ]; then
  cd "$BUILD_WORKSPACE_DIRECTORY"
else
  cd "$(dirname "$0")/.."
fi

echo "Running unit tests..."
./gradlew test

echo "Running instrumented tests..."
./gradlew :compose-app:pixel5api34Check
