#!/bin/bash
set -e
cd "$(dirname "$0")/.."

# Import Analysis Script
# Analyzes import patterns across modules to understand dependencies.
#
# Usage: ./scripts/analyze-imports.sh [--json] [--markdown]
#
# Outputs:
#   --json      Output JSON to stdout (default: .reports/imports.json)
#   --markdown  Output Markdown to stdout (default: .reports/imports.md)
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

# Get list of modules
get_modules() {
    find . -name "build.gradle.kts" -not -path "./buildSrc/*" -not -path "./.gradle/*" | \
        sed 's|/build.gradle.kts||' | \
        sed 's|^\./||' | \
        grep -v '^\.$' | \
        sort
}

# Analyze imports in a module
analyze_module_imports() {
    local module="$1"

    # Find all Kotlin files and extract imports
    find "$module" -name "*.kt" -not -path "*/build/*" -not -path "*/.gradle/*" 2>/dev/null | \
        xargs grep -h "^import " 2>/dev/null | \
        sed 's/import //' | \
        sort | uniq -c | sort -rn || true
}

# Get project package prefix
get_project_prefix() {
    echo "com.chriscartland.batterybutler"
}

# Categorize an import
categorize_import() {
    local import="$1"
    local project_prefix=$(get_project_prefix)

    if [[ "$import" == "$project_prefix"* ]]; then
        echo "internal"
    elif [[ "$import" == kotlin* ]] || [[ "$import" == java* ]] || [[ "$import" == javax* ]]; then
        echo "stdlib"
    elif [[ "$import" == android* ]] || [[ "$import" == androidx* ]]; then
        echo "android"
    elif [[ "$import" == io.ktor* ]]; then
        echo "ktor"
    elif [[ "$import" == kotlinx* ]]; then
        echo "kotlinx"
    elif [[ "$import" == me.tatarka.inject* ]]; then
        echo "kotlin-inject"
    elif [[ "$import" == com.google.protobuf* ]] || [[ "$import" == *Proto* ]]; then
        echo "protobuf"
    elif [[ "$import" == org.jetbrains.compose* ]] || [[ "$import" == androidx.compose* ]]; then
        echo "compose"
    elif [[ "$import" == io.grpc* ]]; then
        echo "grpc"
    else
        echo "third-party"
    fi
}

# Build import analysis data
build_import_data() {
    local modules=$(get_modules)
    local project_prefix=$(get_project_prefix)
    local json_modules="["
    local first=true

    local total_imports=0
    local total_internal=0
    local total_external=0

    for module in $modules; do
        local imports=$(analyze_module_imports "$module")
        [[ -z "$imports" ]] && continue

        # Count categories
        local internal_count=0
        local stdlib_count=0
        local android_count=0
        local kotlinx_count=0
        local compose_count=0
        local ktor_count=0
        local grpc_count=0
        local protobuf_count=0
        local kotlin_inject_count=0
        local third_party_count=0
        local total_count=0

        # Track internal module dependencies
        local internal_deps=""

        while IFS= read -r line; do
            [[ -z "$line" ]] && continue
            local count=$(echo "$line" | awk '{print $1}')
            local import=$(echo "$line" | awk '{print $2}')

            total_count=$((total_count + count))
            total_imports=$((total_imports + count))

            local category=$(categorize_import "$import")
            case "$category" in
                internal)
                    internal_count=$((internal_count + count))
                    total_internal=$((total_internal + count))
                    # Extract the sub-package to identify the module
                    local subpkg=$(echo "$import" | sed "s|${project_prefix}\.||" | cut -d'.' -f1)
                    if [[ -n "$subpkg" ]] && [[ "$internal_deps" != *"$subpkg"* ]]; then
                        internal_deps="${internal_deps}${subpkg},"
                    fi
                    ;;
                stdlib) stdlib_count=$((stdlib_count + count)) ;;
                android) android_count=$((android_count + count)) ;;
                kotlinx) kotlinx_count=$((kotlinx_count + count)) ;;
                compose) compose_count=$((compose_count + count)) ;;
                ktor) ktor_count=$((ktor_count + count)) ;;
                grpc) grpc_count=$((grpc_count + count)) ;;
                protobuf) protobuf_count=$((protobuf_count + count)) ;;
                kotlin-inject) kotlin_inject_count=$((kotlin_inject_count + count)) ;;
                third-party) third_party_count=$((third_party_count + count)) ;;
            esac
        done <<< "$imports"

        total_external=$((total_external + total_count - internal_count))

        # Remove trailing comma from internal_deps
        internal_deps="${internal_deps%,}"

        # Build internal_deps array
        local deps_arr="["
        local deps_first=true
        IFS=',' read -ra DEPS <<< "$internal_deps"
        for dep in "${DEPS[@]}"; do
            [[ -z "$dep" ]] && continue
            $deps_first || deps_arr+=","
            deps_first=false
            deps_arr+="\"$dep\""
        done
        deps_arr+="]"

        $first || json_modules+=","
        first=false

        json_modules+="{\"name\":\"$module\",\"total\":$total_count,\"internal\":$internal_count,\"stdlib\":$stdlib_count,\"android\":$android_count,\"kotlinx\":$kotlinx_count,\"compose\":$compose_count,\"ktor\":$ktor_count,\"grpc\":$grpc_count,\"protobuf\":$protobuf_count,\"kotlin_inject\":$kotlin_inject_count,\"third_party\":$third_party_count,\"internal_deps\":$deps_arr}"
    done

    json_modules+="]"

    echo "{\"modules\":$json_modules,\"totals\":{\"total\":$total_imports,\"internal\":$total_internal,\"external\":$total_external}}"
}

