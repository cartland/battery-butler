# Testing Principles

This document describes the testing philosophy for Battery Butler. Tests are an investment — they should protect real behavior, not accumulate as busywork.

## Core Principles

### 1. Tests Must Test Real Code

Every test must exercise production code through its public API. A test that only verifies fake behavior is worse than no test — it gives false confidence.

```kotlin
// GOOD: Tests real use case logic through its public invoke()
val useCase = AddBatteryEventUseCase(repo, updateLastReplaced)
useCase(event)
assertEquals(replacementDate, updatedDevice.batteryLastReplaced)

// BAD: Tests the fake, not the production code
val repo = FakeDeviceRepository()
repo.addDevice(device)
assertTrue(repo.devices.contains(device))  // This tests the fake
```

Fakes exist to isolate the unit under test, not to become the unit under test.

### 2. Tests Must Be Useful

A test is useful when it would catch a regression that matters. Ask: "If this test broke, would I care?" If the answer is no, delete it.

Signs a test is useful:
- It protects a business rule (e.g., adding a battery event also updates the device's last-replaced timestamp)
- It guards a boundary (e.g., null/blank input returns a default value)
- It would catch a real mistake someone would make during refactoring

Signs a test is not useful:
- It only verifies that a function calls another function with the same arguments (pure delegation with no logic)
- It duplicates coverage already provided by a higher-level test
- It tests framework behavior rather than application behavior

### 3. Test Behavior, Not Implementation

Tests should verify what code does, not how it does it. This makes tests resilient to refactoring.

```kotlin
// GOOD: Tests observable behavior
useCase("Smoke Detector")
assertTrue(engine.recordedPrompts[0].contains("Available Icons:"))

// BAD: Tests internal implementation details
verify(exactly = 1) { engine.generateResponse(match { it.startsWith("***") }, null) }
```

If you refactor the prompt format but the behavior is equivalent, the good test still passes. The bad test breaks for no useful reason.

### 4. One Concept Per Test

Each test should verify exactly one behavior. When a test fails, its name should tell you what broke.

```kotlin
// GOOD: Each test has a clear purpose
fun `returns existing type ID when name matches`()
fun `creates new type with default icon when name not found`()
fun `returns default_type for null name`()

// BAD: Testing multiple things
fun `test FindOrCreateDeviceType`()  // What aspect? Everything?
```

### 5. Tests Are Documentation

A well-written test suite is the most accurate documentation of how code behaves. Test names should read as specifications.

```kotlin
class AddBatteryEventUseCaseTest {
    fun `invoke adds event to repository`()
    fun `invoke updates device lastReplaced timestamp`()
}
// Reading these tells you: adding a battery event persists it AND updates the device.
```

## What to Test

### High Value (Always Test)
- **Use cases with logic**: Any use case that does more than delegate — combines data, applies rules, coordinates multiple operations
- **Boundary conditions**: Null inputs, empty collections, edge cases in domain logic
- **Error handling**: What happens when AI fails, when a device isn't found, when data is malformed
- **Business rules**: "A device's last-replaced date reflects its most recent battery event" — this is the kind of rule that breaks silently

### Medium Value (Test When Practical)
- **Pure functions**: Sorting, grouping, formatting — cheap to test, easy to break during refactoring
- **Data transformations**: Mapping between layers (domain to DTO, proto to domain)
- **Flow emissions**: Verifying that reactive streams emit correct values when underlying data changes

### Low Value (Skip Unless There's a Specific Risk)
- **Pure delegation**: A use case that only calls `repository.doThing()` with no additional logic
- **Data classes and enums**: The compiler already tests these
- **Framework integration**: Testing that Compose renders a button — use screenshot tests instead

## Test Infrastructure

### Fakes Over Mocks

We use hand-written fakes (e.g., `FakeDeviceRepository`, `FakeAiEngine`) rather than mocking libraries. Fakes are:
- **Readable**: You can open the fake and understand its behavior
- **Reusable**: The same fake works across dozens of test files
- **Refactor-friendly**: Changing a method signature updates one fake, not fifty mock setups
- **Multiplatform**: No platform-specific mocking library needed

All fakes live in the `test-common` module so they're shared across `usecase`, `viewmodel`, `data`, and other test suites.

### TestDevices Builders

Use `TestDevices.createDevice()`, `.createDeviceType()`, `.createBatteryEvent()` to construct test data. Every parameter has a sensible default, so you only specify what matters for your test:

```kotlin
// Only the fields relevant to this test are specified
val device = TestDevices.createDevice(id = "d1", location = "Kitchen")
```

### Coroutine Testing

- Use `runTest { }` for all coroutine tests
- Use `flow.first()` to collect a single emission
- Use `flow.toList()` when verifying multiple emissions or empty flows

## Conventions

- **Package**: Tests mirror their source package (e.g., `usecase/src/commonTest/.../usecase/`)
- **Naming**: `ClassUnderTestTest.kt` with backtick test names describing behavior
- **Assertions**: `kotlin.test` only (`assertEquals`, `assertTrue`, `assertNull`, etc.)
- **No test utilities in production code**: Test helpers belong in `test-common`, never in main source sets
- **Opt-in annotations**: Add `@OptIn(ExperimentalTime::class)` when using `kotlin.time.Instant`

## Anti-Patterns

### The Tautology Test
```kotlin
// This test is worthless — it restates the implementation
fun `getDeviceTypes returns repository types`() {
    repo.setDeviceTypes(listOf(type))
    assertEquals(listOf(type), useCase().first())
    // All you've proven is that the fake returns what you put in
}
```
This is acceptable for simple delegation use cases as a smoke test, but don't pretend it's providing meaningful coverage. The value is in establishing the wiring works, not in testing logic.

### The Overfitted Test
```kotlin
// Breaks if you change whitespace or prompt phrasing
assertEquals(
    "[Context: Current date/time: 2024-01-15T10:30 (America/Los_Angeles)]\n\n" +
    "=== User's Inventory ===\n...",
    engine.recordedPrompts[0]
)
```
Prefer `contains()` checks for string-based assertions unless exact output matters.

### The Test That Tests Nothing
```kotlin
fun `use case does not throw`() = runTest {
    useCase()  // No assertion — what did this prove?
}
```
If there's no assertion, there's no test.

---

## What Passing Tests Prove

When CI passes, here's what each layer of the test pyramid proves — and what it does NOT prove.

### What CI Proves

| Layer | What It Proves | Test Count |
|-------|----------------|------------|
| **UseCase unit tests** | Business logic is correct: batch parsing, deduplication, date handling, device-event relationships, data export formatting, AI context building | ~87 tests |
| **ViewModel unit tests** | State management works: loading → success/error transitions, user action processing, sort/group/filter logic, form validation | ~107 tests |
| **Convention tests** | Every UseCase has `operator invoke()`, every ViewModel has a corresponding test file | 2 tests |
| **Screenshot tests** | UI renders pixel-perfectly against reference images — catches unintended visual regressions | ~50 tests |
| **Instrumented tests** | App navigates correctly on a real Android device, Room database schema is valid, migrations work | ~10 tests |
| **Architecture checks** | Module dependency rules are enforced (domain depends on nothing, viewmodel doesn't import data, etc.) | Gradle task |
| **Detekt + Spotless** | Code style and Compose rules (modifier naming, parameter order) are enforced | Gradle tasks |

### What CI Does NOT Prove

| Gap | Why | Mitigation |
|-----|-----|------------|
| E2E server round-trip | E2E tests are manual-only (require live server) | `e2e-tests.sh` run before releases |
| AI response quality | FakeAiEngine returns deterministic responses, not real AI | Manual testing with Gemini |
| Real network sync | Tests use `FakeDeviceRepository`, not real gRPC | E2E tests cover this path |
| iOS SwiftUI snapshots | Swift snapshot tests exist but run separately | `xcodebuild test` in CI |
| Performance / ANR | No performance benchmarks in CI | Manual profiling |
| Multi-device sync conflicts | Single-client tests only | Server-side conflict resolution is last-write-wins |

### Confidence Summary

If CI is green, we are **confident** that:
- All business rules implemented in UseCases are correct
- All ViewModel state machines transition correctly
- The UI looks exactly as designed (screenshot baselines)
- The app navigates without crashes on Android
- The database schema and migrations are valid
- No architecture violations exist
- Code style is consistent

We are **not confident** that:
- The app works end-to-end with a real server (requires E2E tests)
- AI produces useful responses (requires manual testing)
- The app performs well under load (no benchmarks)

---

## Coverage Matrix — By Screen

Every screen in `Screen.kt` mapped to its ViewModel test, screenshot tests, and instrumented tests.

| Screen | ViewModel | ViewModel Tests | Screenshot Tests | Instrumented | Confidence |
|--------|-----------|----------------|-----------------|-------------|------------|
| Login | LoginViewModel | LoginViewModelTest (13) | LoginScreenshotTest | ComposeUITest | **HIGH** |
| Devices | HomeViewModel | HomeViewModelTest (9) | MainTabsScreenshotTest | ComposeUITest | **HIGH** |
| History | HistoryListViewModel | HistoryListViewModelTest (9) | MainTabsScreenshotTest | ComposeUITest | **HIGH** |
| Types | DeviceTypeListViewModel | DeviceTypeListViewModelTest (6) | MainTabsScreenshotTest | ComposeUITest | **HIGH** |
| Settings | SettingsViewModel | SettingsViewModelTest (13) | ScreensScreenshotTest | ComposeUITest | **HIGH** |
| AddDevice | AddDeviceViewModel | AddDeviceViewModelTest (7) | AddDeviceScreenshotTest | ComposeUITest | **HIGH** |
| AddBatteryEvent | AddBatteryEventViewModel | AddBatteryEventViewModelTest (5) | ScreensScreenshotTest | ComposeUITest | **HIGH** |
| AddDeviceType | AddDeviceTypeViewModel | AddDeviceTypeViewModelTest (4) | ScreensScreenshotTest | ComposeUITest | **HIGH** |
| DeviceDetail | DeviceDetailViewModel | DeviceDetailViewModelTest (8) | DeviceDetailScreenshotTest | ComposeUITest | **HIGH** |
| EditDevice | EditDeviceViewModel | EditDeviceViewModelTest (5) | — | ComposeUITest | **MEDIUM** |
| EventDetail | EventDetailViewModel | EventDetailViewModelTest (6) | — | ComposeUITest | **MEDIUM** |
| EditDeviceType | EditDeviceTypeViewModel | EditDeviceTypeViewModelTest (5) | — | ComposeUITest | **MEDIUM** |
| AI Chat (overlay) | AiChatViewModel | AiChatViewModelTest (7) | AiOverlayScreenshotTest | — | **MEDIUM** |

**Legend:**
- **HIGH** = ViewModel tests + screenshot tests + instrumented tests
- **MEDIUM** = ViewModel tests + (screenshot OR instrumented), but not both
- **LOW** = Missing ViewModel tests

---

## Coverage Matrix — By Business Rule

Key business rules mapped to the tests that protect them.

| Rule | Test | Confidence |
|------|------|------------|
| Adding event updates device lastReplaced | UpdateDeviceLastReplacedUseCaseTest (10) | **HIGH** |
| Batch import parses natural language (devices) | BatchAddDevicesUseCaseTest (1) | **HIGH** |
| Batch import parses natural language (types) | BatchAddDeviceTypesUseCaseTest (3) | **HIGH** |
| Batch import parses natural language (events) | BatchAddBatteryEventsUseCaseTest (3) | **HIGH** |
| Batch import deduplicates existing types | BatchAddDeviceTypesUseCaseTest | **HIGH** |
| Data syncs bidirectionally | DefaultDeviceRepositoryTest (34) | **HIGH** |
| AI chat augments messages with context | SendChatMessageUseCaseTest (4), BuildAiContextUseCaseTest (5) | **HIGH** |
| AI tool handler routes to correct operations | DeviceToolHandlerTest (17) | **HIGH** |
| Export includes all data types | ExportDataUseCaseTest (8) | **HIGH** |
| Find-or-create avoids duplicate devices | FindOrCreateDeviceUseCaseTest (4) | **HIGH** |
| Find-or-create avoids duplicate types | FindOrCreateDeviceTypeUseCaseTest (4) | **HIGH** |
| Preloaded types seeded on first launch | PreloadCommonTypesUseCaseTest (3) | **HIGH** |
| Icon suggestion returns valid icon name | SuggestDeviceIconUseCaseTest (4) | **HIGH** |
| Every UseCase has operator invoke | UseCaseConventionTest | **HIGH** |
| Every ViewModel has a test class | ViewModelTestConventionTest | **HIGH** |
| Device sort/group/filter works correctly | HomeViewModelTest (9) | **HIGH** |
| Type sort/group works correctly | DeviceTypeListViewModelTest (6) | **HIGH** |
| History sort/filter works correctly | HistoryListViewModelTest (9) | **HIGH** |
| Room migrations preserve data | MigrationTest | **HIGH** |
| Module dependencies enforce architecture | ArchitectureCheckTask (Gradle) | **HIGH** |
