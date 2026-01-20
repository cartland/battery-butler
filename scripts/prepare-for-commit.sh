#!/bin/bash
set -e

echo "--- 1. Spotless Apply ---"
./scripts/spotless-apply.sh

echo "--- 2. Update Screenshots ---"
./scripts/update-screenshots.sh

echo "--- 3. Validate ---"
./scripts/validate.sh

echo "✅ Ready for commit!"
