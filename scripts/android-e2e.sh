#!/bin/bash
set -e
cd "$(dirname "$0")/.."

# Build Server
echo "Building server..."
./gradlew :server:app:installDist

# Start Server
echo "Starting local server..."
./server/app/build/install/app/bin/app &
SERVER_PID=$!
trap "kill $SERVER_PID 2>/dev/null || true" EXIT

# Wait for server
echo "Waiting for server on port 50051..."
for i in $(seq 1 60); do
    if nc -z localhost 50051 2>/dev/null; then
        echo "Server ready."
        break
    fi
     if [ "$i" -eq 60 ]; then
        echo "ERROR: Server failed to start."
        exit 1
    fi
    sleep 1
done

# Run Test using Gradle Managed Device (pixel5api34)
# This will create and launch an isolated emulator automatically
echo "Running Android E2E Tests on pixel5api34..."
./gradlew :compose-app:pixel5api34DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chriscartland.batterybutler.composeapp.e2e.AndroidE2eTest

echo "E2E Tests Completed Successfully."
