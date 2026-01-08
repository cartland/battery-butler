
import os
import re

def fix_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Pattern: .. followed by a lowercase letter
    # We expect this to be a corrupted capitalized class name, e.g. ..ddDevice -> .AddDevice
    def replacer(match):
        char = match.group(1)
        return f".{char.upper()}"

    new_content = re.sub(r'\.\.([a-z])', replacer, content)
    
    if new_content != content:
        print(f"Fixing {filepath}")
        with open(filepath, 'w') as f:
            f.write(new_content)

for root, dirs, files in os.walk('.'):
    if 'build' in dirs:
        dirs.remove('build')
    if '.git' in dirs:
        dirs.remove('.git')
    
    for file in files:
        if file.endswith('.kt'):
            fix_file(os.path.join(root, file))
