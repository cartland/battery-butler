---
description: Run project tests (unit and instrumented)
---

// turbo-all

1. Unit Tests (All Modules)
   `./gradlew test`

2. Unit Tests (Specific Module)
   - Domain: `./gradlew :domain:test`
   - UseCase: `./gradlew :usecase:test`
   - ViewModel: `./gradlew :viewmodel:test`

3. Android Instrumented Tests
   `./gradlew pixel5api34Check`

4. Screenshot Tests
   - Update: `./gradlew :android-screenshot-tests:updateDebugScreenshotTest`
   - Verify: `./gradlew :android-screenshot-tests:validateDebugScreenshotTest`

5. iOS Tests
   `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 15'`

6. Full CI Suite
   `./scripts/validate.sh`
