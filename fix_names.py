
import os

replacements = [
    ("DdBatteryEvent", "AddBatteryEvent"),
    ("DdDevice", "AddDevice"),
    ("DdDeviceType", "AddDeviceType"),
    ("EviceDetail", "DeviceDetail"),
    ("EviceTypeList", "DeviceTypeList"),
    ("DitDeviceType", "EditDeviceType"),
    ("DitDevice", "EditDevice"),
    ("VentDetail", "EventDetail"),
    ("IstoryList", "HistoryList"),
    ("Ome", "Home"),
    ("Ettings", "Settings"),
    ("IstoryItem", "HistoryItem"), # For HistoryItemUiModel
    # Special cases if any
    ("Roup", "Group"), # GroupOption
    ("Ort", "Sort"),   # SortOption
]

suffixes = ["ViewModel", "Factory", "UiState", "Option", "UiModel"]

def fix_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    new_content = content
    for bad_prefix, good_prefix in replacements:
        for suffix in suffixes:
            bad = bad_prefix + suffix
            good = good_prefix + suffix
            if bad in new_content:
                new_content = new_content.replace(bad, good)
    
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
