#!/usr/bin/env bash
#
# Checks Android screenshot reference images for likely rendering failures.
#
# Detects:
# - Blank/tiny PNGs (< 1KB) — the preview rendered empty, usually because
#   it depends on runtime state (ViewModel, DI, CompositionLocal) unavailable
#   in tests
#
# This is a WARNING tool, not a blocker. Broken screenshots may be caused
# by app errors, preview errors, or test infrastructure issues. The goal
# is to surface problems early so they can be prioritized.
#
# Usage: ./scripts/check-screenshot-health.sh
# Exit code: always 0 (warnings only, never blocks)

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
REFERENCE_DIR="$REPO_ROOT/android-screenshot-tests/src/screenshotTestDebug/reference"
BLANK_THRESHOLD=1024  # bytes — PNGs under this are likely blank renders

echo "=== Screenshot Health Check ==="
echo ""

if [ ! -d "$REFERENCE_DIR" ]; then
    echo "Reference directory not found: $REFERENCE_DIR"
    echo "Nothing to check. Run ./scripts/generate-android-screenshots.sh first."
    exit 0
fi

# Find suspiciously small PNGs (likely blank renders).
echo "--- Checking for blank/tiny screenshots (< ${BLANK_THRESHOLD} bytes) ---"
blank_found=0
while IFS= read -r png_file; do
    size=$(wc -c < "$png_file" | tr -d ' ')
    if [ "$size" -lt "$BLANK_THRESHOLD" ]; then
        rel_path="${png_file#"$REPO_ROOT/"}"
        echo "  WARNING: Likely blank (${size}B): $rel_path"
        blank_found=$((blank_found + 1))
    fi
done < <(find "$REFERENCE_DIR" -name "*.png" -type f 2>/dev/null)

if [ "$blank_found" -eq 0 ]; then
    echo "  All screenshots are >= ${BLANK_THRESHOLD} bytes"
else
    echo ""
    echo "  $blank_found likely blank screenshot(s) found."
    echo "  These previews probably depend on runtime state (ViewModel,"
    echo "  CompositionLocal, DI) that is unavailable in screenshot tests."
    echo "  Fix by creating stateless preview overloads that accept demo"
    echo "  data as parameters."
fi
echo ""

total=$(find "$REFERENCE_DIR" -name "*.png" -type f 2>/dev/null | wc -l | tr -d ' ')
healthy=$((total - blank_found))

echo "--- Summary ---"
echo "  Total screenshots: $total"
echo "  Likely blank:      $blank_found"
echo "  Healthy:           $healthy"

# Always exit 0 — this is a warning tool, not a gate.
exit 0
