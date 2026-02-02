#!/bin/bash
set -e
cd "$(dirname "$0")/.."

# Architecture Analysis Script
# Analyzes module dependencies and validates clean architecture principles.
#
# Usage: ./scripts/analyze-architecture.sh [--json] [--markdown]
#
# Outputs:
#   --json      Output JSON to stdout (default: .reports/architecture.json)
#   --markdown  Output Markdown to stdout (default: .reports/architecture.md)
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

# Define architecture layers (outer to inner)
# Outer layers can depend on inner layers, but not vice versa
# Using a function instead of associative array for bash 3.x compatibility
get_layer_level() {
    local module="$1"
    case "$module" in
        # App/Presentation layer (outermost)
        compose-app|android-screenshot-tests|ios-swift-di) echo 5 ;;

        # ViewModel layer
        viewmodel|presentation-feature|presentation-core) echo 4 ;;

        # UseCase layer
        usecase) echo 3 ;;

        # Data layer
        data|data-local|data-network) echo 2 ;;

        # Domain layer (innermost - should have no dependencies on other layers)
        domain|presentation-model) echo 1 ;;

        # Shared/Utility (can be used by any layer)
        ai|fixtures|compose-resources|test-common) echo 0 ;;

        # Server modules (separate architecture)
        server/app) echo 5 ;;
        server/data) echo 2 ;;
        server/domain) echo 1 ;;

        # Unknown modules
        *) echo 99 ;;
    esac
}

# Extract dependencies from a build.gradle.kts file
extract_dependencies() {
    local file="$1"
    local module_name="$2"

    # Extract project dependencies (exclude commented lines)
    grep -E "(implementation|api)\s*\(?\s*(project|projects)\s*[\(:]" "$file" 2>/dev/null | \
        grep -v '^\s*//' | \
        sed -E 's/.*project[s]?\s*[\(:]?\s*[":]*([^":\)]+)[":]*\)?.*/\1/' | \
        sed 's/^://' | \
        sort -u || true
}

# Check if dependency violates layer rules
check_layer_violation() {
    local from_module="$1"
    local to_module="$2"

    local from_level=$(get_layer_level "$from_module")
    local to_level=$(get_layer_level "$to_module")

    # Skip if either module is unknown or utility (level 0)
    # Utility modules can depend on any layer and be depended on by any layer
    if [[ $from_level -eq 99 ]] || [[ $to_level -eq 99 ]] || [[ $from_level -eq 0 ]] || [[ $to_level -eq 0 ]]; then
        return 1
    fi

    # Violation if depending on outer layer (higher level)
    if [[ $to_level -gt $from_level ]]; then
        return 0
    fi

    return 1
}

# Detect circular dependencies using DFS
# Returns cycles as newline-separated strings
detect_circular_dependencies() {
    local modules_json="$1"

    python3 -c "
import json
import sys

data = json.loads(sys.stdin.read())
modules = {m['name']: m['dependencies'] for m in data['modules']}

def find_cycles():
    visited = set()
    rec_stack = set()
    cycles = []

    def dfs(node, path):
        if node in rec_stack:
            # Found a cycle - extract the cycle portion
            cycle_start = path.index(node)
            cycle = path[cycle_start:] + [node]
            cycles.append(' -> '.join(cycle))
            return
        if node in visited:
            return

        visited.add(node)
        rec_stack.add(node)
        path.append(node)

        for dep in modules.get(node, []):
            if dep in modules:  # Only follow known modules
                dfs(dep, path[:])

        rec_stack.remove(node)

    for module in modules:
        if module not in visited:
            dfs(module, [])

    # Remove duplicate cycles (same cycle can be found from different starting points)
    unique_cycles = []
    seen = set()
    for cycle in cycles:
        parts = cycle.split(' -> ')
        # Normalize by rotating to start with alphabetically smallest
        min_idx = parts[:-1].index(min(parts[:-1]))
        normalized = ' -> '.join(parts[min_idx:-1] + parts[:min_idx] + [parts[min_idx]])
        if normalized not in seen:
            seen.add(normalized)
            unique_cycles.append(cycle)

    for cycle in unique_cycles:
        print(cycle)

find_cycles()
" <<< "$modules_json" 2>/dev/null
}

