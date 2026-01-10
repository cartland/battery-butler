#!/bin/bash

post-commit-impl() {
    # Get list of changed files in the latest commit
    changed_files=$(git show --name-only --format="" HEAD)

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
