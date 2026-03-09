# Engineering Goals

A living reference documenting what we believe about software engineering, where we are, and where we're headed. Organized around four pillars.

> **Relationship to other docs:** ADRs record *what we decided*; this document articulates *what we believe and where we're headed*. It links to — but never duplicates — existing docs.

---

## Pillar 1 — API Design

*Easy to understand, hard to break.*

### Principles

- **Errors are values, not exceptions.** Callers handle errors explicitly via `Result<D, E>` — never via try-catch except at external dependency boundaries.
- **Sealed hierarchies make invalid states unrepresentable.** Exhaustive `when` on sealed types means the compiler catches missing cases at build time.
- **Callable conventions reduce ceremony.** `operator fun invoke()` on use cases means call sites read like plain functions.
- **Deprecation is a migration tool.** `@Deprecated` with `ReplaceWith` guides callers to the new API; the old API compiles but warns.

### Achieved

| Technique | Implementation | Location |
|-----------|---------------|----------|
| `Result<D, E>` sealed type | `Success<D>` / `Error<E>` with `map`, `flatMap`, `getOrNull`, `onSuccess`, `onError` | `domain/src/commonMain/.../model/Result.kt` |
| Sealed error hierarchies | `DataError` → `Network`, `Database`, `Ai`, `Unknown` subtypes | `domain/src/commonMain/.../model/DataResult.kt` |
| `AuthError` sealed hierarchy | `ConfigurationNotConfigured`, `SignInFailed`, `SignOutFailed` | `domain/src/commonMain/.../model/AuthError.kt` |
| Try-catch only at boundaries | Room/SQLite in `DefaultDeviceRepository`, gRPC in network layer, Gemini in AI engine | `data/src/commonMain/.../DefaultDeviceRepository.kt` |
| `operator fun invoke()` | All use cases enforce callable convention | `usecase/src/commonMain/` (enforced by `UseCaseConventionTest`) |
| Exhaustive `when` on sealed types | ViewModels switch on `Result`, `SyncStatus`, `Screen` | `viewmodel/src/commonMain/` |
| `@Deprecated` with `ReplaceWith` | `DataResult<T>` → `Result<T, DataError>` migration | `domain/src/commonMain/.../model/DataResult.kt` |

### Aspirational

- **`throw` checker Gradle task** — Scan non-boundary modules for `throw` statements (excluding `CancellationException`) and fail the build.
- **`Result.combine()` extension** — Aggregate multiple `Result` values (e.g., parallel fetches) into a single `Result<List<D>, E>`.
- **Exhaustive `when` linter** — Warn when a `when` on a sealed type uses `else` instead of listing all branches (catches accidental catch-all after new subtypes are added).
- **Error code catalog** — Centralize all `DataError` subtypes into a reference table in docs, linking each to its boundary source.

### Key References

- [ADR-001: UseCase Dependencies](architecture/adr-001-usecase-dependencies.md)
- [Error handling patterns in `.agent/project.md`](../.agent/project.md) (§ Error Handling)

---

## Pillar 2 — Architecture

*Single responsibility, good naming, safe refactoring.*

### Principles

- **Modules encode dependency direction.** If module A cannot import module B, no developer can accidentally couple them — the compiler enforces the boundary.
- **Each class has one reason to change.** When a class grows beyond ~150 lines or accumulates unrelated tests, it's time to extract.
- **Names carry intent.** A `FindOrCreateDeviceTypeUseCase` tells you exactly what it does; a `DeviceTypeHelper` does not.
- **Refactoring should be safe.** Convention tests and architecture checks catch regressions that code review alone would miss.

### Achieved

