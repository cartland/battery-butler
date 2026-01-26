# Architecture Reconciliation Plan

This document outlines the plan to reconcile the architecture documentation with the actual implementation, while also enforcing stricter design patterns.

## Background

An audit of the codebase revealed discrepancies between `docs/Architecture.md` and the actual module dependencies. This plan addresses both documentation gaps and architectural violations.

## Goals

1. Enforce Clean Architecture principles
2. Reduce module coupling
3. Document all modules accurately
4. Add automated enforcement

## Decisions Made

| Decision | Description |
|----------|-------------|
| `:usecase` isolation | Usecase depends only on `:domain` interfaces, not `:data` directly |
| `:ai` foundation | `:ai` depends only on `:domain`, nothing else |
| `:viewmodel` purity | `:viewmodel` depends only on `:usecase`, `:domain`, `:presentation-model` |
| Type boundaries | Usecase maps AI types to domain types; viewmodel never imports from `:ai` |
| Stateless screens | `:presentation-feature` remains stateless, no `:viewmodel` dependency |
| Enforcement | Gradle `checkArchitecture` task to fail builds on violations |

## Current Violations

| Module | Current Dependency | Violation |
|--------|-------------------|-----------|
| `:usecase` | `:data` | Should only use `:domain` interfaces |
| `:viewmodel` | `:ai` | Should not import AI types directly |
| `:presentation-feature` | `:ai` | Should not depend on `:ai` |

## Target Dependency Graph

```
                         ┌─────────────┐
                         │   :domain   │  (zero dependencies)
                         └──────┬──────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                 │
              ▼                 ▼                 ▼
      ┌───────────────┐ ┌───────────────┐ ┌───────────────┐
      │  :data-local  │ │ :data-network │ │     :ai       │
      └───────┬───────┘ └───────┬───────┘ └───────┬───────┘
              │                 │                 │
              └────────┬────────┘                 │
                       ▼                          │
               ┌───────────────┐                  │
               │    :data      │                  │
               └───────┬───────┘                  │
                       │                          │
                       ▼                          ▼
               ┌─────────────────────────────────────┐
               │              :usecase               │
               │  (maps AI types → domain types)     │
               └───────────────┬─────────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
    ┌─────────────────┐ ┌───────────┐ ┌─────────────────┐
    │:presentation-   │ │:viewmodel │ │:presentation-   │
    │     model       │ └─────┬─────┘ │     core        │
    └────────┬────────┘       │       └────────┬────────┘
             │                │                │
             └────────────────┼────────────────┘
                              ▼
                    ┌─────────────────┐
                    │:presentation-   │
                    │    feature      │  (stateless screens)
                    └────────┬────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │  :compose-app / :ios-swift-di │
              │       (wires everything)      │
              └──────────────────────────────┘
```

## Implementation Phases

### Phase 1: Type Boundaries (Estimated: 2-3 PRs)

#### Step 1.1: Create domain result type
- **File**: `domain/src/commonMain/.../model/BatchOperationResult.kt`
- **Task**: Create a domain type to represent batch operation results
- **Example**:
  ```kotlin
  sealed interface BatchOperationResult {
      data class Progress(val message: String) : BatchOperationResult
      data class Success(val message: String) : BatchOperationResult
      data class Error(val message: String) : BatchOperationResult
  }
  ```

#### Step 1.2: Update usecase to map types
- **Files**: `usecase/src/commonMain/.../BatchAdd*UseCase.kt`
- **Task**: Map `AiMessage` → `BatchOperationResult` before returning
- **Example**:
  ```kotlin
  // Before
  fun invoke(input: String): Flow<AiMessage>

  // After
  fun invoke(input: String): Flow<BatchOperationResult>
  ```

#### Step 1.3: Update viewmodel to use domain types
- **Files**: `viewmodel/src/commonMain/.../*ViewModel.kt`
- **Task**: Replace `AiMessage` with `BatchOperationResult`
- **Task**: Remove `:ai` from viewmodel's `build.gradle.kts`

#### Step 1.4: Update presentation-feature
- **Files**: `presentation-feature/build.gradle.kts`
- **Task**: Remove `:ai` dependency
- **Task**: Update any AI type references to use domain/presentation-model types

### Phase 2: Usecase Isolation (Estimated: 1-2 PRs)

#### Step 2.1: Audit usecase's :data usage
- **Task**: Identify why `:usecase` depends on `:data`
- **Task**: Determine if it's using concrete implementations or just needs interfaces

