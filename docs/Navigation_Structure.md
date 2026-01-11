# Navigation Backstack Structure

This project uses a **Stack-based Tab System** where the "Tabs" are integral parts of the navigation stack, rather than parallel containers.

## Core Concept
The navigation structure consists of two layers:
1.  **Base Tab Layer**: The foundation of the app (Devices, Types, History).
2.  **Configuration Layer**: Stacks on top of the base layer (Add, Edit, Details).

## 1. Base Tab Layer
The "Devices" screen serves as the root of the application. Other tabs are pushed onto the stack.

- **Devices Tab** (Home)
    - Stack: `[Devices]`
- **Types Tab**
    - Stack: `[Devices, Types]`
    - Back Action: Pops `Types` → Returns to `Devices`.
- **History Tab**
    - Stack: `[Devices, History]`
    - Back Action: Pops `History` → Returns to `Devices`.

## 2. Configuration Layer
Configuration screens (Add, Edit, Settings) are pushed on top of the *current* Base Tab stack.

### Examples
- **Adding a Device**:
    - Stack: `[Devices, Add Device]`
- **Adding a Type from Types Tab**:
    - Stack: `[Devices, Types, Add Type]`
- **Nested Configuration**:
    - Stack: `[Devices, History, Add History, Edit Device, Add Type]`

## Navigation Behavior
- **Tab Switching**:
    - Clicking "Devices" clears the stack above `Devices` (or resets to `[Devices]`).
    - Clicking "Types" sets the stack to `[Devices, Types]`.
    - Clicking "History" sets the stack to `[Devices, History]`.
- **Global Access**:
    - "Add" actions can be triggered from any state.
    - Example: `[Devices, Add History]` is valid.

## Implementation Details
- **Navigation Library**: Navigation 3 (`androidx.navigation3`).
- **Keys**: All screens are defined as `@Serializable` keys in `App.kt`.
- **Shell**: `MainScreenShell` provides the `Scaffold`, `TopAppBar`, and `NavigationBar` for the Base Tab Layer screens.
