#!/bin/bash
# =============================================================================
# EMBED MERMAID CONTENT INTO MARKDOWN
# =============================================================================
#
# PURPOSE:
#   Splices the contents of a .mmd file into a markdown file between unique
#   HTML comment markers, wrapping the content in a fenced mermaid code block.
#
# USAGE:
#   ./scripts/embed-mermaid.sh                           # Default files
#   ./scripts/embed-mermaid.sh <mmd-file> <target-file>  # Explicit files
#
# MARKERS:
#   <!-- GENERATED:BEGIN code_distribution.mmd -->
#   <!-- GENERATED:END code_distribution.mmd -->
#
# The script is idempotent — running it twice produces the same result.
#
# =============================================================================

set -euo pipefail

MMD_FILE="${1:-docs/diagrams/code_distribution.mmd}"
TARGET_FILE="${2:-docs/CODE_ANALYSIS.md}"

# Validate inputs
if [[ ! -f "$MMD_FILE" ]]; then
    echo "ERROR: Mermaid source file not found: $MMD_FILE" >&2
    exit 1
fi

if [[ ! -f "$TARGET_FILE" ]]; then
    echo "ERROR: Target markdown file not found: $TARGET_FILE" >&2
    exit 1
fi

MMD_BASENAME=$(basename "$MMD_FILE")
BEGIN_MARKER="<!-- GENERATED:BEGIN $MMD_BASENAME -->"
END_MARKER="<!-- GENERATED:END $MMD_BASENAME -->"

# Verify markers exist in target file
if ! grep -qF -- "$BEGIN_MARKER" "$TARGET_FILE"; then
    echo "ERROR: Begin marker not found in $TARGET_FILE" >&2
    echo "  Expected: $BEGIN_MARKER" >&2
    exit 1
fi

if ! grep -qF -- "$END_MARKER" "$TARGET_FILE"; then
    echo "ERROR: End marker not found in $TARGET_FILE" >&2
    echo "  Expected: $END_MARKER" >&2
    exit 1
fi

# Use awk to replace everything between markers (inclusive of old content,
# preserving the markers themselves). The .mmd file is read inside awk
# to avoid multi-line string issues with -v.
awk -v begin="$BEGIN_MARKER" -v end="$END_MARKER" -v mmd_file="$MMD_FILE" '
    $0 == begin {
        print
        print "```mermaid"
        while ((getline line < mmd_file) > 0) print line
        close(mmd_file)
        print "```"
        skip = 1
        next
    }
    $0 == end {
        skip = 0
        print
        next
    }
    !skip { print }
' "$TARGET_FILE" > "${TARGET_FILE}.tmp"

mv "${TARGET_FILE}.tmp" "$TARGET_FILE"

echo "Embedded $MMD_FILE into $TARGET_FILE"
