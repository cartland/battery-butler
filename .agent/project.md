# Project Knowledge

Shared project-specific knowledge for all AI agents. This supplements the workflow rules in `AGENTS.md` with technical context about the codebase.

## Architecture

**Battery Butler** is a Kotlin Multiplatform (KMP) app with a Ktor gRPC server.

- **Platforms**: Android, iOS (SwiftUI + Compose), Desktop
- **Server**: Ktor + gRPC on AWS ECS Fargate
- **Database**: Room (local), RDS PostgreSQL (server)
- **Build**: Gradle (app), Bazel (iOS protos), Terraform (infrastructure)
- **CI**: GitHub Actions
- **Task tracking**: `bd` (Beads CLI)

### Offline-First Sync

The app works entirely offline and syncs bidirectionally when online:
- Local changes persist immediately to Room database
- Changes sync to server when connectivity is available
- Server changes sync back to local on reconnect

### Module Dependencies

Architecture is enforced by `buildSrc/.../ArchitectureCheckTask.kt`. Key rules:
- `:domain` depends on nothing (pure interfaces and models)
- `:usecase` depends on `:domain`, `:presentation-model`
- `:viewmodel` depends on `:usecase`, `:domain`, `:presentation-model`
- `:ai` contains platform implementations (`AndroidAiEngine`, `DynamicAiEngine`, `OnDeviceAiEngine`, `NoOpAiEngine`); interfaces live in `:domain:model:ai`
- `:usecase` contains `SendChatMessageUseCase` (augments AI messages with time + user context) and `BuildAiContextUseCase` (builds user inventory summary from DeviceRepository)
- `:data` provides implementations of domain interfaces

### DI Wiring (kotlin-inject)

- `AppComponent` constructor parameters are the DI roots — ALL platform creation sites must be updated when parameters change:
  - Android: `BatteryButlerApplication.kt`
  - Desktop: `compose-app/src/desktopMain/.../Main.kt`
  - iOS Compose: `IosComponentHelper.kt` (3 files: iosArm64Main, iosSimulatorArm64Main, iosX64Main)
  - iOS Native: `NativeComponent.kt` in `ios-swift-di` (uses `@Provides` methods, not constructor params)
- Non-Android platforms use `InMemoryAiPreferencesRepository` (in `:data`); Android uses `AiPreferencesRepositoryImpl` with `SharedPreferencesSettings`
- Modules using `@Inject` need BOTH `kotlin-inject-runtime` (commonMain) AND `kotlin-inject-compiler` (KSP)
- `multiplatform-settings` is `implementation` in `:data` — not transitive. Add directly to modules using platform-specific Settings classes (e.g. `SharedPreferencesSettings`)

### Coroutine Dispatching

Use `DispatcherProvider` (in `:domain:model`) instead of hardcoding `Dispatchers.Default/IO/Main`:
- Inject `DispatcherProvider` into use cases and repositories
- `DefaultDispatcherProvider` (in `:data:provider`) provides real dispatchers
- Tests use `UnconfinedTestDispatcher` for synchronous execution
- kotlinx-coroutines 1.10.2+ provides `Dispatchers.IO` in common code (no expect/actual needed)

### Error Handling

**Project code NEVER throws exceptions except `CancellationException`.**

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

Key types (see `domain/model/DataResult.kt`):
- `DataResult<T>` - Success/Error wrapper for operations
- `DataError` - Sealed hierarchy: Network, Database, Ai, Unknown
- Catch library exceptions at data layer boundaries, return typed errors

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

### UI Theme Constants

Padding constants live in `presentation-core/.../theme/Padding.kt`. Use `Padding.standard` (16.dp), `Padding.small` (8.dp), `Padding.large` (24.dp), etc. instead of hardcoded dp values.

Icon sizes live in `presentation-core/.../theme/IconSize.kt`.

Custom app colors beyond Material3 live in `ButlerColors` (data class) provided via `LocalButlerColors` composition local. Access with `LocalButlerColors.current.batteryWarning`. New custom colors should be added to `ButlerColors`, provided in `BatteryButlerTheme`, and defined in `Color.kt`.

Icon colors use the **`IconColorRole` enum** (`presentation-core/.../theme/IconColorRole.kt`) — all device icons use **Primary (sage green)**. `DeviceIconMapper.getIconColorRole(iconName)` returns `IconColorRole.Primary` for all inputs. `DeviceIconMapperTest` (in `presentation-core/src/commonTest/`) guards against regression to per-category colors. `DeviceIconMapper.getResolvedIconAccent(iconName)` returns the resolved `(containerColor, contentColor)` pair. `IconAccent.kt` has been deleted — do not recreate it.

