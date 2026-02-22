# ADR-004: Single Responsibility Principle

## Status
Accepted

## Context
As the codebase grows, classes can accumulate multiple responsibilities that make them harder to test, understand, and modify. The SRP refactoring in PR #582 identified and resolved several violations, establishing patterns that should guide future development.

## Decision
Each class should have one reason to change. When a class accumulates distinct responsibilities, extract them into focused collaborators.

### When to Extract

| Signal | Example |
|--------|---------|
| Class has 2+ distinct responsibilities with minimal interaction points | CRUD delegation + sync infrastructure in one repository |
| Identical logic duplicated across 3+ locations | "Find or create" pattern repeated in 4 use cases |
| Class exceeds ~150 lines with clearly separable concerns | Sort/group logic inline in multiple ViewModels |
| Two sets of tests in one file test unrelated behaviors | CRUD tests + sync retry tests in one test class |

### When NOT to Extract

| Signal | Example |
|--------|---------|
| Tight bidirectional coupling between the responsibilities | Auth flow + token scheduling mutually depend on shared state |
| Extracted code would be <40 lines with no reuse opportunity | A small helper used in only one place |
| Responsibilities share mutable state extensively | Splitting would just move coupling to an interface boundary |

### Established Patterns

These patterns were established by the SRP refactoring and serve as templates for future work:

#### 1. Separate Infrastructure from CRUD
Extract sync, caching, or scheduling logic from repositories into dedicated managers.

- `DefaultSyncManager` owns subscription, push, retry, status (`data/.../repository/DefaultSyncManager.kt`)
- `DefaultDeviceRepository` delegates CRUD to LocalDataSource and sync to SyncManager (`data/.../repository/DefaultDeviceRepository.kt`)
- Key design: SyncManager takes `LocalDataSource` directly to avoid circular DI dependencies

#### 2. Deduplicate via Use Cases
When the same multi-step logic appears in 3+ places, extract a dedicated use case.

- `FindOrCreateDeviceTypeUseCase` — single canonical "find by name or create" operation (`usecase/.../FindOrCreateDeviceTypeUseCase.kt`)
- `FindOrCreateDeviceUseCase` — composes with FindOrCreateDeviceTypeUseCase (`usecase/.../FindOrCreateDeviceUseCase.kt`)
- Consumers (DeviceToolHandler, BatchAddDevicesUseCase, BatchAddBatteryEventsUseCase) become simple delegators

#### 3. Extract Shared Utilities
When two classes contain structurally identical logic, extract a generic utility.

- `sortAndGroup()` in `CollectionExtensions.kt` — generic sort + group with ascending/descending support (`viewmodel/.../CollectionExtensions.kt`)
- Used by both `HomeViewModel` and `DeviceTypeListViewModel` with different type parameters

#### 4. Separate Definitions from Consumers
When a sealed interface or data model is embedded in a large file, move it to its own file.

- `Screen` sealed interface + `navigateTo` extension in `navigation/Screen.kt` (`compose-app/.../navigation/Screen.kt`)
- Separated from the 300+ line `App.kt` composable

### Extraction Checklist

When extracting a responsibility:

1. **Identify the seam** — Find the minimal interaction points between the two responsibilities
2. **Create an interface** if the extracted class will be injected (add to domain module for repository-level abstractions, or keep module-local for utilities)
3. **Move the code** — Extract to a new class, keeping the same behavior
4. **Update DI bindings** — Add `@Provides` method in the relevant Component/Module
5. **Split tests** — Extracted class gets its own test file; original class tests simplify
6. **Verify** — Run `./gradlew test` to ensure no regressions

## Consequences

### Positive
- Smaller, focused classes are easier to test in isolation
- Changes to sync logic don't risk breaking CRUD operations (and vice versa)
- Duplicated logic is maintained in one place, reducing bug surface
- New contributors can understand each class without context-switching between responsibilities

### Negative
- More files and classes to navigate
- DI graph becomes slightly larger (more bindings)
- Over-extraction risk: not every 100-line class needs splitting

## Notes
- The ~150 line threshold is a guideline, not a hard rule. A 200-line class with one cohesive responsibility is fine.
- Prefer composition over inheritance when connecting extracted classes.
- When in doubt, leave it and revisit when tests reveal pain or the class grows further.
