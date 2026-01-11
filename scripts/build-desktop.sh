#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# Builds the desktop application.
# This script packages the desktop application for the current operating system.
./gradlew :compose-app:packageDistributionForCurrentOS