# Build dependency data
build_dependency_data() {
    local json_modules="["
    local first=true
    local violations=()
    local total_deps=0

    for gradle_file in $(find . -name "build.gradle.kts" -not -path "./buildSrc/*" -not -path "./.gradle/*" | sort); do
        local dir=$(dirname "$gradle_file")
        local module_name="${dir#./}"

        # Skip root build.gradle.kts
        [[ "$module_name" == "." ]] && continue

        local deps=$(extract_dependencies "$gradle_file" "$module_name")
        local dep_array="[]"
        local module_violations="[]"

        if [[ -n "$deps" ]]; then
            dep_array="["
            local dep_first=true
            while IFS= read -r dep; do
                [[ -z "$dep" ]] && continue
                total_deps=$((total_deps + 1))

                $dep_first || dep_array+=","
                dep_first=false
                dep_array+="\"$dep\""

                # Check for layer violations
                if check_layer_violation "$module_name" "$dep"; then
                    local from_level=$(get_layer_level "$module_name")
                    local to_level=$(get_layer_level "$dep")
                    violations+=("$module_name (L$from_level) -> $dep (L$to_level)")

                    [[ "$module_violations" == "[]" ]] && module_violations="["
                    [[ "$module_violations" != "[" ]] && module_violations+=","
                    module_violations+="{\"dependency\":\"$dep\",\"reason\":\"Depends on outer layer\"}"
                fi
            done <<< "$deps"
            dep_array+="]"
            [[ "$module_violations" != "[]" ]] && module_violations+="]"
        fi

        $first || json_modules+=","
        first=false

        local level=$(get_layer_level "$module_name")
        [[ "$level" == "99" ]] && level="null"
        json_modules+="{\"name\":\"$module_name\",\"layer\":$level,\"dependencies\":$dep_array,\"violations\":$module_violations}"
    done

    json_modules+="]"

    # Build violations array
    local json_violations="["
    local v_first=true
    for v in "${violations[@]}"; do
        $v_first || json_violations+=","
        v_first=false
        json_violations+="\"$v\""
    done
    json_violations+="]"

    # Detect circular dependencies
    local temp_json="{\"modules\":$json_modules}"
    local cycles=$(detect_circular_dependencies "$temp_json")
    local json_cycles="["
    local c_first=true
    if [[ -n "$cycles" ]]; then
        while IFS= read -r cycle; do
            [[ -z "$cycle" ]] && continue
            $c_first || json_cycles+=","
            c_first=false
            json_cycles+="\"$cycle\""
        done <<< "$cycles"
    fi
    json_cycles+="]"
    local cycle_count=0
    if [[ -n "$cycles" ]]; then
        cycle_count=$(echo "$cycles" | grep -c . || echo 0)
    fi

    echo "{\"modules\":$json_modules,\"violations\":$json_violations,\"circular_dependencies\":$json_cycles,\"stats\":{\"total_modules\":$(echo "$json_modules" | grep -o '"name"' | wc -l | tr -d ' '),\"total_dependencies\":$total_deps,\"total_violations\":${#violations[@]},\"total_circular\":$cycle_count}}"
}