**Standard dropdown component**: Use `ButlerDropdownMenu` (`presentation-core/.../components/ButlerDropdownMenu.kt`) instead of `DropdownMenu` directly. Note: CMP 1.10.0 `DropdownMenu` does NOT support `enter`/`exit` animation params (Jetpack Compose only).

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

### AI Overlay UI Architecture

The AI chat is an overlay on top of the main tab UI, not a separate tab/screen. Key design:
- **`AiChatViewModel`** is hoisted to App scope so state survives tab navigation.
- **`isAiExpanded`** and **`tabTransitionForward`** state live in `App.kt` (both `rememberSaveable`). `App.kt` sets `isAiExpanded = false` in `onTabSelected`. `MainScreenShell` also collapses via predictive back gesture or collapse button.
- **Main tab navigation** flows through each tab's `ScreenRoot` composable down to `MainScreenShell`.
- **`MainScreenShell`** bottom bar (`presentation-feature/main/MainScreen.kt`) owns the always-visible AI input (`OutlinedTextField` + send `IconButton`) wrapped in `Surface` with `imePadding()` on the `Scaffold`. Tapping send expands the overlay and routes through `onSendAiMessage`.
- **AI overlay height animation**: `MainScreenShell` uses `Animatable<Float>` (0f = hidden, 1f = full target height) to drive chat overlay height. `PredictiveBackHandler` (expect/actual in `presentation-core/.../components/PredictiveBackHandler.kt`) is composed after `content()` for higher priority than NavDisplay's handler. On Android, the back gesture smoothly tracks finger position; on desktop/iOS, the handler is a no-op.
- **`AiTabContent`** (`presentation-feature/aichat/AiTabContent.kt`) has a `showInput: Boolean = true` param. The overlay passes `showInput = false` so only the chat history slides up.
- **AI messages** include an `[Active tab: <name>]` context prefix so the AI knows which screen the user is viewing.
- **Tab transitions** are directional: `tabTransitionForward` is set before each backstack mutation based on tab index. `NavDisplay.transitionSpec` reads it to slide left or right.
- **`MainTab.AI`** enum value remains in the codebase but is dead code — the AI is now an overlay, not a nav tab.
- **Predictive back (Android 13+)**: Opted in via `android:enableOnBackInvokedCallback="true"` in `AndroidManifest.xml`. AI overlay collapse uses `PredictiveBackHandler` for gesture-tracked animation. The detail stack back handling disables the handler when Login is the only entry (prevents revealing unauthenticated tabs). Full back gesture contract documented in `docs/TESTING.md`.


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

## Testing

### Test Types

| Type | Command | Server? | Emulator? | CI Job |
|------|---------|---------|-----------|--------|
| Unit | `./gradlew test` | No | No | `validation_test` |
| Instrumented | `./scripts/test.sh` | No | Yes | `validation_instrumented` |
| Screenshot | `./gradlew :android-screenshot-tests:validateDebugScreenshotTest` | No | No | `validation_screenshots` |
| E2E | `./scripts/e2e-tests.sh` | Yes | No | Manual only |

### Audits
- **100 Personas Audit**: We occasionally run an AI-driven audit embodying 100 distinct professional perspectives (e.g., Security Engineer, Accessibility Specialist) to identify testing improvements and meta-testing insights. See `docs/100_personas_audit_plan.md` and `docs/100_personas_audit_progress.md` for methodology and generated PRs.

### When Tests Run

| Trigger | Unit | Instrumented | Screenshot | E2E |
|---------|------|-------------|------------|-----|
| PR with code changes | Yes | Yes | Yes | No |
| PR with docs only | No | No | No | No |
| `./scripts/validate.sh` (local) | Yes | Yes | Yes | No |
| Manual | — | — | — | Yes |

