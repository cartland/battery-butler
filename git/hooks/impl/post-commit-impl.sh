#!/bin/bash

run_spotless_check() {
    changed_files="$1"
    # Check if any .kt or .kts files were modified
    if echo "$changed_files" | grep -qE "\.kt$|\.kts$"; then
        echo "Kotlin files modified in this commit. Running spotlessCheck..."
        ./gradlew spotlessCheck
        
        # Capture exit code
        EXIT_CODE=$?
        
        if [ $EXIT_CODE -ne 0 ]; then
            echo "***************************************************"
            echo "* spotlessCheck FAILED!                           *"
            echo "* Please run './gradlew spotlessApply' and amend. *"
            echo "***************************************************"
        else
            echo "spotlessCheck passed."
        fi
    fi
}

update_diagrams() {
    changed_files="$1"
    # Always run because Gradle cache will handle UP-TO-DATE checks efficiently
    echo "Updating diagrams..."
    ./scripts/update-diagrams.sh
    
    # Check if any diagram files were actually modified by the script
        # specific files: docs/diagrams/*.mmd or docs/diagrams/*.svg
        # We use git status --porcelain because they are tracked files that would be modified.
        if git status --porcelain | grep -qE "docs/diagrams/.*(mmd|svg)"; then
             echo "***************************************************************"
             echo "* Diagrams updated. Please 'git add' and 'git commit --amend' *"
             echo "***************************************************************"
        else
             echo "Diagrams were up to date."
        fi
}

post-commit-impl() {
    # Get list of changed files in the latest commit
    changed_files=$(git show --name-only --format="" HEAD)

    run_spotless_check "$changed_files"
    update_diagrams "$changed_files"
}
