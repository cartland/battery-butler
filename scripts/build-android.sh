#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# Builds the Android application.
# This script assembles the debug version of the Android application.
./gradlew :compose-app:assembleDebug
