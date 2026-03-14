# Experimental: KMP Reference Architecture

A reference implementation of clean architecture for scalable, testable Kotlin Multiplatform mobile apps (Android + iOS + Desktop). Optimized for AI agent operation, code review clarity, and rapid feature development.

The feature is intentionally trivial (a counter) so the architecture stands out, not the business logic.

## What it does well

### Screen-scoped features with KMP ViewModel

`CounterViewModel` manages four independent features (counter, app counter, observe, get) with separate `StateFlow` properties. Each can start/stop independently. The ViewModel is shared across Android, iOS, and Desktop via KMP.

### Screen isolation with Nav3

Compose uses `NavDisplay` with a `rememberSaveable` back stack (`ExperimentalApp.kt`). iOS uses `NavigationStack` (`ContentView.swift`). Screens are sealed interfaces (`ExperimentalScreen`), keeping navigation type-safe.

### Business logic isolation via UseCase layer

Each use case has a single responsibility and a single `operator fun invoke()`:

- `GetCounterUseCase` — one-shot read, returns `Result<Long, CounterError>`
- `IncrementCounterUseCase` — single increment, returns `Result<Long, CounterError>`
- `RunCounterUseCase` — infinite loop with 1s delay, returns `Result<Nothing, CounterError>` on failure
- `ObserveCounterUseCase` — returns `Flow<Long>`

### Data access isolation via Repository pattern

`CounterRepository` is a domain interface. `DefaultCounterRepository` wraps a `LocalCounterDataSource`, converting exceptions to `Result<T, CounterError>`. Try-catch only exists at this boundary.

### Data isolation via DataSource layer

`LocalCounterDataSource` is an interface with two implementations:
- `InMemoryLocalCounterDataSource` — `StateFlow`-backed, used by default
- `DataStoreCounterDataSource` — `androidx.datastore.preferences`-backed, for persistence

### Dependency injection with kotlin-inject (KSP)

`ExperimentalAppComponent` is the root `@Component`. Use cases and repositories use `@Inject`. Bindings are declared in `ExperimentalDataComponent` and `ExperimentalDataModule`. No manual wiring in feature code.

### Coroutine lifecycle management

The counter has two identical 1-second increment loops running in different scopes to demonstrate lifecycle differences:

- **VM Counter** (`startCounter`/`stopCounter`) — runs `RunCounterUseCase` in `viewModelScope`. Automatically cancelled when the ViewModel is cleared (e.g., navigating away, closing the screen). Tied to UI lifecycle.
- **App Counter** (`startAppCounter`/`stopAppCounter`) — delegates to `DefaultAppCounterService`, which runs in `appScope` (`SupervisorJob + Dispatchers.Default`). Survives ViewModel destruction. Keeps incrementing until explicitly stopped or the app process dies.

Both write to the same `CounterRepository`, so `ObserveCounterUseCase` sees increments from both. This makes the lifecycle difference visible: stop the VM counter by navigating away, and the app counter keeps going.

Other lifecycle patterns:
- `Job` references for explicit cancellation (`stopCounter()`, `stopObserving()`)
- `DefaultAppCounterService` manages its own `Job` internally, independent of any ViewModel

### Dispatcher management via DispatcherProvider

`DispatcherProvider` is injectable and swappable. Production uses real dispatchers; tests use `UnconfinedTestDispatcher`. No hardcoded `Dispatchers.*` calls in business logic.

### Test usefulness and ease

All logic is testable with `FakeLocalCounterDataSource` and `FakeAppCounterService` — no platform mocks, no Android instrumentation. Fakes live in `commonMain` (not `commonTest`) so every module can use them. Tests cover:

- **data-local** (5 files) — repository error wrapping, service timing, idempotent start/stop
- **usecase** (4 files) — use case logic, coroutine timing, error propagation
- **viewmodel** (1 file, 20+ tests) — state management, feature independence, combined scenarios

## What it does NOT try to do well

- **Beautiful UI** — minimal styling, no design system. `CounterContent` is functional, not polished.
- **Production-ready error handling** — no retry, no exponential backoff, no user-facing error recovery.
- **Complex navigation** — two screens, no deep links, no nested graphs, no tabs.
- **Network/remote data** — everything is local. No HTTP client, no remote data source, no serialization.

## Future additions

- Additional feature screens demonstrating multi-feature architecture
- Room/SQLDelight local persistence example
- Network layer with remote DataSource (Ktor client)
- Complex navigation (deep links, nested graphs, bottom tabs)
- SKIE integration for iOS (replacing Timer-based `StateFlow` polling in `CounterViewModelWrapper`)

## Architecture

### Module dependency direction

```
domain  <-  usecase  <-  viewmodel  <-  presentation-core  <-  compose-app
                                                                ios-app
```

`domain` depends on nothing. Each layer only depends on the layer to its left. `compose-app` and `ios-app` are leaf modules that wire everything together.

### Module map

| Module | Purpose | Key files |
|---|---|---|
| `domain` | Interfaces + models | `CounterRepository`, `AppCounterService`, `CounterState`, `CounterError` |
| `data-local` | Implementations + fakes | `DefaultCounterRepository`, `InMemoryLocalCounterDataSource`, `DataStoreCounterDataSource`, `DefaultAppCounterService`, `FakeLocalCounterDataSource` |
| `usecase` | Single-responsibility operations | `GetCounterUseCase`, `IncrementCounterUseCase`, `RunCounterUseCase`, `ObserveCounterUseCase` |
| `viewmodel` | State management | `CounterViewModel` |
| `presentation-core` | Shared Compose UI | `CounterContent` |
| `compose-app` | Android/Desktop entry + DI | `ExperimentalAppComponent`, `ExperimentalApp`, `ExperimentalActivity` |
| `ios-app` | SwiftUI entry + bridge | `ExperimentalIOSApp`, `CounterScreen`, `CounterViewModelWrapper` |

### iOS bridge pattern

`CounterViewModelWrapper` bridges KMP `StateFlow` to SwiftUI `@Published` properties via 60Hz `Timer.publish` polling. The iOS UI uses a two-layer pattern: `CounterScreen` (stateful `@StateObject` wrapper) and `CounterContentView` (stateless `@Binding` receiver).

### Testing pattern

```
Test  ->  UseCase/ViewModel  ->  FakeLocalCounterDataSource (in commonMain)
```

No platform mocks. No Android test runners. Pure Kotlin `commonTest` for everything except `DataStoreCounterDataSource` (which uses `desktopTest` for filesystem access).
