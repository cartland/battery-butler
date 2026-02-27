# Feature Parity Mapping

This document tracks the feature parity and screen equivalence between the **Compose Multiplatform (CMP)** app (Android and iOS) and the **native iOS SwiftUI** app.

## Overview
The architecture of this project centers on sharing ViewModels, UseCases, Repositories, and domain state across all platforms. 
- **Android CMP & iOS CMP** use shared `@Composable` UI code via `presentation-feature`.
- **iOS SwiftUI** uses native SwiftUI views that observe the exact same Kotlin ViewModels via SKIE and `ViewModelWrapper` classes.

The goal is to provide maximum consistency across applications, identifying any missing features.

## Screens and Feature Implementation

| Feature / Screen | Android CMP | iOS CMP | iOS SwiftUI | Notes & Differences |
|------------------|-------------|---------|-------------|---------------------|
| **Tabs / Main Routing** | ✅ Yes | ✅ Yes | ✅ Yes | CMP uses `MainScreenShell`; SwiftUI uses standard `TabView`. |
| **Home (Device List)** | ✅ Yes | ✅ Yes | ✅ Yes | CMP uses `LazyColumn` grouping; SwiftUI uses native `List` and `Section`. |
| **History (Global)**  | ✅ Yes | ✅ Yes | ✅ Yes | Uses exactly the same `HistoryListViewModel`. |
| **Device Types List** | ✅ Yes | ✅ Yes | ✅ Yes | |
| **Settings**          | ✅ Yes | ✅ Yes | ✅ Yes | Network configuration and debug settings matching. |
| **AI Chat Overlay**   | ✅ Yes | ✅ Yes | ✅ Yes | SwiftUI implements native `ChatBubbleShape` for an iMessage feel. |
| **Add Device**        | ✅ Yes | ✅ Yes | ✅ Yes | SwiftUI presents as modal (`.sheet`). |
| **Device Detail**     | ✅ Yes | ✅ Yes | ✅ Yes | |
| **Edit Device**       | ✅ Yes | ✅ Yes | ✅ Yes | SwiftUI presents as modal (`.sheet`). |
| **Add Battery Event** | ✅ Yes | ✅ Yes | ✅ Yes | SwiftUI presents as modal (`.sheet`). |
| **Event Detail**      | ✅ Yes | ✅ Yes | ✅ Yes | Supports Edit Date and Delete in all platforms. |
| **Add Device Type**   | ✅ Yes | ✅ Yes | ✅ Yes | |
| **Edit Device Type**  | ✅ Yes | ✅ Yes | ✅ Yes | |
| **Login / Auth**      | ✅ Yes | ✅ Yes | ✅ Yes | AuthState coordinates root view layer across all. |

## Next Steps
- Parity is complete for all documented screens. Continue to maintain feature parity as new screens are added to the Compose Multiplatform implementation.
