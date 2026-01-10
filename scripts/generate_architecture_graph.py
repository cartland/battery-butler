#!/usr/bin/env python3
import os
import re
import sys

# Configuration: Mapping module paths to Human Readable Names and Subgraphs
# Modules not in this map will be placed in a "Others" subgraph with capitalized names.
MODULE_CONFIG = {
    ":compose-app": {"name": "ComposeApp", "group": "App & Entry Points"},
    ":shared": {"name": "Shared", "group": "App & Entry Points"},
    ":server:app": {"name": "ServerApp", "group": "App & Entry Points"},

    ":ui-feature": {"name": "UIFeature", "group": "UI Layer"},
    ":ui-core": {"name": "UICore", "group": "UI Layer"},

    ":viewmodel": {"name": "ViewModel", "group": "Presentation Layer"},

    ":usecase": {"name": "UseCase", "group": "Domain Layer"},
    ":domain": {"name": "Domain", "group": "Domain Layer"},
    ":server:domain": {"name": "ServerDomain", "group": "Domain Layer"},

    ":data": {"name": "Data", "group": "Data Layer"},
    ":networking": {"name": "Networking", "group": "Data Layer"},
    ":server:data": {"name": "ServerData", "group": "Data Layer"},
}

# Subgraph Order (for display consistency)
GROUP_ORDER = [
    "App & Entry Points",
    "UI Layer",
    "Presentation Layer",
    "Domain Layer",
    "Data Layer"
]

def find_root_dir():
    # Assume script is in scripts/ folder, so root is one level up
    script_dir = os.path.dirname(os.path.abspath(__file__))
    return os.path.dirname(script_dir)

def parse_settings_gradle(root_dir):
    settings_path = os.path.join(root_dir, "settings.gradle.kts")
    modules = []
    if not os.path.exists(settings_path):
        print(f"Error: {settings_path} not found.")
        sys.exit(1)
    
    with open(settings_path, 'r') as f:
        content = f.read()
        # Find include(":module") or include(":group:module")
        # Regex handles single and multiple includes
        matches = re.findall(r'include\s*\(\s*"([^"]+)"\s*\)', content)
        modules.extend(matches)
    
    return sorted(list(set(modules)))

def get_build_gradle_path(root_dir, module_name):
    # Convert :group:module to group/module
    path = module_name.replace(":", "/").strip("/")
    return os.path.join(root_dir, path, "build.gradle.kts")

def parse_dependencies(build_gradle_path):
    dependencies = set()
    if not os.path.exists(build_gradle_path):
        return dependencies
    
    with open(build_gradle_path, 'r') as f:
        content = f.read()
        # Look for implementation(project(":module")), api(project(":module")), export(project(":module"))
        # Also handles named arguments like implementation(project(path = ":module"))
        # Regex simplified for common cases
        
        # Matches: project(":foo")
        matches = re.findall(r'project\s*\(\s*"([^"]+)"\s*\)', content)
        dependencies.update(matches)
        
        # Matches: project(path = ":foo")
        matches_path = re.findall(r'project\s*\(\s*path\s*=\s*"([^"]+)"\s*\)', content)
        dependencies.update(matches_path)
        
    return sorted(list(dependencies))

def generate_mermaid(root_dir, modules):
    edges = []
    
    # 1. Collect Edges
    for module in modules:
        build_file = get_build_gradle_path(root_dir, module)
        deps = parse_dependencies(build_file)
        
        source_name = get_node_id(module)
        
        for dep in deps:
            if dep in modules: # Only link internal modules
                target_name = get_node_id(dep)
                edges.append((source_name, target_name))
    
    # Unique and sort edges
    edges = sorted(list(set(edges)))
    
    # 2. Build Graph Content
    lines = ["graph TD"]
    
    # Group modules by Subgraph
    groups = {}
    for module in modules:
        config = MODULE_CONFIG.get(module, {"name": get_node_id(module), "group": "Others"})
        group_name = config["group"]
        if group_name not in groups:
            groups[group_name] = []
        groups[group_name].append(module)
        
    # Generate Subgraphs defined in ORDER
    # First, user defined groups
    for group_name in GROUP_ORDER:
        if group_name in groups:
            lines.append(f'    subgraph "{group_name}"')
            for module in groups[group_name]:
                node_id = get_node_id(module)
                label = f'"{module}"'
                lines.append(f'        {node_id}[{label}]')
            lines.append('    end')
            lines.append('')
            del groups[group_name] # Remove so we don't duplicate
            
    # Remaining groups (e.g. "Others")
    for group_name in sorted(groups.keys()):
        lines.append(f'    subgraph "{group_name}"')
        for module in groups[group_name]:
            node_id = get_node_id(module)
            label = f'"{module}"'
            lines.append(f'        {node_id}[{label}]')
        lines.append('    end')
        lines.append('')

    # Add Edges
    lines.append('    %% Dependencies')
    for source, target in edges:
        lines.append(f'    {source} --> {target}')
        
    return "\n".join(lines)

def get_node_id(module_path):
    # Check config first
    if module_path in MODULE_CONFIG:
        return MODULE_CONFIG[module_path]["name"]
    
    # Fallback: :group:module -> GroupModule
    clean = module_path.replace(":", " ").title().replace(" ", "")
    return clean

def main():
    root_dir = find_root_dir()
    modules = parse_settings_gradle(root_dir)
    
    # Filter out empty modules or legacy ones if needed (optional)
    modules = [m for m in modules if m != ":shared" or os.path.exists(get_build_gradle_path(root_dir, ":shared"))]

    mermaid_content = generate_mermaid(root_dir, modules)
    
    # Output to stdout or file
    # If -o is passed, write to file
    if len(sys.argv) > 2 and sys.argv[1] == "-o":
        with open(sys.argv[2], "w") as f:
            f.write(mermaid_content)
    else:
        print(mermaid_content)

if __name__ == "__main__":
    main()
