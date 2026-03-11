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
| **Edit Battery Event** | `EditBatteryEventScreen.kt` | `EditBatteryEventScreen.swift` | `EditBatteryEventViewModel` (via Factory) |
| **Add Device Type** | `AddDeviceTypeScreen.kt` | `AddDeviceTypeScreen.swift` | `AddDeviceTypeViewModel` |
| **Device Type Detail** | `DeviceTypeDetailScreen.kt` | `DeviceTypeDetailScreen.swift` | `DeviceTypeDetailViewModel` (via Factory) |
| **Edit Device Type** | `EditDeviceTypeScreen.kt` | `EditDeviceTypeScreen.swift` | `EditDeviceTypeViewModel` (via Factory) |
| **Settings** | `SettingsScreen.kt` | `SettingsScreen.swift` | `SettingsViewModel` |
| **Login** | `LoginScreen.kt` | `LoginScreen.swift` | `LoginViewModel` |

## Feature Parity Matrix

The following table tracks specific feature gaps between the Compose Multiplatform UI and the native SwiftUI UI. Each gap references a bead ID for tracking.

| Feature | Compose | iOS SwiftUI | Gap |
|---------|---------|-------------|-----|
| Sort/Group controls (Home) | Yes | Yes | ~~bb-847x~~ (PR #971) |
| Sort/Group controls (Types) | Yes | Yes | ~~bb-847x~~ (PR #971) |
| Settings: Network mode | Yes | Yes | ~~bb-abn6~~ (PR #972) |
| Settings: AI engine picker | Yes | Yes | ~~bb-abn6~~ (PR #972) |
| Settings: Account section | Yes | Yes | ~~bb-abn6~~ (PR #972) |
| History enrichment (icons, location) | Yes | Yes | ~~bb-wddv~~ (PR #973) |
| Sync status indicator | Yes | Yes | ~~bb-wddv~~ (PR #973) |
| Device Detail: location, stats | Yes | Yes | ~~bb-rrs4.1~~ (PR #970) |
| Device Detail: mapped icons | Yes | Yes | ~~bb-rrs4.1~~ (PR #970) |
| Event Detail: delete button | Yes | Yes | ~~bb-0km4~~ (PR #961) |
| Add Device Type: icon picker | Yes | Yes | ~~bb-rrs4.3~~ (PR #978) |
| AI Chat | Split-screen overlay | Full-screen (toolbar) | Design decision (bb-ke1y closed) |
| Nav animations | Yes | N/A | Platform-specific (iOS uses native UIKit transitions) |

## Detailed Breakdown & Unique Characteristics

### 1. Main Navigation, Tabs, & The "Content" Asymmetry
- **Compose:** The bottom tab navigation is structured using a `MainScreenShell` which wraps pure UI components (`...Content` functions) to provide standard Top and Bottom app bars. Because of this, the 4 top-level tabs don't have individual `...Screen.kt` files in the same way standalone screens do. Instead, `MainTabsScreens.kt` declares `DevicesScreenRoot`, `HistoryScreenRoot`, etc., which parse the ViewModels and pass state to `MainScreen.kt` wrappers (`DevicesScreen`, `HistoryScreen`), which finally host the pure `HomeScreenContent.kt`, `HistoryListContent.kt`, etc.
- **SwiftUI:** Uses a native `TabView` inside `MainScreen.swift`. Since each tab in iOS conceptually acts as its own navigation hierarchy, the SwiftUI app instantiates `HomeScreen.swift`, `HistoryListScreen.swift`, etc. directly inside the `TabView`. Each of these Swift screens declares its own `NavigationStack`, eliminating the need for a shared shell mechanism or `...Content` separation.

> **Known Feature Gaps** (see [FEATURE_PARITY_MAPPING.md](FEATURE_PARITY_MAPPING.md) for details):
> - No persistent AI input bar in SwiftUI (AI is a separate tab, not an always-visible overlay)
> - No split-screen AI overlay mode — Compose expands chat inline while tabs remain visible

### 2. Home / Devices
- **Compose:** Handled by `HomeScreenContent.kt` representing the pure UI, while state collection happens in `DevicesScreenRoot`. Uses `LazyColumn` with dynamic grouped headers for locations.
- **SwiftUI:** Handled by `HomeScreen.swift` wrapping its own state collection. Utilizes `List` and `Section` which automatically apply native iOS styling and grouping (similar to clustered tables in UIKit).

> **Resolved Feature Gaps:**
> - Sort/group controls added (PR #971) — Name/Location/Battery Age/Type sort + group by None/Type/Location
> - Sync status indicator added (PR #973) — animated "Syncing..." pill overlay
>
> **Remaining Gaps:**
> - No mapped device icons or battery age colors in Home list (icons/colors are on Device Detail only)

### 3. Adding & Editing (Devices, Types, Events)
- **Compose:** Often presents these via regular `NavHost` pushed screens or modals.
- **SwiftUI:** Makes heavy use of the `.sheet()` modifier for presenting entry forms modally (e.g., `AddDeviceScreen`, `AddBatteryEventScreen`, `EditDeviceScreen`, `EditBatteryEventScreen`). Form items use the native `Form` component.

> **Resolved:**
> - Add Device Type: icon picker, battery quantity, AI suggestion added (PR #978)
>
> **Remaining Gaps:**
> - Add Device: no location field, no loading indicator during save
> - Add Device Type: batch import deferred
> - Edit Device: no delete confirmation dialog (deletes immediately)
> - Edit Device Type: no icon picker or battery quantity controls

### 4. Details (Device Detail & Event Detail)
- **Both:** Display read-only parameters and related lists (e.g., Device showing Battery History).
- **SwiftUI:** Integrates `ViewModelWrappers` using parameter-based factories derived from `NativeComponent` instead of generic `koinViewModel()`/`inject()` navigation parameters. Examples include `DeviceDetailViewModelFactory` and `EventDetailViewModelFactory`.

> **Resolved Feature Gaps:**
> - Device Detail: mapped icons via SFSymbolMapper, location field, stat cards with battery age colors (PR #970)
> - Event Detail: delete button (PR #961)
>
> **Remaining Gaps:**
> - Event Detail: no Edit button or navigation to associated device

### 5. Authentication (Login)
- **Compose:** Implements the `LoginScreen` through the shared Compose navigation graph to manage AuthState.
- **SwiftUI:** Handles `LoginScreen` conditionally at the root (`WindowGroup`) in `iOSApp.swift` before the main application topology (`TabView`/`MainScreen`) is initialized. Both layers listen to the same `AuthState` from `LoginViewModel`.

> **Known Feature Gaps** (see [FEATURE_PARITY_MAPPING.md](FEATURE_PARITY_MAPPING.md) for details):
> - SwiftUI shows a generic "Failed to sign in" message; Compose maps 7+ `AuthError` subtypes to specific user-facing messages

### 6. AI Chat & Capabilities
- **Compose:** The AI input is always visible in `MainScreenShell`'s bottom bar (`OutlinedTextField` + send button). Sending a message expands an `AnimatedVisibility` overlay (`AiTabContent` with `showInput = false`) that slides up the chat history while the input stays fixed. `BackHandler(enabled = isAiExpanded)` in `App.kt` collapses the overlay on back press. Tab switches dismiss the overlay via `onTabSelected`. `AiChatScreen.kt` also exists as a standalone full-screen chat (accessed via nav). Custom chat bubbles using basic Composables and Modifiers; handles state changes via Kotlin `StateFlow`.
- **SwiftUI:** Uses nested `ScrollView`/`LazyVStack` and custom `Shape` paths for native iMessage-like bubbles via `ChatBubbleShape`. Collects state changes asynchronously using SKIE wrappers mapping `StateFlow` to `@Published` variables.

> **Design Decision (bb-ke1y closed):**
> - Compose uses a split-screen overlay pattern integrated into MainScreenShell's bottom bar
> - SwiftUI uses a full-screen AI chat accessed from toolbar buttons — this is the platform-appropriate pattern for iOS TabView architecture
> - The split-screen overlay doesn't map well to iOS's per-tab NavigationStack model
> - Both approaches provide equivalent functionality through platform-native UX patterns

### 7. Settings
- **Compose:** `SettingsScreen.kt` provides a full settings experience: network mode selector (Prod/Dev/Local/Mock/None), AI engine selector, sign-out button, account info, export data, check for updates, and dynamic app version from `BuildConfig`.
- **SwiftUI:** `SettingsScreen.swift` currently displays only the app version string.

> **Resolved Feature Gaps (PR #972):**
> - Account section with user info and Sign Out
> - Network mode selector (DisclosureGroup)
> - AI engine selector
> - Enhanced app version display
>
> **Remaining Gaps:**
> - No check-for-updates link

## SKIE & ViewModel Wrapper Pattern

A major distinction for the SwiftUI app is the **ViewModel Wrapper** pattern. Since Kotlin's `StateFlow` doesn't map directly to SwiftUI's reactive `@Published` variables, every screen in SwiftUI is accompanied by a `*ViewModelWrapper.swift`:

1.  **Bridging:** The wrapper acts as an `ObservableObject`.
2.  **SKIE AsyncSequence:** Thanks to SKIE, Kotlin Coroutines and Flows are exposed natively to Swift as `AsyncSequence`.
3.  **Observation:** The wrapper initializes a Swift `Task`, iterates `for await state in viewModel.flow`, and updates local `@Published` vars.
4.  **Disposal:** Uses `KmpViewModelStore` to ensure proper ViewModel scoping until the wrapper is deallocated (`deinit`).
