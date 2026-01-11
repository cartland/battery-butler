#!/bin/bash
set -ex
cd "$(dirname "$0")/.."
./gradlew :compose-app:wasmJsBrowserDevelopmentRun
