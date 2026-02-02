#!/bin/bash
set -e
cd "$(dirname "$0")/.."

# Code Metrics Analysis Script
# Analyzes code size and distribution across modules.
#
# Usage: ./scripts/analyze-metrics.sh [--json] [--markdown]
#
# Outputs:
#   --json      Output JSON to stdout (default: .reports/metrics.json)
#   --markdown  Output Markdown to stdout (default: .reports/metrics.md)
#   (no args)   Generate both files in .reports/

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Parse arguments
OUTPUT_JSON=false
OUTPUT_MD=false
if [[ "$1" == "--json" ]]; then
    OUTPUT_JSON=true
elif [[ "$1" == "--markdown" ]]; then
    OUTPUT_MD=true
fi

# Get list of modules (directories with build.gradle.kts)
get_modules() {
    find . -name "build.gradle.kts" -not -path "./buildSrc/*" -not -path "./.gradle/*" | \
        sed 's|/build.gradle.kts||' | \
        sed 's|^\./||' | \
        grep -v '^\.$' | \
        sort
}

# Count lines of code (excluding blanks and comments)
count_loc() {
    local dir="$1"
    local extension="$2"

    find "$dir" -name "*.${extension}" -not -path "*/build/*" -not -path "*/.gradle/*" 2>/dev/null | \
        xargs cat 2>/dev/null | \
        grep -v '^\s*$' | \
        grep -v '^\s*//' | \
        grep -v '^\s*\*' | \
        grep -v '^\s*/\*' | \
        wc -l | \
        tr -d ' ' || echo 0
}

# Count total lines (including blanks)
count_total_lines() {
    local dir="$1"
    local extension="$2"

    find "$dir" -name "*.${extension}" -not -path "*/build/*" -not -path "*/.gradle/*" 2>/dev/null | \
        xargs cat 2>/dev/null | \
        wc -l | \
        tr -d ' ' || echo 0
}

# Count files
count_files() {
    local dir="$1"
    local extension="$2"

    find "$dir" -name "*.${extension}" -not -path "*/build/*" -not -path "*/.gradle/*" 2>/dev/null | \
        wc -l | \
        tr -d ' ' || echo 0
}

# Count classes/interfaces/objects in Kotlin files
count_classes() {
    local dir="$1"

    find "$dir" -name "*.kt" -not -path "*/build/*" -not -path "*/.gradle/*" 2>/dev/null | \
        xargs grep -hE "^(class|interface|object|data class|sealed class|enum class|abstract class)" 2>/dev/null | \
        wc -l | \
        tr -d ' ' || echo 0
}

# Count functions in Kotlin files
count_functions() {
    local dir="$1"

    find "$dir" -name "*.kt" -not -path "*/build/*" -not -path "*/.gradle/*" 2>/dev/null | \
        xargs grep -hE "^\s*(fun|suspend fun|override fun|private fun|internal fun|protected fun)" 2>/dev/null | \
        wc -l | \
        tr -d ' ' || echo 0
}

# Build metrics data
build_metrics_data() {
    local modules=$(get_modules)
    local json_modules="["
    local first=true

    local total_kt_loc=0
    local total_kt_files=0
    local total_classes=0
    local total_functions=0

    for module in $modules; do
        local kt_loc=$(count_loc "$module" "kt")
        local kt_files=$(count_files "$module" "kt")
        local classes=$(count_classes "$module")
        local functions=$(count_functions "$module")

        # Skip modules with no Kotlin code
        [[ "$kt_files" -eq 0 ]] && continue

        local avg_file_size=0
        if [[ "$kt_files" -gt 0 ]]; then
            avg_file_size=$((kt_loc / kt_files))
        fi

        local avg_class_size=0
        if [[ "$classes" -gt 0 ]]; then
            avg_class_size=$((kt_loc / classes))
        fi

        total_kt_loc=$((total_kt_loc + kt_loc))
        total_kt_files=$((total_kt_files + kt_files))
        total_classes=$((total_classes + classes))
        total_functions=$((total_functions + functions))

        $first || json_modules+=","
        first=false

        json_modules+="{\"name\":\"$module\",\"kt_loc\":$kt_loc,\"kt_files\":$kt_files,\"classes\":$classes,\"functions\":$functions,\"avg_file_size\":$avg_file_size,\"avg_class_size\":$avg_class_size}"
    done

    json_modules+="]"

    local avg_total_file=0
    if [[ "$total_kt_files" -gt 0 ]]; then
        avg_total_file=$((total_kt_loc / total_kt_files))
    fi

    echo "{\"modules\":$json_modules,\"totals\":{\"kt_loc\":$total_kt_loc,\"kt_files\":$total_kt_files,\"classes\":$total_classes,\"functions\":$total_functions,\"avg_file_size\":$avg_total_file}}"
}

# Generate Markdown report
generate_markdown() {
    local json_data="$1"

    cat << 'EOF'
# Code Metrics Report

This report analyzes code size and distribution across modules.

## Summary

EOF

    echo "$json_data" | python3 -c '
import json, sys
data = json.loads(sys.stdin.read())
t = data["totals"]
kt_loc, kt_files, classes, functions, avg = t["kt_loc"], t["kt_files"], t["classes"], t["functions"], t["avg_file_size"]
print(f"- **Total Kotlin LOC**: {kt_loc:,}")
print(f"- **Total Kotlin files**: {kt_files}")
print(f"- **Total classes**: {classes}")
print(f"- **Total functions**: {functions}")
print(f"- **Avg file size**: {avg} LOC")
print("")
print("## Module Breakdown")
print("")
print("| Module | LOC | Files | Classes | Functions | Avg File |")
print("|--------|----:|------:|--------:|----------:|---------:|")

modules = sorted(data["modules"], key=lambda m: m["kt_loc"], reverse=True)
for m in modules:
    name, loc, files, cls, funcs, avgf = m["name"], m["kt_loc"], m["kt_files"], m["classes"], m["functions"], m["avg_file_size"]
    print(f"| {name} | {loc:,} | {files} | {cls} | {funcs} | {avgf} |")

print("")
print("## Code Distribution")
print("")
print("```")
max_loc = max(m["kt_loc"] for m in modules) if modules else 1
for m in modules:
    loc = m["kt_loc"]
    bar_len = int((loc / max_loc) * 40)
    bar = "█" * bar_len
    pct = (loc / kt_loc * 100) if kt_loc > 0 else 0
    name = m["name"][:20].ljust(20)
    print(f"{name} {bar} {pct:.1f}%")
print("```")
'

    echo ""
    echo "---"
    echo "*Generated by \`scripts/analyze-metrics.sh\`*"
}

# Main execution
echo -e "${BLUE}Analyzing code metrics...${NC}" >&2

json_data=$(build_metrics_data)

if $OUTPUT_JSON; then
    echo "$json_data"
elif $OUTPUT_MD; then
    generate_markdown "$json_data"
else
    # Generate both files
    mkdir -p .reports
    echo "$json_data" > .reports/metrics.json
    generate_markdown "$json_data" > .reports/metrics.md

    # Print summary - use Python for reliable JSON parsing
    read total_loc total_files <<< $(python3 -c "
import json
import sys
data = json.loads(sys.stdin.read())
print(data['totals']['kt_loc'], data['totals']['kt_files'])
" <<< "$json_data")

    echo -e "${GREEN}Reports generated:${NC}"
    echo "  .reports/metrics.json"
    echo "  .reports/metrics.md"
    echo ""
    echo -e "${GREEN}✓ Analyzed ${total_loc} LOC across ${total_files} Kotlin files${NC}"
fi
