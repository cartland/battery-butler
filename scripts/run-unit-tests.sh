#!/bin/bash
set -e

# Runs unit tests for the Compose App module
echo "Running unit tests..."
./gradlew :compose-app:testDebugUnitTest
