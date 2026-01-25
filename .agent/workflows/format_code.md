---
description: Run Spotless to format all code
---

// turbo-all

1. Apply formatting to all files
   `./gradlew spotlessApply`

2. Verify formatting is correct
   `./gradlew spotlessCheck`

> [!NOTE]
> The format script is automatically run as part of `/prepare-commit-then-push`.
