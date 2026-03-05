#!/bin/bash
# Generate iOS Screenshot Gallery from committed reference PNGs
# This is Option C: generates markdown from existing PNGs (no xcodebuild needed)
set -e
cd "$(dirname "$0")/.."

OUTPUT_FILE="ios-app-swift-ui/SCREENSHOT_GALLERY.md"
REFERENCE_DIR="ios-app-swift-ui/iosAppSwiftUITests/__Snapshots__"

echo "Generating iOS Screenshot Gallery..."

cat > "$OUTPUT_FILE" << 'HEADER'
# iOS Screenshot Gallery

Auto-generated gallery of iOS snapshot test references.

HEADER

if [ ! -d "$REFERENCE_DIR" ]; then
    echo "No snapshots directory found at $REFERENCE_DIR"
    echo "*No snapshots found.*" >> "$OUTPUT_FILE"
    exit 0
fi

CURRENT_CLASS=""
find "$REFERENCE_DIR" -name "*.png" | sort | while read -r filepath; do
    # Extract test class from directory name
    REL_PATH="${filepath#$REFERENCE_DIR/}"
    CLASS_DIR=$(echo "$REL_PATH" | cut -d'/' -f1)
    FILENAME=$(basename "$filepath" .1.png)

    if [ "$CLASS_DIR" != "$CURRENT_CLASS" ]; then
        CURRENT_CLASS="$CLASS_DIR"
        echo "" >> "$OUTPUT_FILE"
        echo "## $CURRENT_CLASS" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    fi

    echo "### $FILENAME" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    echo "![${FILENAME}](iosAppSwiftUITests/__Snapshots__/${REL_PATH})" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
done

echo "Gallery generated at $OUTPUT_FILE"
