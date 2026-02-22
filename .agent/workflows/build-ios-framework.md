# Build iOS Framework

Build the Kotlin Multiplatform framework for iOS (needed by both iOS apps).

## Steps

1. For iOS Simulator (Apple Silicon Mac):
   ```bash
   ./gradlew :ios-swift-di:linkDebugFrameworkIosSimulatorArm64
   ```

2. For iOS Simulator (Intel Mac):
   ```bash
   ./gradlew :ios-swift-di:linkDebugFrameworkIosX64
   ```

3. For physical iOS device:
   ```bash
   ./gradlew :ios-swift-di:linkDebugFrameworkIosArm64
   ```

4. For release builds:
   ```bash
   ./gradlew :ios-swift-di:linkReleaseFrameworkIosArm64
   ```

## Output Location

The framework will be generated at:
- Simulator: `ios-swift-di/build/bin/iosSimulatorArm64/debugFramework/shared.framework`
- Device: `ios-swift-di/build/bin/iosArm64/debugFramework/shared.framework`

## Notes

- Both iOS apps (Compose and SwiftUI) depend on this framework
- The framework bundles KMP code including ViewModels, UseCases, and Domain logic
- Xcode build scripts may automatically trigger framework builds, but running manually first helps catch errors early
