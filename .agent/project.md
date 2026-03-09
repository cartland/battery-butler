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

**`Result<D, E>` is the standard API pattern** for all fallible operations. Try-catch should only appear at external dependency boundaries (Room/SQLite, gRPC, AI engine). Exemplar: `AuthRepository.signIn()` → `Result<User, AuthError>`.

Key types (see `domain/model/Result.kt`):
- `Result<D, E : AppError>` — sealed interface with `Success<D>` and `Error<E>`
- `DataError` — sealed hierarchy: Network, Database, Ai, Unknown (in `DataResult.kt`)
- `DataResult<T>` — **deprecated**, use `Result<T, DataError>` instead
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
- **`MainTab.AI`** enum value remains in the codebase but is dead code — the AI is now a split-screen panel, not a nav tab.
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
