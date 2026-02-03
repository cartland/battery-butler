# ADR-001: UseCase Layer Dependencies

## Status
Accepted

## Context
UseCases need a clear strategy for what dependencies they can have. Without guidelines, the codebase could accumulate inconsistent patterns that are hard to test and maintain.

## Decision
UseCases may depend on the following types of abstractions:

### Allowed Dependencies

| Type | Pattern | Layer | Purpose |
|------|---------|-------|---------|
| Repository | `*Repository` | Domain | Data persistence and retrieval |
| Engine | `*Engine` | Utility | Computation, AI, processing |
| UseCase | `*UseCase` | UseCase | Composition of operations |

### Dependency Evaluation Framework

Before adding a new dependency type to a UseCase, evaluate:

1. **Is it an abstraction?**
   - Must be an interface or abstract class
   - No concrete implementations

2. **What layer does it live in?**
   - Allowed: Domain, Utility, UseCase
   - Not allowed: Data (implementations), Presentation (UI/ViewModel)

3. **What is its responsibility?**
   - Repository: CRUD operations on data
   - Engine: Stateless computation
   - UseCase: Orchestration of business logic

4. **Is it testable?**
   - Must be mockable/fakeable
   - No hidden dependencies

5. **Side effects?**
   - Pure computation: Always allowed
   - Managed effects (via Repository): Allowed
   - Unmanaged effects: Not allowed

### Examples

```kotlin
// Data Access - depends on Repository
class GetDevicesUseCase(
    private val deviceRepository: DeviceRepository
)

// Computation - depends on Engine
class SuggestDeviceIconUseCase(
    private val aiEngine: AiEngine
)

// Compound - depends on Repository + Engine
class BatchAddDevicesUseCase(
    private val deviceRepository: DeviceRepository,
    private val aiEngine: AiEngine
)

// Composition - depends on Repository + UseCase
class AddBatteryEventUseCase(
    private val deviceRepository: DeviceRepository,
    private val updateDeviceLastReplaced: UpdateDeviceLastReplacedUseCase
)
```

## Consequences

### Positive
- Clear guidelines for code review
- Consistent patterns across codebase
- Easy to test (all dependencies are abstractions)
- Layer boundaries are enforced

### Negative
- May need to create new interfaces for edge cases
- Requires discipline to follow

## Notes
- If a new dependency type is needed, update this ADR
- Consider `*Validator`, `*Mapper`, `*Service` patterns for future needs
