#!/bin/bash
# End-to-end tests against a real gRPC server.
#
# Usage:
#   ./scripts/e2e-tests.sh              # Against local server (auto-started)
#   ./scripts/e2e-tests.sh --remote     # Against E2E_SERVER_URL env var
#   E2E_SERVER_URL=http://...:80 ./scripts/e2e-tests.sh --remote

set -e
cd "$(dirname "$0")/.."

if [ "$1" = "--remote" ]; then
    URL="${E2E_SERVER_URL:?Set E2E_SERVER_URL for remote testing}"
else
    echo "Starting local server in background..."
    ./gradlew :server:app:run &
    SERVER_PID=$!
    trap "kill $SERVER_PID 2>/dev/null || true" EXIT

    # Poll until gRPC port is ready (up to 60s — server compile can be slow)
    echo "Waiting for server to start on port 50051..."
    for i in $(seq 1 60); do
        if nc -z localhost 50051 2>/dev/null; then
            echo "Server is ready on port 50051."
            break
        fi
        if [ "$i" -eq 60 ]; then
            echo "ERROR: Server failed to start within 60 seconds."
            exit 1
        fi
        sleep 1
    done
    URL="http://localhost:50051"
fi

echo "Running E2E tests against: $URL"
./gradlew :e2e-tests:test -De2e.server.url="$URL" --info
