#!/bin/bash
set -e

# Update Architecture Diagram
echo "Generating Mermaid definition from build files..."
./gradlew generateMermaidGraph

echo "Generating Architecture Diagram Image..."
npx -y @mermaid-js/mermaid-cli -i docs/diagrams/architecture.mmd -o docs/diagrams/architecture.svg -t default --cssFile ""

echo "Done!"
