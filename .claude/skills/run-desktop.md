---
description: Build and run the Compose Multiplatform Desktop (JVM) app.
allowed-tools: Bash(*)
user-invocable: true
---

# Run Desktop

Run the Desktop (JVM) application.

## Steps

1. Run the desktop app:
   ```bash
   ./gradlew :compose-app:run
   ```

2. The Compose Desktop window should launch automatically.

## Notes

- The desktop app uses the same Compose Multiplatform UI as Android
- Requires JDK 17 or higher
