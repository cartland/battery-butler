---
description: Update all generated documentation
---

// turbo-all

1. Generate architecture diagrams
   `./gradlew generateMermaidGraph`

2. Generate code share analysis report
   `./gradlew analyzeCodeShare`

3. Review and commit changes
   `git add docs/`
   `git commit -m "docs: Update diagrams and code analysis"`
