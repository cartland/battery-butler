#!/bin/bash

# Navigate to the repo root
REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT" || exit 1

HOOKS_DIR="git/hooks"
GIT_HOOKS_DIR="$(git rev-parse --git-common-dir)/hooks"

echo "Installing hooks from $HOOKS_DIR to $GIT_HOOKS_DIR..."

# Create .git/hooks directory if it doesn't exist
mkdir -p "$GIT_HOOKS_DIR"

# Iterate over files in git/hooks
for hook in "$HOOKS_DIR"/*; do
    filename=$(basename "$hook")
    
    # Skip directories
    if [ -d "$hook" ]; then
        continue
    fi

    # Copy the hook to .git/hooks
    cp "$hook" "$GIT_HOOKS_DIR/$filename"
    chmod +x "$GIT_HOOKS_DIR/$filename"
    
    echo "Installed $filename"
done

echo "Git hooks installed successfully."
