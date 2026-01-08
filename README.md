# Blanket (Battery Butler)

**Blanket** is a Kotlin Multiplatform (KMP) application designed to help users track battery usage and replacements for their household devices. It leverages modern Android and KMP technologies including Compose Multiplatform, Room, Koin, and on-device AI integration.

## 🚀 Features

*   **Device Management**: Add, edit, and delete devices with custom attributes (name, location).
*   **Battery History**: Track battery replacement events for each device.
*   **Device Types**: Manage reusable device types (e.g., "TV Remote" uses 2x AAA batteries).
*   **AI Integration**:
    *   **Magic AI Button**: Automatically suggests device types and icons based on descriptions.
    *   **Batch Import**: Parse natural language notes (e.g., "Replaced smoke detector battery yesterday") to batch create events.
*   **Cross-Platform**: Runs on Android, iOS, and Desktop (JVM).

## 🏗 Architecture

The project follows **Clean Architecture** principles adapted for Kotlin Multiplatform, with a focus on modularity and separation of concerns.

### Module Structure (Flat Hierarchy)

The project is organized into the following Gradle modules:

*   **`:composeApp`**: The main entry point for Android and Desktop applications. Contains the Navigation graph and platform-specific wiring.
*   **`:iosApp`**: The native iOS application entry point (SwiftUI).
*   **`:domain`**: Pure Kotlin module. Contains **Entities** (Data Classes), **Repository Interfaces**, and **Business Logic Objects**. No platform dependencies.
*   **`:data`**: Implements the Repository interfaces. Handles data persistence using **Room** (SQLite) and **DataStore**.
*   **`:usecase`**: Contains granular **Use Cases** that encapsulate specific business rules and orchestration logic (e.g., `AddDeviceUseCase`). Bridges the ViewModel and Repository.
*   **`:viewmodel`**: Contains **ViewModels** (holding `UiState` via `StateFlow`). These use the UseCases to perform actions and expose state to the UI.
*   **`:ui-core`**: Reusable UI components (Design System), formatting utilities, and base UI classes used across features.
*   **`:ui-feature`**: Feature-specific UI Composables (Screens and Content). These depend on `:viewmodel` and `:ui-core`.

### Tech Stack

*   **Language**: Kotlin 2.0+
*   **UI**: [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) (Android, Desktop), SwiftUI (iOS).
*   **Dependency Injection**: [Koin](https://insert-koin.io/) & [Kotlin Inject](https://github.com/evant/kotlin-inject).
*   **Persistence**: [Room for KMP](https://developer.android.com/kotlin/multiplatform/room).
*   **Concurrency**: Kotlin Coroutines & Flow.
*   **AI**: Google AI Client SDK (Gemini) / ML Kit.
*   **Date/Time**: `kotlinx-datetime`.

## 🛠 Building and Running

### Android
*   Open the project in **Android Studio**.
*   Select the `composeApp` configuration.
*   Run on an Emulator or connected Device.

### Desktop (JVM)
*   Run the Gradle task: `./gradlew :composeApp:run`

### iOS
*   Open `iosApp/iosApp.xcodeproj` in **Xcode**.
*   Ensure you have built the KMP framework at least once (`./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`).
*   Run on an iPhone Simulator or Device.

## 🔑 AI Configuration (Optional)

To enable the AI features (Gemini), you need an API Key.
1.  Obtain an API Key from [Google AI Studio](https://aistudio.google.com/).
2.  Add it to your `local.properties` file:
    ```properties
    GEMINI_API_KEY=your_api_key_here
    ```

## 🤝 Contributing

This project uses `Spotless` for code formatting.
Run `./gradlew spotlessApply` before committing to ensure your code follows the style guidelines.