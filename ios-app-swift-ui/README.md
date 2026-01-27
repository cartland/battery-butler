# Battery Butler iOS Native App

This directory contains the Swift source code for the native iOS application of Battery Butler, using Kotlin Multiplatform ViewModels and SwiftUI.

## Setup Instructions

1.  **Generate the Framework**:
    Run the following Gradle command from the root of the repository to generate the debug framework for the simulator:
    ```bash
    ./gradlew :ios-swift-di:linkDebugFrameworkIosSimulatorArm64
    ```
    (Or `linkDebugFrameworkIosArm64` for physical device).

2.  **Create Xcode Project**:
    -   Open Xcode.
    -   Create a new Project -> iOS -> App.
    -   Product Name: `BatteryButlerNative`.
    -   Interface: SwiftUI.
    -   Language: Swift.
    -   Save it inside this `ios-app-swift-ui` directory (e.g., `ios-app-swift-ui/BatteryButlerNative`).

3.  **Link Framework**:
    -   In Xcode, select the project target.
    -   Go to "General" -> "Frameworks, Libraries, and Embedded Content".
    -   Click "+" -> "Add Other..." -> "Add Files...".
    -   Navigate to `BatteryButler/ios-swift-di/build/bin/iosSimulatorArm64/debugFramework/shared.framework`.
    -   **Important**: Ensure "Embed & Sign" is selected.
    -   Also add "Search Paths" in Build Settings if necessary (search for "Framework Search Paths" and add the path to the debugFramework folder).

4.  **Add Source Files**:
    -   Drag `BatteryButlerApp.swift`, `ContentView.swift`, `DeviceRow.swift`, and `ViewModelWrapper.swift` into the Xcode project group.
    -   Delete the default `BatteryButlerNativeApp.swift` (or rename `BatteryButlerApp` to match your project creation).
    -   Delete default `ContentView.swift` (replace with provided).

5.  **Build and Run**:
    -   Select a Simulator.
    -   Run (Cmd+R).

## Architecture

-   **DI**: Uses `kotlin-inject` via `IosNativeHelper` initialized in `BatteryButlerApp`.
-   **ViewModel**: `HomeViewModel` (KMP) is wrapped in `ViewModelWrapper` (Swift) to adapt `StateFlow` to SwiftUI's `ObservableObject`.
-   **UI**: Pure SwiftUI.
