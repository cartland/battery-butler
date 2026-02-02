#!/bin/bash
set -e
cd "$(dirname "$0")/.."

# Naming Convention Analysis Script
# Catalogs class naming patterns across the codebase.
#
# Usage: ./scripts/analyze-naming.sh [--json] [--markdown]
#
# Outputs:
#   --json      Output JSON to stdout (default: .reports/naming.json)
#   --markdown  Output Markdown to stdout (default: .reports/naming.md)
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

# Patterns to analyze (just pattern and description, no enforcement)
PATTERNS=(
    "UseCase|Business logic orchestration"
    "Repository|Data access abstraction"
    "ViewModel|Presentation state management"
    "DataSource|Data layer interface/implementation"
    "Entity|Database entity classes"
    "Component|Dependency injection components"
    "Factory|Object creation patterns"
)

# Find all Kotlin files with class definitions matching a pattern
find_classes() {
    local pattern="$1"
    grep -rlE "(class|interface|object|abstract class)\s+[A-Za-z0-9]*${pattern}(\s|\(|:|<|\{|$)" \
        --include="*.kt" \
        . 2>/dev/null | \
        grep -v "/build/" | \
        grep -v "/.gradle/" | \
        sort || true
}

# Extract class info from file
extract_class_info() {
    local file="$1"
    local pattern="$2"
    # Get just the class name
    grep -oE "(class|interface|object)\s+[A-Za-z0-9]*${pattern}" "$file" 2>/dev/null | \
        head -1 | \
        sed -E 's/(class|interface|object)\s+//' || true
}

# Get module from file path
get_module() {
    local file="$1"
    file="${file#./}"
    echo "$file" | cut -d'/' -f1
}

# Check if file is in test directory
is_test_file() {
    local file="$1"
    [[ "$file" == *"/test/"* ]] || [[ "$file" == *"/commonTest/"* ]] || [[ "$file" == *"/androidTest/"* ]]
}

# Build analysis data
build_naming_data() {
    local json_patterns="["
    local first_pattern=true
    local total_classes=0
    local total_prod_classes=0
    local total_test_classes=0

    for pattern_spec in "${PATTERNS[@]}"; do
        IFS='|' read -r pattern description <<< "$pattern_spec"

        local files=$(find_classes "$pattern")
        local pattern_classes="["
        local modules_json="{}"
        local first_class=true
        local class_count=0
        local prod_count=0
        local test_count=0

        # Track modules
        declare -a module_list=()

        if [[ -n "$files" ]]; then
            while IFS= read -r file; do
                [[ -z "$file" ]] && continue

                local class_info=$(extract_class_info "$file" "$pattern")
                [[ -z "$class_info" ]] && continue

                class_count=$((class_count + 1))
                total_classes=$((total_classes + 1))

                # Remove leading ./
                local clean_file="${file#./}"
                local module=$(get_module "$file")
                local is_test="false"

                if is_test_file "$file"; then
                    is_test="true"
                    test_count=$((test_count + 1))
                    total_test_classes=$((total_test_classes + 1))
                else
                    prod_count=$((prod_count + 1))
                    total_prod_classes=$((total_prod_classes + 1))
                fi

                # Track module
                if [[ ! " ${module_list[*]} " =~ " ${module} " ]]; then
                    module_list+=("$module")
                fi

                $first_class || pattern_classes+=","
                first_class=false
                pattern_classes+="{\"name\":\"$class_info\",\"file\":\"$clean_file\",\"module\":\"$module\",\"is_test\":$is_test}"
            done <<< "$files"
        fi

        pattern_classes+="]"

        # Build modules array
        local modules_arr="["
        local first_mod=true
        for mod in "${module_list[@]}"; do
            $first_mod || modules_arr+=","
            first_mod=false
            modules_arr+="\"$mod\""
        done
        modules_arr+="]"

        $first_pattern || json_patterns+=","
        first_pattern=false

        json_patterns+="{\"pattern\":\"$pattern\",\"description\":\"$description\",\"count\":$class_count,\"prod_count\":$prod_count,\"test_count\":$test_count,\"modules\":$modules_arr,\"classes\":$pattern_classes}"
    done

    json_patterns+="]"

    echo "{\"patterns\":$json_patterns,\"stats\":{\"total_classes\":$total_classes,\"total_prod\":$total_prod_classes,\"total_test\":$total_test_classes}}"
}