### Convention Tests
- **`UseCaseConventionTest`** (`usecase/src/jvmTest/`): JVM-only test that uses Kotlin reflection to scan all `*UseCase` classes in the `com.chriscartland.batterybutler.usecase` package and asserts each has `operator fun invoke` (suspend or non-suspend). Runs as part of `./gradlew :usecase:jvmTest`. Requires `kotlin("reflect")` in jvmTest dependencies.
- **`ViewModelTestConventionTest`** (`viewmodel/src/desktopTest/`): Desktop-only test (JVM reflection) that scans all `*ViewModel` classes and verifies each has a corresponding `*ViewModelTest` class. Excludes `*Factory` and `KmpViewModelStore`. Runs as part of `./gradlew :viewmodel:desktopTest`. Requires `kotlin("reflect")` in desktopTest dependencies. Note: the viewmodel module uses `jvm("desktop")` not `jvm()`, so the source set is `desktopTest/` not `jvmTest/`.
- The tests use `kotlin.test.assertTrue(value, message)` (NOT the trailing-lambda form, which doesn't exist in `kotlin.test`).
- **`desktopTest`** is in the detekt FunctionNaming excludes list (alongside `jvmTest`, `commonTest`, etc.) to allow backtick test names.

### Unit Tests (`./gradlew test`)
- Pure Kotlin tests across all modules (domain, data, viewmodel, usecase, server, etc.)
- Located in `src/commonTest/`, `src/test/`
- **Coroutine test gotcha**: `DefaultSyncManager` has an infinite `subscribeWithRetry()` loop in `init`. Never use `advanceUntilIdle()` in tests that create a SyncManager with a subscribe source that throws or completes (it schedules infinite tasks). Use `testDispatcher.scheduler.advanceTimeBy(ms)` + `runCurrent()` instead, and always call `scope.cancel()` at end.
- `applyRemoteUpdate` and `nextBackoff` are `internal` on `DefaultSyncManager` for direct testing without the subscribe loop
- **Crash-proof ViewModel tests** (`CrashProof*Test.kt`): Test error handling gaps in ViewModels. Two patterns:
  - **Pattern A** (safeStateIn): Throwing repo flow → verify `safeStateIn` catches exception but UI stays stuck at initial value (e.g., `Loading`). Tests pass, documenting the broken UX.
  - **Pattern B** (viewModelScope.launch): Can't use `assertFailsWith` because `SupervisorJob` sends exceptions to the thread's uncaught handler asynchronously (not through `advanceUntilIdle()`). `runTest` catches these and fails. Use **intercepting repo** pattern instead: record exception without rethrowing, then assert no error state exists on the ViewModel.

### Instrumented Tests (`scripts/test.sh`)
- Require an Android emulator (CI uses managed Pixel 5 API 34 with KVM)
- All tests are offline-capable — no server needed (app defaults to `NetworkMode.None`)
- `compose-app/src/androidInstrumentedTest/`: `ComposeUITest` (UI navigation), `ExampleInstrumentedTest` (app context)
- `data/src/androidInstrumentedTest/`: `DatabaseSanityTest` (Room schema), `MigrationTest` (Room migrations 3→4→5)
- **BackHandler priority**: The app uses two NavDisplay stacks (tab + detail). When both have entries, the tab NavDisplay's BackHandler (deeper in composition tree) takes priority over the App-level detail stack BackHandler. In tests, use actual UI back buttons (Cancel/Done/Back arrow) instead of `Espresso.pressBack()` to avoid this conflict.
- **Managed device test filtering**: `--tests` flag doesn't work with managed device tasks. Use `-Pandroid.testInstrumentationRunnerArguments.class=com.example.TestClass#testMethod` instead.
- **Test isolation**: Tests within a single managed device run share database state. Avoid tests that depend on empty-state UI when other tests create persistent data.

### Screenshot Tests
- Pixel-perfect UI regression tests against reference images
- Failures indicate UI changes, not broken infrastructure
- **Android**: Use `updateDebugScreenshotTest` / `validateDebugScreenshotTest` (Paparazzi/Roborazzi).
  - All preview composables must be time-deterministic — never let `Clock.System.now()` reach a screenshot preview
  - Use `Instant.parse("2026-01-18T17:00:00Z")` as the standard fixed instant in previews
  - Pass explicit `nowInstant` / date parameters through the full composable chain — don't rely on defaults
  - `updateDebugScreenshotTest` and `validateDebugScreenshotTest` can't run in the same Gradle invocation (the update task's clean step deletes references mid-build)
  - **Never use `--tests` filter with `updateDebugScreenshotTest`** — the gallery generator deletes reference images for non-included test classes. Always use `scripts/generate-screenshots-sequentially.sh` to regenerate all baselines safely (runs one test file at a time to avoid OOM)
  - **OOM guard**: `updateDebugScreenshotTest` and `validateDebugScreenshotTest` are blocked by default — a `doFirst` guard in `build.gradle.kts` prevents all-at-once runs that OOM. The sequential script bypasses via `-PretainedReferenceScreenshots`. To force a direct run, pass `-PforceAllScreenshots`.
  - **Validating specific classes after regen**: `-PforceAllScreenshots` still OOMs on large previews (e.g. `PlayStoreAddDeviceTest_Light`). After regenerating baselines, validate only the affected test classes: `./gradlew :android-screenshot-tests:validateDebugScreenshotTest --tests "com.chriscartland.batterybutler.androidscreenshottests.<TestClass>Kt" -PforceAllScreenshots`. Full all-at-once local validation is unreliable; CI handles the full suite. See `bb-cpe4` for the sequential validation script task.
  - When refactoring shared components (e.g. list items), ALL screen-level baselines that embed those components will change — regenerate everything, not just the component tests
  - **Always regenerate and commit reference images** when adding or changing screenshot tests. Run `./scripts/generate-screenshots-sequentially.sh`, then `git add` the new/updated PNGs in `android-screenshot-tests/src/screenshotTestDebug/reference/` and `SCREENSHOT_GALLERY.md`. PRs that add screenshot tests without reference images are incomplete.
  - **Preview coverage enforcement**: `./gradlew checkPreviewCoverage` scans `presentation-core` and `presentation-feature` for `@Preview` composables and verifies each has a corresponding screenshot test import. Fails the build on gaps. Also generates `docs/Preview_Coverage_Report.md` (gitignored). When adding a new `@Preview`, also add a screenshot test or the coverage check will fail.
  - **Two-tier structure**: Screenshot tests have exactly two tiers — (1) **full-screen** (with Scaffold, tabs, app bar) and (2) **individual components** (reusable design-system pieces). Intermediate layouts (e.g. just the filter row, just the list section, just a sub-section) must not have standalone screenshot tests. When removing an intermediate-layout screenshot test, also remove the `@Preview` annotation from the source composable (keep the composable function itself; just drop the `@Preview`).
  - **Battery age states** (`DeviceListItemOldPreview`, `DeviceListItemVeryOldPreview`) are component-level tests — they verify distinct visual states (amber warning ≥180 days, red bold ≥365 days) that matter for regression detection.
  - **Platform API overrides for previews**: When a composable reads a platform API (e.g., `WindowInsets.ime`) that always returns a fixed value in previews, use **parameter hoisting** — add a parameter with the platform read as its default (e.g., `imeVisible: Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0`). Previews pass the desired value directly. Do NOT use CompositionLocals for test-only overrides — that leaks test concerns into production code.
