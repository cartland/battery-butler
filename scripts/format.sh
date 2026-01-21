#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# Applies Spotless code formatting to the project.
./gradlew spotlessApply
