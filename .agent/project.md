# Project Knowledge

Shared project-specific knowledge for all AI agents. This supplements the workflow rules in `AGENTS.md` with technical context about the codebase.

## Architecture

**Battery Butler** is a Kotlin Multiplatform (KMP) app with a Ktor gRPC server. For the project's engineering beliefs, current techniques, and aspirational goals, see [`docs/ENGINEERING_GOALS.md`](../docs/ENGINEERING_GOALS.md).

- **Platforms**: Android, iOS (SwiftUI + Compose), Desktop
- **Server**: Ktor + gRPC on AWS ECS Fargate
- **Database**: Room (local), RDS PostgreSQL (server)
- **Build**: Gradle (app), Bazel (iOS protos), Terraform (infrastructure)
- **CI**: GitHub Actions
- **Task tracking**: `TODO.md` (plain markdown checklist at repo root)

### Offline-First Sync

The app works entirely offline and syncs bidirectionally when online:
- Local changes persist immediately to Room database
- Changes sync to server when connectivity is available
- Server changes sync back to local on reconnect

**On-demand refresh (`SyncManager.resync()`)**: separate from the background
`subscribeWithRetry()` loop, `resync()` (added for pull-to-refresh) does a single
bounded fetch — `withTimeout(15.seconds) { remoteDataSource.subscribe().first() }`
then `applyRemoteUpdate()` — catching timeout/error into `SyncStatus.Failed` rather
than throwing. `ResyncUseCase` is a thin pass-through (`@NoTestRequired`). ViewModels
that support pull-to-refresh (`HomeViewModel`, `DeviceTypeListViewModel`,
`HistoryListViewModel`) expose `isRefreshing: StateFlow<Boolean>` and `onRefresh()`.

**`PullToRefreshBox` requires a scrollable descendant.** Material3's
`PullToRefreshBox` detects the pull gesture via nested-scroll interop — it does
nothing if its content can't itself scroll (e.g. a short empty-state or loading
screen that fits on one screen). This is why `EmptyStateContent` and
`LoadingWithLabel` (`presentation-core`) both wrap their root in
`.verticalScroll(rememberScrollState())`, even though their content rarely
overflows — without it, pull-to-refresh silently doesn't work on those states.
Confirmed via live on-device logcat testing, not just reading the Compose source.

### Module Dependencies

Architecture is enforced by `buildSrc/.../ArchitectureCheckTask.kt`. Key rules:
- `:domain` depends on nothing (pure interfaces and models)
- `:usecase` depends on `:domain`, `:presentation-model`
- `:viewmodel` depends on `:usecase`, `:domain`, `:presentation-model`
- `:ai` contains platform implementations (`AndroidAiEngine`, `DynamicAiEngine`, `OnDeviceAiEngine`, `NoOpAiEngine`); interfaces live in `:domain:model:ai`
- `:usecase` contains `SendChatMessageUseCase` (augments AI messages with time + user context) and `BuildAiContextUseCase` (builds user inventory summary from DeviceRepository)
- `:data` provides implementations of domain interfaces

Import-level boundaries are enforced by `ImportBoundaryCheckTask` (in `validate.sh` and CI):
- `:presentation-feature` cannot import `.domain.repository.*`, `.usecase.*`, `.data.*`, `.ai.*` — must go through ViewModel
- `:presentation-core` cannot import `.data.*`, `.usecase.*`, `.viewmodel.*` — lower UI layer
- `:usecase` cannot import `.data.*`, `.viewmodel.*` — domain interfaces only
- `:domain` cannot import `.data.*`, `.presentation.*` — innermost layer
- Exempt individual lines with `// @ImportBoundaryExempt: <reason>`

### DI Wiring (kotlin-inject)

- `AppComponent` constructor parameters are the DI roots — ALL platform creation sites must be updated when parameters change:
  - Android: `BatteryButlerApplication.kt`
  - Desktop: `compose-app/src/desktopMain/.../Main.kt`
  - iOS Compose: `IosComponentHelper.kt` (3 files: iosArm64Main, iosSimulatorArm64Main, iosX64Main)
  - iOS Native: `NativeComponent.kt` in `ios-swift-di` (uses `@Provides` methods, not constructor params)
