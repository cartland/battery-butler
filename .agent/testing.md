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
- **`:domain` has no `kotlinx-coroutines-test` dependency** (it depends on nothing — see Module Dependencies in `project.md`). Tests of suspend code in `domain/src/commonTest/` use plain `kotlinx.coroutines.runBlocking` with `kotlinx.coroutines.flow.first()`, not `runTest`/`TestScope`. See `DataModeKeyedStateTest.kt` for the pattern.
- **Coroutine test gotcha**: `DefaultSyncManager` has an infinite `subscribeWithRetry()` loop in `init`. Never use `advanceUntilIdle()` in tests that create a SyncManager with a subscribe source that throws or completes (it schedules infinite tasks). Use `testDispatcher.scheduler.advanceTimeBy(ms)` + `runCurrent()` instead, and always call `scope.cancel()` at end.
- `applyRemoteUpdate` and `nextBackoff` are `internal` on `DefaultSyncManager` for direct testing without the subscribe loop
- **Crash-proof ViewModel tests** (`CrashProof*Test.kt`): Test error handling gaps in ViewModels. Two patterns:
  - **Pattern A** (safeStateIn): Throwing repo flow → verify `safeStateIn` catches exception but UI stays stuck at initial value (e.g., `Loading`). Tests pass, documenting the broken UX.
  - **Pattern B** (viewModelScope.launch): Can't use `assertFailsWith` because `SupervisorJob` sends exceptions to the thread's uncaught handler asynchronously (not through `advanceUntilIdle()`). `runTest` catches these and fails. Use **intercepting repo** pattern instead: record exception without rethrowing, then assert no error state exists on the ViewModel.
  - **Real production hit of Pattern B (2026-07, `bb-dimg` device photos)**: `EditDeviceViewModel.uploadPhoto()`/`removePhoto()` set a `_photoUploading` loading flag to `true`, then only cleared it on the line *after* the operation returned successfully — no `try/finally`. An unexpected (non-`Result`) exception from deep in the call chain (the local Room cache write, in this case) skipped that line entirely: no crash, no error shown, `_photoUploading` stuck at `true` forever — a real device confirmed the exact "spinner never clears" symptom this pattern predicts. Fix (PR #1371): wrap in `try { ... } catch (e: CancellationException) { throw e } catch (e: Exception) { /* surface as a typed error */ } finally { /* always clear the flag */ }`. **General rule going forward: any ViewModel loading-state flag set inside `viewModelScope.launch` must be cleared in a `finally`, not the line after the happy path** — this is now a concrete regression class, not just theoretical. Regression-tested via `FakeDeviceImageRepository.uploadThrows`/`deleteThrows` (an *unexpected* throw, distinct from a typed `DeviceImageError` `Result.Error`) — see `EditDeviceViewModelTest.kt`.
  - **Technique for proving a regression test actually catches the bug**: temporarily revert the fix locally, re-run the new test and confirm it fails, then restore the fix and confirm it passes again. Used twice in the `bb-dimg-reliability`/stuck-spinner fixes (PRs #1370, #1371) — both times the "obvious" test would have passed even against the buggy code if written slightly differently (e.g., without pausing a fake mid-operation to actually interleave a cancellation, or without a fake that can throw an *unexpected* exception distinct from a typed error). Cheap insurance against a test that looks like it covers the bug but doesn't.

### Testing Room-backed Flows — `runTest` silently proves nothing

**Room runs queries and invalidation on its own executors, which a `TestScheduler`'s virtual clock does not drive.** A `runTest` + `advanceUntilIdle()` harness around a Room `@Query` Flow will simply never observe an emission — the test then fails on its own precondition, or worse, passes because it asserted nothing. Use `runBlocking` with real dispatchers and a real `withTimeout` for these. `RestoreLateCollectorTest` (`data-local/src/jvmTest`) is the pattern.

This is a large part of why the `bb-lg42` restore regression had no automated coverage for so long: the obvious harness looks correct and reports nothing.

Two related traps from writing that test:

- **Never assert on a list that a *different* subscription populated.** The first version launched a collector accumulating emissions into a `CopyOnWriteArrayList`, then waited on a separate `first { it.isNotEmpty() }` before asserting the list had the data. Those are independent subscriptions — the second seeing it proves nothing about the first having appended. It passed 5/5 locally and failed on CI. Hand emissions to the test through a `Channel` and drain *that*, so you observe exactly the collector under test.
- **`seedLegacyFile` closes and renames the OFFLINE database file.** Seed the legacy fixture *before* constructing `DynamicDatabaseProvider`, or you pull the file out from under the provider's eagerly-created instance and nothing ever emits. Easy to misread as a product bug.

### Mutation-check every regression test

Extends the "temporarily revert the fix" technique above: after writing a regression test, reintroduce the bug and confirm the test fails. Applied to `RestoreLateCollectorTest` — reverting `restoreFromLegacy` to create the new database without publishing it to `_database` fails both tests.

That exercise also produced a useful negative result: removing `rebindSignal` from `RoomLocalDataSource.bound()` does **not** fail those tests, because `restoreFromLegacy` assigns a new `_database` value and that `StateFlow` change alone re-triggers `flatMapLatest`. `rebindSignal` is defense-in-depth for the same-instance case, covered separately by `DynamicDatabaseProviderTest`. Worth knowing before anyone "simplifies" it.

## Headless Compose UI Tests (jvmTest, no emulator)

Introduced with the record-replacement flight animation (PR #1347,
`presentation-feature/src/jvmTest/.../RecordReplacementFlightUiTest.kt`). Desktop
Compose UI tests run **headless** under plain `./gradlew :<module>:jvmTest` — real
composition, layout, semantics, and animation frames, no emulator or window. Use
them to verify interactive/animated Compose behavior that screenshot tests (static)
and instrumented tests (slow, emulator-bound) can't cover cheaply.

Setup (see `presentation-feature/build.gradle.kts`):

```kotlin
jvmTest.dependencies {
    implementation(libs.kotlin.test)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    implementation(compose.uiTest)
    implementation(compose.desktop.currentOs)
}
```

Patterns and gotchas:

- Use `runComposeUiTest { ... }` (`@OptIn(ExperimentalTestApi::class)`) with
  `mainClock.autoAdvance = false` + `mainClock.advanceTimeBy(ms)` to drive
  animations deterministically. Coroutine `delay()`s inside `LaunchedEffect` are
  virtualized by the same clock.
- **String resources are NOT findable as text** in headless jvmTest —
  `onNodeWithText("Record Replacement")` fails for a `composeStringResource(...)`
  label even though data-driven strings (e.g. a device name) resolve fine. Find
  resource-labeled nodes by `Modifier.testTag(...)` instead (tags live in an
  internal `*TestTags` object next to the composable, e.g. `RecordFlightTestTags`).
- `assertDoesNotExist()` is a member of `SemanticsNodeInteraction` — don't import it.
- Semantics exist even at `graphicsLayer` alpha 0, so "hidden" can't be asserted
  via `assertDoesNotExist` — assert on the mechanism (e.g. a ghost overlay's tag
  appearing/disappearing) plus pure unit tests of the state machine's alpha logic.

## Instrumented Tests (`scripts/test.sh`)

- Require an Android emulator (CI uses managed Pixel 5 API 34 with KVM)
- All tests are offline-capable — no server needed (app defaults to `DataMode.None`)
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
- **Never use `--tests` filter with `updateDebugScreenshotTest` WITHOUT ALSO passing `-PretainedReferenceScreenshots`** — without that flag, the task's `cleanReferenceScreenshots` dependency wipes the ENTIRE reference directory before regenerating only your filtered subset, destroying every other baseline. **With** `-PretainedReferenceScreenshots`, `--tests` is safe and useful: it skips the clean step, so `./gradlew :android-screenshot-tests:updateDebugScreenshotTest -PretainedReferenceScreenshots --tests "...SomeTestKt"` updates only that class's references, leaving everything else untouched (confirmed via `git status` showing zero diff outside the targeted class — this is exactly what `scripts/generate-android-screenshots.sh` does internally, just looped over every file; use the direct command instead of the full script when only one or two classes actually changed).
- **OOM guard**: `updateDebugScreenshotTest` and `validateDebugScreenshotTest` are blocked by default — a `doFirst` guard in `build.gradle.kts` prevents all-at-once runs that OOM. The sequential script bypasses via `-PretainedReferenceScreenshots`. To force a direct run, pass `-PforceAllScreenshots`.
- **Heap accumulates across runs in one Gradle daemon session** — even scoping to a single test class with `--tests` can still OOM if the daemon already has memory pressure from earlier work in the same session. Run `./gradlew --stop` before each `validateDebugScreenshotTest`/`updateDebugScreenshotTest` invocation to start from a clean daemon (observed 2026-07-06: a class that OOM'd immediately after other work succeeded on retry right after a daemon restart, no other change).
- **Flakiness gotcha — don't blindly accept a diff on a screen you didn't touch.** Re-running `validateDebugScreenshotTest`/`updateDebugScreenshotTest` on an unchanged commit can produce pixel-different (but *visually identical*) renders for screens unrelated to your change — likely font-hinting/anti-aliasing nondeterminism in the preview renderer between separate JVM invocations, not a real regression (observed 2026-07-06, `bb-screenshot-flake` in `TODO.md`). Before accepting an `updateDebugScreenshotTest` baseline change for a screen your diff didn't touch, `git show HEAD:<path>.png` the old version and compare side-by-side (Read tool renders PNGs) — if visually identical, `git checkout -- <path>.png` to revert it rather than silently baking in an unreviewed render.
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
