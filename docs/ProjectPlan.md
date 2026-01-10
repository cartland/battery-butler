# Project Refactoring Plan: Aligning with Target Architecture

This document outlines the necessary steps to refactor the "Blanket" application from its initial template structure to the granular, feature-based architecture defined in `docs/Architecture.md` and `docs/Navigation.md`.

Try to make changes in small, verifiable steps. Check to make sure the app can build after each step.

If you discover new challenges preventing the plan from progressing, update this plan `docs/ProjectPlan.md` with the proposed actions. You also will need to update the other documentation to match future decisions.

## Phase 1: Module Restructuring & Dependency Setup

The first phase focuses on creating the new module structure and configuring dependencies.

1.  **Migrate Legacy Module**:
    *   [ ] Migrate `:shared` to other modules. All its functionality will be migrated to the new core and feature modules.
    *   [ ] Remove the `:shared` module entirely.

2.  **Update Gradle Settings**:
    *   [ ] Modify `settings.gradle.kts` to remove `:shared` and include the new module paths.
    *   Example `settings.gradle.kts`:
        ```kotlin
        include(":composeApp")
        // Add the modules below one-at-a-time. Do not attempt to add all at once.
        include(":core:data")
        include(":core:di")
        include(":core:model")
        include(":core:navigation")
        include(":core:network")
        include(":feature:home:api")
        include(":feature:home:impl")
        ```

3.  **Create Module Directories**:
    *   [ ] Create the directory structure for all new core and feature modules as specified in `docs/Architecture.md`.
    *   [ ] Each new module should contain a `build.gradle.kts` file and a `src` directory with `commonMain`, `androidMain`, `iosMain`, etc.

4.  **Configure Dependencies**:
    *   [ ] Update `gradle/libs.versions.toml` with the required versions for Koin, Ktor, Room, Navigation 3, and DataStore.
    *   [ ] Populate the `build.gradle.kts` file for each new module, applying the KMP plugin and adding its specific dependencies (e.g., `:core:network` depends on Ktor, `:feature:home:impl` depends on `:feature:home:api` and `:core:navigation`).

## Phase 2: Implement Core Modules

This phase involves building the foundational layers of the application.

1.  **`:core:navigation`**:
    *   [ ] Define the `expect interface Navigator` for triggering navigation events from ViewModels.

2.  **`:core:network`**:
    *   [ ] Implement the Ktor client.
    *   [ ] Use an `expect`/`actual` factory pattern to provide platform-specific HTTP engines (e.g., `OkHttp` for Android, `Darwin` for iOS).

3.  **`:core:data`**:
    *   [ ] Set up Room for KMP. Define `expect`/`actual` functions for the database builder.
    *   [ ] Set up DataStore Preferences, also using an `expect`/`actual` builder pattern.

4.  **`:core:di`**:
    *   [ ] Configure Koin.
    *   [ ] Implement an `expect`/`actual` `initKoin()` function for platform-specific setup.
    *   [ ] Define Koin modules for the core services created above.

5.  **`:core:model`**:
    *   [ ] Create any initial data classes that will be shared across the entire application.

## Phase 3: Refactor Application Modules

With the core modules in place, the main application modules can be updated to use them.

1.  **`:composeApp`**:
    *   [ ] Update `compose-app/build.gradle.kts` to depend on the necessary `feature` and `core` modules.
    *   [ ] In `compose-app/src/commonMain/kotlin/.../App.kt`, implement the primary navigation host using `NavDisplay`, `rememberNavBackStack`, and the `entryProvider` pattern described in `docs/Navigation.md`.
    *   [ ] Add the `rememberViewModelStoreNavEntryDecorator()` to the `NavDisplay`.
    *   [ ] Call `initKoin()` in the Android (`MainActivity.kt`) and Desktop (`main.kt`) entry points.

2.  **`:iosApp`**:
    *   [ ] Update the Xcode project build phases to link the new frameworks produced by the core and feature modules.
    *   [ ] In `iOSApp.swift`, call the Koin initialization function.
    *   [ ] Implement the UI using SwiftUI, including a `NavigationStack`.
    *   [ ] Create an `actual` implementation of the `Navigator` interface that interacts with the SwiftUI `NavigationStack`.

## Phase 4: Create the First Feature (`:feature:home`)

This phase serves as a template for all future feature development.

1.  **`:feature:home:api`**:
    *   [ ] Define the `@Serializable data object HomeKey`.
    *   [ ] (Optional) Define an interface for the public-facing feature Composable, e.g., `interface HomeEntry`.

2.  **`:feature:home:impl`**:
    *   [ ] Create `HomeViewModel`, inheriting from a shared `ViewModel` class. It should be injected with dependencies (e.g., a repository, the `Navigator`).
    *   [ ] Create the `HomeScreen` Composable, which uses `viewModel()` to get a correctly-scoped ViewModel instance.
    *   [ ] Implement the navigation logic as described in `docs/Navigation.md`: UI calls ViewModel -> ViewModel updates state -> UI observes state and calls `backStack` modification methods.
    *   [ ] Add the Koin module definition for this feature's services (ViewModel, UseCases, etc.).

3.  **Integration**:
    *   [ ] In `composeApp`, add the `HomeKey` case to the `when` expression in the `entryProvider` to link the key to the `HomeScreen`.

## Phase 5: Cleanup and Verification

The final phase ensures the refactoring is complete and the application is stable.

1.  **Remove Old Code**:
    *   [ ] Delete `Greeting.kt` and `Platform.kt`.
    *   [ ] Double-check that no code remains from the original `:shared` module.

2.  **Build and Test**:
    *   [ ] Run `./gradlew build` to ensure the entire project compiles.
    *   [ ] Run `./gradlew check` to verify code quality standards.
    *   [ ] Execute all unit tests.

3.  **Manual Verification**:
    *   [ ] Deploy and run the application on Android, iOS, and Desktop.
    *   [ ] Manually test the navigation flow for the new "Home" feature to confirm everything is working end-to-end.
