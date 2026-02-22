#!/bin/bash
set -e

# Ensure we're in the repository root
if [ ! -d ".git" ]; then
    echo "Error: Must be run from the repository root"
    exit 1
fi

HOOKS_DIR=".git/hooks"
PRE_COMMIT_HOOK="$HOOKS_DIR/pre-commit"

echo "Setting up Git hooks..."

# Create the pre-commit hook
cat << 'EOF' > "$PRE_COMMIT_HOOK"
#!/bin/bash

# Check if any .swift files in the ios-app-swift-ui/Features directory are staged for commit
if git diff --cached --name-only | grep -q "ios-app-swift-ui/Features/.*\.swift$"; then
  echo "iOS Swift files modified. Syncing Xcode project files..."
  
  cd ios-app-swift-ui
  ruby sync_pbxproj.rb
  cd ..
  
  # Stage the project file in case it was modified by the script
  git add ios-app-swift-ui/iosAppSwiftUI.xcodeproj/project.pbxproj
fi
EOF

# Make the hook executable
chmod +x "$PRE_COMMIT_HOOK"

echo "Git hooks installed successfully."
