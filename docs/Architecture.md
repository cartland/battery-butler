# Blanket: Application Architecture Specification

This document outlines the architectural guidelines for the "Blanket" Kotlin Multiplatform (KMP) application, adapted from a standard KMP template.

## 1. Core Philosophy

The architecture is designed to maximize code sharing for business logic and data, while embracing a hybrid UI approach. Key principles are high cohesion, low coupling, feature-based modularity, and a great developer experience.

## 2. Modularization Strategy

The project uses a **flat, layer-based modularization** strategy for simplicity and clarity.
 
 * **Application Modules**:
     * `:composeApp`: The primary application module for Compose-based targets (Android, Desktop). It contains shared UI, platform-specific entry points, and the main navigation host.
     * `:iosApp`: The dedicated application module for the iOS target, containing the native SwiftUI implementation.
 * **Feature Modules**:
     * `:ui-feature`: Contains all feature-specific UI screens and content.
     * `:viewmodel`: Contains all ViewModels and UI state management logic.
     * `:usecase`: Contains business rule encapsulation.
 * **Core Modules**:
     * `:domain`: Pure business entities and repository interfaces.
     * `:data`: Repository implementations, database, and network logic.
     * `:ui-core`: Reusable UI components and design system.

## 3. Project Structure
 
 ```sh
 .
 ├── .github/
 │   └── workflows/
 │       ├── build-and-test.yml
 │       ├── internal-distribution.yml
 │       └── release-deploy.yml
 ├── compose-app/
 │   ├── build.gradle.kts
 │   └── src/
 │       ├── androidMain/kotlin/com/blanket/MainActivity.kt
 │       ├── commonMain/kotlin/com/blanket/App.kt
 │       └── desktopMain/kotlin/com/blanket/main.kt
 ├── domain/
 ├── data/
 ├── usecase/
 ├── viewmodel/
 ├── ui-core/
 ├── ui-feature/
 ├── gradle/
 │   └── libs.versions.toml
 ├── ios-app-compose-ui/
 │   ├── iosApp/
 │   │   ├── ContentView.swift
 │   │   └── iOSApp.swift
 │   └── iosApp.xcodeproj/
 ├── ios-app-swift-ui/
 ├── scripts/
 │   └── ...
 ├── build.gradle.kts
 └── settings.gradle.kts
 ```

## 4. UI and Presentation Layer

* **Pattern**: **Model-View-ViewModel (MVVM)**. A shared `ViewModel` resides in the `commonMain` of each `:feature:impl` module.
* **UI Strategy (Hybrid)**:
    * **Shared Compose UI**: The `:compose-app/src/commonMain` contains the shared Compose UI for Android and Desktop targets. The central `App.kt` will host the `NavDisplay`.

## 5. Navigation

A hybrid navigation model is used to support the hybrid UI.
* **Compose Targets (in `:composeApp`)**: **Jetpack Navigation 3** is the primary navigation library. The `NavDisplay`, `rememberNavBackStack` call, and `entryProvider` are implemented in `compose-app/src/commonMain/kotlin/com/blanket/App.kt`.
* **iOS Target (in `:iosApp`)**: **SwiftUI `NavigationStack`** is used for the native UI.
* **Abstraction (`:core:navigation`)**: An `expect interface Navigator` is defined in `commonMain` and injected into ViewModels. This allows shared logic to trigger navigation events, which are then handled natively by either Navigation 3 or `NavigationStack`.

## 6. Core Technologies & Implementation

### 6.1. Data Persistence (`:core:data`)
* **Database**: **Room for KMP** for SQL-backed storage. `@Database`, `@Dao`, and `@Entity` classes reside in `commonMain`.
* **Preferences**: **DataStore Preferences** for simple key-value storage.
* **Instantiation**: Uses an `expect`/`actual` factory pattern for platform-specific database and datastore builders.

### 6.2. Networking (`:core:network`)
* **Library**: **Ktor** for all remote API communication.
* **Instantiation**: Uses an `expect`/`actual` factory to provide platform-specific HTTP engines (`OkHttp` for Android, `Darwin` for iOS).

### 6.3. Dependency Injection (`:core:di`)
* **Library**: **Koin** for its simplicity and strong KMP support.
* **Implementation**: All modules are defined in `commonMain`, with each feature providing its own module. An `expect`/`actual` `initKoin()` function handles platform-specific startup.

## 7. Development Lifecycle & Automation

### 7.1. CI/CD (GitHub Actions)
The project utilizes three distinct workflows for a secure and flexible pipeline:
* **`build-and-test.yml`**: Triggered on pushes/PRs to `main`. Builds the project and runs all unit tests.
* **`internal-distribution.yml`**: Triggered manually (`workflow_dispatch`). Builds signed artifacts for internal testing and uploads them to a service like Firebase App Distribution.
* **`release-deploy.yml`**: Triggered by pushing a version tag (e.g., `v1.2.0`) or manually via `workflow_dispatch` with a `track` input (`alpha`, `beta`, `production`). Deploys the application to the appropriate track in Google Play or uploads it to TestFlight for App Store distribution.

### 7.2. Automated Versioning
* **Version Name** (`1.2.0`): Managed manually in source code (`build.gradle.kts`) to signify feature releases.
* **Build Number** (`versionCode` / `CFBundleVersion`): **Managed automatically by CI/CD**. The `github.run_number` is used as the build number, ensuring every build artifact is uniquely traceable to the CI run that produced it. This number is passed as an environment variable to Gradle and set via command-line tools for Xcode.

### 7.3. Developer Experience Scripts (`scripts/`)
A collection of shell scripts is provided in the root `scripts/` directory to standardize and simplify common local development tasks.
* **Convention**: All scripts begin with `set -ex` to exit on error and print each command before execution, ensuring clarity and safety.
* **Key Scripts**:
    * `validate.sh`: Mimics the main CI workflow by building and testing the entire project.
    * `test-all.sh`: Runs all shared unit tests.
    * `run-android.sh`: Installs the debug Android app on a connected device.
    * `build-ios-framework.sh`: Builds the shared framework for Xcode.