# Generate Markdown report
generate_markdown() {
    local json_data="$1"

    cat << 'EOF'
# Architecture Analysis Report

This report analyzes the module dependencies and validates clean architecture principles.

## Layer Definitions

| Level | Layer | Description |
|-------|-------|-------------|
| 5 | App/Presentation | UI, platform-specific code |
| 4 | ViewModel | Presentation logic, UI state |
| 3 | UseCase | Business logic orchestration |
| 2 | Data | Repository implementations, data sources |
| 1 | Domain | Core entities, repository interfaces |
| 0 | Utility | Shared utilities (no layer restrictions) |

**Rule**: Inner layers (lower numbers) should NOT depend on outer layers (higher numbers).

## Summary

EOF

    local total_modules=$(echo "$json_data" | grep -o '"total_modules":[0-9]*' | cut -d: -f2)
    local total_deps=$(echo "$json_data" | grep -o '"total_dependencies":[0-9]*' | cut -d: -f2)
    local total_violations=$(echo "$json_data" | grep -o '"total_violations":[0-9]*' | cut -d: -f2)
    local total_circular=$(echo "$json_data" | grep -o '"total_circular":[0-9]*' | cut -d: -f2)

    echo "- **Modules analyzed**: $total_modules"
    echo "- **Total dependencies**: $total_deps"

    if [[ "$total_violations" -eq 0 ]]; then
        echo "- **Layer violations**: 0 ✓"
    else
        echo "- **Layer violations**: $total_violations ✗"
    fi

    if [[ "$total_circular" -eq 0 ]]; then
        echo "- **Circular dependencies**: 0 ✓"
    else
        echo "- **Circular dependencies**: $total_circular ✗"
    fi

    echo ""
    echo "## Module Dependencies"
    echo ""
    echo "| Module | Layer | Dependencies |"
    echo "|--------|-------|--------------|"

    # Parse modules from JSON using Python for reliable parsing
    python3 -c "
import json
import sys
data = json.loads(sys.stdin.read())
for m in data['modules']:
    name = m['name']
    layer = m['layer']
    deps = ', '.join(m['dependencies']) if m['dependencies'] else '-'
    print(f'| {name} | {layer} | {deps} |')
" <<< "$json_data"

    echo ""

    if [[ "$total_violations" -gt 0 ]]; then
        echo "## Layer Violations"
        echo ""
        echo "The following dependencies violate clean architecture layer rules:"
        echo ""
        python3 -c "
import json
import sys
data = json.loads(sys.stdin.read())
for v in data['violations']:
    print(f'- ⚠️  {v}')
" <<< "$json_data"
        echo ""
    fi

    if [[ "$total_circular" -gt 0 ]]; then
        echo "## Circular Dependencies"
        echo ""
        echo "The following circular dependencies were detected:"
        echo ""
        python3 -c "
import json
import sys
data = json.loads(sys.stdin.read())
for c in data['circular_dependencies']:
    print(f'- 🔄 {c}')
" <<< "$json_data"
        echo ""
    fi

    echo "---"
    echo "*Generated by \`scripts/analyze-architecture.sh\`*"
}

# Main execution
echo -e "${BLUE}Analyzing architecture...${NC}" >&2

json_data=$(build_dependency_data)

if $OUTPUT_JSON; then
    echo "$json_data"
elif $OUTPUT_MD; then
    generate_markdown "$json_data"
else
    # Generate both files
    mkdir -p .reports
    echo "$json_data" > .reports/architecture.json
    generate_markdown "$json_data" > .reports/architecture.md

    # Print summary
    total_violations=$(echo "$json_data" | grep -o '"total_violations":[0-9]*' | cut -d: -f2)
    total_circular=$(echo "$json_data" | grep -o '"total_circular":[0-9]*' | cut -d: -f2)

    echo -e "${GREEN}Reports generated:${NC}"
    echo "  .reports/architecture.json"
    echo "  .reports/architecture.md"
    echo ""

    has_errors=false

    if [[ "$total_violations" -eq 0 ]]; then
        echo -e "${GREEN}✓ No layer violations found${NC}"
    else
        echo -e "${RED}✗ Found $total_violations layer violation(s)${NC}"
        has_errors=true
    fi

    if [[ "$total_circular" -eq 0 ]]; then
        echo -e "${GREEN}✓ No circular dependencies found${NC}"
    else
        echo -e "${RED}✗ Found $total_circular circular dependency(ies)${NC}"
        has_errors=true
    fi

    if $has_errors; then
        echo ""
        echo "  See .reports/architecture.md for details"
        exit 1
    fi
fi
