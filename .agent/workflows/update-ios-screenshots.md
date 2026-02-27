---
description: Update reference snapshots for iOS SwiftUI visual regression testing.
allowed-tools: Bash(*), Read, Glob, Grep
---

# Update iOS Screenshots

Update reference snapshots for iOS SwiftUI regression testing. This runs the snapshot tests using `swift-snapshot-testing` and captures new baselines if they are missing or if `isRecording` is manually set to true.

## Steps

1. **Delete Existing Snapshots (Optional):**
   If you need to forcefully update existing snapshots rather than just recording new ones, you must delete the existing `__Snapshots__` directory first. The `xcodebuild` CLI does not have a native `--update-snapshots` flag for `swift-snapshot-testing`.
   ```bash
   rm -rf ios-app-swift-ui/iosAppSwiftUITests/__Snapshots__
   ```

2. **Run the Tests to Record:**
   Execute the `xcodebuild test` command. This will iterate over the test suite and automatically write new `.png` files for any snapshot assertion that lacks a reference image on disk.
   ```bash
   cd ios-app-swift-ui && xcodebuild test -project iosAppSwiftUI.xcodeproj -scheme iosAppSwiftUITests -destination "platform=iOS Simulator,name=iPhone 16 Pro,OS=18.5"
   ```

3. **Verify the Changes:**
   Check the `ios-app-swift-ui/iosAppSwiftUITests/__Snapshots__` directory to ensure the `.png` files were updated or created successfully.
   ```bash
   git status ios-app-swift-ui/iosAppSwiftUITests/__Snapshots__
   ```

## Notes

- Unlike Paparazzi/Roborazzi for Android, there isn't a separate Gradle task for updating vs. validating. A missing snapshot dictates a recording event.
- If a test fails with "Record mode is on" or "No reference was found on disk", it means a new snapshot was just generated. Subsequent runs will validate against it and pass.
- For testing SwiftUI views connected to KMP, ensure the `Screen` structures are separated into stateless `ContentView` structures to bypass the `NativeComponent` DI graph during testing.