- Non-Android platforms use `InMemoryAiPreferencesRepository` (in `:data`); Android uses `AiPreferencesRepositoryImpl` with `SharedPreferencesSettings`
- **Shared no-op implementations** (PR #951): `NoOpRemoteDataSource` (`:data-network`), `NoOpAuthRepository` (`:domain`), `NoOpAiEngine` (`:ai`). Platform entry points use these instead of inline anonymous objects. `IosNativeHelper.kt` in `ios-swift-di` delegates to these shared objects.
- Modules using `@Inject` need BOTH `kotlin-inject-runtime` (commonMain) AND `kotlin-inject-compiler` (KSP)
- `multiplatform-settings` is `implementation` in `:data` — not transitive. Add directly to modules using platform-specific Settings classes (e.g. `SharedPreferencesSettings`)

### Compose Stability Configuration

Cross-module types passed into composables are inferred unstable by the Compose compiler unless told otherwise (the compiler can't see implementations across module boundaries). `compose_compiler_config.conf` at the repo root tells the Compose Compiler plugin to treat the following packages/types as **stable** without forcing those modules to depend on `androidx.compose.runtime`:

- `com.chriscartland.batterybutler.presentationmodel.**` — all ScreenState data classes and sealed-interface variants
- `com.chriscartland.batterybutler.domain.model.**` — Device, BatteryEvent, DeviceType, AppVersion, User, etc.
- `kotlinx.collections.immutable.**` — when used
- `kotlin.time.Instant`, `kotlin.time.Duration`
- `kotlinx.datetime.LocalDate`, `LocalDateTime`, `TimeZone`

The file is wired into all 7 Compose-using modules via a `composeCompiler { stabilityConfigurationFiles.add(...) }` block in each module's `build.gradle.kts`.

**Conf-file format gotcha**: the parser used by the current Kotlin Compose Compiler plugin does **not** accept `#` comments. Keep `compose_compiler_config.conf` to plain pattern lines only — a comment causes `Error parsing stability configuration file on line 0`.

**Verifying stability**: temporarily add `metricsDestination` / `reportsDestination` to a module's `composeCompiler { }` block, run `./gradlew :<module>:compileKotlinJvm --rerun-tasks`, and inspect `<module>-composables.txt` for `stable` / `unstable` annotations on parameters. Revert the temp edits when done. (See the agent-memory note on Compose metrics for the full procedure.)

**`List<T>` is still unstable** even when `T` is stable — Kotlin's stdlib `List<T>` interface allows mutability. Switching hot-path collection fields to `kotlinx.collections.immutable.ImmutableList<T>` is a follow-up that would make recomposition more skippable.

### Coroutine Dispatching

Use `DispatcherProvider` (in `:domain:model`) instead of hardcoding `Dispatchers.Default/IO/Main`:
- Inject `DispatcherProvider` into use cases and repositories
- `DefaultDispatcherProvider` (in `:data:provider`) provides real dispatchers
- Tests use `UnconfinedTestDispatcher` for synchronous execution
- kotlinx-coroutines 1.10.2+ provides `Dispatchers.IO` in common code (no expect/actual needed)

### Error Handling

**Project code NEVER throws exceptions except `CancellationException`.**

**`Result<D, E>` is the standard API pattern** for all fallible operations. Try-catch should only appear at external dependency boundaries (Room/SQLite, gRPC, AI engine). Exemplar: `AuthRepository.signIn()` → `Result<User, AuthError>`.

Key types (see `domain/model/Result.kt`):
- `Result<D, E : AppError>` — sealed interface with `Success<D>` and `Error<E>`
- `DataError` — sealed hierarchy: Network, Database, Ai, Unknown (in `DataError.kt`)
- Extension functions: `map`, `flatMap`, `getOrNull`, `getOrElse`, `getOrThrow`, `onSuccess`, `onError`

Pattern for repository implementations (try-catch at boundary):
```kotlin
override suspend fun addDevice(device: Device): Result<Unit, DataError> =
    try {
        localDataSource.addDevice(device)
        Result.Success(Unit)
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) {
        Result.Error(DataError.Database.WriteFailed(message = e.message ?: "Failed"))
    }
```

Pattern for ViewModels (when on Result):
```kotlin
when (val result = addDeviceUseCase(device)) {
    is Result.Success -> { /* success path */ }
    is Result.Error -> _actionError.value = result.error.message
}
```

Use sealed class hierarchies for exhaustive `when` expressions:
```kotlin
// GOOD: Required sealed type - compiler enforces handling all cases
data class Failed(val error: DataError) : SyncStatus

when (status) {
    is SyncStatus.Failed -> when (status.error) {
        is DataError.Network.ConnectionFailed -> // handle
        is DataError.Network.Timeout -> // handle
        // ... compiler error if cases missing
    }
}

// BAD: Optional field - callers can ignore typed error
data class Failed(val message: String, val error: DataError? = null)
```

### Single Responsibility Principle

Classes should have one reason to change. See `docs/architecture/adr-004-single-responsibility-principle.md` for full guidelines and examples.

**Red flags** (consider extracting):
- Identical logic duplicated across 3+ locations
- Class >150 lines with clearly separable concerns
- Mixing CRUD with infrastructure (sync, caching, retry logic)
- Two unrelated groups of tests in one test file

**Extraction checklist:**
1. Identify the seam (minimal interaction points between responsibilities)
2. Create an interface if the new class will be injected
3. Move code to a new class, keeping the same behavior
4. Update DI bindings (`DataComponent`, `AppDataModule`, `NativeComponent`)
5. Split tests into focused test files
6. Run `./gradlew test` to verify no regressions

**Established patterns:** SyncManager extraction (data module), FindOrCreate use cases (usecase module), `sortAndGroup()` utility (viewmodel module), Screen sealed interface separation (compose-app module).

### Experimental Module

The `experimental/` directory is a reference architecture for KMP apps. See `experimental/EXPERIMENTAL.md` for full documentation. Key facts for agents:

- **8 modules**: `domain` ← `usecase` ← `viewmodel` ← `presentation-core` ← `compose-app`, plus `data-local` ← `data` and `ios-app`
- **DI**: kotlin-inject with `ExperimentalAppComponent` root; `ExperimentalDataComponent` (in `data`) + `ExperimentalDataModule` for data bindings
- **Use cases**: `GetCounterUseCase`, `IncrementCounterUseCase`, `RunCounterUseCase`, `ObserveCounterUseCase`, `StartAppScopedCounterUseCase`, `StopAppScopedCounterUseCase`, `ObserveAppScopedCounterRunningUseCase` — all `@Inject`
- **Testing**: `FakeLocalCounterDataSource` in `data-local/src/commonMain/`, `FakeAppScopedCounter` in `data/src/commonMain/`
- **iOS bridge**: `CounterViewModelWrapper` polls KMP `StateFlow` via 60Hz Timer (no SKIE)

### CLI Module (`:cli`)

A JVM-only Kotlin CLI (`cli/src/main/kotlin/.../cli/Main.kt`) for talking to the
Labs REST backend directly, without the app. Depends on `:data-network` and
reuses its wire types (`SyncSnapshotWire`, `SyncPushRequestWire`,
`SyncPushResponseWire`). Subcommands: `get` (fetch the current snapshot), `push`
(send a sync payload). Auth via `--token` / `BB_LABS_ID_TOKEN` env var — get a
live token from the app's Settings screen ("Copy Labs ID Token", Labs mode +
signed in only) since the token is short-lived (~1hr). `--env` selects
staging/prod; `--url` overrides directly. Run with
`./gradlew :cli:run --args="get --env staging --token ..."`.

**`push`'s file argument must already be `SyncPushRequestWire`-shaped JSON** — it is
NOT the same format as the app's own Settings → Export Data backup file (that's
`BackupDto`'s wrapped `{"data": {...}}` shape with ISO date strings; see Data
Export / Import below). Passing an export file to `push` silently no-ops (every
field defaults to empty) rather than erroring — see `bb-cli-backup-import` in
`TODO.md`. Also: writes to `--env prod` require an `editors` scope enforced by the
Labs backend itself (a separate service, not in this repo) — a `HTTP 403 requires
scope 'editors'` means the account needs that granted on the backend's admin side,
not a client-side bug.

### UI Theme Constants

Padding constants live in `presentation-core/.../theme/Padding.kt`. Use `Padding.standard` (16.dp), `Padding.small` (8.dp), `Padding.large` (24.dp), etc. instead of hardcoded dp values. Also `Padding.extraSmall` (4.dp), `Padding.medium` (12.dp), `Padding.extraLarge` (32.dp). PR #1108 swept single-arg `padding(N.dp)` sites; PR #1125 swept multi-arg `padding(horizontal = N.dp, ...)` sites in three feature files (AddDeviceType, EditDeviceType, DeviceDetail). Multi-arg padding in other feature files is a deferred follow-up.

Shape tokens live in `presentation-core/.../theme/Shapes.kt` as `BatteryButlerShapes`, exposed via Material3 `MaterialTheme.shapes.*`:
- `shapes.extraSmall` = `RoundedCornerShape(4.dp)`
- `shapes.small` = `RoundedCornerShape(8.dp)`
- `shapes.medium` = `RoundedCornerShape(12.dp)`
- `shapes.large` = `RoundedCornerShape(16.dp)`
- `shapes.extraLarge` = `RoundedCornerShape(28.dp)`

