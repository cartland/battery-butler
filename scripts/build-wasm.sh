#!/bin/bash
set -ex

# Builds the WebAssembly application.
# This script builds the WebAssembly browser application.
./gradlew :compose-app:wasmJsBrowserDistribution