- **iOS**: Uses `swift-snapshot-testing`. 
  - To test SwiftUI views connected to KMP, ensure the `Screen` structures are separated into stateless `ContentView` structures to bypass the `NativeComponent` DI graph during testing.
  - Native iOS snapshot tests execute inside the simulator via `xcodebuild test -project iosAppSwiftUI.xcodeproj -scheme iosAppSwiftUITests -destination "platform=iOS Simulator,name=iPhone 16 Pro,OS=18.5"` from the `ios-app-swift-ui` directory.
  - There is no native update flag; tests will automatically record missing snapshots, or you can delete `__Snapshots__` to force a recreation of all references.

### Detekt
- Composable functions must order params: no-default params first, then `modifier: Modifier = Modifier`, then other defaulted params, then trailing lambda. Detekt's compose rule enforces this.

### Spotless / ktlint
- ktlint enforces the **single top-level declaration filename rule**: if a `.kt` file contains only one top-level declaration (class, object, etc.), the file must be named after that declaration. If you remove a declaration leaving only one, rename the file accordingly.

### E2E Tests (`e2e-tests/`)
- Wire gRPC client tests against a real server (`SyncPushE2eTest`, `ServerHealthE2eTest`)
- NOT included in CI or `validate.sh` — manual only
  ```bash
  ./scripts/e2e-tests.sh                    # Auto-starts local server (auto-generates auth token)
  ./scripts/e2e-tests.sh --remote           # Uses E2E_SERVER_URL and E2E_AUTH_TOKEN env vars
  E2E_SERVER_URL=http://<nlb>:80 E2E_AUTH_TOKEN=<token> ./scripts/e2e-tests.sh --remote  # Against cloud
  ./gradlew :e2e-tests:test -De2e.server.url=http://localhost:50051 -De2e.auth.token=<token>  # Direct
  ```
