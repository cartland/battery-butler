# UI Screens Mapping

This document details the mapping between the Compose Multiplatform UI screens and their native SwiftUI counterparts, highlighting the similarities, mappings, and unique aspects of each implementation.

## Overview

The Battery Butler app uses a dual-UI paradigm on iOS:
- **Compose App:** A shared UI layer written in Kotlin using Compose Multiplatform, running on both Android and iOS.
- **SwiftUI App:** A native iOS UI written in Swift and SwiftUI, leveraging the shared KMP logic (ViewModels, UseCases, Repositories) via SKIE and the `NativeComponent` DI bridge.

The goal is to achieve feature parity between these two UI implementations by connecting the same ViewModels to analogous UI components in both frameworks.

## Screen Mapping

| Feature | Compose Component | SwiftUI Component | ViewModel |
|---|---|---|---|
| **Main/Tabs** | `MainScaffold` / `MainTabsScreens.kt` | `MainScreen.swift` | N/A (Routing) |
| **Home (Tab)** | `DevicesScreenRoot` -> `HomeScreenContent.kt` | `HomeScreen.swift` | `HomeViewModel` |
| **History (Tab)** | `HistoryScreenRoot` -> `HistoryListContent.kt` | `HistoryListScreen.swift` | `HistoryListViewModel` |
| **Device Types (Tab)** | `TypesScreenRoot` -> `DeviceTypeListContent.kt` | `DeviceTypeListScreen.swift` | `DeviceTypeListViewModel` |
| **AI Chat (Overlay + Standalone)** | `MainScreenShell` bottom bar + `AiTabContent` overlay; `AiChatScreen.kt` for standalone | `AiChatScreen.swift` | `AiChatViewModel` |
| **Add Device** | `AddDeviceScreen.kt` | `AddDeviceScreen.swift` | `AddDeviceViewModel` |
| **Device Detail** | `DeviceDetailScreen.kt` | `DeviceDetailScreen.swift` | `DeviceDetailViewModel` (via Factory) |
| **Edit Device** | `EditDeviceScreen.kt` | `EditDeviceScreen.swift` | `EditDeviceViewModel` (via Factory) |
| **Add Battery Event** | `AddBatteryEventScreen.kt` | `AddBatteryEventScreen.swift` | `AddBatteryEventViewModel` |
| **Event Detail** | `EventDetailScreen.kt` | `EventDetailScreen.swift` | `EventDetailViewModel` (via Factory) |
| **Add Device Type** | `AddDeviceTypeScreen.kt` | `AddDeviceTypeScreen.swift` | `AddDeviceTypeViewModel` |
| **Edit Device Type** | `EditDeviceTypeScreen.kt` | `EditDeviceTypeScreen.swift` | `EditDeviceTypeViewModel` (via Factory) |
| **Settings** | `SettingsScreen.kt` | `SettingsScreen.swift` | `SettingsViewModel` |
| **Login** | `LoginScreen.kt` | `LoginScreen.swift` | `LoginViewModel` |

## Detailed Breakdown & Unique Characteristics

### 1. Main Navigation, Tabs, & The "Content" Asymmetry
- **Compose:** The bottom tab navigation is structured using a `MainScreenShell` which wraps pure UI components (`...Content` functions) to provide standard Top and Bottom app bars. Because of this, the 4 top-level tabs don't have individual `...Screen.kt` files in the same way standalone screens do. Instead, `MainTabsScreens.kt` declares `DevicesScreenRoot`, `HistoryScreenRoot`, etc., which parse the ViewModels and pass state to `MainScreen.kt` wrappers (`DevicesScreen`, `HistoryScreen`), which finally host the pure `HomeScreenContent.kt`, `HistoryListContent.kt`, etc.
- **SwiftUI:** Uses a native `TabView` inside `MainScreen.swift`. Since each tab in iOS conceptually acts as its own navigation hierarchy, the SwiftUI app instantiates `HomeScreen.swift`, `HistoryListScreen.swift`, etc. directly inside the `TabView`. Each of these Swift screens declares its own `NavigationStack`, eliminating the need for a shared shell mechanism or `...Content` separation.

### 2. Home / Devices 
- **Compose:** Handled by `HomeScreenContent.kt` representing the pure UI, while state collection happens in `DevicesScreenRoot`. Uses `LazyColumn` with dynamic grouped headers for locations.
- **SwiftUI:** Handled by `HomeScreen.swift` wrapping its own state collection. Utilizes `List` and `Section` which automatically apply native iOS styling and grouping (similar to clustered tables in UIKit).

### 3. Adding & Editing (Devices, Types, Events)
- **Compose:** Often presents these via regular `NavHost` pushed screens or modals.
- **SwiftUI:** Makes heavy use of the `.sheet()` modifier for presenting entry forms modally (e.g., `AddDeviceScreen`, `AddBatteryEventScreen`, `EditDeviceScreen`). Form items use the native `Form` component.

### 4. Details (Device Detail & Event Detail)
- **Both:** Display read-only parameters and related lists (e.g., Device showing Battery History).
- **SwiftUI:** Integrates `ViewModelWrappers` using parameter-based factories derived from `NativeComponent` instead of generic `koinViewModel()`/`inject()` navigation parameters. Examples include `DeviceDetailViewModelFactory` and `EventDetailViewModelFactory`.

### 5. Authentication (Login)
- **Compose:** Implements the `LoginScreen` through the shared Compose navigation graph to manage AuthState.
- **SwiftUI:** Handles `LoginScreen` conditionally at the root (`WindowGroup`) in `iOSApp.swift` before the main application topology (`TabView`/`MainScreen`) is initialized. Both layers listen to the same `AuthState` from `LoginViewModel`.

### 6. AI Chat & Capabilities
- **Compose:** The AI input is always visible in `MainScreenShell`'s bottom bar (`OutlinedTextField` + send button). Sending a message expands an `AnimatedVisibility` overlay (`AiTabContent` with `showInput = false`) that slides up the chat history while the input stays fixed. `BackHandler(enabled = isAiExpanded)` in `App.kt` collapses the overlay on back press. Tab switches dismiss the overlay via `onTabSelected`. `AiChatScreen.kt` also exists as a standalone full-screen chat (accessed via nav). Custom chat bubbles using basic Composables and Modifiers; handles state changes via Kotlin `StateFlow`.
- **SwiftUI:** Uses nested `ScrollView`/`LazyVStack` and custom `Shape` paths for native iMessage-like bubbles via `ChatBubbleShape`. Collects state changes asynchronously using SKIE wrappers mapping `StateFlow` to `@Published` variables.

## SKIE & ViewModel Wrapper Pattern

A major distinction for the SwiftUI app is the **ViewModel Wrapper** pattern. Since Kotlin's `StateFlow` doesn't map directly to SwiftUI's reactive `@Published` variables, every screen in SwiftUI is accompanied by a `*ViewModelWrapper.swift`:

1.  **Bridging:** The wrapper acts as an `ObservableObject`.
2.  **SKIE AsyncSequence:** Thanks to SKIE, Kotlin Coroutines and Flows are exposed natively to Swift as `AsyncSequence`.
3.  **Observation:** The wrapper initializes a Swift `Task`, iterates `for await state in viewModel.flow`, and updates local `@Published` vars.
4.  **Disposal:** Uses `KmpViewModelStore` to ensure proper ViewModel scoping until the wrapper is deallocated (`deinit`).