# Generate Markdown report
generate_markdown() {
    local json_data="$1"

    cat << 'EOF'
# Import Analysis Report

This report analyzes import patterns across modules to understand dependencies.

## Import Categories

| Category | Description |
|----------|-------------|
| Internal | Project code (`com.chriscartland.batterybutler.*`) |
| Stdlib | Kotlin/Java standard library |
| Android | Android SDK and AndroidX |
| Compose | Jetpack Compose UI |
| Kotlinx | Kotlin extensions (coroutines, serialization) |
| Ktor | Ktor HTTP client |
| gRPC | gRPC networking |
| Protobuf | Protocol Buffers |
| kotlin-inject | Dependency injection |
| Third-party | Other external libraries |

## Summary

EOF

    echo "$json_data" | python3 -c '
import json, sys
data = json.loads(sys.stdin.read())
t = data["totals"]
total, internal, external = t["total"], t["internal"], t["external"]
internal_pct = (internal / total * 100) if total > 0 else 0
external_pct = (external / total * 100) if total > 0 else 0
print(f"- **Total imports**: {total:,}")
print(f"- **Internal imports**: {internal:,} ({internal_pct:.1f}%)")
print(f"- **External imports**: {external:,} ({external_pct:.1f}%)")
print("")
print("## Module Import Breakdown")
print("")
print("| Module | Total | Internal | Stdlib | Android | Compose | Kotlinx | Other |")
print("|--------|------:|---------:|-------:|--------:|--------:|--------:|------:|")

modules = sorted(data["modules"], key=lambda m: m["total"], reverse=True)
for m in modules:
    name = m["name"]
    tot, intl, stdlib, android, compose, kotlinx = m["total"], m["internal"], m["stdlib"], m["android"], m["compose"], m["kotlinx"]
    other = m["ktor"] + m["grpc"] + m["protobuf"] + m["kotlin_inject"] + m["third_party"]
    print(f"| {name} | {tot} | {intl} | {stdlib} | {android} | {compose} | {kotlinx} | {other} |")

print("")
print("## Internal Dependencies")
print("")
print("Shows which internal packages each module imports:")
print("")
for m in modules:
    if m["internal_deps"]:
        name = m["name"]
        deps = ", ".join(sorted(m["internal_deps"]))
        print(f"- **{name}** -> {deps}")
'

    echo ""
    echo "---"
    echo "*Generated by \`scripts/analyze-imports.sh\`*"
}

# Main execution
echo -e "${BLUE}Analyzing import patterns...${NC}" >&2

json_data=$(build_import_data)

if $OUTPUT_JSON; then
    echo "$json_data"
elif $OUTPUT_MD; then
    generate_markdown "$json_data"
else
    # Generate both files
    mkdir -p .reports
    echo "$json_data" > .reports/imports.json
    generate_markdown "$json_data" > .reports/imports.md

    # Print summary
    read total internal external <<< $(echo "$json_data" | python3 -c '
import json, sys
data = json.loads(sys.stdin.read())
t = data["totals"]
print(t["total"], t["internal"], t["external"])
')

    echo -e "${GREEN}Reports generated:${NC}"
    echo "  .reports/imports.json"
    echo "  .reports/imports.md"
    echo ""
    echo -e "${GREEN}✓ Analyzed $total imports ($internal internal, $external external)${NC}"
fi
