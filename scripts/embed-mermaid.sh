#!/bin/bash
# =============================================================================
# EMBED MERMAID CONTENT INTO MARKDOWN
# =============================================================================
#
# PURPOSE:
#   Splices the contents of a .mmd file into markdown files between unique
#   HTML comment markers, wrapping the content in a fenced mermaid code block.
#
# USAGE:
#   ./scripts/embed-mermaid.sh                                    # Default files
#   ./scripts/embed-mermaid.sh <mmd-file> <target1> [target2...]  # Explicit files
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
shift || true

# Default target files if none specified
if [[ $# -eq 0 ]]; then
    TARGET_FILES=("docs/CODE_ANALYSIS.md" "README.md")
else
    TARGET_FILES=("$@")
fi

# Validate mmd source
if [[ ! -f "$MMD_FILE" ]]; then
    echo "ERROR: Mermaid source file not found: $MMD_FILE" >&2
    exit 1
fi

MMD_BASENAME=$(basename "$MMD_FILE")
BEGIN_MARKER="<!-- GENERATED:BEGIN $MMD_BASENAME -->"
END_MARKER="<!-- GENERATED:END $MMD_BASENAME -->"

embed_into_file() {
    local target="$1"

    if [[ ! -f "$target" ]]; then
        echo "WARNING: Target file not found, skipping: $target" >&2
        return 0
    fi

    # Check for markers
    if ! grep -qF -- "$BEGIN_MARKER" "$target"; then
        echo "WARNING: Begin marker not found in $target, skipping" >&2
        return 0
    fi

    if ! grep -qF -- "$END_MARKER" "$target"; then
        echo "ERROR: End marker not found in $target" >&2
        return 1
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
    ' "$target" > "${target}.tmp"

    mv "${target}.tmp" "$target"
    echo "Embedded $MMD_FILE into $target"
}

# Embed into each target file
for target in "${TARGET_FILES[@]}"; do
    embed_into_file "$target"
done
