#!/bin/bash
# =============================================================================
# VALIDATE GENERATED FILES
# =============================================================================
#
# PURPOSE:
#   Validates that CI-generated artifacts (diagrams, screenshots, analysis)
#   are present and valid. Fails with clear error messages when something
#   is obviously broken.
#
# USAGE:
#   ./scripts/validate-generated.sh --diagrams     # Validate Mermaid diagrams
#   ./scripts/validate-generated.sh --screenshots  # Validate screenshot baselines
#   ./scripts/validate-generated.sh --analysis     # Validate code share analysis
#   ./scripts/validate-generated.sh --all          # Validate everything
#
# EXIT CODES:
#   0 - All validations passed
#   1 - Validation failed (see error output)
#
# =============================================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
ERRORS=0
WARNINGS=0

# -----------------------------------------------------------------------------
# Helper Functions
# -----------------------------------------------------------------------------

error() {
    echo -e "${RED}ERROR:${NC} $1" >&2
    ((ERRORS++))
}

warning() {
    echo -e "${YELLOW}WARNING:${NC} $1" >&2
    ((WARNINGS++))
}

success() {
    echo -e "${GREEN}✓${NC} $1"
}

info() {
    echo -e "  $1"
}

# Check if a file exists and is non-empty
check_file_exists() {
    local file="$1"
    local description="$2"

    if [[ ! -f "$file" ]]; then
        error "$description not found: $file"
        echo "::error::$description not found: $file"
        echo "::error::This usually means the generation task failed silently."
        echo "::error::Try running the generation command locally to debug."
        return 1
    fi

    if [[ ! -s "$file" ]]; then
        error "$description is empty: $file"
        echo "::error::$description is empty: $file"
        echo "::error::The file exists but contains no content."
        echo "::error::Check if the generation task completed successfully."
        return 1
    fi

    return 0
}

# Count files matching a pattern
count_files() {
    local dir="$1"
    local ext="$2"
    find "$dir" -maxdepth 1 -name "*.$ext" -type f 2>/dev/null | wc -l | xargs
}

# -----------------------------------------------------------------------------
# Diagram Validation
# -----------------------------------------------------------------------------

validate_diagrams() {
    echo ""
    echo "=== Validating Mermaid Diagrams ==="
    echo ""

    local diagram_dir="docs/diagrams"
    local expected_diagrams=("kotlin_module_structure.mmd" "full_system_structure.mmd")
    local min_diagram_size=100  # Minimum bytes for a valid diagram

    # Check diagram directory exists
    if [[ ! -d "$diagram_dir" ]]; then
        error "Diagram directory not found: $diagram_dir"
        echo "::error::Diagram directory not found: $diagram_dir"
        echo "::error::"
        echo "::error::Expected location: docs/diagrams/"
        echo "::error::Run: ./gradlew generateMermaidGraph"
        return 1
    fi

    # Check each expected diagram
    for diagram in "${expected_diagrams[@]}"; do
        local file="$diagram_dir/$diagram"

        if ! check_file_exists "$file" "Diagram $diagram"; then
            echo "::error::"
            echo "::error::To generate diagrams, run:"
            echo "::error::  ./gradlew generateMermaidGraph"
            continue
        fi

        # Check minimum size
        local size=$(wc -c < "$file" | tr -d ' ')
        if [[ "$size" -lt "$min_diagram_size" ]]; then
            error "Diagram $diagram is suspiciously small ($size bytes)"
            echo "::error::Diagram $diagram is only $size bytes (expected >$min_diagram_size)"
            echo "::error::This usually means the project structure wasn't parsed correctly."
            continue
        fi

        # Check for Mermaid syntax markers
        if ! grep -q "graph\|flowchart\|classDiagram\|sequenceDiagram" "$file"; then
            warning "Diagram $diagram may not contain valid Mermaid syntax"
            echo "::warning::Diagram $diagram doesn't contain recognizable Mermaid diagram type"
        fi

        success "Diagram $diagram is valid ($size bytes)"
    done

    # Check for SVG outputs (optional but expected)
    local svg_count=$(count_files "$diagram_dir" "svg")
    if [[ "$svg_count" -eq 0 ]]; then
        warning "No SVG diagrams found - consider generating SVG for documentation"
    else
        info "Found $svg_count SVG diagram(s)"
    fi

    # Count total .mmd files
    local mmd_count=$(count_files "$diagram_dir" "mmd")
    if [[ "$mmd_count" -eq 0 ]]; then
        error "No .mmd files found in $diagram_dir"
        echo "::error::No Mermaid diagram files generated"
        echo "::error::Run: ./gradlew generateMermaidGraph"
    else
        success "Found $mmd_count Mermaid diagram file(s)"
    fi
}

# -----------------------------------------------------------------------------
# Screenshot Validation
# -----------------------------------------------------------------------------

