# Architecture & Dependency Graph

This project follows a strict **Clean Architecture** combined with **Kotlin Multiplatform (KMP)** best practices. The codebase is modularized to separate concerns, improve build times, and enforce unidirectional data flow.

## Module Graph

The following Mermaid graph illustrates the dependency structure between modules. Arrows indicate a dependency (e.g., `A --> B` means A depends on B).

![Architecture Diagram](docs/diagrams/kotlin_module_structure.svg)

<details>
  <summary>Click to see Mermaid Source</summary>
  
```mermaid
render_diffs(file:///Users/cartland/github/cartland/battery-butler/docs/diagrams/kotlin_module_structure.mmd)
graph TD
    subgraph "App & Entry Points"
        ComposeApp[":compose-app"]
        ServerApp[":server:app"]
        Shared[":shared"]
    end

    subgraph "UI Layer"
        UICore[":ui-core"]
        UIFeature[":ui-feature"]
    end

    subgraph "Presentation Layer"
        ViewModel[":viewmodel"]
    end

    subgraph "Domain Layer"
        Domain[":domain"]
        ServerDomain[":server:domain"]
        UseCase[":usecase"]
    end

    subgraph "Data Layer"
        Data[":data"]
        Networking[":networking"]
        ServerData[":server:data"]
    end

    %% Dependencies
    ComposeApp --> Data
    ComposeApp --> Networking
    ComposeApp --> UICore
    ComposeApp --> UIFeature
    ComposeApp --> UseCase
    ComposeApp --> ViewModel

    Data --> Domain

    Networking --> Domain

    ServerApp --> ServerData
    ServerApp --> ServerDomain

    ServerData --> ServerDomain

    ServerDomain --> Domain

    Shared --> Data
    Shared --> Domain
    Shared --> UseCase
    Shared --> ViewModel

    UICore --> Domain

    UIFeature --> Domain
    UIFeature --> UICore
    UIFeature --> ViewModel

    UseCase --> Domain

    ViewModel --> Domain
    ViewModel --> UseCase
```
</details>

## Module Descriptions

### Core Layers

*   **`:domain`**: The core of the application. Contains entities, repository interfaces, and pure business objects.
    *   **Dependencies**: Zero dependencies on other modules. Pure Kotlin.
    *   **Role**: Defines the "What" of the app.

*   **`:usecase`**: specific business logic scenarios (Interactors).
    *   **Dependencies**: `:domain`.
    *   **Role**: Orchestrates data flow between Repositories and ViewModels. Encapsulates business rules.

*   **`:data`**: Implementation of the Data Layer (Database, Preferences).
    *   **Dependencies**: `:domain` (implements Repositories).
    *   **Role**: Manages local data persistence (Room, SqlDelight, etc.).

*   **`:networking`**: Implementation of remote data fetching.
    *   **Dependencies**: `:domain` (implements Repositories or Data Sources).
    *   **Role**: gRPC / REST clients (Wire, Ktor).

### Presentation & UI Layers

*   **`:viewmodel`**: KMP State Handling.
    *   **Dependencies**: `:usecase`, `:domain`. (Scope: `androidx.lifecycle.viewmodel`).
    *   **Role**: Manages UI state and handles user intents. Exposes `StateFlow` to UI.

*   **`:ui-core`**: Reusable Design System elements.
    *   **Dependencies**: `:domain` (for core models), Compose Runtime/Material3.
    *   **Role**: Theming, common widgets, basic layout components.

*   **`:ui-feature`**: Feature-specific screens and flows.
    *   **Dependencies**: `:viewmodel`, `:ui-core`, `:domain`.
    *   **Role**: Composable screens (e.g., `AddDeviceScreen`). Connects ViewModels to UI.

### Application Entry Points

*   **`:compose-app`**: The root Android & Desktop application.
    *   **Dependencies**: All modules (to perform Dependency Injection).
    *   **Role**: Bootstrap, DI Graph creation (`AppComponent`), Navigation host.

*   **`:shared`**: The Logic Framework for iOS.
    *   **Dependencies**: Exports `:domain`, `:viewmodel`. Dependencies on `:data`, `:usecase`.
    *   **Role**: Bundles KMP code into an iOS Framework. Swift code interacts with this.

### Server Side

*   **`:server:domain`**: Server-specific business logic + Common Domain.
    *   **Dependencies**: `:domain`.
    *   **Role**: Server side business entities.

*   **`:server:data`**: Server data persistence.
    *   **Dependencies**: `:server:domain`.
    *   **Role**: Database access for the server.

*   **`:server:app`**: The gRPC Server application.
    *   **Dependencies**: `:server:domain`, `:server:data`.
    *   **Role**: Runs the gRPC service, handles requests.

## Strict Dependency Rules

1.  **Vertical Separation**: `UI` never communicates directly with `Data`. It must go through `ViewModel`.
2.  **Domain Purity**: `:domain` cannot see any other module.
3.  **Data Hiding**: `:data` and `:networking` are implementation details. `:viewmodel` and `:ui-*` should not depend on them directly (only via `:domain` interfaces), except for the `:compose-app` root which needs them for DI.
