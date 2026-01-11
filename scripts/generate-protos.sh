#!/bin/bash
set -ex
cd "$(dirname "$0")/.."

# Build the protos
bazel build //protos:battery_butler_java_proto //protos:battery_butler_swift_proto

# Java Output Location
JAVA_OUT="networking/src/generated/java"
rm -rf "$JAVA_OUT"
mkdir -p "$JAVA_OUT"

# Extract the srcjar. 
# We look for *-src.jar in bazel-bin/protos.
# Based on inspection, it seems to be named battery_butler_proto-speed-src.jar or similar.
# We pick the first matching src jar.
SRC_JAR=$(find bazel-bin/protos -name "*-src.jar" | head -n 1)

if [ -z "$SRC_JAR" ]; then
    echo "Error: Could not find Java source jar in bazel-bin/protos"
    exit 1
fi

echo "Extracting Java sources from $SRC_JAR"
unzip -o "$SRC_JAR" -d "$JAVA_OUT"

# Usage of 'src' jar might include META-INF etc, we only care about com/chriscartland...
# But unzip extracts everything. It is fine as long as package structure 'com/...' is preserved.


# Swift Output Location
SWIFT_OUT="ios-app-swift-ui/Generated/Proto"
rm -rf "$SWIFT_OUT"
mkdir -p "$SWIFT_OUT"

# Find generated swift files.
# They might be in a temporary directory under bazel-bin.
echo "Copying Swift sources..."
# Copy Swift sources.
# Use -f to overwrite if duplicates exist (though ideally we shouldn't have them).
find bazel-bin/protos -name "*.swift" -exec cp -f {} "$SWIFT_OUT" \;

# Verify Swift files exist
count=$(ls "$SWIFT_OUT"/*.swift 2>/dev/null | wc -l)
if [ "$count" -eq "0" ]; then
    echo "Warning: No Swift files found copied to $SWIFT_OUT. Check Bazel output."
    # List bazel-bin/protos to help debug if needed (but we are in script)
fi
