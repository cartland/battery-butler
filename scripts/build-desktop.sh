#!/bin/bash
set -ex

# Builds the desktop application.
# This script packages the desktop application for the current operating system.
./gradlew :compose-app:packageDistributionForCurrentOS