PR #1125 swept 25 `RoundedCornerShape(N.dp)` literals across 8 files to `MaterialTheme.shapes.*`. Use the tokens for any rounded surface, button, card, or TextField shape. One-off sizes (e.g. 6.dp, 10.dp, 24.dp) that don't map to a token stay as literals.

Icon sizes live in `presentation-core/.../theme/IconSize.kt`.

Custom app colors beyond Material3 live in `ButlerColors` (data class) provided via `LocalButlerColors` composition local. Access with `LocalButlerColors.current.batteryWarning`. New custom colors should be added to `ButlerColors`, provided in `BatteryButlerTheme`, and defined in `Color.kt`.

Icon colors use the **`IconColorRole` enum** (`presentation-core/.../theme/IconColorRole.kt`) with per-category color assignments. `DeviceIconMapper.getIconColorRole(iconName)` maps icons to roles: **Primary** (home/shapes), **Secondary** (tools), **Tertiary** (electronics), **Error** (safety), **Surface** (other). `DeviceIconMapperTest` (in `presentation-core/src/commonTest/`) verifies per-category assignments and ensures all available icons have explicit mappings. `DeviceIconMapper.getResolvedIconAccent(iconName)` returns the resolved `(containerColor, contentColor)` pair. `IconAccent.kt` has been deleted — do not recreate it.

**Standard dropdown component**: Use `ButlerDropdownMenu` (`presentation-core/.../components/ButlerDropdownMenu.kt`) instead of `DropdownMenu` directly. Note: CMP 1.10.0 `DropdownMenu` does NOT support `enter`/`exit` animation params (Jetpack Compose only).

**Loading state with label**: Use `LoadingWithLabel` (`presentation-core/.../components/LoadingWithLabel.kt`) for top-level "screen is loading" states — a centered `CircularProgressIndicator` plus a status `Text` underneath. Pair with a screen-specific status string (`status_loading_devices`, `status_loading_device_types`, etc.) so the label tells the user *what* is loading. Bare `CircularProgressIndicator` is reserved for inline cases (e.g. the sync overlay in `HomeScreenContent`).

**Material icons — prefer AutoMirrored**: For glyphs that have an `Icons.AutoMirrored.Filled.*` variant (`Notes`, `OpenInNew`, `Logout`, `List`, etc.), use the AutoMirrored variant. They flip in RTL locales, which is the recommended behavior; the non-AutoMirrored versions emit deprecation warnings.

### Form Validation UX

