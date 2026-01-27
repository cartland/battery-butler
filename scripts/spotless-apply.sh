#!/bin/bash
set -e

# Run Spotless Apply to format code
echo "Run Spotless Apply..."
./gradlew spotlessApply
echo "Spotless Apply Completed."
