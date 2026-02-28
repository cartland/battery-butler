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
