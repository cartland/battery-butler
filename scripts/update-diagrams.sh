#!/bin/bash
set -e

# Update Architecture Diagram
echo "Generating Mermaid definition from build files..."
./gradlew generateMermaidGraph

echo "Done!"

