# Navigation

## Overview

Battery Butler uses the Navigation 3 library (`androidx.navigation3`) for state-driven navigation. The UI is a direct function of a list of keys representing the back stack — we modify this list to navigate, and the UI updates automatically.

## App Navigation Structure

The app uses a **Stack-based Tab System** where tabs are integral parts of the navigation stack, rather than parallel containers.

The navigation structure consists of two layers:
1. **Base Tab Layer**: The foundation of the app (Devices, Types, History).
2. **Configuration Layer**: Stacks on top of the base layer (Add, Edit, Details).

### Base Tab Layer

The "Devices" screen serves as the root of the application. Other tabs are pushed onto the stack.

- **Devices Tab** (Home)
    - Stack: `[Devices]`
- **Types Tab**
    - Stack: `[Devices, Types]`
    - Back Action: Pops `Types` → Returns to `Devices`.
- **History Tab**
    - Stack: `[Devices, History]`
    - Back Action: Pops `History` → Returns to `Devices`.

### Configuration Layer

Configuration screens (Add, Edit, Settings) are pushed on top of the *current* Base Tab stack.

**Examples:**
- **Adding a Device**:
    - Stack: `[Devices, Add Device]`
- **Adding a Type from Types Tab**:
    - Stack: `[Devices, Types, Add Type]`
- **Nested Configuration**:
    - Stack: `[Devices, History, Add History, Edit Device, Add Type]`

### Tab Switching Behavior

- Clicking "Devices" clears the stack above `Devices` (or resets to `[Devices]`).
- Clicking "Types" sets the stack to `[Devices, Types]`.
- Clicking "History" sets the stack to `[Devices, History]`.
- "Add" actions can be triggered from any state (e.g., `[Devices, Add History]` is valid).

## Implementation Patterns

### Navigation Keys

Any `@Serializable` object can serve as a navigation key. There is no need to implement a shared base interface, as Navigation 3 can use any serializable type.

* **Requirement:** All keys must be annotated with `@Serializable`.
* **Implementation:** Keys should be a `data object` for screens without parameters or a `data class` for screens that require arguments.

```kotlin
// in :feature:home:api
@Serializable
data object HomeKey : NavKey

// in :feature:products:api
@Serializable
data class ProductDetailKey(val productId: String) : NavKey
```

### Back Stack Management

The back stack's state should be created and managed at the Composable level using `rememberNavBackStack()`.

```kotlin
@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(HomeKey) // Start with Home
    // ... pass backStack to NavDisplay
}
```

### Key-to-Screen Resolution

A `when` expression inside the `entryProvider` resolves navigation keys to screen Composables.

* **Requirement:** The `entryProvider` lambda for `NavDisplay` **must** use a `when` expression on the key.
* **Important:** You **must** include an `else` branch to handle unknown keys gracefully (e.g., logging an error or displaying a "Not Found" screen).

```kotlin
NavDisplay(
    backStack = backStack,
    entryProvider = { key ->
        when (key) {
            is HomeKey -> NavEntry(key) { HomeScreen() }
            is ProductDetailKey -> NavEntry(key) { ProductDetailScreen(key) }
            else -> NavEntry(key) { UnknownScreen(key) } // Handle unknown keys
        }
    },
    // ...
)
```

### ViewModel Scoping

Every major screen should have its own ViewModel scoped to the lifecycle of its corresponding `NavEntry`. This is achieved by adding `rememberViewModelStoreNavEntryDecorator()` to the `NavDisplay`.

```kotlin
NavDisplay(
    entryDecorators = listOf(rememberViewModelStoreNavEntryDecorator()),
    // ...
)

// Inside a screen's Composable (e.g., HomeScreen)
val viewModel: HomeViewModel = viewModel() // This is now scoped correctly
```

### Navigation Events

Business logic is separated from the final navigation action:

1. **UI Action:** `Button(onClick = { viewModel.save() })`.
2. **ViewModel Logic:** The `save()` function calls a repository and, upon success, updates a state flow: `_navigationEvent.value = GoBack`.
3. **UI Reaction:** A `LaunchedEffect` in the screen Composable observes `viewModel.navigationEvent` and, when it sees `GoBack`, calls `backStack.removeLast()`.

## Implementation Details

- **Navigation Library**: Navigation 3 (`androidx.navigation3`).
- **Keys**: All screens are defined as `@Serializable` keys in `App.kt`.
- **Shell**: `MainScreenShell` provides the `Scaffold`, `TopAppBar`, and `NavigationBar` for the Base Tab Layer screens.

## User Journey Cross-Reference

Each navigation path corresponds to a user journey documented in [USER_JOURNEYS.md](USER_JOURNEYS.md).

| Navigation Path | Stack Example | Journey |
|----------------|---------------|---------|
| Home (Devices tab) | `[Devices]` | J2: Browse Devices |
| Types tab | `[Devices, Types]` | J7: Browse Device Types |
| History tab | `[Devices, History]` | J10: Browse History |
| Add Device | `[Devices, AddDevice]` | J3: Add Device |
| Add Type from Types | `[Devices, Types, AddDeviceType]` | J8: Add Device Type |
| Add Event from History | `[Devices, History, AddBatteryEvent]` | J6: Record Battery Replacement |
| Device Detail | `[Devices, DeviceDetail(id)]` | J4: View Device Detail |
| Edit Device | `[Devices, DeviceDetail(id), EditDevice(id)]` | J5: Edit Device |
| Edit Device Type | `[Devices, Types, EditDeviceType(id)]` | J9: Edit Device Type |
| Event Detail | `[Devices, History, EventDetail(id)]` | J11: View/Edit Event |
| Settings | `[..., Settings]` | J12: Configure Settings |
| AI Chat | Overlay on any tab (not in stack) | J13: AI Chat |

**Note:** The AI Chat overlay is not part of the navigation stack. It is managed by `AiChatViewModel` at App scope and toggled by `isAiExpanded` state. See [USER_JOURNEYS.md](USER_JOURNEYS.md) J13 for details.

## Deferred Topics

- Per-entry `metadata`.
- Custom `Scenes` and `SceneStrategy` for adaptive layouts.
- Custom screen transition animations.