# Generate Markdown report
generate_markdown() {
    local json_data="$1"

    cat << 'EOF'
# Naming Convention Analysis Report

This report catalogs class naming patterns across the codebase.

## Naming Patterns

| Pattern | Purpose |
|---------|---------|
| `*UseCase` | Business logic orchestration (single responsibility) |
| `*Repository` | Data access abstraction (interface in domain, impl in data) |
| `*ViewModel` | Presentation state management |
| `*DataSource` | Data layer interface/implementation |
| `*Entity` | Database entity classes (Room/ORM) |
| `*Component` | Dependency injection components |
| `*Factory` | Object creation patterns |

## Summary

EOF

    python3 -c "
import json
import sys
data = json.loads(sys.stdin.read())
stats = data['stats']
print(f\"- **Total pattern classes**: {stats['total_classes']}\")
print(f\"  - Production: {stats['total_prod']}\")
print(f\"  - Test: {stats['total_test']}\")
print('')
print('## Pattern Distribution')
print('')
print('| Pattern | Total | Prod | Test | Modules |')
print('|---------|-------|------|------|---------|')
for p in data['patterns']:
    modules = ', '.join(p['modules'][:3])
    if len(p['modules']) > 3:
        modules += f' (+{len(p[\"modules\"]) - 3})'
    print(f\"| {p['pattern']} | {p['count']} | {p['prod_count']} | {p['test_count']} | {modules} |\")
print('')
print('## Detailed Breakdown')
print('')
for p in data['patterns']:
    if p['count'] == 0:
        continue
    print(f\"### {p['pattern']}\")
    print('')
    print(f\"*{p['description']}*\")
    print('')
    print(f\"**Total**: {p['count']} ({p['prod_count']} prod, {p['test_count']} test)\")
    print('')
    print(f\"**Modules**: {', '.join(p['modules'])}\")
    print('')
    # Group by module
    by_module = {}
    for c in p['classes']:
        mod = c['module']
        if mod not in by_module:
            by_module[mod] = {'prod': [], 'test': []}
        if c['is_test']:
            by_module[mod]['test'].append(c)
        else:
            by_module[mod]['prod'].append(c)

    for mod, classes in sorted(by_module.items()):
        if classes['prod']:
            print(f\"**{mod}/** ({len(classes['prod'])})\")
            for c in classes['prod'][:5]:
                # Clean up name - remove keywords and trailing chars
                name = c['name']
                for kw in ['abstract class ', 'data class ', 'sealed class ', 'class ', 'interface ', 'object ']:
                    name = name.replace(kw, '')
                # Remove trailing characters
                for ch in ['(', '{', ':', '<', ' ']:
                    name = name.rstrip(ch)
                print(f\"- \`{name}\`\")
            if len(classes['prod']) > 5:
                print(f\"- *... and {len(classes['prod']) - 5} more*\")
            print('')
        if classes['test']:
            print(f\"**{mod}/** (test: {len(classes['test'])})\")
            for c in classes['test'][:3]:
                name = c['name']
                for kw in ['private ', 'internal ', 'abstract class ', 'data class ', 'sealed class ', 'class ', 'interface ', 'object ']:
                    name = name.replace(kw, '')
                for ch in ['(', '{', ':', '<', ' ']:
                    name = name.rstrip(ch)
                print(f\"- \`{name}\` *(test)*\")
            if len(classes['test']) > 3:
                print(f\"- *... and {len(classes['test']) - 3} more test classes*\")
            print('')
" <<< "$json_data"

    echo "---"
    echo "*Generated by \`scripts/analyze-naming.sh\`*"
}

# Main execution
echo -e "${BLUE}Analyzing naming conventions...${NC}" >&2

json_data=$(build_naming_data)

if $OUTPUT_JSON; then
    echo "$json_data"
elif $OUTPUT_MD; then
    generate_markdown "$json_data"
else
    # Generate both files
    mkdir -p .reports
    echo "$json_data" > .reports/naming.json
    generate_markdown "$json_data" > .reports/naming.md

    # Print summary
    total_classes=$(echo "$json_data" | grep -o '"total_classes":[0-9]*' | cut -d: -f2)
    total_prod=$(echo "$json_data" | grep -o '"total_prod":[0-9]*' | cut -d: -f2)

    echo -e "${GREEN}Reports generated:${NC}"
    echo "  .reports/naming.json"
    echo "  .reports/naming.md"
    echo ""
    echo -e "${GREEN}✓ Analyzed $total_classes classes ($total_prod production)${NC}"
fi