| Technique | Implementation | Location |
|-----------|---------------|----------|
| Module dependency enforcement | `ArchitectureCheckTask` — allowed-dependency map, `GradleException` on violation | `buildSrc/src/main/kotlin/architecture/ArchitectureCheckTask.kt` |
| Theme layer boundary | `ThemeLayerCheckTask` — blocks raw `Color()`, `isSystemInDarkTheme()` in presentation-feature | `buildSrc/src/main/kotlin/themelayer/ThemeLayerCheckTask.kt` |
| Domain purity | `:domain` depends on nothing — pure interfaces and models | `domain/` module (enforced by `ArchitectureCheckTask`) |
| Clean architecture layers | domain → usecase → viewmodel → presentation, with data behind interfaces | `settings.gradle.kts`, `ArchitectureCheckTask` |
| Single responsibility extraction | SyncManager, FindOrCreate use cases, `sortAndGroup()` utility, Screen sealed interface | See [ADR-004](architecture/adr-004-single-responsibility-principle.md) |
| kotlin-inject DI | Constructor injection with `@Inject` / `@Component`, platform-specific wiring | `compose-app/.../di/AppComponent.kt` |
| `DispatcherProvider` abstraction | Interface in domain, `DefaultDispatcherProvider` in data, testable with `UnconfinedTestDispatcher` | `domain/.../model/DispatcherProvider.kt`, `data/.../provider/DefaultDispatcherProvider.kt` |
| ADR documentation | 4 architectural decision records | `docs/architecture/adr-*.md` |

### Aspirational

- **SRP heuristic convention test** — Flag classes exceeding a line count or public-function-count threshold (configurable) as candidates for extraction.
- **`Dispatchers.*` usage checker** — Gradle task that scans for direct `Dispatchers.Default`/`IO`/`Main` usage outside `DefaultDispatcherProvider`, enforcing the abstraction everywhere.
- **Naming convention enforcement** — Detekt or custom rule ensuring repositories end with `Repository`, use cases end with `UseCase`, etc.
- **Module README completeness check** — Verify every module has a `README.md` describing its purpose and public API.

### Key References

- [ADR-001: UseCase Dependencies](architecture/adr-001-usecase-dependencies.md)
- [ADR-004: Single Responsibility Principle](architecture/adr-004-single-responsibility-principle.md)
- [Architecture diagram](Architecture.md)
- [Code Analysis (module size distribution)](CODE_ANALYSIS.md)

---

## Pillar 3 — Testing

*Variety of techniques, practical pre/post merge mix, move quickly.*

### Principles

- **Test real code, not mocks.** Fakes that implement domain interfaces are preferred over mocking frameworks — they catch integration bugs that mocks hide.
- **One concept per test.** A test that checks three behaviors is three tests pretending to be one. Short, focused tests with descriptive names are easier to debug.
- **Pre-merge gates must be fast.** The two-mode CI system lets development PRs run in ~5.5 minutes while release PRs get the full suite.
- **Screenshot tests are documentation.** They capture what the UI looks like, not just that it compiles.

### Achieved

