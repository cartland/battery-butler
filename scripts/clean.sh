#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# Cleans the project by removing build artifacts.
# This is useful for ensuring a clean build.
./gradlew clean
