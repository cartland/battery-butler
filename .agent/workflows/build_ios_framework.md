---
description: Build the Kotlin Multiplatform framework for iOS
---

// turbo-all

1. Choose the target architecture
   - **Simulator (Apple Silicon):**
     `./gradlew :ios-swift-di:linkDebugFrameworkIosSimulatorArm64`
   - **Simulator (Intel):**
     `./gradlew :ios-swift-di:linkDebugFrameworkIosX64`
   - **Physical Device:**
     `./gradlew :ios-swift-di:linkDebugFrameworkIosArm64`

2. For release builds (physical device)
   `./gradlew :ios-swift-di:linkReleaseFrameworkIosArm64`

> [!NOTE]
> The framework will be used by both the Compose and SwiftUI iOS apps.
