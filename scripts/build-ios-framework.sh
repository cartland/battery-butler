#!/bin/bash
set -e

# Links the Debug Framework for iOS X64 (Simulator)
# Matches 'iOS Build (Compose UI)' Run Configuration
echo "Linking iOS Debug Framework (X64)..."
./gradlew :compose-app:linkDebugFrameworkIosX64