# Navigation 3: Official Project Guidance

This document outlines the standard patterns and best practices for implementing screen navigation in the "Battery Butler" application.

#### Core Principle

Our navigation is state-driven. The UI is a direct function of a list of keys representing the back stack. We modify this list to navigate, and the UI updates automatically.

#### 1. Defining Navigation Keys

Any `@Serializable` object can serve as a navigation key. There is no need to implement a shared base interface, as the Navigation 3 library can use any serializable type. This allows each feature module to define its own keys independently.

*   **Requirement:** All keys must be annotated with `@Serializable`.
*   **Implementation:** Keys should be a `data object` for screens without parameters or a `data class` for screens that require arguments.

**Example:**

```kotlin
// in :feature:home:api
@Serializable
data object HomeKey : NavKey

// in :feature:products:api
@Serializable
data class ProductDetailKey(val productId: String) : NavKey
```

#### 2. Creating and Managing the Back Stack

The back stack's state should be created and managed at the Composable level.

*   **Requirement:** Use the `@Composable fun rememberNavBackStack()` function to create the back stack instance.

**Example:**

```kotlin
@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(HomeKey) // Start with Home
    // ... pass backStack to NavDisplay
}
```

#### 3. Resolving Keys to Screens

To resolve a navigation key to its corresponding screen Composable, we use a `when` expression inside the `entryProvider`.

*   **Requirement:** The `entryProvider` lambda for the `NavDisplay` **must** be implemented using a `when` expression on the key.
*   **Important:** You **must** include an `else` branch to handle unknown keys gracefully, for example by logging an error or displaying a "Not Found" screen. This prevents crashes if a key from a module is not yet handled in the main `composeApp`.

**Example:**

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

#### 4. ViewModel Scoping

Every major screen should have its own ViewModel to manage its UI state and business logic.

* **Requirement:** ViewModels must be scoped to the lifecycle of their corresponding `NavEntry`. This is achieved by adding the `rememberViewModelStoreNavEntryDecorator()` to the `NavDisplay`.

**Example:**
```kotlin
NavDisplay(
    entryDecorators = listOf(rememberViewModelStoreNavEntryDecorator()),
    // ...
)

// Inside a screen's Composable (e.g., HomeScreen)
val viewModel: HomeViewModel = viewModel() // This is now scoped correctly
```

#### 5. Handling Navigation Events

We will use a pattern that separates business logic from the final navigation action.

* **Requirement:** The Composable UI element (e.g., `Button`) should call a method on the ViewModel to handle user actions. The ViewModel performs any necessary business logic and updates its state. The Composable observes this state and triggers the actual navigation by modifying the `backStack` directly.

**Example Flow:**
1.  **UI Action:** `Button(onClick = { viewModel.save() })`.
2.  **ViewModel Logic:** The `save()` function in the `HomeViewModel` calls a repository and, upon success, updates a state flow: `_navigationEvent.value = GoBack`.
3.  **UI Reaction:** A `LaunchedEffect` in the `HomeScreen` Composable observes `viewModel.navigationEvent` and, when it sees `GoBack`, it calls `backStack.removeLast()`.

---
This guide defines a consistent, safe, and testable approach. The following advanced topics are deferred for now, and library defaults will be used:

* **Deferred:** Per-entry `metadata`.
* **Deferred:** Custom `Scenes` and `SceneStrategy` for adaptive layouts.
* **Deferred:** Custom screen transition animations.