For forms with required fields (AddDeviceContent, AddDeviceTypeContent, EditDeviceContent — set in PR #1135), the convention is:

1. `var hasAttemptedSubmit by rememberSaveable { mutableStateOf(false) }` owned by the screen-level composable.
2. Per required-field error string starts as `null` and turns to a `composeStringResource(Res.string.form_error_X_required)` once `hasAttemptedSubmit == true` AND the field is empty. Errors clear live as the user types.
3. Save action **stays enabled** (`enabled = !isLoading`). The onClick sets `hasAttemptedSubmit = true` and only proceeds with `onSave(...)` if all fields are valid. Clicking a disabled button gives no feedback about *why* — keeping it enabled and surfacing the error is the more informative pattern.
4. Pass the error string to `OutlinedTextField` via `isError = nameError != null` + `supportingText = nameError?.let { msg -> { Text(msg) } }`.

String resources for form errors: `form_error_device_name_required`, `form_error_device_type_required`, `form_error_battery_type_required`, `form_error_type_name_required` (extend as new required fields are added).

### Retry With Fresh Subscription (ViewModel pattern)

`retryableStateIn(...)` in `viewmodel/.../ViewModelExtensions.kt` (added in PR #1136) wraps a source flow factory in `retryTrigger.flatMapLatest { source().catch { onError } }`. Emitting a new value to the trigger cancels the prior inner subscription (including any errored `.catch`) and starts fresh. The `.catch` is INSIDE the `flatMapLatest` so it re-arms on every retry — outside, the downstream chain would be permanently dead after the first error.

ViewModels that surface a user-visible retry button (HomeViewModel, DeviceTypeListViewModel, HistoryListViewModel — set in PR #1136) declare:
```kotlin
private val retryTrigger = MutableStateFlow(0)
fun retry() { retryTrigger.update { it + 1 } }

val uiState = retryableStateIn(
    scope = viewModelScope,
    retryTrigger = retryTrigger,
    started = defaultWhileSubscribed(),
    initialValue = ...,
    onError = { ScreenState.Error(it.message ...) },
    source = { combine(...) { ... } },
)
```

Other ViewModels keep the older `combine(...).safeStateIn(...)` form. Switch a ViewModel to `retryableStateIn` only when the screen exposes a Retry button.

Content composables that pair with these ViewModels take an `onRetry: () -> Unit` parameter and render `Button(onClick = onRetry) { Text(Res.string.action_try_again) }` as the `action` slot of the error `EmptyStateContent`. Wiring happens in `MainTabsScreens.kt` (`onRetry = { viewModel.retry() }`).

**Gotcha: `initialValue = ScreenState()` uses the State class's OWN constructor defaults, not the ViewModel's internal flow defaults.** `retryableStateIn`'s `initialValue` (and `safeStateIn`'s) is shown before the real combined flow first emits. If a ViewModel changes an internal `MutableStateFlow`'s default (e.g. `HomeViewModel`'s `sortOptionFlow`/`isSortAscendingFlow` defaults, PR #1326), the `HomeScreenState()` placeholder's own constructor defaults must be updated too — otherwise there's a brief flash of the old default before real data loads, and a naive `viewModel.uiState.first()` in a test will silently observe the stale placeholder default instead of the new one (this bit the `` `toggleSortDirection inverts sort direction` `` test in PR #1326 — it passed before the sort-default change only because both defaults happened to agree).

### Per-Data-Mode State (`DataModeKeyedState<T>`)

`domain/model/DataModeKeyedState.kt` holds a separate value per `DataMode`
(keyed by a caller-supplied `keyFor` function) instead of one shared
`MutableStateFlow`. It exists to prevent a whole bug category: state from one
Labs environment (e.g. staging) silently leaking into another (e.g. prod) when
the user switches `DataMode` — discovered as a real bug in `DefaultLabsAuthRepository`,
where a single `_labsAuthState` meant signing in to staging could appear
"already authenticated" after switching to prod.

```kotlin
val authStateByMode = DataModeKeyedState<AuthState>(
    dataMode = dataModeRepository.dataMode,
    keyFor = { apiKeyForMode(it, labsFirebaseApiKey) },
    default = AuthState.Unauthenticated,
)
val labsAuthState: Flow<AuthState> = authStateByMode.current
// mutate with authStateByMode.setCurrent(...) / .updateCurrent { ... }
```

`current` is a `Flow<T>` (not `StateFlow`) — it re-derives via
`dataMode.map(keyFor).distinctUntilChanged().flatMapLatest { stateFor(it) }`,
so switching modes always reads that mode's own value, defaulting fresh if it's
never been set. Note the explicit type argument (`DataModeKeyedState<AuthState>`)
is required — passing a concrete subtype as `default` (e.g. `AuthState.Unauthenticated`)
without it lets `T` infer to the narrow subtype instead of the sealed class.
See `domain/src/commonTest/.../DataModeKeyedStateTest.kt` for the exact
switch-and-leak scenario this guards against. **Reach for this instead of a bare
`MutableStateFlow` for any new per-environment (Labs staging/prod) state.**

### Settings Data Mode Picker & `FeatureFlag`-Gated Options

`SettingsViewModel.availableDataModes` shows **Device only / Production /
Staging / Mock** (`DataMode.None` / `LabsProd` / `LabsStaging` / `Mock`) by
default, in that order. The legacy own-backend modes (`GrpcLocal`, `GrpcAws`,
`GrpcDev` — AWS infrastructure is hibernated) are appended only when
`featureFlagProvider.isEnabled(FeatureFlag.LEGACY_DATA_MODES)` is true; that
flag is not added to the `enabledFeatures` set in either `AppComponent.kt`
(Android/Desktop) or `NativeComponent.kt` (iOS), so it's disabled by default
everywhere. This is the established pattern for hiding a build-time-disabled
option from a picker without deleting the code path: add a `FeatureFlag` enum
entry, gate the option's inclusion in the list with
`featureFlagProvider.isEnabled(...)`, and simply don't add it to either
component's `enabledFeatures` set. See `domain/model/FeatureFlag.kt` /
`domain/repository/FeatureFlagProvider.kt` for the underlying mechanism.

### Persisted Local-Database Isolation (`DatabaseOption`)

`data-local/.../room/DatabaseOption.kt` is the on-disk counterpart to
`DataModeKeyedState<T>` above — same "don't let one environment's state leak
into another's" goal, but for the actual Room SQLite file instead of an
in-memory flag. `DynamicDatabaseProvider` opens a distinct SQLite file per
`DatabaseOption.fromDataMode(mode)` and swaps atomically when the mode
changes (this per-subtype isolation existed since PR #1052).

**Fixed in PR #1324**: `fromDataMode` used to match on the `DataMode`
sealed subtype only, discarding the `url` payload `GrpcLocal`/`GrpcAws`/`GrpcDev`/
`LabsStaging`/`LabsProd` all carry — so two different real backends sharing a
subtype but a different url (reachable via the `ServerUrlReceiver` adb debug
tool) would silently share one file. `DatabaseOption` is now a data class keyed
on `(category: DatabaseCategory, fileName: String)`, where `fileName` gets a
hash-of-normalized-url suffix whenever the mode carries a real url; a null/blank
url still resolves to the bare per-category file name (`DatabaseOption.baseFileNames`),
so existing installs aren't affected in the common case (fixed, build-configured
urls). A one-time migration, `promoteBareFileIfNeeded` (wired into each
platform's `DatabaseFactory.createNewDatabase`, called before Room opens the
file), renames an existing bare file to the new url-suffixed name the first time
a url-bearing mode opens it — so an existing user's data is inherited, not
orphaned or duplicated. See `DatabaseOptionTest.kt` (commonTest, pure key
derivation) and `DatabaseFilePromotionTest.kt` (jvmTest, the actual
promotion/isolation behavior with real file I/O) for the exact guarantees.
**Reach for this pattern (identity-aware file keying + rename-forward migration)
for any future per-environment persisted state** — don't key local storage on a
`DataMode` subtype alone if the subtype can carry a variable url.

### Clear Local Data On Sign-Out (`SignOutLabsUseCase`/`SignInToLabsUseCase`)

File-level isolation (above) guarantees different Labs environments' caches
never mix, but it doesn't address a separate, real gap: `HomeViewModel`/Devices
tab reads local Room data with **zero awareness of Labs auth state** (deliberate
offline-first design), and nothing ever cleared local data on sign-out — so a
previously-synced environment's devices stayed visible indefinitely after
signing out (`bb-labs-signout-clear` in TODO.md, fixed 2026-07-06).

`DeviceRepository.clearAllLocalData()` (→ `LocalDataSource.clearAll()` → three
new `DELETE FROM ...` DAO queries) is composed into sign-in/out via two
UseCases rather than inside `DefaultLabsAuthRepository` itself:

```kotlin
class SignOutLabsUseCase(labsAuthRepository: LabsAuthRepository, deviceRepository: DeviceRepository) {
    suspend operator fun invoke() {
        labsAuthRepository.signOutLabs()
        deviceRepository.clearAllLocalData()
    }
}
class SignInToLabsUseCase(labsAuthRepository: LabsAuthRepository, deviceRepository: DeviceRepository) {
    suspend operator fun invoke(): Result<User, AuthError> {
        val result = labsAuthRepository.signInToLabs()
        if (result is Result.Success) deviceRepository.resync()
        return result
    }
}
```

**Why a UseCase, not inside the repository**: `DefaultLabsAuthRepository` takes
an unfakeable platform `expect class` (`GoogleSignInBridge`) and is explicitly
exempted from direct unit testing (`test-coverage-exemptions.txt`). Composing at
the usecase layer — depending only on the two plain repository interfaces —
keeps this fully unit-testable with `FakeLabsAuthRepository`/`FakeDeviceRepository`,
and matches the existing pattern of usecases composing multiple repositories
into one workflow (e.g. `BuildAiContextUseCase`). `LoginViewModel`/`SettingsViewModel`
call these UseCases instead of the repository's sign-in/out methods directly —
**any new Labs sign-in/out call site should go through these UseCases, not the
repository directly**, or it'll silently skip the clear/resync guarantee.

`SignInToLabsUseCase`'s `resync()` reuses the same bounded (15s timeout)
mechanism pull-to-refresh uses; on a cold Labs backend this can time out before
data arrives (falls back to the ambient background sync loop, which still
recovers — not a correctness bug, see `bb-labs-cold-resync` in TODO.md).

Deferred: the app's own gRPC `AuthRepository.signOut()` has the identical gap
(one global account across `GrpcLocal`/`GrpcAws`/`GrpcDev`, vs. Labs's
per-environment sessions) — not fixed, since "which mode's data to clear" is
less unambiguous there.

### Persisted "Believed Signed In To Labs" (`LabsSessionStorage`)

Follow-up to the sign-out fix above (`bb-labs-persist-signin-belief` in
TODO.md, fixed 2026-07-07). `labsAuthState` is otherwise in-memory only and
always resets to `Unauthenticated` on process launch — since Android kills
backgrounded processes fairly often, a genuinely-signed-in user could see
"Sign in to Labs" at the same moment the Devices/Types/History tabs still
showed their real cached data, after nothing more than a process restart. A
naive "clear local data whenever `Unauthenticated` is observed" fix was
rejected: every cold start starts `Unauthenticated`, so that would wipe the
cache on every restart and defeat offline-first caching.

Mirrors the pattern `DefaultAuthRepository` (the app's own gRPC auth) already
used for this identical problem: `authStateByMode`'s per-key default changed
from `AuthState.Unauthenticated` to `AuthState.Unknown` (a state the UI
already treats as "loading" — see `LoginContent.kt`), and a `DefaultLabsAuthRepository`
`init` block resolves each key's `Unknown` from `LabsSessionStorage` /
`DataStoreLabsSessionStorage` (`data-local/.../auth/`) — a lightweight,
per-Labs-environment (staging/prod, keyed the same way as the existing
in-memory `DataModeKeyedState`, via `apiKeyForMode`) persisted flag storing
only profile info (`User`: id/email/displayName/photoUrl), **no tokens, no
expiry tracking**. The resolve is guarded by a new generic
`DataModeKeyedState.compareAndSet(key, expected, newValue)` so it can never
clobber a real sign-in/out transition that already happened for that key.
`signInToLabs()`'s success path saves the belief; `signOutLabs()` (already
invoked through `SignOutLabsUseCase`) clears it alongside the local data
clear — no usecase-layer changes needed, this is entirely internal to the
repository.

**Deliberately does not re-validate a real session** by itself (unlike
`DefaultAuthRepository`'s `scheduleTokenExpiry`) — this fix alone leaves the
real Labs session/token in `LabsAuthGateway` unrestored. That gap is closed
opportunistically by the next section, `LabsSessionStorage` itself still only
tracks the lightweight UI-facing belief, no tokens, no expiry.

### Opportunistic Silent Labs Re-Auth (`GoogleSignInBridge.signInSilentlyWithClient`)

Follow-up to the section above (`bb-labs-silent-reauth` in TODO.md, fixed
2026-07-07). The persisted-belief fix alone left a real gap: the UI shows
"Authenticated" after a restart, but `LabsAuthGateway`'s actual session
(process-lifetime, in-memory) is never re-established — so background sync
could keep failing silently, with the explicit sign-in affordance now hidden
since the UI believes it's already signed in.

`GoogleSignInBridge` (`data-network/.../auth/`, `expect class`) gained
`signInSilentlyWithClient(clientId, clientSecret)`: like `signInWithClient`
but never shows UI. **Android** actual reuses `performSignIn`'s existing
Credential Manager call with `setFilterByAuthorizedAccounts(true)` instead of
`false` — succeeds only if the account was previously authorized, fails
cleanly (`NoCredentialException`, logged at info not warning level, since
"not yet authorized" is an expected outcome here) otherwise. **iOS/Desktop**
actuals always return `Result.Error` immediately — neither bridge uses a
native Sign-In SDK (both are hand-rolled interactive OAuth flows: iOS
`ASWebAuthenticationSession`, Desktop browser-loopback PKCE), so there's no
persisted session to silently check without opening a browser/sheet.

`DefaultLabsAuthRepository`'s `init` block calls a new `attemptSilentReauth()`
right after resolving `Unknown` from the persisted belief — guarded by
`DataModeKeyedState.compareAndSet` now returning `Boolean` (true only when
it actually performed the swap), so the silent attempt fires exactly once per
belief-resolution and never redundantly re-fires on a subsequent *explicit*
sign-in (which already has a real session). On success, completes the same
`labsAuthGateway.signInToLabsWithGoogle` exchange `exchangeForLabsSession`
uses. **Best-effort by design**: failure touches neither `authStateByMode`
nor `labsSessionStorage` — the UI keeps showing the believed-signed-in state
regardless, matching the exact tolerance the persisted-belief fix already
established ("I don't actually care about expired auth"). Verified on a real
emulator: sign in, force-stop, relaunch → logcat shows `"Silent Labs re-auth
succeeded; real session re-established"` with zero UI/dialog shown on that
particular single-account run.

**Correction (`bb-silent-reauth-cooldown`, 2026-07-11)**: "zero UI/dialog
shown" above is **not a guarantee** — it was one emulator observation, not a
property of the underlying API. On Android, `signInSilentlyWithClient` uses
the *same* Credential Manager `getCredential()` call as the interactive
picker; `filterByAuthorizedAccounts(true)` narrows candidates, it doesn't
suppress UI. With multiple on-device Google accounts, or Play Services
deciding a credential needs re-confirmation, this call can still show a
chooser/bottom-sheet — and because this repository is a DI singleton
recreated fresh (re-running `attemptSilentReauth()`) on every process
restart, a user who frequently has the app killed and reopened saw that OS
dialog frequently. Fixed by throttling attempts with a 6h cooldown
(`SilentReauthCooldown`, persisted per-environment via
`LabsSessionStorage.getLastSilentReauthAttemptMs`/`recordSilentReauthAttempt`)
— see the `bb-silent-reauth-cooldown` entry in TODO.md's Done section for the
full writeup, including the analogous `DefaultAuthRepository` fix below.

**Deferred**: iOS/Desktop still have no silent-restore path — would require
adopting a native Sign-In SDK or building refresh-token persistence for their
existing PKCE flows, a materially bigger feature than this fix. Those
platforms rely solely on the persisted-belief UI fix; an explicit re-sign-in
is still required there to restore a real session after a restart.

**Superseded (`bb-labs-refresh-token-persistence`, 2026-07-18)**: the 6h
cooldown above only *throttled* the UI-risky `signInSilentlyWithClient` call —
it never removed the risk, and a normal "open in the morning, return in the
evening" gap exceeds 6h, so users still saw the dialog. `attemptSilentReauth`
(Credential-Manager-based) was replaced entirely by
`DefaultLabsAuthRepository.restoreLabsSession`, which calls the new
`LabsAuthGateway.restoreSession()` — a plain, non-interactive
`securetoken.googleapis.com` REST call against a *persisted* Labs Firebase
refresh token (new `domain.repository.LabsRefreshTokenPersistence` port,
`data-local`'s `DataStoreLabsRefreshTokenPersistence` impl). This carries no
OS-UI risk at all, so it needs no cooldown and runs on every process start.
Only an authoritative rejection (`AuthError.Token.Invalid`, HTTP 4xx from the
refresh call — the token was actually revoked) clears the belief and signs
the user out; a transient failure (no persisted token, network unreachable)
is left alone exactly like the old best-effort design. `SilentReauthCooldown`
and its `LabsSessionStorage` timestamp fields were deleted as dead code. This
also resolves the iOS/Desktop deferral immediately above: `FirebaseIdToken
Provider` is `commonMain` and unrelated to `GoogleSignInBridge`'s
platform-specific silent methods, so persisting its refresh token benefits
all three platforms — iOS/Desktop no longer need a full interactive
OAuth sheet/browser after every process restart for Labs mode. See
`bb-labs-refresh-token-persistence` in TODO.md's Done section for the full
writeup, including why the refresh token is deliberately held client-side
(standard for native/mobile OAuth, matching Firebase's own SDK) rather than
server-mediated.

### Own-Backend Silent Session Refresh (`GoogleSignInBridge.signInSilently`)

`bb-auth-session-length` in TODO.md, fixed 2026-07-08. Distinct from the Labs
sections above: this is the app's *own* gRPC-backend account
(`DefaultAuthRepository`), whose session token isn't itself refreshable
(`refreshToken()` is a stub) and expired after 24h (server-verified) or a
hardcoded 1h (`LOCAL_TOKEN_EXPIRY_MS`, local-only fallback when the server was
unreachable at sign-in) — previously always forcing the fully interactive
account picker (`GoogleSignInBridge.signIn()`) to get back in.

`GoogleSignInBridge` gained `signInSilently()` — same shape as
`signInSilentlyWithClient` but uses the bridge's already-`initialize()`d
default client (the own-backend account has no per-environment client the way
Labs staging/prod do, so no client-id parameter is needed). Android reuses
`performSignIn(clientId, filterByAuthorizedAccounts = true)`; iOS/Desktop
always return `Result.Error` (identical reasoning to
`signInSilentlyWithClient`).

`DefaultAuthRepository.scheduleTokenExpiry` no longer clears the token
directly when its timer fires (or immediately on a cold start with an
already-expired stored token) — both paths now call a new
`attemptSilentRefresh()`. On success it calls the existing `verifyWithServer`
exactly like an explicit sign-in, which re-arms the next
`scheduleTokenExpiry` itself (self-perpetuating: as long as Credential
Manager keeps confirming the account, the session renews indefinitely with
zero prompts). On failure it falls back to the original clear-token +
`Unauthenticated` behavior — no regression for iOS/Desktop or a device with
no cached account. `StoredAuthToken.toAuthState()` no longer checks expiry
(always `Authenticated`) so a cold start with an expired token shows the
believed-signed-in user immediately instead of a signed-out flash while the
silent refresh runs — mirroring the Labs persisted-belief UX exactly.

**Follow-up (`bb-silent-reauth-cooldown`, 2026-07-11)**: a background
`attemptSilentRefresh()` that reached `verifyWithServer` but got rejected by
the *server* (not the local Credential Manager step) used to unconditionally
drop `_authState` to `AuthState.Failed`, which `LoginContent.kt` auto-shows as
an `ErrorDialog` with "Try Again" wired to the interactive `signIn()` — i.e. a
background attempt could leave an unprompted dialog waiting the next time the
user opened the app. `verifyWithServer` now takes `isExplicitAttempt`; a
background-triggered server rejection falls back to `AuthState.Unauthenticated`
quietly instead, matching how a failed `signInSilently()` call was already
handled. This path is gated behind the disabled-by-default `LEGACY_DATA_MODES`
flag, so lower real-world impact than the Labs fix above, but the same class
of bug. See TODO.md's `bb-silent-reauth-cooldown` Done entry.

### KMP-Friendly Class Naming Convention

Enforced by `checkNamingConventions` Gradle task (in `validate.sh` and CI):

- **No "Ui" in class names** (`no-ui-in-class-name`): Collides with iOS UIKit (`UIView`, `UIColor`). Use `ScreenState` for screen states, or drop "Ui" for models. Example: `HomeScreenState` (not `HomeUiState`), `ChatMessage` (not `ChatUiMessage`).
- **No "View" in class names** (`no-view-in-class-name`): Ambiguous between Android View system and Compose. Exception: `ViewModel` is allowed (standard Jetpack term).
- **Inline suppression**: Add `// @NamingExempt: <reason>` on the declaration line if a name must break convention.

### String Resource Convention

Enforced by two parallel mechanisms:
1. **`checkHardcodedStrings` Gradle task** (regex-based, in `validate.sh` and CI)
2. **Custom Detekt rules** in `detekt-rules/` (AST-level, runs with `detekt`/`detektAndroidMain`)

Both enforce the same rules — the Detekt rules provide IDE integration and AST-level precision:

- All user-visible strings in Compose UI must use `composeStringResource(Res.string.xxx)` instead of hardcoded string literals.
- **`no-hardcoded-text` / `HardcodedComposeText`**: `Text("literal")` and `Text(text = "literal")` are forbidden — use `Text(composeStringResource(Res.string.xxx))`.
- **`no-hardcoded-content-description` / `HardcodedContentDescription`**: `contentDescription = "literal"` is forbidden — use `contentDescription = composeStringResource(Res.string.xxx)`.
- String resources live in `compose-resources/src/commonMain/composeResources/values/strings.xml`.
- **Inline suppression**: Add `// @StringResourceExempt` on the line if a hardcoded string is intentional.
- `@Preview` functions are automatically exempt (no suppression needed).
- The `experimental/` directory is exempt (simple demo apps).

### Data View/Edit Pattern

All data types follow a consistent **List → Detail (read-only) → Edit** architecture:

| Data Type | List Screen | Detail Screen | Edit Screen |
|-----------|------------|---------------|-------------|
| Devices | `Screen.Devices` | `Screen.DeviceDetail` | `Screen.EditDevice` |
| Device Types | `Screen.Types` | `Screen.DeviceTypeDetail` | `Screen.EditDeviceType` |
| History | `Screen.History` | `Screen.EventDetail` | `Screen.EditBatteryEvent` |

**Pattern**: List click → read-only detail (with "Edit" button in top bar) → edit form (with "Cancel"/"Save" in top bar + "Delete" button at bottom). Delete pops two screens (edit + stale detail) back to the list.

**File layers per data type**: UiState (`presentation-model`), ViewModel + Factory (`viewmodel`), Content composable (`presentation-feature`), Screen wrapper (`compose-app`).

### UI Architecture Mapping

For a detailed breakdown of how the shared Compose Multiplatform UI maps to the native SwiftUI implementation (and why they intuitively differ structurally), see `docs/UI_SCREENS_MAPPING.md`.

When implementing a new Compose UI feature, check if a corresponding iOS SwiftUI implementation is needed. If so, add a task to `TODO.md` for the iOS gap and reference it in `docs/UI_SCREENS_MAPPING.md`.

### Data Export / Import

Exporting and importing data uses a versioned JSON envelope. Key pieces:

- **`BackupDto`** (`usecase/.../BackupDto.kt`) — shared DTOs for `Device`, `DeviceType`, `BatteryEvent`. Format version is stored at the envelope level so future readers can branch on it.
- **`ExportDataUseCase`** — serializes the current local state into the envelope. Called from `SettingsViewModel.onExportData()`.
- **`ImportDataUseCase`** — envelope-first version parsing; returns `Result<ImportResult, DataError>`. Skips malformed records with reporting (does not abort the whole import on one bad row). Tested with 16 cases in `ImportDataUseCaseTest` (happy path, format versioning, malformed JSON/dates, idempotency, upsert).
- **`FileLoader`** (`presentation-core/.../util/FileLoader.kt`) — `expect/actual` abstraction for reading a file. Android uses `ContentResolver` via `AndroidFileLoader`; iOS uses `NSString.create(contentsOfURL:)` (NSData K/N interop has issues — see `IosFileLoader`); Desktop uses `java.io.File`.
- **`LocalFileLoader`** — CompositionLocal that provides the `FileLoader` instance. Add to `detekt.yml` `CompositionLocalAllowlist`.
- **iOS K/N gotcha**: do not use `String(bytes: ByteArray)` — only `String(chars: CharArray)` exists on K/N, and even then it's deprecated. Use `byteArray.decodeToString()` (UTF-8) for cross-platform safety.

### Secure Clipboard

For copying sensitive values (e.g. the Labs ID token in Settings → Advanced), use `SecureClipboard` (`presentation-core/.../util/SecureClipboard.kt`), an `expect/actual`-style abstraction exposed via the `LocalSecureClipboard` CompositionLocal (added to `detekt.yml` `CompositionLocalAllowlist`) — do not write sensitive values with the plain platform clipboard APIs directly. Platform behavior:
- **Android** (`AndroidSecureClipboard.kt`): sets `ClipDescription.EXTRA_IS_SENSITIVE` via `PersistableBundle`, guarded by `Build.VERSION.SDK_INT >= TIRAMISU` (API 33+; masks the value in clipboard-history UIs on supporting OEMs/launchers).
- **iOS** (`IosSecureClipboard.kt`): `UIPasteboard.generalPasteboard.setItems(...)` with `UIPasteboardOptionLocalOnly` (not synced via Handoff/iCloud) and a 60-second `UIPasteboardOptionExpirationDate`.
- **Desktop** (`DesktopSecureClipboard.kt`): plain `java.awt.datatransfer` copy — no OS-level secure-clipboard equivalent exists on the JVM.

### Android Splash Screen

The app uses `androidx.core:core-splashscreen` (added in PR #1109) for cold-start:

- `compose-app/src/androidMain/res/values/themes.xml` defines `Theme.BatteryButler.Starting` (parent `Theme.SplashScreen`) with `windowSplashScreenAnimatedIcon = @mipmap/ic_launcher_round` and `postSplashScreenTheme = @android:style/Theme.Material.Light.NoActionBar`.
- `AndroidManifest.xml` sets `android:theme="@style/Theme.BatteryButler.Starting"` on the application.
- `MainActivity.onCreate()` calls `installSplashScreen()` **before** `enableEdgeToEdge()` and `super.onCreate()`.
- `compose-app/src/androidMain/res/values/colors.xml` defines `splash_background` (currently `#FFFFFF`; could be promoted to a Material role later).

### AI Architecture

AI messages are augmented in `SendChatMessageUseCase` before reaching the AI engine:
1. **Time context**: Current date/time/timezone prepended via `buildTimeContext()`
2. **User context**: Device inventory, types, and recent events via `BuildAiContextUseCase`
3. The augmented message is sent to `AiEngine.generateResponse()`

The AI system instruction (in `AndroidAiEngine`) is immutable after model creation (Gemini API constraint), so dynamic context is prepended to user messages rather than modifying the system instruction.

**AI Tools (DeviceToolHandler):**
- 9 tools total: `addDevice`, `addDeviceType`, `recordBatteryReplacement`, `updateDevice`, `deleteDevice`, `updateDeviceType`, `deleteDeviceType`, `updateBatteryEvent`, `deleteBatteryEvent`
- Tool names and params are constants in `domain/.../ai/AiTools.kt` (`AiToolNames`, `AiToolParams`)
- `BuildAiContextUseCase` includes entity IDs as `[id:...]` prefixes so the AI can target specific items
- Delete tools execute immediately; confirmation is enforced via system prompt instruction (not code guard)
- `deleteDeviceType` has referential integrity: blocks if devices still reference the type
- Event update/delete recalculates device `batteryLastReplaced` via `UpdateDeviceLastReplacedUseCase`
- Gemini may send numeric params as String or Number — `parseIntParam()` handles both

### Navigation Architecture (Unified Back Stack)

The app uses a **single unified back stack** (`backStack` in `App.kt`) with one `NavDisplay`. All screens (tabs, login, detail, edit, settings) share the same stack. Shell chrome (top bar, bottom nav, AI overlay) is conditionally shown based on whether the top screen is a tab screen.

Key design:
- **Initial state**: `[Devices, Login]` — Login on top (full-screen, no chrome). After login, Login is removed, revealing Devices with shell.
- **`Screen.isTabScreen`** extension property identifies Devices/Types/History as tab screens.
- **`MainScreenShell.showChrome`** parameter: when `false`, hides top bar, bottom nav, AI input/overlay, and predictive back handler. Sets `contentWindowInsets = WindowInsets(0,0,0,0)` so detail screens handle their own insets.
- **Tab switching** clears the entire stack and rebuilds with the tab hierarchy (e.g., Types = `[Devices, Types]`).
- **`navigateTo`** does type-based dedup: same `::class` replaces top entry (e.g., `DeviceDetail("a")` → `DeviceDetail("b")`).
- **`isTabTransition`** flag (set `true` by tab navigation, `false` by detail navigation) controls directional animations in `transitionSpec`.
- **NavDisplay `transitionSpec` gotcha**: `initialState`/`targetState` in the lambda are wrapper types (not raw `Screen`), so extension properties like `isTabScreen` can't be called on them. Use external flags instead.

### AI Chat UI Architecture

The AI chat uses a **split-screen layout** — chat and tab content share the screen in a `Column`, not z-stacked. Key design:
- **`AiChatViewModel`** is hoisted to App scope so state survives tab navigation.
- **`isAiExpanded`** and **`tabTransitionForward`** state live in `App.kt` (both `rememberSaveable`). All detail navigation sets `isAiExpanded = false` before pushing. `MainScreenShell` also collapses via predictive back gesture or collapse button.
- **`MainScreenShell`** bottom bar (`presentation-feature/main/MainScreen.kt`) owns the always-visible AI input (`OutlinedTextField` + send `IconButton`) wrapped in `Surface` with `imePadding()` on the `Scaffold`. Tapping send expands the chat panel and routes through `onSendAiMessage`.
- **Split-screen layout**: `MainScreenShell` uses a `Column` with `content` in a `Box(Modifier.weight(1f))` and the chat panel below it. Content shrinks as the chat expands. This replaced the previous z-stacked overlay approach (PR #831).
- **Chat height animation**: `Animatable<Float>` (0f = hidden, 1f = full target height) drives chat panel height. Target is 50% of available height normally, 100% when IME is visible. `PredictiveBackHandler` (expect/actual in `presentation-core/.../components/PredictiveBackHandler.kt`) is composed after `content()` for higher priority than NavDisplay's handler. On Android, the back gesture smoothly tracks finger position; on desktop/iOS, the handler is a no-op.
- **Scroll anchoring**: `AiTabContent` uses `snapshotFlow { listState.layoutInfo.viewportSize.height }` to keep chat anchored at the bottom when the viewport resizes (chat panel appearing/resizing, IME toggling).
- **`AiTabContent`** (`presentation-feature/aichat/AiTabContent.kt`) has a `showInput: Boolean = true` param. The split-screen chat panel passes `showInput = false` so only the chat history appears.
- **AI messages** include an `[Active tab: <name>]` context prefix so the AI knows which screen the user is viewing.
- **Tab transitions** are directional: `tabTransitionForward` is set before each backstack mutation based on tab index. `NavDisplay.transitionSpec` reads it to slide left or right.
- **`MainTab.AI`** was removed (PR #952) — the AI is a split-screen panel, not a nav tab. The `MainTab` enum now only contains `DEVICES`, `TYPES`, `HISTORY`.
- **Predictive back (Android 13+)**: Opted in via `android:enableOnBackInvokedCallback="true"` in `AndroidManifest.xml`. AI chat collapse uses `PredictiveBackHandler` for gesture-tracked animation. Back on Login is a no-op (prevents revealing unauthenticated tabs). Full back gesture contract documented in `docs/TESTING.md`.

## Common Commands

```bash
# Format code
./scripts/spotless-apply.sh

# Full validation (matches CI)
./scripts/validate.sh

# Build platforms
./gradlew :compose-app:assembleDebug          # Android
./gradlew :compose-app:desktopJar             # Desktop
./gradlew :server:app:build                   # Server
xcodebuild -project ios-app-swift-ui/...      # iOS
ruby ios-app-swift-ui/sync_pbxproj.rb         # Sync Swift files to Xcode
```

## Build System

- **Bazel disk cache issue**: When running `bazel build` in scripts called from Xcode, use `--disk_cache=""` to ensure outputs are materialized locally. The disk cache can return metadata without creating actual files.

## Releases

**NEVER push git tags manually. Always use the release scripts.**

```bash
# Android release
./scripts/release-android.sh

# Server release
./scripts/release-server.sh

# Promote server dev → prod
./scripts/promote-server.sh
```

Release scripts check for existing tags, increment correctly, provide confirmation prompts, and ensure you're on the right commit.

## Task Management

### Two Task Systems

- **`TODO.md`** (repo root) — Cross-session project tracking. Plain markdown,
  committed to git. Use for epics, bugs, features that span multiple sessions.
- **Claude's TaskCreate/TaskList** — Within-session team coordination. Ephemeral. Use for breaking work into subtasks during a team session.

**Rules:**
- Teammates in a Claude Code team use TaskCreate/TaskList for coordination (never `TODO.md`)
- Read `TODO.md` at session start and update it at session end for project-level items
- Don't duplicate: if it's a single-session task, it doesn't need a `TODO.md` entry

### `TODO.md` Conventions

`TODO.md` is a plain markdown file — edit it directly, no special tooling.

- Tasks are grouped under `## P2` / `## P3` / `## P4` priority headings, each task
  a `### bb-xxxx — <title>` subsection. The `bb-xxxx` IDs are stable anchors that
  other docs/workflow comments cross-reference — preserve them; mint new ones as
  needed (any short unique slug is fine).
- Add a new task under the right priority heading with a self-contained description.
- When a task is done, move it to the `## Done` section with a one-line outcome, or
  delete it. Commit the change like any other doc edit.

## Claude Code Hooks

Pre-tool-use hooks in `.claude/hooks/` enforce guardrails on agent Bash commands:

- **`block-admin-bypass.sh`** — Mode-aware: blocks `gh --admin` in release mode, warns in development mode (reads `.github/ci-mode.txt`).
- **`git-guardrails.sh`** — 8 guardrails:
  1. Warn on `git push` without prior `./scripts/validate.sh` (checks `.claude/.validation-passed` marker)
  2. Block push to main/master
  3. Block force push (`--force`, `-f`, `--force-with-lease`)
  4. Enforce `--squash` on `gh pr merge`
  5. Block tag creation/modification (only `git tag -l`/`--list` allowed)
  6. Block destructive commands (`reset --hard`, `clean -f`, `checkout .`, `restore .`)
  7. Block `git -C` (run git from repo root instead)
  8. Warn on shell control flow keywords (`for`, `while`, `if`, etc.) — `&&`/`||`/`;`/`|` are allowed

Hooks strip heredoc bodies and quoted strings before matching to avoid false positives on prose in commit messages or PR bodies.

Registered in `.claude/settings.json` under `hooks.PreToolUse`.

## Efficiency Rules

- **NEVER use `sleep` commands** - Don't wait for CI. Find productive work instead.
- **Always iterate locally** - Run local validation while CI runs remotely.
- **Check CI status without waiting** - Use `gh pr view` or `gh run list` without `--watch`.
- **Work in parallel** - While one PR's CI runs, work on other tasks from `TODO.md`.

## File Index

| File | Contains | Read when |
|------|----------|-----------|
| `project.md` (this file) | Architecture, modules, DI, error handling, UI patterns | Always (entry point) |
| `AGENTS.md` | Safety rules, git workflow, core processes | Always (entry point) |
| `testing.md` | Test types, screenshot tests, convention tests, E2E | Writing/running tests |
| `ci.md` | CI modes, path filtering, auto-generate, concurrency | Modifying CI or debugging failures |
| `ios.md` | SwiftUI architecture, design system, xcodebuild, snapshots | iOS work |
| `server.md` | Deployment, URLs, secrets, Terraform, E2E auth | Server work |
| `merge-strategy.md` | PR merge workflow, batch merging, integration branches | Merging PRs |
| `workflows/` | Step-by-step playbooks (29 files) | Specific operations |
