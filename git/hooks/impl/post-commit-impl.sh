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
    
    # Check if ARCHITECTURE.md or diagram sources were modified
    if echo "$changed_files" | grep -qE "ARCHITECTURE.md|docs/diagrams/kotlin_module_structure.mmd|docs/diagrams/full_system_structure.mmd"; then
        echo "Architecture docs modified. Updating diagrams..."
        ./scripts/update-diagrams.sh
        echo "***************************************************************"
        echo "* Diagrams updated. Please 'git add' and 'git commit --amend' *"
        echo "***************************************************************"
    fi
}
