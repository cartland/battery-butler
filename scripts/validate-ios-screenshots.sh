#!/bin/bash
# Validate iOS screenshot references: light/dark pairing and gallery link integrity.
set -e
cd "$(dirname "$0")/.."

REFERENCE_DIR="ios-app-swift-ui/iosAppSwiftUITests/__Snapshots__"
GALLERY_FILE="ios-app-swift-ui/SCREENSHOT_GALLERY.md"
ERRORS=0
ERROR_TMPFILE=$(mktemp)

echo "Validating iOS screenshots..."

# --- Check 1: Every .light.png has a .dark.png and vice versa ---
echo ""
echo "Check 1: Light/dark pairing"

for f in $(find "$REFERENCE_DIR" -name "*.light.png" | sort); do
    DARK="${f%.light.png}.dark.png"
    if [ ! -f "$DARK" ]; then
        echo "  MISSING dark pair: $DARK"
        ERRORS=$((ERRORS + 1))
    fi
done

for f in $(find "$REFERENCE_DIR" -name "*.dark.png" | sort); do
    LIGHT="${f%.dark.png}.light.png"
    if [ ! -f "$LIGHT" ]; then
        echo "  MISSING light pair: $LIGHT"
        ERRORS=$((ERRORS + 1))
    fi
done

LIGHT_COUNT=$(find "$REFERENCE_DIR" -name "*.light.png" | wc -l | tr -d ' ')
DARK_COUNT=$(find "$REFERENCE_DIR" -name "*.dark.png" | wc -l | tr -d ' ')
echo "  Found $LIGHT_COUNT light + $DARK_COUNT dark PNGs"

if [ "$LIGHT_COUNT" -eq "$DARK_COUNT" ]; then
    echo "  OK: counts match"
else
    echo "  MISMATCH: $LIGHT_COUNT light vs $DARK_COUNT dark"
    ERRORS=$((ERRORS + 1))
fi

# --- Check 2: Gallery exists and all image src paths resolve ---
echo ""
echo "Check 2: Gallery link integrity"

if [ ! -f "$GALLERY_FILE" ]; then
    echo "  MISSING gallery file: $GALLERY_FILE"
    ERRORS=$((ERRORS + 1))
else
    # Extract all src= paths from img tags (macOS-compatible, no -P)
    grep -o 'src="[^"]*"' "$GALLERY_FILE" | sed 's/src="//;s/"//' | while read -r img_path; do
        FULL_PATH="ios-app-swift-ui/$img_path"
        if [ ! -f "$FULL_PATH" ]; then
            echo "  BROKEN link: $img_path"
            echo "1" >> "$ERROR_TMPFILE"
        fi
    done

    if [ -s "$ERROR_TMPFILE" ]; then
        LINK_ERRORS=$(wc -l < "$ERROR_TMPFILE" | tr -d ' ')
        ERRORS=$((ERRORS + LINK_ERRORS))
        : > "$ERROR_TMPFILE"
        echo "  FAILED: $LINK_ERRORS broken links"
    else
        LINK_COUNT=$(grep -o 'src="' "$GALLERY_FILE" | wc -l | tr -d ' ')
        echo "  OK: all $LINK_COUNT image links resolve"
    fi
fi

# --- Check 3: Every snapshot PNG appears in the gallery ---
echo ""
echo "Check 3: Gallery completeness"

if [ -f "$GALLERY_FILE" ]; then
    MISSING_FROM_GALLERY=0
    for f in $(find "$REFERENCE_DIR" -name "*.png" | sort); do
        REL_PATH="${f#ios-app-swift-ui/}"
        if ! grep -qF "$REL_PATH" "$GALLERY_FILE"; then
            echo "  NOT in gallery: $REL_PATH"
            MISSING_FROM_GALLERY=$((MISSING_FROM_GALLERY + 1))
        fi
    done

    if [ "$MISSING_FROM_GALLERY" -eq 0 ]; then
        TOTAL=$(find "$REFERENCE_DIR" -name "*.png" | wc -l | tr -d ' ')
        echo "  OK: all $TOTAL PNGs referenced in gallery"
    else
        echo "  MISSING: $MISSING_FROM_GALLERY PNGs not in gallery"
        ERRORS=$((ERRORS + MISSING_FROM_GALLERY))
    fi
fi

# --- Summary ---
echo ""
rm -f "$ERROR_TMPFILE"

if [ "$ERRORS" -gt 0 ]; then
    echo "FAILED: $ERRORS error(s) found"
    exit 1
else
    echo "PASSED: all checks OK"
fi
