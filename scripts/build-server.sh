#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# Builds the server application.
# This script assembles the server application into a runnable JAR file.
./gradlew :server:app:build
