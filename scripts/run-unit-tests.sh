#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# Runs unit tests for the Compose App module
echo "Running unit tests..."
./gradlew test
