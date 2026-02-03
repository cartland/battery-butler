# ADR-002: Test Coverage Strategy

## Status
Accepted

## Context
Not all code needs the same level of test coverage. We need a strategy that focuses testing effort on code that benefits most from it, while avoiding low-value tests that add maintenance burden.

## Decision

### What Needs Tests

| Layer | Test Type | When Required |
|-------|-----------|---------------|
| UseCase | Unit test | Has business logic (conditionals, transformations, orchestration) |
| ViewModel | Unit test | Has state management logic |
| Repository impl | Integration test | Complex data operations |
| UI/Composables | Screenshot test | Visual components |
| Domain models | Unit test | Has computed properties or validation |

### What Does NOT Need Tests

**Thin wrapper UseCases** - Single-line delegation to Repository:
```kotlin
// NO TEST NEEDED - no logic, just delegation
class GetDevicesUseCase(private val repo: DeviceRepository) {
    operator fun invoke() = repo.getAllDevices()
}
```

**Simple CRUD UseCases** - Direct pass-through with no transformation:
- `AddDeviceUseCase` (if just calls `repo.add()`)
- `DeleteDeviceUseCase` (if just calls `repo.delete()`)
- `GetDeviceTypesUseCase`, `GetBatteryEventsUseCase`, etc.

**DI configuration** - `*Component` classes are tested implicitly by app startup.

**Test utilities** - `test-common`, `fixtures` modules exist to support tests.

**Resource-only modules** - `compose-resources` has no logic.

### What DOES Need Tests

**Complex UseCases** with real logic:
- `BatchAdd*UseCase` - parsing, validation, AI integration
- `ExportDataUseCase` - data transformation
- `UpdateDeviceLastReplacedUseCase` - date comparison logic
- `SuggestDeviceIconUseCase` - AI prompt handling

**ViewModels** with state logic:
- State transitions
- Error handling
- User action processing

**Repository implementations** with complex queries or caching.

## Current Coverage

| Module | Strategy | Coverage |
|--------|----------|----------|
| usecase | Unit tests for complex logic | 8/24 files (covers complex cases) |
| viewmodel | Unit tests | 3 ViewModels tested |
| presentation-feature | Screenshot tests | Covered by android-screenshot-tests |
| data | Integration tests | Repository tests exist |
| domain | Unit tests | Model tests exist |

## Consequences

### Positive
- Testing effort focused on high-value areas
- Less maintenance burden from trivial tests
- Faster test suite execution
- Clear guidelines for code review

### Negative
- Raw file count coverage metrics look low
- Requires judgment on what's "thin wrapper" vs "has logic"

## Guidelines for New Code

1. **Adding a new UseCase?**
   - If it has conditionals, loops, or transformations → write tests
   - If it's a single-line Repository call → skip tests

2. **Adding a new ViewModel?**
   - Always write tests for state management logic

3. **Adding UI components?**
   - Add screenshot tests for visual regression

4. **Unsure?**
   - Ask: "What bug could this test catch?"
   - If the answer is "Repository bug" → test the Repository instead
