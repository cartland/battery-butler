---
description: Run project tests — unit, instrumented, screenshot, E2E, or the full suite.
allowed-tools: Bash(*), Read, Glob, Grep
---

# Run Tests

Run project tests (unit tests, instrumented tests, screenshot tests, E2E tests).

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

## Android Screenshot Tests

Update reference screenshots:
```bash
./gradlew :android-screenshot-tests:updateDebugScreenshotTest
```

Verify screenshots:
```bash
./gradlew :android-screenshot-tests:validateDebugScreenshotTest
```

## iOS Snapshot Tests

Run the iOS SwiftUI Snapshot tests in the simulator (this will also record new baselines automatically if they are missing):
```bash
./scripts/test-ios.sh                      # all tests
./scripts/test-ios.sh BatteryAgeHelperTests  # one class
```

It resolves an available simulator itself (preferring iPhone 17, then 16, then 15) rather than pinning a model that ages out of the runner image. Override with `IOS_SIMULATOR_DEVICE="iPhone Air" ./scripts/test-ios.sh`.

## E2E Tests

Run against local server (auto-started):
```bash
./scripts/e2e-tests.sh
```

Run against a remote environment:
```bash
E2E_SERVER_URL=http://<nlb>:80 E2E_AUTH_TOKEN=<token> ./scripts/e2e-tests.sh --remote
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
- E2E tests require a running server (local or remote)
- The validate script runs the complete CI test suite locally
