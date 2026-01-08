
import os
import re

# Build mapping Class -> Full Package Path
class_map = {}
with open('viewmodel_files.txt', 'r') as f:
    for line in f:
        path = line.strip()
        # path example: viewmodel/src/commonMain/kotlin/com/chriscartland/batterybutler/viewmodel/editdevice/EditDeviceViewModel.kt
        # extract package: com.chriscartland.batterybutler.viewmodel.editdevice
        # extract class: EditDeviceViewModel
        
        parts = path.split('/')
        filename = parts[-1]
        classname = filename.replace('.kt', '')
        
        # reconstruct package from path
        # find 'kotlin/' index ?
        try:
            k_index = parts.index('kotlin')
            pkg_parts = parts[k_index+1:-1] # excluding filename
            pkg = ".".join(pkg_parts)
            class_map[classname] = f"{pkg}.{classname}"
            
            # Also handle potentially moved inner classes if they were top level before? 
            # or heuristics for UiState/Factory if they live in the same file.
            # For now focus on the main class in the file.
        except ValueError:
            pass

# Add extra mappings for known suffixes that usually live with the ViewModel
# This is a heuristic, but helpful.
extended_map = {}
for cls, fqn in class_map.items():
    extended_map[cls] = fqn
    if "ViewModel" in cls:
        base = cls.replace("ViewModel", "")
        # Factory
        extended_map[f"{base}ViewModelFactory"] = fqn.replace("ViewModel", "ViewModelFactory")
        # UiState
        extended_map[f"{base}UiState"] = fqn.replace("ViewModel", "UiState")

class_map.update(extended_map)

# Exclude some if they don't exist? The previous script verified file existence, this one infers.
# But imports won't hurt if valid.

print(f"Loaded {len(class_map)} class mappings.")

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    original_content = content
    lines = content.split('\n')
    
    # 1. Replace FQNs with Simple Names
    # FQN pattern: com.chriscartland.batterybutler.viewmodel.[pkg].[Class]
    # We can iterate our map and replace.
    
    # Sort by length desc to replace longest matches first (common prefix issue)
    sorted_fqns = sorted(class_map.values(), key=len, reverse=True)
    
    for fqn in sorted_fqns:
        if fqn in content:
            if f"import {fqn}" not in content and f"package {fqn.rsplit('.', 1)[0]}" not in content:
                # Use regex to ensure we don't match partials if possible, but FQN is usually distinct.
                # Avoid touching the import statement itself if it exists (but we checked it doesn't).
                # Actually we DO replace FQN in code, but we must ensure we Add Import.
                
                simple_name = fqn.split('.')[-1]
                # Replace FQN in code body (not import/package lines)
                # Regex negative lookbehind for 'import ' or 'package '
                # simplified: just replace all, and fix up imports later.
                pass

    # Better approach:
    # 1. Identify needed imports based on SimpleName usage in code.
    # 2. Add imports if missing.
    # 3. Replace keys (FQNs) with values (SimpleNames) in code body.
    
    # Step 1 & 2: Identifiy and Add Imports
    needed_imports = set()
    for simple_name, fqn in class_map.items():
        # Check if SimpleName is used
        # We need a regex boundary check to avoid matching substrings
        if re.search(r'\b' + re.escape(simple_name) + r'\b', content) or fqn in content:
            # check if verify fqn matches the one we want (vs same name different pkg)
            # If FQN is used, we definitely need import.
            # If SimpleName is used, we assume it refers to our class if it's not imported from elsewhere.
            
            # Check if already imported
            if f"import {fqn}" not in content:
                # Check if it's the package of the file itself
                pkg_match = re.search(r'^package\s+([\w\.]+)', content)
                file_pkg = pkg_match.group(1) if pkg_match else ""
                class_pkg = fqn.rsplit('.', 1)[0]
                
                if file_pkg != class_pkg:
                    needed_imports.add(fqn)

    if needed_imports:
        # Insert imports
        # Find last import or package
        last_import_idx = -1
        package_idx = -1
        for i, line in enumerate(lines):
            if line.startswith('package '):
                package_idx = i
            if line.startswith('import '):
                last_import_idx = i
        
        insert_idx = last_import_idx + 1 if last_import_idx != -1 else package_idx + 2
        
        # sort needed imports
        sorted_imports = sorted(list(needed_imports))
        
        for imp in reversed(sorted_imports):
            if f"import {imp}" not in lines: # double check
                lines.insert(insert_idx, f"import {imp}")
    
    new_content = "\n".join(lines)
    
    # Step 3: Replace FQNs with Simple Names
    for simple_name, fqn in class_map.items():
         if fqn in new_content:
             # Don't replace in import/package statements
             # regex replacement
             pattern = re.compile(r'(?<!import )(?<!package )' + re.escape(fqn))
             new_content = pattern.sub(simple_name, new_content)

    if new_content != original_content:
        print(f"Modifying {filepath}")
        with open(filepath, 'w') as f:
            f.write(new_content)

for root, dirs, files in os.walk('.'):
    if 'build' in dirs:
        dirs.remove('build')
    if '.git' in dirs:
        dirs.remove('.git')
    
    for file in files:
        if file.endswith('.kt') and not file == "fix_imports_fqn.py":
            process_file(os.path.join(root, file))
