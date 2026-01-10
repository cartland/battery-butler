#!/bin/bash
set -ex

# Builds the Android application.
# This script assembles the debug version of the Android application.
./gradlew :compose-app:assembleDebug
