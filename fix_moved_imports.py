
import os

# Load moved classes
moved_classes = set()
with open('moved_classes.txt', 'r') as f:
    for line in f:
        moved_classes.add(line.strip())

# Additional moved classes that might be inside files (like Options/UiStates/UiModels)
# Only if we want to be very robust, but usually they are top level or imported with the file class name if nested (static).
# But Kotlin allows importing nested classes.
# For now, let's assume imports usually target the filename class.
# But wait, `GroupOption` and `SortOption` were inside `HomeViewModel.kt` (or same file).
# If the import target simple name matches, we update.

def is_moved_class(class_name):
    if class_name in moved_classes:
        return True
    # Check suffixes
    if class_name.endswith("UiState") or class_name.endswith("Option") or class_name.endswith("UiModel") or class_name.endswith("Factory"):
        # Heuristic: if it looks like a viewmodel related class, it probably moved.
        return True
    return False

def fix_imports(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()

    new_lines = []
    modified = False
    for line in lines:
        if line.startswith('import com.chriscartland.batterybutler.feature.'):
            parts = line.split('.')
            # parts[-1] is "ClassName\n" or "ClassName"
            # We need to handle `as Alias` too? Assuming simple imports first.
            # "import com.chriscartland.batterybutler.feature.home.HomeViewModel"
            # parts: ['import com', 'chriscartland', 'batterybutler', 'feature', 'home', 'HomeViewModel\n']
            
            # extract class name
            import_path = line.strip().split(' ')[1] # com.chriscartland.batterybutler.feature.home.HomeViewModel
            class_name = import_path.split('.')[-1]
            
            if is_moved_class(class_name):
                # Replace .feature. with .viewmodel.
                new_line = line.replace('.feature.', '.viewmodel.')
                new_lines.append(new_line)
                modified = True
            else:
                new_lines.append(line)
        else:
            new_lines.append(line)

    if modified:
        print(f"Fixing imports in {filepath}")
        with open(filepath, 'w') as f:
            f.writelines(new_lines)

for root, dirs, files in os.walk('.'):
    if 'build' in dirs:
        dirs.remove('build')
    if '.git' in dirs:
        dirs.remove('.git')
    
    for file in files:
        if file.endswith('.kt'):
            fix_imports(os.path.join(root, file))