- **E2E Auth**: Server reads `E2E_TEST_TOKEN` env var and pre-seeds a synthetic session. Tests attach the token as a Bearer header via OkHttp interceptor. This tests the real auth path (not a bypass).
- **Local mode**: Script auto-generates a UUID token and passes it to both server and tests.
- **Remote mode**: Token must match the `E2E_TEST_TOKEN` GitHub secret deployed to the dev server. Token value stored in `local.properties` (gitignored).
- **GitHub secret**: `E2E_TEST_TOKEN` — only set for dev environment. After setting/rotating, must redeploy dev for the container to pick it up.
- **Build cache disabled**: E2E tests use `outputs.cacheIf { false }` because they test a live server whose state is external to Gradle inputs. Without this, Gradle can serve stale cached results instead of actually running the tests.

## Build System

- **Bazel disk cache issue**: When running `bazel build` in scripts called from Xcode, use `--disk_cache=""` to ensure outputs are materialized locally. The disk cache can return metadata without creating actual files.
- **iOS protos**: Run `./scripts/generate-protos.sh` before iOS builds if proto files changed. The script generates Swift protobuf files from Bazel.
- **iOS Swift wrapper API sync**: When a KMP shared ViewModel or use case changes its public API (e.g., adding a parameter to `sendMessage`), the corresponding Swift wrapper in `ios-app-swift-ui/Features/*/` must be updated too. The iOS build (`build_ios_native`) is the canary — a mismatch causes a Swift compile error with "missing argument for parameter". After any KMP API change, grep `ios-app-swift-ui/` for the function name to catch wrappers that need updating.

## Server URL Management

> **Note:** AWS is hibernated. The URLs in gradle.properties and GitHub secrets
> point to decommissioned endpoints. Only GrpcLocal mode works.

Server URLs (prod and dev) flow through the system as follows:

**Source of truth:** GitHub secrets `PRODUCTION_SERVER_URL` and `DEV_SERVER_URL`, auto-synced from terraform output after each deploy.

**How it propagates:**
1. Terraform creates NLB → deploy workflows capture `nlb_dns_name` → `gh secret set PRODUCTION_SERVER_URL` / `DEV_SERVER_URL`
2. CI workflows set `ORG_GRADLE_PROJECT_PRODUCTION_SERVER_URL` and `ORG_GRADLE_PROJECT_DEV_SERVER_URL` env vars from the secrets
3. Gradle reads them as project properties → `data-network/build.gradle.kts` generates `BuildConfig.kt` with both constants
4. Code accesses via `BuildConfig.PRODUCTION_SERVER_URL` and `BuildConfig.DEV_SERVER_URL`

**DI pattern for modules without data-network dependency:**
- `ProductionServerUrl` and `DevServerUrl` data classes (in `domain/model/`) wrap the URLs for type-safe injection
- `AppComponent` (Android/Desktop) and `NativeComponent` (iOS) provide both from BuildConfig
- ViewModels and other components receive them via constructor injection

**NetworkMode variants:**
- `NetworkMode.GrpcAws(url)` — Prod server
- `NetworkMode.GrpcDev(url)` — Dev server
- `NetworkMode.GrpcLocal(url)` — Local development server
- `NetworkMode.Mock` — Offline mock data
- `NetworkMode.None` — Network disabled (default)

Settings UI displays them in this order: Prod Server / Dev Server / gRPC Local / Mock / None (Offline).

**Key rules:**
- **NEVER hardcode NLB hostnames** in Kotlin source — use `BuildConfig.PRODUCTION_SERVER_URL` / `BuildConfig.DEV_SERVER_URL` or `ProductionServerUrl` / `DevServerUrl`
- `gradle.properties` has fallback values for local dev only; CI always overrides from secrets
- `release-android.yml` validates server connectivity before uploading to Play Store
- When adding a new NetworkMode variant, update all `when` branches (check: DelegatingGrpcClient, DelegatingRemoteDataSource, DynamicDatabaseProvider, DataStoreNetworkModeRepository, SettingsContent, DebugNetworkReceiver, NetworkModeTest)

## Secrets Management

