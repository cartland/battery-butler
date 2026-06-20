# Testing

Test types, coverage enforcement, and testing patterns for Battery Butler.

> **Parent doc:** See `project.md` for architecture and `AGENTS.md` for workflow rules.

## Test Types

| Type | Command | Server? | Emulator? | CI Job |
|------|---------|---------|-----------|--------|
| Unit | `./gradlew test` | No | No | `validation_test` |
| Instrumented | `./scripts/test.sh` | No | Yes | `validation_instrumented` |
| Screenshot | `./gradlew :android-screenshot-tests:validateDebugScreenshotTest` | No | No | `validation_screenshots` |
| E2E | `./scripts/e2e-tests.sh` | Yes | No | Manual only |

## When Tests Run

| Trigger | Unit | Instrumented | Screenshot | E2E |
|---------|------|-------------|------------|-----|
| PR with code changes | Yes | Yes | Yes | No |
| PR with docs only | No | No | No | No |
| `./scripts/validate.sh` (local) | Yes | Yes | Yes | No |
| Manual | — | — | — | Yes |

## Convention Tests

- **`UseCaseConventionTest`** (`usecase/src/jvmTest/`): JVM-only test that uses Kotlin reflection to scan all `*UseCase` classes in the `com.chriscartland.batterybutler.usecase` package and asserts each has `operator fun invoke` (suspend or non-suspend). Runs as part of `./gradlew :usecase:jvmTest`. Requires `kotlin("reflect")` in jvmTest dependencies. **JAR-URI gotcha** (PR #1095): when a dependency JAR is on the test classpath, `File(url.toURI())` throws `IllegalArgumentException("URI is not hierarchical")` for non-`file:` URIs. The test now skips `url.protocol != "file"` entries. Adding a new use-case dependency that pulls in JARs (e.g. `kotlinx.serialization`) used to crash the whole convention test; this is fixed.
- **`ViewModelTestConventionTest`** (`viewmodel/src/desktopTest/`): Desktop-only test (JVM reflection) that scans all `*ViewModel` classes and verifies each has a corresponding `*ViewModelTest` class. Excludes `*Factory` and the test class itself. Runs as part of `./gradlew :viewmodel:desktopTest`. Requires `kotlin("reflect")` in desktopTest dependencies. Note: the viewmodel module uses `jvm("desktop")` not `jvm()`, so the source set is `desktopTest/` not `jvmTest/`.
- The tests use `kotlin.test.assertTrue(value, message)` (NOT the trailing-lambda form, which doesn't exist in `kotlin.test`).
- **`SealedScreenStateConventionTest`** (`presentation-model/src/jvmTest/`): Verifies sealed interfaces have `*ScreenState` suffix and concrete subtypes follow naming conventions. Runs as part of `./gradlew :presentation-model:jvmTest`. Requires `kotlin("reflect")` in jvmTest dependencies.
- Both `desktopTest` and `jvmTest` source sets need `kotlin("reflect")` for reflection-based convention tests.
- **`desktopTest`** is in the detekt FunctionNaming excludes list (alongside `jvmTest`, `commonTest`, etc.) to allow backtick test names.

## Unit Tests (`./gradlew test`)

- Pure Kotlin tests across all modules (domain, data, viewmodel, usecase, server, etc.)
- Located in `src/commonTest/`, `src/test/`
- **Coroutine test gotcha**: `DefaultSyncManager` has an infinite `subscribeWithRetry()` loop in `init`. Never use `advanceUntilIdle()` in tests that create a SyncManager with a subscribe source that throws or completes (it schedules infinite tasks). Use `testDispatcher.scheduler.advanceTimeBy(ms)` + `runCurrent()` instead, and always call `scope.cancel()` at end.
- `applyRemoteUpdate` and `nextBackoff` are `internal` on `DefaultSyncManager` for direct testing without the subscribe loop
- **Crash-proof ViewModel tests** (`CrashProof*Test.kt`): Test error handling gaps in ViewModels. Two patterns:
  - **Pattern A** (safeStateIn): Throwing repo flow → verify `safeStateIn` catches exception but UI stays stuck at initial value (e.g., `Loading`). Tests pass, documenting the broken UX.
  - **Pattern B** (viewModelScope.launch): Can't use `assertFailsWith` because `SupervisorJob` sends exceptions to the thread's uncaught handler asynchronously (not through `advanceUntilIdle()`). `runTest` catches these and fails. Use **intercepting repo** pattern instead: record exception without rethrowing, then assert no error state exists on the ViewModel.

## Instrumented Tests (`scripts/test.sh`)

- Require an Android emulator (CI uses managed Pixel 5 API 34 with KVM)
- All tests are offline-capable — no server needed (app defaults to `NetworkMode.None`)
- `compose-app/src/androidInstrumentedTest/`: `ComposeUITest` (UI navigation), `ExampleInstrumentedTest` (app context)
- `data/src/androidInstrumentedTest/`: `DatabaseSanityTest` (Room schema), `MigrationTest` (Room migrations 3→4→5)
- **BackHandler priority**: The app uses a single unified NavDisplay back stack. The AI overlay's `PredictiveBackHandler` composes deeper than NavDisplay's handler, giving it higher priority when expanded. In tests, use actual UI back buttons (Cancel/Done/Back arrow) instead of `Espresso.pressBack()` to avoid BackHandler conflicts.
- **Managed device test filtering**: `--tests` flag doesn't work with managed device tasks. Use `-Pandroid.testInstrumentationRunnerArguments.class=com.example.TestClass#testMethod` instead.
- **Test isolation**: Tests within a single managed device run share database state. Avoid tests that depend on empty-state UI when other tests create persistent data.

## Screenshot Tests

Pixel-perfect UI regression tests against reference images. Failures indicate UI changes, not broken infrastructure.

### Android

- Use `updateDebugScreenshotTest` / `validateDebugScreenshotTest` (Paparazzi/Roborazzi).
- All preview composables must be time-deterministic — never let `Clock.System.now()` reach a screenshot preview
- Use `Instant.parse("2026-01-18T17:00:00Z")` as the standard fixed instant in previews
- Pass explicit `nowInstant` / date parameters through the full composable chain — don't rely on defaults
- `updateDebugScreenshotTest` and `validateDebugScreenshotTest` can't run in the same Gradle invocation (the update task's clean step deletes references mid-build)
- **Never use `--tests` filter with `updateDebugScreenshotTest`** — the gallery generator deletes reference images for non-included test classes. Always use `scripts/generate-android-screenshots.sh` to regenerate all baselines safely (runs one test file at a time to avoid OOM)
- **OOM guard**: `updateDebugScreenshotTest` and `validateDebugScreenshotTest` are blocked by default — a `doFirst` guard in `build.gradle.kts` prevents all-at-once runs that OOM. The sequential script bypasses via `-PretainedReferenceScreenshots`. To force a direct run, pass `-PforceAllScreenshots`.
- **File size limit (decision)**: Keep screenshot test files to ~10 tests (20 images with Light/Dark). Files with 20+ tests OOM on CI runners (2 GB Gradle heap). The fix is **splitting files, NOT increasing heap** — splitting keeps memory predictable and the sequential script (`generate-android-screenshots.sh`) processes one file per Gradle invocation. When adding new screen-level tests, create a new file if the existing one has ~10 tests.
- **Validating specific classes after regen**: `-PforceAllScreenshots` still OOMs on large previews (e.g. `PlayStoreAddDeviceTest_Light`). After regenerating baselines, validate only the affected test classes: `./gradlew :android-screenshot-tests:validateDebugScreenshotTest --tests "com.chriscartland.batterybutler.androidscreenshottests.<TestClass>Kt" -PforceAllScreenshots`. Full all-at-once local validation is unreliable; CI handles the full suite. See `bb-cpe4` for the sequential validation script task.
- When refactoring shared components (e.g. list items), ALL screen-level baselines that embed those components will change — regenerate everything, not just the component tests
- **Always regenerate and commit reference images** when adding or changing screenshot tests. Run `./scripts/generate-android-screenshots.sh`, then `git add` the new/updated PNGs in `android-screenshot-tests/src/screenshotTestDebug/reference/` and `SCREENSHOT_GALLERY.md`. PRs that add screenshot tests without reference images are incomplete.
- **Preview coverage enforcement**: `./gradlew checkPreviewCoverage` scans `presentation-core` and `presentation-feature` for `@Preview` composables and verifies each has a corresponding screenshot test import. Fails the build on gaps. Also generates `docs/Preview_Coverage_Report.md` (gitignored). When adding a new `@Preview`, also add a screenshot test or the coverage check will fail.
- **`scanFunctions` flag**: `TestCoverageCheckTask` has a `scanFunctions` flag that enables scanning top-level `fun` declarations (not just classes). Content composables in `presentation-feature` are top-level functions, not classes — `scanFunctions = true` is required for that module.
- **Test coverage enforcement**: `./gradlew checkTestCoverage` scans `usecase` (`*UseCase`), `viewmodel` (`*ViewModel`), `data` (`Default*`), and `ai` (`*AiEngine`, `*AiConfig`) for classes matching enforced patterns and verifies each has a corresponding `*Test.kt` file. Fails the build on gaps. Generates `docs/Test_Coverage_Report.md` (gitignored). Two suppression mechanisms: (1) inline `// @NoTestRequired: <reason>` above the class, (2) central `test-coverage-exemptions.txt` with glob patterns. Hard-coded exclusions: `*Factory`, `*Component`, `di/` and `provider/` directories. When adding a new class matching an enforced pattern, also add a test file or use a suppression.
- **Two-tier structure**: Screenshot tests have exactly two tiers — (1) **full-screen** (with Scaffold, tabs, app bar) and (2) **individual components** (reusable design-system pieces). Intermediate layouts (e.g. just the filter row, just the list section, just a sub-section) must not have standalone screenshot tests. When removing an intermediate-layout screenshot test, also remove the `@Preview` annotation from the source composable (keep the composable function itself; just drop the `@Preview`).
- **Battery age states** (`DeviceListItemOldPreview`, `DeviceListItemVeryOldPreview`) are component-level tests — they verify distinct visual states (amber warning ≥180 days, red bold ≥365 days) that matter for regression detection.
- **Platform API overrides for previews**: When a composable reads a platform API (e.g., `WindowInsets.ime`) that always returns a fixed value in previews, use **parameter hoisting** — add a parameter with the platform read as its default (e.g., `imeVisible: Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0`). Previews pass the desired value directly. Do NOT use CompositionLocals for test-only overrides — that leaks test concerns into production code.
- **Blank-screenshot detection**: `./scripts/check-screenshot-health.sh` (also exposed as `/check-screenshot-health` skill) scans reference PNGs and reports any under 1 KB — these are usually previews that depend on runtime state (`ViewModel`, `LocalFileSaver`, `LocalFileLoader`, `appComponent`) and rendered empty in screenshot tests. The fix is creating a stateless preview overload that accepts demo data as parameters. Run this after `/update-android-screenshots` or after changing preview composables.

### iOS

See [ios.md](ios.md) for iOS snapshot testing details.

## Detekt

- Composable functions must order params: no-default params first, then `modifier: Modifier = Modifier`, then other defaulted params, then trailing lambda. Detekt's compose rule enforces this.

## Spotless / ktlint

- ktlint enforces the **single top-level declaration filename rule**: if a `.kt` file contains only one top-level declaration (class, object, etc.), the file must be named after that declaration. If you remove a declaration leaving only one, rename the file accordingly.

## E2E Tests (`e2e-tests/`)

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
