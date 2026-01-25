# Run iOS Compose

Build and run the iOS Compose Multiplatform app.

## Steps

1. Build the KMP framework for iOS:
   ```bash
   ./gradlew :compose-app:embedAndSignAppleFrameworkForXcode
   ```

2. Open the iOS Compose project in Xcode:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

3. In Xcode:
   - Select an iPhone simulator or connected device
   - Click Run (or press Cmd+R)

## Notes

- This is the Compose Multiplatform iOS app (uses Kotlin Compose UI)
- For the SwiftUI version, use `/run-ios-swiftui` instead
- The framework build step is automatically triggered by Xcode build scripts, but running it manually first can help catch issues earlier
