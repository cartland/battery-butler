#!/bin/bash
# Sync iOS SwiftUI files with the Xcode project.
# Adds/removes source and test files in the pbxproj to match the filesystem.
set -euo pipefail
cd "$(dirname "$0")/../ios-app-swift-ui"

echo "Syncing Xcode project..."
ruby sync_pbxproj.rb
