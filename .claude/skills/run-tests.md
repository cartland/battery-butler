# Run Tests

Run project tests (unit tests and instrumented tests).

## Unit Tests

Run all unit tests across all modules:
```bash
./gradlew test
```

Run tests for a specific module:
```bash
./gradlew :domain:test
./gradlew :usecase:test
./gradlew :viewmodel:test
```

## Android Instrumented Tests

Run instrumented tests on Android emulator/device:
```bash
./gradlew pixel5api34Check
```

Or for connected devices:
```bash
./gradlew connectedAndroidTest
```

## Screenshot Tests

Update reference screenshots:
```bash
./gradlew :android-screenshot-tests:updateDebugScreenshotTest
```

Verify screenshots:
```bash
./gradlew :android-screenshot-tests:validateDebugScreenshotTest
```

## iOS Tests

Run iOS tests (requires Xcode):
```bash
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 15'
```

## Full Test Suite

Run all tests (as done in CI):
```bash
./scripts/validate.sh
```

## Notes

- Unit tests are fast and don't require Android/iOS runtime
- Instrumented tests require an emulator or connected device
- Screenshot tests generate visual regression baselines
- The validate script runs the complete CI test suite locally
