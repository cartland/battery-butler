#!/bin/bash
set -e

# Script to verify iOS Framework linking to catch dependency mismatches.
# This forces the linker to report "Unbound symbols" as errors instead of warnings.

echo "Verifying iOS Framework Linkage..."
./gradlew :compose-app:linkDebugFrameworkIosSimulatorArm64 \
    -Pkotlin.native.binary.partialLinkage=disable \
    --info

echo "Linkage Verification Successful!"