| Technique | Implementation | Location |
|-----------|---------------|----------|
| ~200 unit tests | UseCase, ViewModel, data layer, AI layer, Gradle task unit tests | `*/src/commonTest/`, `*/src/jvmTest/`, `*/src/desktopTest/`, `buildSrc/src/test/` |
| Convention tests (2) | `UseCaseConventionTest` (invoke convention), `ViewModelTestConventionTest` (test file existence) | `usecase/src/jvmTest/`, `viewmodel/src/desktopTest/` |
| Android screenshot tests | 131 PNGs, 74 test functions, 14 files — Tier 1 complete (all screens × key states × light/dark) | `android-screenshot-tests/src/screenshotTestDebug/` |
| iOS snapshot tests | 92 PNGs, 46 test functions, 19 files — Tier 1 complete with dark mode | `ios-app-swift-ui/iosAppSwiftUITests/` |
| Fakes over mocks | `FakeDeviceRepository`, `FakeAuthRepository`, `FakeAiEngine`, `FakeLocalDataSource`, etc. | `test-common/src/commonMain/.../testcommon/` |
| Test data builders | `TestDevices.createDevice()`, `createDeviceType()`, `createBatteryEvent()` with defaults | `test-common/src/commonMain/.../testcommon/TestDevices.kt` |
| iOS test data | `TestData.swift` — Swift test data factory | `ios-app-swift-ui/iosAppSwiftUITests/TestData.swift` |
| CrashProof pattern | Tests that ViewModel `viewModelScope.launch` errors surface as `Result.Error`, not uncaught exceptions | `viewmodel/src/commonTest/.../CrashProof*Test.kt` (4 files) |
| Tiered screenshot strategy | Tier 1 (required), Tier 2 (recommended), Tier 3 (optional) | [SCREENSHOT_STRATEGY.md](SCREENSHOT_STRATEGY.md) |
| Coverage matrices | By-screen and by-business-rule test mapping | [TESTING.md](TESTING.md) |
| Two-mode CI | Development mode (fast checks only) vs release mode (full suite) via `.github/ci-mode.txt` | `.github/workflows/ci.yml` |
| `validate.sh` CI-parity gate | Local script matching CI checks, writes `.validation-passed` marker | `scripts/validate.sh` |
| Test coverage enforcement | `checkTestCoverage` Gradle task — enforced class patterns must have corresponding test files | `buildSrc/src/main/kotlin/testcoverage/TestCoverageCheckTask.kt` |
| Preview coverage check | `checkPreviewCoverage` — all `@Preview` composables must have screenshot tests | `buildSrc/src/main/kotlin/screenshotcoverage/PreviewCoverageCheckTask.kt` |
| Navigate-all-screens smoke test | Instrumented test visiting 12 of 13 screens | `compose-app/src/androidTest/` |

### Aspirational

- **Property-based tests** — Use a property testing library (e.g., kotest-property) for pure functions like `sortAndGroup()`, date formatting, and `Result` extensions.
- **Mutation testing** — Run a mutation testing tool (e.g., Pitest) to measure test effectiveness beyond line coverage.
- **Room DAO integration tests** — Verify database migrations and complex queries against an in-memory SQLite instance.
- **Sealed UiState → @Preview convention test** — Ensure every variant of a sealed `UiState` class has a corresponding `@Preview` function (and thus a screenshot test).
- **Performance regression tests** — Baseline `LazyColumn` scroll performance and `Room` query times, failing on regressions.

### Key References

- [TESTING.md](TESTING.md) — Test principles, coverage matrices, confidence narrative
- [SCREENSHOT_STRATEGY.md](SCREENSHOT_STRATEGY.md) — Tier definitions, parity matrix
- [ADR-002: Test Coverage Strategy](architecture/adr-002-test-coverage-strategy.md)
- [USER_JOURNEYS.md](USER_JOURNEYS.md) — 16 user-reachable paths with screen references

---

## Pillar 4 — Custom Enforcement

*Automate rules when libraries are unavailable.*

### Principles

- **A lint that fails the build is worth more than a paragraph in docs.** If a rule matters, encode it in a check that runs automatically.
- **Choose the right enforcement mechanism for the rule.** Not every rule needs a custom Gradle task — sometimes enabling a detekt rule or writing a convention test is simpler and more maintainable.
- **Error messages must be actionable.** Every violation message includes: file path, line number (when applicable), what's wrong, and how to fix it.
- **Hooks guard the agent, not the human.** `.claude/hooks/` enforce workflow guardrails specific to AI agent sessions; human developers use the same linters and CI checks everyone else does.

### Achieved

