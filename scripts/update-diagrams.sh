#!/bin/bash
set -e

# Update Architecture Diagram
echo "Generating Architecture Diagram..."
npx -y @mermaid-js/mermaid-cli -i docs/diagrams/architecture.mmd -o docs/diagrams/architecture.svg -t default --cssFile ""

echo "Done!"