#### Step 2.2: Inject via interfaces
- **Task**: Ensure usecase only uses `DeviceRepository` interface from `:domain`
- **Task**: Remove `:data` from usecase's `build.gradle.kts`
- **Task**: Verify DI wiring in `:compose-app` still works

### Phase 3: Enforcement (Estimated: 1 PR)

#### Step 3.1: Create checkArchitecture task
- **File**: `buildSrc/src/main/kotlin/architecture/ArchitectureCheckTask.kt`
- **Task**: Parse module `build.gradle.kts` files
- **Task**: Check for forbidden dependencies
- **Rules**:
  ```kotlin
  val forbiddenDependencies = mapOf(
      ":usecase" to listOf(":data", ":data-local", ":data-network"),
      ":viewmodel" to listOf(":ai", ":data", ":data-local", ":data-network"),
      ":presentation-feature" to listOf(":ai", ":viewmodel", ":data"),
      ":presentation-core" to listOf(":ai", ":viewmodel", ":data", ":usecase"),
      ":domain" to listOf("*"),  // domain depends on nothing
      ":ai" to listOf(":usecase", ":data", ":viewmodel", ":presentation-*"),
  )
  ```

#### Step 3.2: Integrate into CI
- **File**: `scripts/validate.sh`
- **Task**: Add `./gradlew checkArchitecture` step
- **File**: `.github/workflows/ci.yml`
- **Task**: Ensure task runs on PR checks

### Phase 4: Documentation (Estimated: 1 PR)

#### Step 4.1: Update Architecture.md
- **Task**: Add all modules organized by layer:
  - **Domain Layer**: `:domain`
  - **Data Layer**: `:data`, `:data-local`, `:data-network`
  - **AI Layer**: `:ai`
  - **Business Logic**: `:usecase`
  - **Presentation State**: `:presentation-model`, `:viewmodel`
  - **Presentation UI**: `:presentation-core`, `:presentation-feature`, `:compose-resources`
  - **Entry Points**: `:compose-app`, `:ios-swift-di`
  - **Testing**: `:fixtures`
  - **Server**: `:server:domain`, `:server:data`, `:server:app`

#### Step 4.2: Fix naming references
- **Task**: Update `ui-core` → `presentation-core`
- **Task**: Update `ui-feature` → `presentation-feature`
- **Task**: Update `networking` → `data-network`

#### Step 4.3: Document dependency rules
- **Task**: Add "Allowed Dependencies" table for each module
- **Task**: Document enforcement mechanism

## Module Dependency Rules (Target State)

| Module | Allowed Dependencies |
|--------|---------------------|
| `:domain` | None |
| `:ai` | `:domain` |
| `:data-local` | `:domain` |
| `:data-network` | `:domain`, `:fixtures` |
| `:data` | `:domain`, `:data-local`, `:data-network` |
| `:usecase` | `:domain`, `:ai` |
| `:presentation-model` | `:domain` |
| `:viewmodel` | `:usecase`, `:domain`, `:presentation-model` |
| `:presentation-core` | `:domain`, `:presentation-model`, `:compose-resources` |
| `:presentation-feature` | `:presentation-core`, `:presentation-model`, `:compose-resources` |
| `:compose-resources` | None |
| `:fixtures` | `:domain` |
| `:compose-app` | All (DI wiring) |
| `:ios-swift-di` | All (DI wiring) |

## Success Criteria

- [ ] `:viewmodel` has no import from `com.chriscartland.batterybutler.ai`
- [ ] `:presentation-feature` has no import from `com.chriscartland.batterybutler.ai`
- [ ] `:usecase` has no `project(":data")` in build.gradle.kts
- [ ] `checkArchitecture` task exists and passes
- [ ] `validate.sh` includes architecture check
- [ ] `Architecture.md` documents all modules with correct names
- [ ] All dependency rules are documented

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking changes during refactor | Run full validation after each step |
| DI wiring issues | Test on both Android and iOS after changes |
| Missing edge cases in type mapping | Review AI message types thoroughly before creating domain equivalents |

## Open Questions

1. Should `BatchOperationResult` be in `:domain` or `:presentation-model`?
   - Recommendation: `:domain` since it represents business operation outcomes

2. Should `:fixtures` be merged into test sources?
   - Deferred: Keep as-is for now, revisit later

## Timeline

| Phase | Estimated PRs | Priority |
|-------|---------------|----------|
| Phase 1: Type Boundaries | 2-3 | High |
| Phase 2: Usecase Isolation | 1-2 | High |
| Phase 3: Enforcement | 1 | Medium |
| Phase 4: Documentation | 1 | Low |

Total: 5-7 PRs
