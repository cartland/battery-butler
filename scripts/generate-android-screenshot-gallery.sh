#!/bin/bash
# Script to generate a Markdown gallery of all reference screenshots.
# This script scans the android-screenshot-tests/src/screenshotTestDebug/reference directory
# and creates android-screenshot-tests/SCREENSHOT_GALLERY.md.

OUTPUT_FILE="android-screenshot-tests/SCREENSHOT_GALLERY.md"
REFERENCE_DIR="android-screenshot-tests/src/screenshotTestDebug/reference"

# Ensure the output file exists and is empty
echo "<!-- GENERATED FILE - DO NOT EDIT -->" > "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "# Screenshot Gallery" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "Generated on $(date)" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "## Table of Contents" >> "$OUTPUT_FILE"

# Temporary file for content
CONTENT_FILE=$(mktemp)

current_class=""

# Find all PNG files, sort them to group by class/test
find "$REFERENCE_DIR" -name "*.png" | sort | while read -r filepath; do
    # filepath example: android-screenshot-tests/src/screenshotTestDebug/reference/com/chriscartland/batterybutler/androidscreenshottests/MainTabsScreenshotTestKt/DevicesScreenPreviewTest_91052883_0.png
    
    # Extract relative path for markdown link (relative to android-screenshot-tests/)
    # We need to strip "android-screenshot-tests/" from the beginning
    relative_path="${filepath#android-screenshot-tests/}"
    
    # Extract Class Name (Parent Directory Name)
    parent_dir=$(dirname "$filepath")
    class_name=$(basename "$parent_dir")
    
    # Extract Test Name (Filename without extension and hash variants if possible)
    filename=$(basename "$filepath")
    test_name="${filename%.*}"
    
    # Check if we moved to a new class
    if [ "$class_name" != "$current_class" ]; then
        current_class="$class_name"
        echo "" >> "$CONTENT_FILE"
        echo "## $class_name" >> "$CONTENT_FILE"
        
        # Add to Table of Contents
        echo "- [$class_name](#$(echo "$class_name" | tr '[:upper:]' '[:lower:]'))" >> "$OUTPUT_FILE"
    fi
    
    echo "" >> "$CONTENT_FILE"
    echo "### $test_name" >> "$CONTENT_FILE"
    echo "<img src=\"$relative_path\" width=\"300\" />" >> "$CONTENT_FILE"
    
done

# Append content to output file
echo "" >> "$OUTPUT_FILE"
cat "$CONTENT_FILE" >> "$OUTPUT_FILE"

# Cleanup
rm "$CONTENT_FILE"

echo "Screenshot gallery generated at $OUTPUT_FILE"