| Technique | Implementation | Location |
|-----------|---------------|----------|
| Custom Gradle tasks (4) | `checkArchitecture`, `checkThemeLayer`, `checkTestCoverage`, `checkPreviewCoverage` | `buildSrc/src/main/kotlin/` |
| Convention tests (2) | `UseCaseConventionTest`, `ViewModelTestConventionTest` | `usecase/src/jvmTest/`, `viewmodel/src/desktopTest/` |
| detekt + compose plugin | Kotlin code patterns, naming, complexity, modifier naming/ordering | `detekt.yml` |
| Spotless + ktlint | Formatting, import ordering, whitespace | `build.gradle.kts` (root) |
| Android Lint | Resource issues, API level compat, accessibility | `lint.xml` |
| Git guardrail hooks | 8 guardrails: push validation, push-to-main block, force-push block, squash enforcement, tag block, destructive cmd block, `git -C` block, shell keyword warning | `.claude/hooks/git-guardrails.sh` |
| Mode-aware admin bypass hook | Warns in development mode, blocks in release mode | `.claude/hooks/block-admin-bypass.sh` |
| CI-parity gate | `validate.sh` writes HEAD hash to `.validation-passed`; hook checks before push | `scripts/validate.sh`, `.claude/hooks/git-guardrails.sh` |
| Post-merge generation | Screenshots, diagrams, and analysis auto-generated on `main`, never pushed in PRs | `.github/workflows/auto-generate.yml` |

### Decision Guide — Which Mechanism to Use

This escalation ladder helps choose the right tool for a new rule:

| Step | Mechanism | Best For | Example |
|------|-----------|----------|---------|
| 1 | **Detekt rule** (`detekt.yml`) | Kotlin code patterns available as existing rules | `ForbiddenMethodCall`, `FunctionNaming` |
| 2 | **Custom Gradle task** (`buildSrc/`) | Cross-file regex scanning, dependency graphs, coverage gaps | `ThemeLayerCheckTask`, `ArchitectureCheckTask` |
| 3 | **Convention test** (test source set) | Runtime reflection: "every X has a Y" | `UseCaseConventionTest` |
| 4 | **Claude Code hook** (`.claude/hooks/`) | Agent-specific workflow guardrails | `git-guardrails.sh` |

### Aspirational

- **`throw` checker Gradle task** — Scan non-boundary modules for `throw` (excluding `CancellationException`). Boundaries: `data/`, `ai/`, `data-network/`.
- **`Dispatchers.*` checker Gradle task** — Flag direct `Dispatchers.Default`/`IO`/`Main` usage outside `DefaultDispatcherProvider`.
- **UiState → Preview convention test** — Reflect on all sealed `UiState` subclasses and assert a `@Preview` function exists for each.
- **`@Deprecated` tracking** — Report or fail on `@Deprecated` usages that remain after a configurable grace period.
- **Custom detekt AST rules** — Graduate complex regex-based Gradle checks (like `ThemeLayerCheckTask`) into proper detekt rules with AST access for fewer false positives.

### Key References

- [Enforcement mechanism table in `.agent/AGENTS.md`](../.agent/AGENTS.md) (§ Linter & Architecture Enforcement)
- [Adding a new Gradle check (template)](../.agent/AGENTS.md) (§ Adding a new Gradle check)

---

## Techniques Inventory

A single table cataloging every engineering technique in use.

