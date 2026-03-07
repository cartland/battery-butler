#!/bin/bash
# Generate iOS Screenshot Gallery from committed reference PNGs
set -e
cd "$(dirname "$0")/.."

OUTPUT_FILE="ios-app-swift-ui/SCREENSHOT_GALLERY.md"
REFERENCE_DIR="ios-app-swift-ui/iosAppSwiftUITests/__Snapshots__"

echo "Generating iOS Screenshot Gallery..."

if [ ! -d "$REFERENCE_DIR" ]; then
    echo "No snapshots directory found at $REFERENCE_DIR"
    echo "# iOS Screenshot Gallery" > "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    echo "*No snapshots found.*" >> "$OUTPUT_FILE"
    exit 0
fi

# Build content into a temp file, then assemble with ToC
CONTENT_FILE=$(mktemp)
TOC_FILE=$(mktemp)
CURRENT_CLASS=""

find "$REFERENCE_DIR" -name "*.png" | sort | while read -r filepath; do
    REL_PATH="${filepath#ios-app-swift-ui/}"
    CLASS_DIR=$(echo "${filepath#$REFERENCE_DIR/}" | cut -d'/' -f1)
    FILENAME=$(basename "$filepath" .1.png)

    if [ "$CLASS_DIR" != "$CURRENT_CLASS" ]; then
        CURRENT_CLASS="$CLASS_DIR"
        ANCHOR=$(echo "$CURRENT_CLASS" | tr '[:upper:]' '[:lower:]')
        echo "- [$CURRENT_CLASS](#$ANCHOR)" >> "$TOC_FILE"
        echo "" >> "$CONTENT_FILE"
        echo "## $CURRENT_CLASS" >> "$CONTENT_FILE"
    fi

    echo "" >> "$CONTENT_FILE"
    echo "### $FILENAME" >> "$CONTENT_FILE"
    echo "" >> "$CONTENT_FILE"
    echo "<img src=\"$REL_PATH\" width=\"300\" />" >> "$CONTENT_FILE"
done

# Assemble final file
echo "<!-- GENERATED FILE - DO NOT EDIT -->" > "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "# iOS Screenshot Gallery" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "Generated on $(date -u '+%Y-%m-%d %H:%M:%S UTC')" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "## Table of Contents" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
cat "$TOC_FILE" >> "$OUTPUT_FILE"
cat "$CONTENT_FILE" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

rm -f "$CONTENT_FILE" "$TOC_FILE"

echo "Gallery generated at $OUTPUT_FILE"
