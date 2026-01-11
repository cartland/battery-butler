#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# Applies Spotless code formatting to the project.
# This script ensures that the code conforms to the project's style guide.
./gradlew spotlessApply
