#!/bin/bash
set -e
cd "$(dirname "$0")/.."

# Update Architecture Diagram
echo "Generating Mermaid definition from build files..."
./gradlew generateMermaidGraph

echo "Done!"

