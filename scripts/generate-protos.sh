#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# Ensure Bazel is in PATH (Xcode environment fix)
export PATH="$PATH:/opt/homebrew/bin:/usr/local/bin"

# Build the tarball containing generated protos
bazel build //protos:mobile_protos_tar

# Locate the output tarball
TAR_FILE=".bazel/bin/protos/mobile_protos.tar"

if [ ! -f "$TAR_FILE" ]; then
    echo "Error: Tarball not found at $TAR_FILE"
    exit 1
fi

# Compute SHA256 checksum
# Check for shasum (macOS) or sha256sum (Linux)
if command -v shasum >/dev/null 2>&1; then
    NEW_SHA=$(shasum -a 256 "$TAR_FILE" | awk '{print $1}')
else
    NEW_SHA=$(sha256sum "$TAR_FILE" | awk '{print $1}')
fi

SHA_FILE=".bazel/bin/protos/.mobile_protos.sha256"

# Check if content changed
if [ -f "$SHA_FILE" ]; then
    OLD_SHA=$(cat "$SHA_FILE")
    if [ "$NEW_SHA" == "$OLD_SHA" ]; then
        echo "Protos unchanged (SHA: $NEW_SHA). Skipping update."
        exit 0
    fi
fi

echo "Protos changed (New SHA: $NEW_SHA). Updating files..."

# Define Output Locations
# Java/Kotlin generation is handled by Wire Gradle plugin for Networking module.
# We only need to extract Swift files for iOS App.
SWIFT_OUT="ios-app-swift-ui/Generated/Proto"

# Clean old files
rm -rf "$SWIFT_OUT"
mkdir -p "$SWIFT_OUT"

# Create temp dir for extraction
TEMP_DIR=$(mktemp -d)

# Extract tarball
tar -xf "$TAR_FILE" -C "$TEMP_DIR"

# Move Swift files
if [ -d "$TEMP_DIR/swift" ]; then
    cp -R "$TEMP_DIR/swift/"* "$SWIFT_OUT/"
fi

# Cleanup
rm -rf "$TEMP_DIR"

# Update SHA file
echo "$NEW_SHA" > "$SHA_FILE"

echo "Proto update complete."
