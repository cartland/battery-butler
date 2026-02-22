# Run iOS SwiftUI

Build and run the iOS native SwiftUI app.

## Steps

1. Build the KMP framework for iOS:
   ```bash
   ./gradlew :ios-swift-di:linkDebugFrameworkIosSimulatorArm64
   ```

   For physical device:
   ```bash
   ./gradlew :ios-swift-di:linkDebugFrameworkIosArm64
   ```

2. Open the SwiftUI Xcode project:
   ```bash
   open ios-app-swift-ui/iosAppSwiftUI.xcodeproj
   ```

3. In Xcode:
   - Select an iPhone simulator (or device if you built for device)
   - Click Run (or press Cmd+R)

## Notes

- This is the native SwiftUI app (uses SwiftUI for UI, KMP for ViewModels and business logic)
- For the Compose Multiplatform version, use `/run-ios-compose` instead
- The framework must be built before opening in Xcode