validate_screenshots() {
    echo ""
    echo "=== Validating Screenshot Baselines ==="
    echo ""

    local screenshot_dir="android-screenshot-tests/src/screenshotTestDebug/reference"
    local min_screenshots=10  # Expect at least this many screenshots
    local min_image_size=500  # Minimum bytes for a valid PNG (some icons are small)

    # Check screenshot directory exists
    if [[ ! -d "$screenshot_dir" ]]; then
        error "Screenshot directory not found: $screenshot_dir"
        echo "::error::Screenshot baseline directory not found"
        echo "::error::"
        echo "::error::Expected location: $screenshot_dir"
        echo "::error::Run: ./gradlew :android-screenshot-tests:updateDebugScreenshotTest"
        return 1
    fi

    # Count screenshots
    local screenshot_count=$(find "$screenshot_dir" -name "*.png" -type f | wc -l | tr -d ' ')

    if [[ "$screenshot_count" -eq 0 ]]; then
        error "No screenshot baselines found"
        echo "::error::Screenshot directory exists but contains no .png files"
        echo "::error::"
        echo "::error::This usually means:"
        echo "::error::  1. Screenshot tests haven't been run"
        echo "::error::  2. The update task failed silently"
        echo "::error::"
        echo "::error::Run: ./gradlew :android-screenshot-tests:updateDebugScreenshotTest"
        return 1
    fi

    if [[ "$screenshot_count" -lt "$min_screenshots" ]]; then
        warning "Only $screenshot_count screenshots found (expected at least $min_screenshots)"
        echo "::warning::Suspiciously low screenshot count: $screenshot_count"
        echo "::warning::Expected at least $min_screenshots screenshots"
        echo "::warning::Some screenshot tests may have been skipped or failed"
    else
        success "Found $screenshot_count screenshot baseline(s)"
    fi

    # Check for empty/corrupt images
    local empty_count=0
    while IFS= read -r -d '' file; do
        local size=$(wc -c < "$file" | tr -d ' ')
        if [[ "$size" -lt "$min_image_size" ]]; then
            ((empty_count++))
            if [[ "$empty_count" -le 3 ]]; then
                warning "Screenshot may be corrupt (too small): $(basename "$file")"
            fi
        fi
    done < <(find "$screenshot_dir" -name "*.png" -type f -print0)

    if [[ "$empty_count" -gt 0 ]]; then
        warning "$empty_count screenshot(s) may be corrupt or empty"
        echo "::warning::$empty_count screenshot(s) are suspiciously small (<$min_image_size bytes)"
    fi
}

# -----------------------------------------------------------------------------
# Code Share Analysis Validation
# -----------------------------------------------------------------------------

validate_analysis() {
    echo ""
    echo "=== Validating Code Share Analysis ==="
    echo ""

    local analysis_file="docs/Code_Share_Analysis.md"
    local min_analysis_size=500  # Minimum bytes for a valid analysis

    # Check file exists
    if ! check_file_exists "$analysis_file" "Code share analysis"; then
        echo "::error::"
        echo "::error::To generate analysis, run:"
        echo "::error::  ./gradlew analyzeCodeShare"
        return 1
    fi

    # Check minimum size
    local size=$(wc -c < "$analysis_file" | tr -d ' ')
    if [[ "$size" -lt "$min_analysis_size" ]]; then
        error "Code share analysis is suspiciously small ($size bytes)"
        echo "::error::Analysis file is only $size bytes (expected >$min_analysis_size)"
        echo "::error::The analysis may have failed to parse modules correctly."
        return 1
    fi

    success "Code share analysis is valid ($size bytes)"

    # Check for expected content markers
    if ! grep -qi "module\|platform\|share\|common" "$analysis_file"; then
        warning "Analysis may not contain expected module/platform information"
        echo "::warning::Analysis file doesn't mention modules or platforms"
        echo "::warning::Content may not reflect actual code sharing"
    fi

    # Count bullet points/sections (the analysis uses bullet lists, not tables)
    local bullet_count
    bullet_count=$(grep -c "^\* " "$analysis_file" 2>/dev/null) || bullet_count=0
    if [[ "$bullet_count" -lt 5 ]]; then
        warning "Analysis has fewer bullet items than expected ($bullet_count items)"
    else
        info "Analysis contains $bullet_count bullet items"
    fi
}

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------

show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --diagrams     Validate Mermaid diagrams"
    echo "  --screenshots  Validate screenshot baselines"
    echo "  --analysis     Validate code share analysis"
    echo "  --all          Validate everything"
    echo "  --help         Show this help message"
    echo ""
    echo "Exit codes:"
    echo "  0 - All validations passed"
    echo "  1 - One or more validations failed"
}

main() {
    local validate_diagrams=false
    local validate_screenshots=false
    local validate_analysis=false

    # Parse arguments
    if [[ $# -eq 0 ]]; then
        show_usage
        exit 1
    fi

    for arg in "$@"; do
        case $arg in
            --diagrams)
                validate_diagrams=true
                ;;
            --screenshots)
                validate_screenshots=true
                ;;
            --analysis)
                validate_analysis=true
                ;;
            --all)
                validate_diagrams=true
                validate_screenshots=true
                validate_analysis=true
                ;;
            --help)
                show_usage
                exit 0
                ;;
            *)
                echo "Unknown option: $arg"
                show_usage
                exit 1
                ;;
        esac
    done

    # Run validations
    if $validate_diagrams; then
        validate_diagrams || true
    fi

    if $validate_screenshots; then
        validate_screenshots || true
    fi

    if $validate_analysis; then
        validate_analysis || true
    fi

    # Summary
    echo ""
    echo "=== Validation Summary ==="
    echo ""

    if [[ "$ERRORS" -gt 0 ]]; then
        echo -e "${RED}$ERRORS error(s)${NC}"
        echo ""
        echo "::error::Validation failed with $ERRORS error(s)"
        echo "::error::See above for details and fix suggestions."
        exit 1
    elif [[ "$WARNINGS" -gt 0 ]]; then
        echo -e "${YELLOW}$WARNINGS warning(s), no errors${NC}"
        success "Validation passed with warnings"
        exit 0
    else
        success "All validations passed"
        exit 0
    fi
}

main "$@"