**GitHub Secrets** (write-only — values can't be read back):
- `GEMINI_API_KEY` — Gemini AI API key, written to `local.properties` during Android release builds
- `E2E_TEST_TOKEN` — Pre-seeds synthetic auth session on dev server
- `PRODUCTION_SERVER_URL` — Auto-synced from terraform after each deploy
- `DEV_SERVER_URL` — Auto-synced from terraform after each dev deploy
- `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` — Android signing
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` — Play Store upload

**Local secrets** in `local.properties` (gitignored) — can get overwritten by IDE/Gradle:
- Back up important keys to macOS Keychain:
  ```bash
  # Save
  security add-generic-password -a "KEY_NAME" -s "battery-butler" -w "value" -U
  # Retrieve
  security find-generic-password -a "KEY_NAME" -s "battery-butler" -w
  ```
- Currently stored in Keychain: `GEMINI_API_KEY`

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

- **`bd` (beads)** — Cross-session project tracking. Persists in git. Use for epics, bugs, features that span multiple sessions.
- **Claude's TaskCreate/TaskList** — Within-session team coordination. Ephemeral. Use for breaking work into subtasks during a team session.

**Rules:**
- Teammates in a Claude Code team use TaskCreate/TaskList for coordination (never `bd`)
- Use `bd` at session start (`bd ready`) and session end (`bd close`) for project-level items
- Don't duplicate: if it's a single-session task, it doesn't need a bead

### `bd` Quick Reference

Use `bd` CLI for all task/issue management. **Never modify `.beads/issues.jsonl` directly.** Run `bd help` for full command list.

```bash
# Session workflow
bd list              # List all open issues
bd ready             # Show tasks ready to work on (no blockers)
bd show <id>         # View full task details
bd create "Title" --type task --priority P2  # Create a task
bd close <id> --reason "Fixed in PR #123"    # Mark complete
bd search "login"    # Search by text
```

### Committing Beads Changes

Beads files should be committed to git like any other code:

```bash
# Include with code changes (recommended)
git add src/... .beads/issues.jsonl
git commit -m "feat: Add feature X (closes bb-123)"

# Standalone beads update (when no code changes)
git add .beads/
git commit -m "chore(beads): Update task tracking"
```

**What gets committed:** `.beads/issues.jsonl`, `.beads/interactions.jsonl`, `.beads/config.yaml`, `.beads/metadata.json`

**What stays local (gitignored):** `*.db*`, `daemon.*`, `bd.sock`

## Server Deployment

> **HIBERNATED (Feb 2026):** AWS infrastructure is not running. Server workflows
> are disabled via GitHub (`gh workflow disable`). Server runs locally only. See server/README.md.

Multi-environment deployment pipeline: dev -> staging -> prod. Same Docker image SHA promoted through environments.

**Deployment rules:**
- **All deploys go to dev first.** Never deploy directly to prod.
- **Prod is always a promotion from dev.** Use `./scripts/promote-server.sh` (or `/promote-server`) to promote the dev image to prod. This ensures prod only runs images that have been validated on dev.

**Workflows:**
- `server-build.yml` -- Auto-deploys to dev on push to main (server changes), syncs `DEV_SERVER_URL` secret
- `server-deploy-staging.yml` -- Manual trigger with `image_tag` input
- `server-deploy-prod.yml` -- Manual trigger with approval gate, syncs `PRODUCTION_SERVER_URL` secret
- `server-destroy.yml` -- Tear down staging/dev infrastructure
- `server-rollback.yml` -- Emergency rollback

**Deploy commands:**
```bash
# Check what's deployed right now
./scripts/deploy-status.sh

# Promote to staging
gh workflow run server-deploy-staging.yml -f image_tag=<sha>

# Promote to prod (requires approval)
gh workflow run server-deploy-prod.yml -f image_tag=<sha>

# Test endpoints
grpcurl -plaintext -proto protos/com/chriscartland/batterybutler/protos/battery_service.proto \
  <nlb-dns>:80 com.chriscartland.batterybutler.proto.BatteryService/GetServerStatus

# Run E2E tests against a live environment
E2E_SERVER_URL=http://<nlb-dns>:80 ./scripts/e2e-tests.sh --remote
```

**Deployment observability:**
- `./scripts/deploy-status.sh` -- Shows image tag, status, commit, and drift warnings for each environment
- GitHub commit statuses (`deploy/dev`, `deploy/staging`, `deploy/prod`) -- Annotated on each commit after deploy, visible on GitHub commit pages
- Deploy workflows always check prod vs dev drift (run `deploy-status.sh` at session start)

**Key architecture decisions:**
- ECR is managed outside terraform (data source, not resource) to avoid state lock issues
- Each environment has separate terraform state (`server/{env}/terraform.tfstate`)
- Concurrency groups prevent parallel deploys to same environment
- IAM permissions documented in `server/iam_policy.json` -- update AWS Console manually when changed
- Deploy workflows auto-sync NLB hostname to GitHub secrets (`PRODUCTION_SERVER_URL`, `DEV_SERVER_URL`)
- `release-android.yml` validates server connectivity before Play Store upload

**AWS free-tier limitations:**
- Only `db.t3.micro` RDS instances allowed
- Max 2 RDS instances -- can't run dev + staging + prod simultaneously
- Use `server-destroy.yml` to tear down unused environments

## CI

### CI Mode (Development vs Release)

CI operates in one of two modes, controlled by `.github/ci-mode.txt`:

- **`development`** (default): Only fast checks (spotless, lint, detekt, unit tests, architecture, theme layer) are required on PRs. Slow jobs (instrumented tests, iOS builds, desktop builds, Android build, server build) are skipped on PRs but always run post-merge on `main`. This speeds up the PR cycle during active development.
- **`release`**: All jobs are required on PRs. Use this before cutting a release to ensure full coverage.

**Switching modes:**
```bash
# Switch to release mode
echo "release" > .github/ci-mode.txt
git add .github/ci-mode.txt
git commit -m "chore: Switch CI to release mode"

# Switch back to development mode
echo "development" > .github/ci-mode.txt
git add .github/ci-mode.txt
git commit -m "chore: Switch CI to development mode"
```

**How it works:**
- The `changes` job reads `.github/ci-mode.txt` and outputs `ci_mode`
- Slow jobs have an additional condition: `github.event_name == 'push' || ci_mode != 'development'`
- The `ci` gate job is mode-aware: in dev mode on PRs, it only checks fast job results
- On push to `main`, ALL jobs run regardless of mode (post-merge safety net)
- Issues caught post-merge in dev mode get fixed in follow-up PRs

**Hook behavior by mode:**
- Shell control flow (`for`/`while`/`if`): Always warning (never blocks)
- `--admin` bypass: Warning in development mode, blocked in release mode
- Validation-before-push: Always warning

### Path Filtering

CI uses `dorny/paths-filter` to skip expensive builds for non-code changes:
- **Beads-only changes** (`.beads/**`): Skip all builds, only run `ci` gate
- **Docs-only changes** (`*.md`, `.agent/**`): Skip all builds
- **Non-code server files** (`server/*.json`, `server/*.md`): Skip all builds
- **Code changes**: Run full build matrix (Android, iOS, Desktop, Server)

### Auto-Generated Content (Diagrams, Screenshots)

**Workflows NEVER push commits to PR branches.** Generated content is updated post-merge on `main` via follow-up PRs.

**How it works:**
1. Code merges to `main` -> `auto-generate.yml` runs
2. Generates diagrams + analysis (Job 1) and screenshots sequentially (Job 2)
3. Screenshots use `scripts/generate-screenshots-sequentially.sh` to avoid OOM on CI runners
4. Creates follow-up PRs on `auto/update-generated-content` and `auto/update-screenshots`
5. Uses `GITHUB_TOKEN` (not `BOT_PAT`) -- loop-proof by design
6. `ci-trigger-auto-prs.yml` dispatches CI on auto PRs (runs on any workflow completion, not just success)

### Concurrency Group Gotcha

CI uses concurrency groups to prevent parallel runs on the same branch. If a `workflow_dispatch` run starts while a `pull_request` run is in-flight, the `pull_request` run gets cancelled. The `ci` gate treats `cancelled` as failure. **PR status checks only track `pull_request`-event runs**, so a successful `workflow_dispatch` run won't clear the red status. Fix: push a new commit to the PR branch to trigger a fresh `pull_request` CI run.

### Dependabot PRs

Dependabot is configured (`.github/dependabot.yml`) for weekly updates.

**Merge criteria:**
- Simple updates (patch/minor versions with passing CI) -> merge
- Needs rebase -> use `@dependabot rebase` comment, then merge if CI passes
- Breaking changes -> close PR (large version jumps, CI compilation errors, critical infrastructure changes)
- PRs that modify `.github/workflows/` files cannot be merged via CLI (GitHub security restriction) -> manual merge via web UI

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
- **Work in parallel** - While one PR's CI runs, work on other tasks from `bd ready`.
