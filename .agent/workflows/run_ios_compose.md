---
description: Build and run the iOS Compose Multiplatform app
---

// turbo-all

1. Build the KMP framework for iOS
   `./gradlew :compose-app:embedAndSignAppleFrameworkForXcode`

2. Open the iOS Compose project in Xcode
   `open iosApp/iosApp.xcodeproj`

3. In Xcode
   - Select an iPhone simulator or connected device
   - Click Run (or press Cmd+R)
