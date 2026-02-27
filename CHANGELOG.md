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
