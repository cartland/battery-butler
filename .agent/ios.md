# iOS Architecture & Design

SwiftUI architecture, design system, xcodebuild patterns, and snapshot testing.

> **Parent doc:** See `project.md` for shared architecture and `AGENTS.md` for workflow rules.

## iOS SwiftUI Architecture

The iOS SwiftUI app follows a **two-layer pattern**:
- **Screen** (e.g., `AddDeviceScreen`): Owns `@State` vars, creates `@StateObject` wrapper, composes the content view
- **ContentView** (e.g., `AddDeviceContentView`): Stateless view with `@Binding` params, renders the UI

**ViewModelWrapper** bridges KMP ViewModels to SwiftUI:
- `@StateObject` in the Screen, wraps the KMP ViewModel
- Uses `KmpViewModelStore` for lifecycle management
- Subscribes to KMP `StateFlow` via async `for await` loops in `Task`s
- Publishes state via `@Published` properties

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
- Settings shows only app version (missing sign-out, network mode, AI engine)
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

`LoginViewModelWrapper.errorInfo(for:)` is `static` (internal) for direct testability.

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

### StateFlow casting patterns in ViewModelWrappers

Three patterns exist across the 14 wrappers (not yet standardized):

1. **guard/fatalError** — `guard let state = flow.value as? ConcreteType else { fatalError() }`
2. **Direct assignment** — `self.property = flow.value` (when types match directly)
3. **as?/fallback** — `self.property = flow.value as? ConcreteType ?? defaultValue`

Pattern #3 is safest (no crash on type mismatch). Standardization is a separate refactor.

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
