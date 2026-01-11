#!/bin/bash
set -ex

# Builds the server application.
# This script assembles the server application into a runnable JAR file.
./gradlew :server:app:build
