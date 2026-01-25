# Battery Butler

**Battery Butler** is a Kotlin Multiplatform (KMP) application designed to help users track battery usage and replacements for their household devices. It leverages modern Android and KMP technologies including Compose Multiplatform, Room, Koin, and on-device AI integration.

## 🚀 Features

*   **Device Management**: Add, edit, and delete devices with custom attributes (name, location).
*   **Battery History**: Track battery replacement events for each device.
*   **Device Types**: Manage reusable device types (e.g., "TV Remote" uses 2x AAA batteries).
*   **AI Integration**:
    *   **Magic AI Button**: Automatically suggests device types and icons based on descriptions.
    *   **Batch Import**: Parse natural language notes (e.g., "Replaced smoke detector battery yesterday") to batch create events.
*   **Cross-Platform**: Runs on Android, iOS, and Desktop (JVM).

## 🏗 Architecture

The project follows **Clean Architecture** principles adapted for Kotlin Multiplatform.

For a detailed deep-dive into the module structure, dependency graph, and strict layer rules, please read the **[Architecture Documentation](docs/Architecture.md)**.

### Tech Stack

*   **Language**: Kotlin 2.0+
*   **UI**: [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) (Android, Desktop), SwiftUI (iOS).
*   **Dependency Injection**: [Kotlin Inject](https://github.com/evant/kotlin-inject) (Koin was previously used but migrated).
*   **Persistence**: [Room for KMP](https://developer.android.com/kotlin/multiplatform/room).
*   **Concurrency**: Kotlin Coroutines & Flow.
*   **AI**: Google AI Client SDK (Gemini) / ML Kit.
*   **Networking**: Ktor & Wire (gRPC).
*   **Date/Time**: `kotlinx-datetime`.

## 🛠 Building and Running
This project uses Gradle for build and test orchestration.

### Common Commands
*   **Format Code**: `./scripts/format.sh` (runs `spotlessApply`)
*   **Run Tests**: `./gradlew test` (Unit) or `./gradlew pixel5api34Check` (Android Instrumented)
*   **Debug Flow**: `./scripts/run-e2e-debug-flow.sh` (Starts Server, App, and monitors logs)
*   **Update Diagram**: `./gradlew generateMermaidGraph`

### Android
*   Open the project in **Android Studio**.
*   Select the `composeApp` configuration.
*   Run on an Emulator or connected Device.

### Desktop (JVM)
*   Run the Gradle task: `./gradlew :compose-app:run`

### iOS
*   Open `ios-app-swift-ui/iosAppSwiftUI.xcodeproj` in **Xcode**.
*   Ensure you have built the KMP framework at least once (`./gradlew :compose-app:embedAndSignAppleFrameworkForXcode`).
*   Run on an iPhone Simulator or Device.

### Server (gRPC)
*   Run locally: `./gradlew :server:app:run`
*   The server listens on port `50051` by default.

## 🔑 AI Configuration (Optional)

To enable the AI features (Gemini), you need an API Key.
1.  Obtain an API Key from [Google AI Studio](https://aistudio.google.com/).
2.  Add it to your `local.properties` file:
    ```properties
    GEMINI_API_KEY=your_api_key_here
    PRODUCTION_SERVER_URL=http://your-server-url:port (Optional, defaults to internal AWS NLB)
    ```

## 🤝 Contributing

This project uses `Spotless` for code formatting.
Run `./scripts/format.sh` before committing to ensure your code follows the style guidelines.
Use `./scripts/prepare-for-commit.sh` to validate your changes before pushing.