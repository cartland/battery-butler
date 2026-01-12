#!/bin/bash
set -e
cd "$(dirname "$0")/.."

# Runs unit tests for the Compose App module
echo "Running unit tests..."
./gradlew :compose-app:testDebugUnitTest :domain:test
