# iOS Architecture & Design

SwiftUI architecture, design system, xcodebuild patterns, and snapshot testing.

> **Parent doc:** See `project.md` for shared architecture and `AGENTS.md` for workflow rules.

## iOS SwiftUI Architecture

The iOS SwiftUI app follows a **two-layer pattern**:
- **Screen** (e.g., `AddDeviceScreen`): Owns `@State` form vars + the KMP ViewModel via `@StateViewModel`, composes the content view
- **ContentView** (e.g., `AddDeviceContentView`): Stateless view with `@Binding`/value params, renders the UI

**KMP-ObservableViewModel** bridges KMP ViewModels to SwiftUI (migrated from hand-written
wrappers in PR #1250 / bb-ovm1 — there is no longer a `*ViewModelWrapper` or `KmpViewModelStore`):
- KMP ViewModels extend `com.rickclephas.kmp.observableviewmodel.ViewModel` (which is-a
  `androidx.lifecycle.ViewModel` on every target we ship, so Compose-MP is unaffected).
- The Screen holds the VM with `@StateViewModel` (owns + clears it, replacing the old
  `@StateObject` wrapper + `Task { for await }` + `deinit`). `@ObservedViewModel` for non-owning.
- State reaches SwiftUI via **observable** StateFlows — build them with
  `safeStateIn(viewModelScope, …)` / `retryableStateIn(viewModelScope, …)` / observable
  `MutableStateFlow(viewModelScope, …)` (in `:viewmodel` `ViewModelExtensions.kt`). `viewModelScope`
  is the rickclephas type; use `viewModelScope.coroutineScope.launch { }` for coroutines.
- The Screen reads current values via a small manual `xxxValue` extension accessor (Option A —
  no KMP-NativeCoroutines; SKIE stays for sealed/enum/default-arg interop). `@StateViewModel`
  re-renders when any observable StateFlow emits, so the accessor is re-read automatically.
- One-time bridge: `Core/ObservableViewModelBridge.swift` =
  `extension shared.ViewModel: @retroactive ViewModel {}`. The SPM package `KMP-ObservableViewModel`
  (pinned `1.0.1` to match the Kotlin artifact for our Kotlin version) is on the app target;
  `ios-swift-di` exports the observableviewmodel core. It's Kotlin-version-pinned — bump in
  lockstep with Kotlin (see bb-k4sk).
- VM actions: call the VM method directly with labels (`viewModel.onSortOptionSelected(option:)`);
  build inputs (`DeviceInput`, `DeviceTypeInput`, `KotlinInstant`) inline in the Screen.

**SKIE AuthError subtypes** in Swift:
- `AuthErrorConfigurationNotConfigured`, `AuthErrorConfigurationServerUnavailable`
- `AuthErrorSignInCancelled`, `AuthErrorSignInFailed` (has `.cause` property)
- `AuthErrorSignInNetworkError`, `AuthErrorTokenInvalid`, `AuthErrorTokenExpired`, `AuthErrorUnknown`
- These are class types — use `is` for type checks, `as let` for property access

**iOS CI note**: `build_ios_native` and `validation_ios_ui` are skipped on PRs in development CI mode (slow jobs). iOS compile errors are only caught post-merge on `main`. `validation_ios_ui` uses `build-for-testing` to compile the test target without running tests — this catches API mismatches (e.g., missing parameters) but does NOT do pixel-level snapshot regression. Snapshots are auto-generated post-merge by `auto-generate.yml` (like Android screenshots). For iOS-only changes, consider local `xcodebuild` verification. **Always run from repo root** — never `cd` into `ios-app-swift-ui/`:
```bash
# Build
xcodebuild -project ios-app-swift-ui/iosAppSwiftUI.xcodeproj -scheme iosAppSwiftUI \
  -destination 'generic/platform=iOS Simulator' build \
  CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO \
  -derivedDataPath ios-app-swift-ui/build/ios-build
# Test (must specify OS version — no 'latest' match)
xcodebuild test -project ios-app-swift-ui/iosAppSwiftUI.xcodeproj -scheme iosAppSwiftUITests \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.5' \
  CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO \
  -derivedDataPath ios-app-swift-ui/build/ios-tests
```

## iOS SwiftUI Feature Parity

Screen parity is 15/15 (all screens exist including DeviceTypeDetail from PR #911) but feature-level parity is ~50%. PR #873 added the design system (icons, colors, battery age), lifting History from MINIMAL to PARTIAL. Remaining gaps are mostly sort/group controls, error states, and interactive features. See `docs/FEATURE_PARITY_MAPPING.md` for per-screen gap tables.

Key systemic gaps:
- No split-screen AI overlay or persistent input bar (AI is a standalone tab)
- No sort/group controls on Home or Device Types lists

## iOS Design System ("Sage & Linen")

PR #873 implemented the full iOS design language from `docs/design/IOS_DESIGN_LANGUAGE.md`:

**Token files** in `Core/Theme/`:
- `ButlerColors.swift` — 20+ semantic color roles with light/dark support via `Color(light:dark:)`
- `ButlerSpacing.swift`, `ButlerCornerRadius.swift`, `ButlerIconSize.swift`

**Component files** in `Core/Components/`:
- `ButlerIconBox.swift` — 44pt icon container with themed background
- `SFSymbolMapper.swift` — maps 37 Android icon names → SF Symbols
- `BatteryAgeHelper.swift` — 3-tier battery age coloring (gray/amber/red)

**Critical Swift gotchas:**
- `Color` extensions used with `.foregroundStyle()` require explicit `Color.` prefix (e.g., `Color.butlerPrimary`). Implicit member syntax (`.butlerPrimary`) fails with "type 'ShapeStyle' has no member" — especially in ternary expressions.
- KMP `Instant` type cannot be referenced by name in Swift. Use `instant.toEpochMilliseconds()` and pass `Int64` to helper functions instead.
- `sync_pbxproj.rb` now syncs both `Features/` and `Core/` subdirectories.
- Settings shows only app version (missing sign-out, data mode, AI engine)
- **Localization**: `iosApp/Localizable.strings` provides en-US base strings (~130 keys). SwiftUI views use dot-notation keys (e.g., `Text("home.title")`). Non-SwiftUI contexts use `String(localized:)`. Dynamic user data is not localized. Short format strings with interpolation (`"\(days)d"`) are not yet localized (need pluralization support). The `iosApp/` directory uses `PBXFileSystemSynchronizedRootGroup` so new files are auto-included in builds.

## KMP Swift Constructor Gotchas

- Kotlin `data class` with ALL default params → Swift exports only full-param init + no-arg init (no partial constructors)
- Kotlin `data object` types export `init()` in Swift (not just `.shared`)
- `AiRole` values: `.user`, `.model`, `.system`, `.tool` (NOT `.assistant`)
- `AiMessage` param order: `id:role:text:isPartial:hints:` (all required in Swift)
- `GroupOption.none`, `SortOption.name` for default enum values

## iOS Unit Tests

28 unit tests cover Swift-only pure/static code (no KMP mock infrastructure needed):
- `BatteryAgeHelperTests` (13 tests) — color thresholds, font weight, daysSince
- `SFSymbolMapperTests` (7 tests) — nil/unknown/known icon mappings
- `LoginErrorInfoTests` (8 tests) — all AuthError subtypes → title/message/showRetry

`LoginErrorInfo.errorInfo(for:)` is a standalone `static` helper for direct testability (relocated
from the deleted `LoginViewModelWrapper` in bb-ovm1; `SettingsDisplay` similarly holds the
data-mode / AI-engine display-name helpers).

**KMP AuthError constructors in Swift**: All subtypes require full-param init including `message:` — the no-arg init is unavailable despite Kotlin defaults. Example: `AuthErrorSignInFailed(message: "Sign-in failed", cause: nil)`.

## KMP Interop Patterns

**KmpInteropTests.swift** is the regression safety net — it constructs every KMP type used at the iOS boundary. If a KMP API change breaks a constructor, these tests fail immediately.

### Type categories and Swift constructor rules

| KMP Type | Swift Constructor | Example |
|----------|------------------|---------|
| `data class` (no defaults) | Full-param labeled init | `Device(id:name:typeId:...)` |
| `data class` (all defaults) | Full-param init OR no-arg init | `HomeScreenState(groups:devices:...)` |
| `data object` | `TypeName()` | `SyncStatusIdle()` |
| `sealed interface` subtype | Flattened name + init | `AuthStateAuthenticated(user:)` |
| `enum class` | Lowercase dot syntax | `.user`, `.model`, `.name` |
| `Companion.shared` | Static access | `KotlinInstant.Companion.shared` |

### Sealed interface naming in Swift (SKIE)

Kotlin nested types flatten to concatenated names:
- `AuthState.Authenticated` → `AuthStateAuthenticated`
- `DeviceDetailScreenState.Success` → `DeviceDetailScreenStateSuccess`
- `SyncStatus.Idle` → `SyncStatusIdle`
- `BatchOperationResult.Progress` → `BatchOperationResultProgress`
- `DataError.Unknown` → `DataErrorUnknown`

### StateFlow value access in Screens (post-wrapper migration, bb-ovm1)

The hand-written ViewModelWrappers were removed. Screens read StateFlow values through a manual
`xxxValue` extension accessor on the KMP ViewModel, e.g.
`extension DeviceDetailViewModel { var uiStateValue: DeviceDetailScreenState { uiState.value } }`.
Boolean StateFlows still box at the boundary — use `(flow.value as? Bool) ?? false`. Sealed-state
matching uses the SKIE-flattened names (`DeviceDetailScreenStateSuccess`, etc.) with `is`/`as?`.

**SKIE `FlowInterop` is load-bearing — do not disable it.** These `.value` reads are strongly
typed *only* because SKIE's `FlowInterop` is enabled: beyond exposing Flows as `AsyncSequence`, it
types `StateFlow<T>.value` as `T` in Swift. Disabling `FlowInterop` degrades `.value` to `Any?` and
breaks every accessor (e.g. `ObservableViewModelBridge.swift`: *"type 'Any' does not conform to
'DeviceDetailScreenState'"*). `SuspendInterop` is disabled (no Swift code awaits a Kotlin `suspend`
fn); `FlowInterop` + enum/sealed/default-arg interop stay on. Config lives in
`ios-swift-di/build.gradle.kts` (PR #1256). Because dev-mode PR CI skips iOS, verify any
`skie { }` / cinterop / framework-export change with a local `./scripts/build-ios.sh`.

**Exposed state must be observable** — building exposed `(Mutable)StateFlow` with the plain `kotlinx`
`MutableStateFlow` (instead of `MutableStateFlow(viewModelScope, …)` / `safeStateIn` / `retryableStateIn`)
compiles but SwiftUI silently never re-renders. `ExposedStateObservabilityConventionTest`
(`:viewmodel:desktopTest`) guards this on every PR; private funnel flows may stay plain.

## iOS Snapshot Tests

All 15 screens follow the two-layer Screen/ContentView pattern. 46 snapshot test functions (92 PNGs — light + dark) cover all ContentViews across 19 test files. Reference images are tracked in git (`__Snapshots__/`). CI uses `build-for-testing` (compile-only, no pixel comparison). Snapshots are auto-recorded post-merge by `auto-generate.yml` on `macos-latest` and committed via follow-up PRs (like Android screenshots). Use `scripts/record-ios-snapshots.sh` to record locally.

To test SwiftUI views connected to KMP, ensure the `Screen` structures are separated into stateless `ContentView` structures to bypass the `NativeComponent` DI graph during testing.

Use the convenience scripts instead of raw xcodebuild commands:
```bash
./scripts/build-ios.sh                        # Build for simulator
./scripts/test-ios.sh                         # Run all tests
./scripts/test-ios.sh BatteryAgeHelperTests   # Run one test class
./scripts/test-ios.sh Foo Bar                 # Run multiple test classes
./scripts/sync-ios-project.sh                 # Sync files into Xcode project
./scripts/record-ios-snapshots.sh             # Record snapshot references
```

Reference images are tracked in git (`__Snapshots__/`). They are auto-generated post-merge by `auto-generate.yml` on CI's `macos-latest` runner, then committed via follow-up PRs (same pattern as Android screenshots).

CI uses `build-for-testing` (compile-only) — does NOT run snapshot comparison tests. Snapshots are a visual record, not a pass/fail gate.

To record locally: `./scripts/record-ios-snapshots.sh` (uses `SNAPSHOT_TESTING_RECORD=all`)

## iOS Build System

- **iOS protos**: Run `./scripts/generate-protos.sh` before iOS builds if proto files changed. The script generates Swift protobuf files from Bazel.
- **iOS Swift wrapper API sync**: When a KMP shared ViewModel or use case changes its public API (e.g., adding a parameter to `sendMessage`), the corresponding Swift wrapper in `ios-app-swift-ui/Features/*/` must be updated too. The iOS build (`build_ios_native`) is the canary — a mismatch causes a Swift compile error with "missing argument for parameter". After any KMP API change, grep `ios-app-swift-ui/` for the function name to catch wrappers that need updating.
- **Stale local DerivedData can break `iosAppSwiftUI` with "Unable to resolve module dependency: 'KMPObservableViewModelCore'"** even though `xcodebuild -resolvePackageDependencies` reports the package resolved fine. Confirmed 2026-07-07 this is a *local dev-machine* artifact, not a real regression — reproduces identically on unmodified `main` and is unrelated to any code change; CI runners start with fresh `DerivedData` so they don't hit it. Fix: `rm -rf ~/Library/Developer/Xcode/DerivedData/iosAppSwiftUI-*` and rebuild. Also: running `./scripts/validate.sh` concurrently across multiple git worktrees on the same machine causes real collisions — the shared Gradle daemon registry (one worktree's `--stop` in the "iOS Checks" section kills every other worktree's in-flight build) and the Android emulator/AVD (two worktrees booting the same managed-device AVD name errors with "Running multiple emulators with the same AVD"). Either serialize `validate.sh` runs across worktrees, or isolate the Gradle daemon registry per run with `GRADLE_OPTS="-Dorg.gradle.daemon.registry.base=<unique-dir>"`.
