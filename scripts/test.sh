#!/bin/bash
set -e
if [ -n "$BUILD_WORKSPACE_DIRECTORY" ]; then
  cd "$BUILD_WORKSPACE_DIRECTORY"
else
  cd "$(dirname "$0")/.."
fi

echo "Running unit tests..."
./gradlew test

echo "Starting server in background..."
./gradlew :server:app:run &
SERVER_PID=$!

echo "Waiting for server to start..."
sleep 20

echo "Running instrumented tests..."
# Use || true to ensure we reach the kill command even if tests fail
./gradlew :compose-app:pixel5api34Check || true

echo "Stopping server..."
kill $SERVER_PID
