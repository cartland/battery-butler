# Changelog

This changelog summarizes the history of changes to the Battery Butler repository, explaining what changed and why.

## How to Maintain This File

> **For Human and AI Authors:** This changelog is manually maintained, not auto-generated. Please update it when making significant changes.

### When to Update

- After merging PRs that add features, fix bugs, or make architectural changes
- When the changelog is missing context about why changes were made
- When grouping related changes that span multiple PRs

### Format Guidelines

- **Most recent changes at the top**
- Group changes by date or release
- Include PR links: `[#123](https://github.com/user/repo/pull/123)`
- Explain the *why*, not just the *what*
- Use categories: Features, Fixes, CI/CD, Refactoring, Documentation, Performance

### Example Entry

```markdown
## 2026-02-01

### Features
- **Device sync**: Added cloud sync for devices ([#100](link)) - Enables users to access their device inventory across multiple devices.
```

---

## 2026-03-09

### Features

- **Module structure diagram in README**: Embedded `full_system_structure.mmd` (module dependency graph) into the README Architecture section using the existing `MermaidEmbedder` pipeline. `GenerateGraphTask` now auto-embeds into README alongside the existing sankey chart. Added `MermaidEmbedderTest` with 7 unit tests. ([#942](https://github.com/cartland/battery-butler/pull/942))

### Testing

- **Close screenshot parity gaps**: Added error state UI + preview + screenshot test for AddDeviceType on Android. Added deleted-device-ref snapshot test for EventDetail on iOS. Updated Settings network modes gap reason to "platform divergence" (iOS has no network mode UI). Updated parity matrix counts (Android: 139 PNGs/78 tests, iOS: 96 PNGs/48 tests).

### CI/CD

- **README in auto-generate change detection**: Added `README.md` to the `git diff` check in `auto-generate.yml` so README embedding changes trigger follow-up PRs. Fixed sankey frontmatter validation to scope within `code_distribution.mmd` markers (was incorrectly matching the first mermaid block when multiple blocks exist). ([#942](https://github.com/cartland/battery-butler/pull/942))
- **Inline CI trigger for auto-generated PRs**: Auto-generate workflow now closes/reopens PRs with `BOT_PAT` immediately after creation to trigger CI within seconds, instead of waiting for `ci-trigger-auto-prs.yml` (which could delay CI by ~16 minutes when `cancel-in-progress` cancelled an earlier run). ([#928](https://github.com/cartland/battery-butler/pull/928))
- **Mermaid files in docs_only filter**: Added `**/*.mmd` to the `docs_only` path filter in `ci.yml` so diagram-only PRs skip full CI. ([#928](https://github.com/cartland/battery-butler/pull/928))

### Testing

- **Close Android Tier 2 screenshot gaps**: Added filled-form previews for AddDevice and AddDeviceType screens (via `initialName`/`initialLocation` parameters) and loading-state preview for EditBatteryEvent. Added corresponding screenshot test wrappers. Updated parity matrix in `SCREENSHOT_STRATEGY.md`. Added `validate-android-screenshots.sh` script for local screenshot validation.

### Releases

- **Android release `android/27`**: Released on commit `af8049cd`.

---

## 2026-03-08

### Linting

- **Exhaustive `when` enforcement**: Enabled the `ElseCaseInsteadOfExhaustiveWhen` detekt rule to discourage `else ->` on sealed/enum `when` expressions. Fixed one `BatchOperationResult` case to list all variants explicitly and added `@Suppress` annotations to 5 intentional catch-all usages. The rule requires type resolution (enforced in IDEs; CI enforcement planned when KMP type-resolution detekt matures).

### Fixes

- **safeStateIn error recovery**: `safeStateIn` now accepts an optional `onError` callback so Flow errors emit an error state instead of leaving the UI stuck at its initial value (permanent loading spinner). Added `Error` variants to `HistoryListUiState` and `DeviceTypeListUiState`, and `error: String?` to `HomeUiState`. ViewModels using `safeStateIn` now surface errors to the UI. CrashProof tests updated to assert error states.

### Documentation

- **Engineering Goals document**: Created `docs/ENGINEERING_GOALS.md` — a living reference organized around four pillars (API Design, Architecture, Testing, Custom Enforcement). Each pillar lists principles, achieved techniques with file locations, aspirational goals, and key references. Includes a techniques inventory table and cross-reference index mapping topics to their primary docs, ADRs, and enforcement mechanisms.

### Refactoring

- **DeviceRepository Result<D, E> migration**: Migrated all 9 `DeviceRepository` suspend functions from throwing to returning `Result<Unit, DataError>`. Try-catch is now confined to the `DefaultDeviceRepository` boundary (Room/SQLite). All 15+ UseCases propagate Result, ViewModels use `when` on Result instead of try-catch, and CrashProof tests verify error handling via `Result.Error` instead of intercepting exceptions. Establishes `Result<D, E>` as the standard API pattern per `AuthRepository` exemplar.

### Fixes

- **Broken Mermaid sankey diagram**: Fixed the Mermaid sankey diagram in README.md and CODE_ANALYSIS.md that GitHub couldn't render. The `SankeyChartGenerator` was emitting a `%% GENERATED FILE` comment before the YAML frontmatter `---` block, but Mermaid requires `---` to be the first line. Moved the comment after the frontmatter closing delimiter. Added buildSrc unit test and validate-generated.sh check to prevent regression.

### Features

- **iOS Login NotConfigured snapshot**: Added snapshot test for Login screen's NotConfigured state (sign-in unavailable, only "Skip for now" visible). Confirmed SwiftUI `.alert()` is not captured in snapshot tests, so Login Error remains an accepted framework limitation. iOS coverage now 94 PNGs (47 test functions).

- **iOS dark mode snapshots**: Added `.preferredColorScheme(.dark)` variants to all 46 iOS snapshot tests, doubling coverage from 46 to 92 PNGs (light + dark). Closes the biggest systemic gap in the screenshot strategy (`bb-b7t2`). Each test function now produces both `testName.light.png` and `testName.dark.png`.

- **iOS DeviceTypeDetail screen** ([#911](https://github.com/cartland/battery-butler/pull/911)): Added the last missing iOS SwiftUI screen — a read-only detail view for device types. Includes centered icon header, battery info card, navigable device list, and Edit toolbar button opening a sheet. Changed DeviceTypeList navigation to go through detail (matching Compose flow). 3 snapshot tests with reference images. Closes `bb-tgd6`.

- **iOS EditBatteryEvent screen** ([#907](https://github.com/cartland/battery-butler/pull/907)): Implemented the EditBatteryEvent iOS SwiftUI screen. Added ViewModelWrapper (KMP-to-SwiftUI bridge with Date/Instant conversion), Screen + ContentView (DatePicker, battery type/notes fields, delete confirmation), Edit button on EventDetailScreen with sheet navigation, and 3 snapshot tests with reference images.

## 2026-03-07

### Documentation

- **Agent instruction restructuring** ([#899](https://github.com/cartland/battery-butler/pull/899)): Split monolithic `.agent/AGENTS.md` (701→292 lines) and `.agent/project.md` (644→293 lines) into focused topic modules. Created 5 new sub-files: `merge-strategy.md`, `testing.md`, `ci.md`, `ios.md`, `server.md`. Agents now read topic files on-demand instead of ~1,350 lines upfront. Fixed broken `.agent/rules.md` references, added File Index tables, and deduplicated safety rules.

## 2026-03-06

### Features

- **iOS "Sage & Linen" design language** ([#873](https://github.com/cartland/battery-butler/pull/873)): Implemented the full iOS design system from `docs/design/IOS_DESIGN_LANGUAGE.md`. Added design tokens (`ButlerColors` with 20+ semantic color roles supporting light/dark, `ButlerSpacing`, `ButlerCornerRadius`, `ButlerIconSize`), reusable components (`ButlerIconBox`, `SFSymbolMapper` mapping 37 device icons, `BatteryAgeHelper` with 3-tier age coloring), and migrated all 13 screens from hardcoded `.blue`/`.green`/`.accentColor` to themed tokens. Key visual improvements: DeviceRow shows mapped icons + battery age trailing, HistoryListScreen has calendar badges replacing raw ISO timestamps, AiChat uses sage green/steel blue themed bubbles. Feature parity lifted from ~40% to ~50%.

### Documentation

- **Sankey chart for code analysis** ([#878](https://github.com/cartland/battery-butler/pull/878)): Added a Mermaid Sankey diagram to `docs/CODE_ANALYSIS.md` visualizing two-level code distribution (Codebase → platform categories → individual modules). Renamed file from `Code_Share_Analysis.md` to `CODE_ANALYSIS.md` to match project naming conventions, updating all references across CI, scripts, and docs.

- **iOS design language specification** ([#871](https://github.com/cartland/battery-butler/pull/871)): Added `docs/design/IOS_DESIGN_LANGUAGE.md` defining the complete visual identity — color palette, spacing scale, corner radii, icon sizes, 6 component patterns, battery age coloring, SF Symbol mapping table, typography mapping, and anti-patterns.

## 2026-03-05

### Features

- **iOS ContentView extraction** ([#862](https://github.com/cartland/battery-butler/pull/862)): Extracted stateless `ContentView` structs from all 10 remaining iOS SwiftUI screens (EventDetail, HistoryList, Settings, Login, AiChat, AddDeviceType, EditDeviceType, AddBatteryEvent, DeviceTypeList, Home). All 13 screens now follow the two-layer Screen/ContentView pattern. Added 27 snapshot tests covering all ContentViews. Screens with navigation destinations use generic type parameters (e.g., `HomeContentView<DeviceDestination, SettingsDestination, AiDestination>`) so tests can inject stubs.

### CI/CD

- **Release build verification** ([#857](https://github.com/cartland/battery-butler/pull/857)): Added `release-build-on-green.yml` that builds a signed release AAB after every green CI on main. Proves the release pipeline (signing, bundling, Gradle config) works without deploying. Artifacts uploaded for 30 days.

- **Pre-release CI gate** ([#854](https://github.com/cartland/battery-butler/pull/854)): `release-android.yml` now verifies CI passed on the tagged commit before building. `release-android.sh` also checks CI status locally before creating tags.

- **iOS snapshot tests in CI** ([#853](https://github.com/cartland/battery-butler/pull/853)): Added `xcodebuild test` step to `validation_ios_ui` CI job and `validate.sh` macOS block, executing iOS snapshot tests alongside the existing compilation step.

- **CI concurrency fix** ([#856](https://github.com/cartland/battery-butler/pull/856)): Push-to-main CI runs now use SHA-based concurrency groups so rapid merges don't cancel each other. PR runs still cancel stale runs on the same branch.

### Fixes

- **iOS test data fix** ([#860](https://github.com/cartland/battery-butler/pull/860)): Fixed test `Device` using `KotlinLong` (raw milliseconds) instead of `KotlinInstant` for `batteryLastReplaced`, which caused a crash when the snapshot test tried to format the date.

### Testing

- **iOS snapshot test gallery** ([#855](https://github.com/cartland/battery-butler/pull/855)): Expanded iOS snapshot test coverage to all screens with a gallery generator script. Created placeholder test files for all screens lacking ContentView extraction.

---

## 2026-03-03

### Documentation

- **Feature parity gap audit**: Rewrote `docs/FEATURE_PARITY_MAPPING.md` with per-feature assessment across all 14 screens. Screen parity is 100% but feature parity is ~40% — most SwiftUI screens are minimal implementations missing sorting, grouping, error handling, icons, and interactive features. Updated `docs/UI_SCREENS_MAPPING.md` with gap annotations per section and added Settings (Section 7). Created 5 new tracking beads under epic `bb-rrs4`.

---

## 2026-03-02

### Features

- **Split-screen AI chat** ([#831](https://github.com/cartland/battery-butler/pull/831)): Replaced the z-stacked AI chat overlay with a Column-based split-screen layout. Chat and tab content now share the screen — content shrinks via `Modifier.weight(1f)` as chat expands from the bottom. Added viewport-resize scroll anchoring in `AiTabContent` to keep newest messages visible during layout changes.

- **Predictive back gesture for AI chat** ([#826](https://github.com/cartland/battery-butler/pull/826)): Added `PredictiveBackHandler` (expect/actual in `presentation-core`) so the AI chat panel smoothly tracks the back gesture progress on Android 13+. The chat shrinks as the user swipes and fully collapses on commit, or restores on cancel. Removed the old `BackHandler` expect/actual from `compose-app` (now redundant).

### Refactoring

- **Unified navigation back stack** ([#828](https://github.com/cartland/battery-butler/pull/828)): Merged dual NavDisplay stacks (tabBackStack + detailBackStack with two layered NavDisplays) into a single unified `backStack` with one `NavDisplay`. Login sits on top of Devices at launch and is removed after auth. Added `showChrome` parameter to `MainScreenShell` to conditionally hide top bar, bottom nav, and AI panel for non-tab screens.

### CI/CD

- **Test coverage enforcement plugin** ([#837](https://github.com/cartland/battery-butler/pull/837)): New `checkTestCoverage` Gradle task that scans `usecase`, `viewmodel`, `data`, and `ai` modules for classes matching enforced patterns and fails the build when they lack corresponding test files. Supports inline `// @NoTestRequired: reason` suppression and central `test-coverage-exemptions.txt`. Added to `validate.sh` and CI as a fast job.

### Releases

- **Android `android/26`**: Includes split-screen AI chat (#831), unified back stack (#828), predictive back gesture (#826), test coverage plugin (#837), and dependency bumps (protobuf 4.34.0, ktor 3.4.0, awsSdk 2.42.4).

---

## 2026-03-01

### Features

- **AI CRUD tools** ([#815](https://github.com/cartland/battery-butler/pull/815)): Added 6 new AI tools (updateDevice, deleteDevice, updateDeviceType, deleteDeviceType, updateBatteryEvent, deleteBatteryEvent) for full CRUD capability in the AI chat. Entity IDs are now exposed in the AI context so the model can precisely target items. System prompt enforces delete confirmation. deleteDeviceType has referential integrity (blocks if devices still use it). Event mutations recalculate device `batteryLastReplaced`.

### Fixes

- **Snap AI overlay height to remove IME desync** ([#822](https://github.com/cartland/battery-butler/pull/822)): Replaced `animateFloatAsState` with a plain value for the overlay height fraction. The spring animation (~500-700ms) was desyncing from the system keyboard animation (~300ms), causing the overlay panel to visually lag behind the input row. Snapping the fraction while `imePadding()` smoothly animates the available space produces a smooth result with zero lag.

### Testing

- **Full-height AI overlay screenshot test** ([#821](https://github.com/cartland/battery-butler/pull/821)): Added screenshot test for the full-height AI overlay variant. Uses parameter hoisting on `MainScreenShell` (`imeVisible: Boolean`) to allow previews to render both half-height and full-height states without modifying production code paths.

### Releases

- **Android `android/25`**: Includes AI overlay IME desync fix (#822).

---

## 2026-02-28

### Features

- **Predictive back support** ([#808](https://github.com/cartland/battery-butler/pull/808)): Enabled Android 13+ predictive back gesture via `enableOnBackInvokedCallback` manifest flag. Fixed a bug where back on the Login screen would pop the detail stack and reveal unauthenticated tab content — now exits the app. Documented the back gesture contract for all screens in `docs/TESTING.md`.

### Infrastructure

- **Hibernate AWS infrastructure** ([#794](https://github.com/cartland/battery-butler/pull/794)): Stopped all AWS spending. Added "(disabled)" labels to cloud server options in the mobile app, reordered network modes (GrpcLocal first), added hibernation comments to `gradle.properties`, updated all docs with hibernation notices and re-enabling checklist. Added screenshot test for all network modes expanded.

- **Fix server workflow disabling** ([#796](https://github.com/cartland/battery-butler/pull/796)): Switched from `if: false` on workflow jobs (which caused GitHub to report failures) to `gh workflow disable` (prevents workflows from triggering at all). All 6 server workflows disabled at the GitHub level.

### CI/CD

- **Make server connectivity check non-blocking** ([#793](https://github.com/cartland/battery-butler/pull/793)): Changed "Validate Server Connectivity" step in `release-android.yml` from a blocker to a warning, since AWS endpoints are down during hibernation.

- **Regenerate architecture diagrams** ([#795](https://github.com/cartland/battery-butler/pull/795)): Auto-generated architecture diagram and analysis update following hibernation changes.

### Testing

- **Close the loop — test gap coverage** ([#787](https://github.com/cartland/battery-butler/pull/787)): Added 18 new tests filling critical coverage gaps: `AiChatViewModelTest` (7 tests — send, blank/processing guards, clearChat, hint augmentation), `EditDeviceTypeViewModelTest` (5 tests — load/notfound/update/delete), `BatchAddDeviceTypesUseCaseTest` (3 tests — tool handler, deduplication, missing name), `BatchAddBatteryEventsUseCaseTest` (3 tests — device+event creation, missing fields). All 13 ViewModels now have dedicated test classes.

- **ViewModel convention enforcement** ([#787](https://github.com/cartland/battery-butler/pull/787)): Added `ViewModelTestConventionTest` (in `viewmodel/src/desktopTest/`) — reflection-based scan that ensures every `*ViewModel` class has a corresponding `*ViewModelTest`. Prevents future ViewModels from being added without tests. Required adding `**/desktopTest/**` to detekt FunctionNaming excludes.

### Documentation

- **User Journeys** ([#787](https://github.com/cartland/battery-butler/pull/787)): Created `docs/USER_JOURNEYS.md` documenting 16 user-reachable paths through the app with Screen.kt references, prerequisites, and edge cases. Cross-referenced from FEATURES.md and Navigation.md.

- **Coverage matrices** ([#787](https://github.com/cartland/battery-butler/pull/787)): Added three sections to `docs/TESTING.md`: "What Passing Tests Prove" (confidence narrative), screen coverage matrix (13 screens mapped to ViewModel/screenshot/instrumented tests), and business rule coverage matrix (20 rules mapped to tests).

- **Feature-test-journey linking** ([#787](https://github.com/cartland/battery-butler/pull/787)): Updated `docs/FEATURES.md` with Journey and Tests columns linking every feature to its user journey and test class. Updated ADR-002 with accurate coverage numbers (13/13 ViewModels, 19/28 UseCases).

- **PR template** ([#787](https://github.com/cartland/battery-butler/pull/787)): Added `.github/PULL_REQUEST_TEMPLATE.md` with checklist for updating docs/tests when features change.

### Testing (prior session, same day)

- **UseCase test coverage phase 1** ([#786](https://github.com/cartland/battery-butler/pull/786)): Added ~40 tests across 13 new test files covering UseCase, AI, and ViewModel utility layers. Created shared `FakeAiEngine` in test-common. Added `docs/TESTING.md` with project testing principles (test real code, test behavior not implementation, one concept per test).

---

## 2026-02-27

### Testing

- **Navigate-all-screens smoke test** ([#774](https://github.com/cartland/battery-butler/pull/774)): Added `testNavigateAllScreens()` instrumented test that visits 12 of 13 app screens (skipping feature-flagged AiChat) to catch rendering crashes during navigation. Uses UI buttons (Cancel/Done/Back arrow) instead of `Espresso.pressBack()` to avoid BackHandler priority conflicts between the tab and detail NavDisplay stacks.

---

## 2026-02-26

### Features

- **iOS SwiftUI Edit Device** ([#772](https://github.com/cartland/battery-butler/pull/772)): Added native SwiftUI `EditDeviceScreen` and `EditDeviceViewModelWrapper` to achieve complete feature parity with the Compose Multiplatform version. The app now supports modifying device names, locations, and types directly from the native iOS UI.

### Documentation

- **Feature Parity Mapping** ([#772](https://github.com/cartland/battery-butler/pull/772)): Created `docs/FEATURE_PARITY_MAPPING.md` to track and document the screen equivalence between Android CMP, iOS CMP, and the native iOS SwiftUI implementations.

---

## 2026-02-23

### Features

- **AI overlay redesign** ([#613](https://github.com/cartland/battery-butler/pull/613)): Replaces the collapsed read-only AI field with an always-visible interactive `OutlinedTextField` + send `IconButton` in the bottom bar. The `AnimatedVisibility` overlay now only slides up/down the chat history (input stays fixed). Back button dismisses the overlay. Tab transitions now slide left/right based on tab index instead of cross-fading. `BackHandler(enabled = isAiExpanded)` intercepts back presses before the nav stack.

- **Static shell during tab transitions** ([#616](https://github.com/cartland/battery-butler/pull/616)): Hoisted `MainScreenShell` above the `NavDisplay` so the top bar, AI input bar, and nav tabs stay fixed while only the content area slides during tab transitions. Previously the entire screen (including shell) animated on tab switches and predictive back.

- **AI chat tap-to-open** ([#616](https://github.com/cartland/battery-butler/pull/616)): Added `onFocusChanged` to the AI `OutlinedTextField` so tapping the field opens the chat overlay immediately (not just on send).

- **Standard dropdown component** ([#618](https://github.com/cartland/battery-butler/pull/618)): Added `ButlerDropdownMenu` wrapper in `presentation-core/components/` as the single place to customize dropdown behavior across the app.

- **Theme-aware icon colors** ([#618](https://github.com/cartland/battery-butler/pull/618)): Replaced hardcoded `IconAccent` light/dark color pairs with `IconColorRole` enum mapping semantic icon categories to `MaterialTheme.colorScheme` container colors. Icons now adapt automatically to theme changes. `IconAccent.kt` deleted.

- **UseCase convention test** ([#630](https://github.com/cartland/battery-butler/pull/630)): Added `UseCaseConventionTest` — a JVM reflection-based test that scans all `*UseCase` classes and asserts each has `operator fun invoke`. Caught and fixed a real violation: `BuildAiContextUseCase` was using a non-invoke `buildContext()` method.

### Fixes

- **Null safety in navigation** ([#613](https://github.com/cartland/battery-butler/pull/613)): `navigateToDevices` now uses `backStack.lastOrNull()` instead of `backStack.last()` to prevent `NoSuchElementException` if the backstack is empty.

- **Chat history overflow** ([#618](https://github.com/cartland/battery-butler/pull/618)): AI overlay `Surface` now applies bottom padding from `innerPadding`, preventing chat messages from rendering behind the navigation bar.

- **Devices screen spacing** ([#616](https://github.com/cartland/battery-butler/pull/616)): Set `contentWindowInsets = WindowInsets(0,0,0,0)` on the inner Scaffold to prevent double-applying insets when nested inside a `NavDisplay` entry.

---

## 2026-02-22

### Refactoring

- **Theme SRP cleanup** ([#589](https://github.com/cartland/battery-butler/pull/589)): Extracted `IconAccent` data class and category vals from `Color.kt` into `IconAccent.kt`. Introduced `ButlerColors` data class + `LocalButlerColors` composition local for custom app colors beyond Material3. Battery warning amber color now uses `LocalButlerColors.current.batteryWarning` instead of inline `isSystemInDarkTheme()` check.

### Fixes

- **Screenshot OOM guard** ([#589](https://github.com/cartland/battery-butler/pull/589)): Added `doFirst` guard to `updateDebugScreenshotTest` and `validateDebugScreenshotTest` that blocks all-at-once runs by default. Developers are directed to `generate-screenshots-sequentially.sh`, with `-PforceAllScreenshots` as an opt-in escape hatch.

### Documentation

- **UI Screens Mapping**: Added `docs/UI_SCREENS_MAPPING.md` detailing the mapping between the shared Compose Multiplatform UI and the native SwiftUI implementation. Explains the structural asymmetry between Compose's `MainScreenShell` vs SwiftUI's `TabView` architecture.

---

## 2026-02-21

### Features

- **Dev Server network mode** ([#560](https://github.com/cartland/battery-butler/pull/560)): Added `GrpcDev` variant to `NetworkMode` so users can switch between Prod Server, Dev Server, Local, Mock, and None in Settings. Includes `DevServerUrl` domain wrapper, `BuildConfig.DEV_SERVER_URL` generation, DI wiring, and CI workflow support. Renamed "AWS Cloud" label to "Prod Server".

- **Check for updates in Settings** ([#554](https://github.com/cartland/battery-butler/pull/554)): Added a "Check for updates" card in Settings that opens the Google Play Store listing via `LocalUriHandler`. Appears between Export Data and App Version.

### Fixes

- **Add cards hard to reach at bottom of lists** ([#554](https://github.com/cartland/battery-butler/pull/554)): Moved "Add" cards from the bottom to the top of LazyColumn in Home (devices), Device Types, and History screens so they're always visible without scrolling through long lists.

- **AI tab chat input excessive bottom padding** ([#554](https://github.com/cartland/battery-butler/pull/554)): Removed redundant `WindowInsets.navigationBars` from the AI tab input Row. The parent Scaffold already includes navigation bar insets via `innerPadding`, which was doubling the bottom space.

- **ServerSyncMapper notes and imagePath bugs** ([#559](https://github.com/cartland/battery-butler/pull/559)): Fixed `notes` mapping converting empty proto string to empty string instead of `null`, and `imagePath` field being omitted entirely in both `ProtoDevice.toDomain()` and `Device.toProto()`.

---

## 2026-02-16

### Refactoring

- **Move AI types to domain module** ([#470](https://github.com/cartland/battery-butler/pull/470)): Moved AI vocabulary types (`AiEngine`, `AiMessage`, `AiRole`, `ToolHandler`, `AiToolNames`, `AiToolParams`, `AiConstants`) from `:ai` to `:domain:model:ai`. Eliminates architecture violations where `:usecase` and `:viewmodel` depended on `:ai`. Updated architecture check to enforce the new dependency rules.

- **Add DispatcherProvider** ([#476](https://github.com/cartland/battery-butler/pull/476)): Introduced `DispatcherProvider` interface in `:domain` and `DefaultDispatcherProvider` implementation in `:data` to replace hardcoded `Dispatchers.Default` in `ExportDataUseCase`. Improves testability by allowing tests to inject `UnconfinedTestDispatcher`.

### Fixes

- **Time-dependent screenshot tests** ([#465](https://github.com/cartland/battery-butler/pull/465)): `DevicesScreen` and `AddBatteryEventContent` previews were rendering live dates via `Clock.System.now()`, causing screenshot baselines to drift on every CI run. Added `nowInstant` parameter to `DevicesScreen` (forwarded through the composable chain) and `initialDate` default parameter to `AddBatteryEventContent`. Previews now use fixed instants matching the project standard (`2026-01-18T17:00:00Z`).

### CI/CD Improvements

- **Narrower CI path filters** ([#465](https://github.com/cartland/battery-butler/pull/465)): Refined `dorny/paths-filter` patterns so module README files (e.g., `domain/README.md`) don't trigger the full build matrix. Added `.claude/**` to the docs-only filter.

- **Clearer auto-generated PR titles** ([#466](https://github.com/cartland/battery-butler/pull/466)): Auto-generated PRs now use `(generated)` conventional commit scope — `chore(generated): Regenerate screenshot baselines` and `docs(generated): Regenerate architecture diagrams and analysis` — making them instantly recognizable in git log.

---

## 2026-02-15

### Fixes

- **P0: Release builds using stale server URL** ([#459](https://github.com/cartland/battery-butler/pull/459)): `release-android.yml` was missing the `ORG_GRADLE_PROJECT_PRODUCTION_SERVER_URL` env var, causing every release build to use the `gradle.properties` fallback instead of the GitHub secret. Added the env var to match `ci.yml`.

- **Hardcoded NLB hostnames** ([#459](https://github.com/cartland/battery-butler/pull/459)): Eliminated literal NLB hostnames from `SettingsViewModel` and `RemoteConnectivityTest`. Introduced `ProductionServerUrl` data class in the domain module for type-safe DI injection, provided by `AppComponent` and `NativeComponent` via `BuildConfig`.

- **Architecture check: allow :viewmodel -> :ai** ([#459](https://github.com/cartland/battery-butler/pull/459)): Added `:ai` to `:viewmodel`'s allowed dependencies. The viewmodel needs `AiMessage`/`AiRole` types exposed in use case return types — this is a type-reference dependency, not a logic dependency.

### CI/CD Improvements

- **Auto-sync terraform output to GitHub secrets** ([#459](https://github.com/cartland/battery-butler/pull/459)): Deploy workflows now capture the NLB DNS name after `terraform apply` and sync it to GitHub secrets (`PRODUCTION_SERVER_URL` for prod, `DEV_SERVER_URL` for dev). Uses `BOT_PAT` with `continue-on-error` so failed syncs don't fail deploys.

- **Server connectivity validation before release** ([#459](https://github.com/cartland/battery-butler/pull/459)): Added a health check step to `release-android.yml` that verifies the production server is reachable before uploading to Play Store. Catches stale URLs before they reach users.

### Documentation

- **Server URL management docs** ([#459](https://github.com/cartland/battery-butler/pull/459)): Added comprehensive documentation to `.agent/project.md` explaining how the server URL flows from terraform through GitHub secrets to BuildConfig. Updated `AGENTS.md` configuration rules, `server/README.md` secrets table, and module READMEs.

- **Update project docs skill** ([#460](https://github.com/cartland/battery-butler/pull/460)): Added Claude Code skill and agent workflow for systematically updating project documentation after code changes.

---

## 2026-02-07

### Features

- **Multi-environment server deployment**: Implemented build-once, deploy-many pipeline with dev (auto), staging (manual), and prod (manual + approval gate) environments ([#405](https://github.com/cartland/battery-butler/pull/405), [#407](https://github.com/cartland/battery-butler/pull/407)) - Same Docker image SHA is promoted through environments unchanged.

- **Server destroy workflow**: Added `server-destroy.yml` to tear down staging or dev infrastructure on demand, with production explicitly blocked ([#411](https://github.com/cartland/battery-butler/pull/411)) - Needed for managing AWS free-tier RDS instance limits.

### CI/CD Improvements

- **CI path filtering for non-code files**: Excluded `server/*.json` and `server/*.md` from the CI code filter so beads-only and docs-only PRs skip expensive builds ([#413](https://github.com/cartland/battery-butler/pull/413)) - Reduces CI time from ~15min to ~30s for documentation changes.

- **Terraform state lock prevention**: Removed ECR from Terraform management (now a `data` source), added concurrency groups to all deploy workflows, and removed the error-prone import step ([#407](https://github.com/cartland/battery-butler/pull/407)) - Eliminates the root cause of repeated DynamoDB state lock issues.

- **Free-tier compatible infrastructure**: Downgraded staging and prod Terraform configs to `db.t3.micro` for AWS free-tier compatibility ([#409](https://github.com/cartland/battery-butler/pull/409), [#410](https://github.com/cartland/battery-butler/pull/410)).

### Fixes

- **Comprehensive IAM permissions**: Audited all Terraform resources and added missing IAM permissions for tag management, inline policies, and ECS operations ([#408](https://github.com/cartland/battery-butler/pull/408)) - Previous deploys failed iteratively due to missing permissions.

- **Terraform import hanging**: Fixed `terraform import` hanging by passing `-var-file` to prevent interactive prompts ([#402](https://github.com/cartland/battery-butler/pull/402)).

### Documentation

- **Updated CLAUDE.md**: Added server deployment documentation, CI path filtering notes, and updated session resume points ([#413](https://github.com/cartland/battery-butler/pull/413)).

- **Agent priority P0.5**: Added priority level for instruction/beads PRs that should be fast-tracked ([#404](https://github.com/cartland/battery-butler/pull/404)).

---

## 2026-02-01

### CI/CD Improvements

- **Auto-PR cleanup strategy**: Workflows now automatically close stale auto-generated PRs before creating new ones ([#180](https://github.com/cartland/battery-butler/pull/180)) - Prevents accumulation of outdated screenshot/diagram PRs that can have merge conflicts.

- **Strict screenshot validation**: The validation script now fails (not just warns) when screenshots are broken 1x1 pixel images ([#177](https://github.com/cartland/battery-butler/pull/177)) - Previously broken screenshots could slip through CI undetected.

- **OIDC authentication for AWS**: Server deployments now support OIDC authentication with fallback to access keys ([#176](https://github.com/cartland/battery-butler/pull/176)) - More secure than long-lived access keys, follows AWS best practices.

- **Scheduled diagram updates**: Added daily cron job to keep architecture diagrams fresh ([#173](https://github.com/cartland/battery-butler/pull/173)) - Prevents large diagram diffs from accumulating over time.

### Fixes

- **Fixed screenshot test stability**: Added `nowInstant` parameter to history-related composables so screenshot tests use fixed dates ([#179](https://github.com/cartland/battery-butler/pull/179)) - Screenshots previously showed "X days ago" that changed daily, causing CI failures.

- **Fixed string resources in previews**: Changed imports to use project's `composeStringResource()` wrapper that uses `LocalAppStrings` ([#174](https://github.com/cartland/battery-butler/pull/174)) - Fixes 6 broken screenshots that were rendering as 1x1 pixels because string resources couldn't resolve in screenshot test context.

### Refactoring

- **Runtime API key injection**: Moved GEMINI_API_KEY from compile-time BuildConfig to runtime `AiConfig` interface ([#175](https://github.com/cartland/battery-butler/pull/175)) - Better separation of concerns, allows different configurations per environment.

---

## 2026-01-31

### CI/CD Improvements

- **Screenshot baseline cleanup**: Workflows now delete old baselines before regenerating ([#168](https://github.com/cartland/battery-butler/pull/168)) - Prevents orphaned screenshots when tests are renamed or deleted.

- **iOS build caching**: Added Xcode DerivedData caching ([#167](https://github.com/cartland/battery-butler/pull/167)) - Significantly speeds up iOS CI builds.

- **Ubuntu for update workflows**: Switched diagram/screenshot update workflows from macOS to Ubuntu ([#166](https://github.com/cartland/battery-butler/pull/166)) - Faster and cheaper CI runs.

- **Build timeouts**: Added `timeout-minutes` to all CI jobs ([#164](https://github.com/cartland/battery-butler/pull/164)) - Prevents hung builds from consuming CI minutes indefinitely.

### Documentation

- **CI architecture docs**: Added comprehensive documentation of CI/CD architecture and improvement plan ([#163](https://github.com/cartland/battery-butler/pull/163))

- **PR merge workflow**: Documented rules for PR merge priorities and task tracking ([#160](https://github.com/cartland/battery-butler/pull/160))

- **Agent self-improvement**: Added instructions for AI agents to update CLAUDE.md with learned best practices ([#159](https://github.com/cartland/battery-butler/pull/159))

### Fixes

- **Bazel disk cache**: Fixed issue where Bazel outputs weren't materialized when called from Xcode scripts ([#157](https://github.com/cartland/battery-butler/pull/157)) - Use `--disk_cache=""` to ensure files are created locally.

- **Duplicate Gradle module**: Removed duplicate `:server:app` entry in settings.gradle.kts ([#156](https://github.com/cartland/battery-butler/pull/156))

---

## 2026-01-30

### Performance

- **LazyColumn optimization**: Added `key` parameter to LazyColumn items for stable recomposition ([#151](https://github.com/cartland/battery-butler/pull/151)) - Improves scroll performance and prevents unnecessary recompositions.

- **Memory leak detection**: Added LeakCanary for debug builds ([#150](https://github.com/cartland/battery-butler/pull/150)) - Helps identify memory leaks during development.

### Accessibility

- **iOS decorative icons**: Hide decorative device icon from VoiceOver ([#146](https://github.com/cartland/battery-butler/pull/146))

- **Content descriptions**: Improved content descriptions for icons across the app ([#144](https://github.com/cartland/battery-butler/pull/144))

### Refactoring

- **Scoped dependency injection**: Introduced `AppDataModule` for better DI organization ([#143](https://github.com/cartland/battery-butler/pull/143))

### Fixes

- **Sort null handling**: Added null fallback for TYPE sort in HomeViewModel ([#140](https://github.com/cartland/battery-butler/pull/140))

- **JVM database migration**: Added missing MIGRATION_4_5 to JVM DatabaseFactory ([#139](https://github.com/cartland/battery-butler/pull/139))

- **Lifecycle-aware state**: Use `collectAsStateWithLifecycle` for proper lifecycle awareness ([#138](https://github.com/cartland/battery-butler/pull/138))

---

## 2026-01-29

### Features

- **Network permissions**: Added explicit INTERNET and ACCESS_NETWORK_STATE permissions for Play Store compliance ([#125](https://github.com/cartland/battery-butler/pull/125))

- **EmptyState preview**: Added preview for EmptyStateContent component ([#137](https://github.com/cartland/battery-butler/pull/137))

### Fixes

- **UI text overflow**: Added text overflow handling to EmptyStateContent ([#136](https://github.com/cartland/battery-butler/pull/136))

- **Keyboard handling**: Fixed keyboard behavior in AddBatteryEventContent ([#135](https://github.com/cartland/battery-butler/pull/135))

- **Navigation issues**: Modernized iOS navigation patterns and added rapid-click protection ([#134](https://github.com/cartland/battery-butler/pull/134), [#133](https://github.com/cartland/battery-butler/pull/133))

- **Icon suggestion debounce**: Added debouncing to prevent rapid API calls ([#132](https://github.com/cartland/battery-butler/pull/132))

- **Server sync**: Fixed missing DeviceType fields and timestamp preservation in ServerSyncMapper ([#131](https://github.com/cartland/battery-butler/pull/131), [#129](https://github.com/cartland/battery-butler/pull/129))

- **Day pluralization**: Fixed "1 days" → "1 day" in device list and history ([#130](https://github.com/cartland/battery-butler/pull/130))

### Refactoring

- **iOS modernization**: Updated Swift patterns and fixed deprecations ([#127](https://github.com/cartland/battery-butler/pull/127))

- **Kotlin data objects**: Converted singleton objects to data objects (Kotlin 2.0+) ([#126](https://github.com/cartland/battery-butler/pull/126))

---

## Earlier History

For changes before January 29, 2026, see the [commit history](https://github.com/cartland/battery-butler/commits/main) and [closed PRs](https://github.com/cartland/battery-butler/pulls?q=is%3Apr+is%3Aclosed).
