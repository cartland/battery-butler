#!/bin/bash
set -e
cd "$(dirname "$0")/.."

# Build the tarball containing generated protos
bazel build //protos:mobile_protos_tar

# Locate the output tarball
TAR_FILE="bazel-bin/protos/mobile_protos.tar"

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

SHA_FILE=".mobile_protos.sha256"

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
JAVA_OUT="networking/src/generated/java"
SWIFT_OUT="ios-app-swift-ui/Generated/Proto"

# Clean old files
rm -rf "$JAVA_OUT"
rm -rf "$SWIFT_OUT"
mkdir -p "$JAVA_OUT"
mkdir -p "$SWIFT_OUT"

# Create temp dir for extraction
TEMP_DIR=$(mktemp -d)

# Extract tarball
tar -xf "$TAR_FILE" -C "$TEMP_DIR"

# Move files to destinations
# The tar structure is:
# java/
# swift/

# Move Java files
# We only want to move contents of java/ to JAVA_OUT
if [ -d "$TEMP_DIR/java" ]; then
    # Use CP/MV logic. cp -R is safer across filesystems.
    cp -R "$TEMP_DIR/java/" "$JAVA_OUT/"
    # Remove the 'java' directory itself from destination if cp included it
    # cp -R src/ dest/ puts CONTENTS key if dest exists...
    # The structure in tar is java/com/..., we want networking/src/generated/java/com/...
    # If we did cp -R output/java/* output/java_out/ it works.
    # But JAVA_OUT depends on cp behavior.
    # Let's simple sync.
    # Actually, rsync is best but let's stick to cp.
    # If JAVA_OUT exists (mkdir -p above):
    # cp -R "$TEMP_DIR/java/"* "$JAVA_OUT/" # wildcard expansion might miss hidden files but protos don't have them
fi

# Move Swift files
if [ -d "$TEMP_DIR/swift" ]; then
    cp -R "$TEMP_DIR/swift/"* "$SWIFT_OUT/"
fi

# Cleanup
rm -rf "$TEMP_DIR"

# Update SHA file
echo "$NEW_SHA" > "$SHA_FILE"

echo "Proto update complete."