| Category | Technique | Location |
|----------|-----------|----------|
| **API Design** | `Result<D, E>` sealed type | `domain/.../model/Result.kt` |
| **API Design** | Sealed error hierarchies (`DataError`, `AuthError`) | `domain/.../model/DataResult.kt`, `domain/.../model/AuthError.kt` |
| **API Design** | `operator fun invoke()` convention | All use cases in `usecase/src/commonMain/` |
| **API Design** | `@Deprecated` with `ReplaceWith` | `DataResult.kt` |
| **Architecture** | Module dependency enforcement | `buildSrc/.../ArchitectureCheckTask.kt` |
| **Architecture** | Theme layer boundary | `buildSrc/.../ThemeLayerCheckTask.kt` |
| **Architecture** | Domain purity (zero dependencies) | `domain/` module |
| **Architecture** | kotlin-inject DI | `compose-app/.../di/AppComponent.kt` |
| **Architecture** | `DispatcherProvider` abstraction | `domain/.../model/DispatcherProvider.kt` |
| **Architecture** | ADR documentation (4 records) | `docs/architecture/adr-*.md` |
| **Architecture** | Unified navigation back stack | `compose-app/.../App.kt` |
| **Testing** | Unit tests (~200) | `*/src/commonTest/`, `*/src/jvmTest/`, `*/src/desktopTest/` |
| **Testing** | Convention tests (2) | `usecase/src/jvmTest/`, `viewmodel/src/desktopTest/` |
| **Testing** | Android screenshot tests (131 PNGs) | `android-screenshot-tests/` |
| **Testing** | iOS snapshot tests (92 PNGs) | `ios-app-swift-ui/iosAppSwiftUITests/` |
| **Testing** | Fakes over mocks (7 fakes) | `test-common/src/commonMain/.../testcommon/` |
| **Testing** | Test data builders | `test-common/.../TestDevices.kt`, iOS `TestData.swift` |
| **Testing** | CrashProof ViewModel pattern | `viewmodel/src/commonTest/.../CrashProof*Test.kt` |
| **Testing** | Tiered screenshot strategy | [SCREENSHOT_STRATEGY.md](SCREENSHOT_STRATEGY.md) |
| **Testing** | Two-mode CI | `.github/workflows/ci.yml`, `.github/ci-mode.txt` |
| **Testing** | Navigate-all-screens smoke test | `compose-app/src/androidTest/` |
| **Enforcement** | Custom Gradle tasks (4) | `buildSrc/src/main/kotlin/` |
| **Enforcement** | detekt + compose plugin | `detekt.yml` |
| **Enforcement** | Spotless + ktlint | `build.gradle.kts` |
| **Enforcement** | Git guardrail hooks (2) | `.claude/hooks/` |
| **Enforcement** | CI-parity gate (`validate.sh`) | `scripts/validate.sh` |
| **Enforcement** | Test coverage enforcement | `buildSrc/.../TestCoverageCheckTask.kt` |
| **Enforcement** | Preview coverage check | `buildSrc/.../PreviewCoverageCheckTask.kt` |
| **Enforcement** | Post-merge auto-generation | `.github/workflows/auto-generate.yml` |

---

## Cross-Reference Index

Maps topics to their primary doc, ADR, and enforcement mechanism.

| Topic | Primary Doc | ADR | Enforcement |
|-------|------------|-----|-------------|
| Module dependencies | [Architecture.md](Architecture.md) | [ADR-001](architecture/adr-001-usecase-dependencies.md) | `checkArchitecture` Gradle task |
| Error handling | [.agent/project.md](../.agent/project.md) § Error Handling | — | Convention (no automated check yet) |
| Test coverage | [TESTING.md](TESTING.md) | [ADR-002](architecture/adr-002-test-coverage-strategy.md) | `checkTestCoverage` Gradle task |
| Alpha dependencies | — | [ADR-003](architecture/adr-003-alpha-dependencies.md) | Manual review |
| Single responsibility | [.agent/project.md](../.agent/project.md) § SRP | [ADR-004](architecture/adr-004-single-responsibility-principle.md) | Convention (no automated check yet) |
| Theme layer | — | — | `checkThemeLayer` Gradle task |
| Screenshot tests | [SCREENSHOT_STRATEGY.md](SCREENSHOT_STRATEGY.md) | — | `checkPreviewCoverage` Gradle task |
| UseCase conventions | — | — | `UseCaseConventionTest` |
| ViewModel test existence | — | — | `ViewModelTestConventionTest` |
| Code formatting | — | — | Spotless + ktlint |
| Kotlin patterns | — | — | detekt + compose plugin |
| Git workflow safety | [.agent/AGENTS.md](../.agent/AGENTS.md) | — | `.claude/hooks/git-guardrails.sh` |
| CI modes | [.agent/ci.md](../.agent/ci.md) | — | `.github/ci-mode.txt` |
| Feature parity | [FEATURE_PARITY_MAPPING.md](FEATURE_PARITY_MAPPING.md) | — | Manual review |
| User journeys | [USER_JOURNEYS.md](USER_JOURNEYS.md) | — | Manual review |
| Features inventory | [FEATURES.md](FEATURES.md) | — | Manual review |
