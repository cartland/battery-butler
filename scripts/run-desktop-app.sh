#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# Runs the desktop application.
./gradlew :compose-app:run
