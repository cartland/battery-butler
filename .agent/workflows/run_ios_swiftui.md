---
description: Build and run the iOS native SwiftUI app
---

// turbo-all

1. Build the KMP framework for iOS
   `./gradlew :ios-swift-di:linkDebugFrameworkIosSimulatorArm64`

   > [!NOTE]
   > For physical device use: `./gradlew :ios-swift-di:linkDebugFrameworkIosArm64`

2. Open the SwiftUI Xcode project
   `open ios-app-swift-ui/iosAppSwiftUI.xcodeproj`

3. In Xcode
   - Select an iPhone simulator (or device if you built for device)
   - Click Run (or press Cmd+R)
