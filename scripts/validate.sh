#!/bin/bash
set -ex

# Cleans, builds, checks, and tests the entire project.
# This script is a comprehensive validation of the project's integrity.
./gradlew clean check
