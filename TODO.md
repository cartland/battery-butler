# TODO

Project task tracking for Battery Butler.

> Migrated from Beads (`bd`) on 2026-06-08. Task IDs (`bb-xxxx`) are preserved as
> stable anchors because other docs and workflow comments cross-reference them.
> Working convention: edit this file directly. Move a task to **Done** (or delete
> it) when complete. Add new tasks under the appropriate priority heading.

## P2

### bb-lg42 — DB restore: ViewModel Flows don't re-emit after `restoreFromLegacy` until app restart

**Symptom (android/30 → still present android/31):** after Settings → Restore
Previous Data, Devices tab shows restored devices, but Device Types and History
tabs stay stuck (android/30: both spin; android/31: Device Types shows empty
state, History still spins). Killing+restarting the app fixes everything. Not a
release regression — the restore code path is byte-identical across releases.

**2026-05-14 update — fix was partial; remaining bug is schema migration, not flow
re-binding.** PR #1190's `rebindSignal` + `bound { }` fix DID make flows
re-subscribe (Device Types went spinner → empty state). But the legacy DB file is
missing rows/tables: `Migrations.kt` only has `MIGRATION_3_4` (devices.location)
and `MIGRATION_4_5` (battery_events.batteryType/notes) — there are NO migrations
that CREATE `device_types` or `battery_events`. A legacy file at schema v3 or
below lacks those tables. History spins because `HistoryListViewModel` uses
`initialValue = Loading` and its `combine(events, devices, types)` never fires
(getAllEvents() likely throwing/hanging); Device Types shows empty because its
`initialValue = Success(emptyMap())`.

**Suggested fix paths (need design discussion):**
a. Add migrations that CREATE missing tables (cleanest if we know which legacy
   versions exist in the wild).
b. `fallbackToDestructiveMigration` safety net (loses data; bad UX).
c. Pre-restore schema validation — inspect the legacy file's schema before copying
   it over the active DB; surface an error instead of restoring to a broken state.
d. Capture Logcat from a repro to see exactly what Room is doing on `getAllEvents()`.

**Next steps:** add the missing end-to-end test (see bb-qz7w); determine the
legacy DB's actual schema version (cross-ref git log of `Migrations.kt`); decide
between (a)/(b)/(c) — likely (a) for production.

### bb-qz7w — Add end-to-end test for `restoreFromLegacy` → late-collector data flow

PR #1190's test only asserts `rebindSignal` emits during `restoreFromLegacy`. It
does NOT exercise the real scenario: a downstream collector unsubscribed during
restore, re-subscribing and querying the new DB. This gap let android/31 ship with
the bb-lg42 symptom still present.

**What to add** (in `data-local/src/jvmTest/...` or appropriate location):
1. Seed a legacy file with a known schema and data.
2. Construct `DynamicDatabaseProvider` + `RoomLocalDataSource` at the current
   schema version.
3. Subscribe to a Room-backed flow (`getAllDeviceTypes()`, `getAllEvents()`).
4. Unsubscribe (simulate `WhileSubscribed` timeout).
5. Call `restoreFromLegacy(legacyFileName)`.
6. Re-subscribe.
7. Assert the flow emits restored data within a timeout (NOT `Loading`/stuck).

Variants: legacy file at v3 (missing tables) to catch the schema-mismatch case
(bb-lg42); concurrent restores (rebindCounter monotonicity); in-flight collection
active during restore (the Devices tab case, which works today). Independent of
bb-lg42 — that's the user-facing bug; this is the regression-prevention infra.

### bb-w73e — Push-to-main CI runs cancelled despite SHA-based concurrency (observed 2026-05-12)

`.agent/ci.md` ("CI Concurrency on Main", PR #856) claims push-to-main runs use
SHA-based concurrency so rapid merges don't cancel each other. But on 2026-05-12
multiple push-to-main runs were cancelled mid-flight (runs 25737957138 /
25738106266 / 25739183100), which made the post-merge safety net file blocking
ci-failure issues #1180/#1181 even though the code was fine — both needed manual
closure to unblock the queue.

**Investigate:** (1) the `concurrency.group` on the `Battery Butler CI` push-to-main
path — should be SHA-based (`group: ci-${{ github.sha }}`); branch-based would
explain it. (2) whether `cancel-in-progress: true` is global vs PR-only. (3)
compare with PR #856 for a regression. If config is correct, look at runner
resource exhaustion / external cancel-workflow actions.

**Complementary fix:** update `scripts/file-ci-failure-issue.sh` to skip *filing*
issues when the run conclusion is `cancelled` and no individual job has a `failure`
conclusion (defensive companion to the bb-2r4g close-on-success guard).

## P3

### bb-j6td — Remove misleading `GITHUB_TOKEN` fallback in `ci-trigger-auto-prs.yml`

Line 41 uses `github-token: ${{ secrets.BOT_PAT || secrets.GITHUB_TOKEN }}`. The
`||` only falls back when `BOT_PAT` is the empty string (unset), NOT when it's set
but invalid (HTTP 401) — the actual 2026-05-12 failure mode. And even if it fired,
`GITHUB_TOKEN` can't trigger workflows, so it's a silent dead end.

**Fix:** (a) remove the `|| secrets.GITHUB_TOKEN` fallback so the step fails loudly
when `BOT_PAT` is unset, or (b) keep it but emit a `::warning::` when the token came
from `GITHUB_TOKEN`. Related to bb-16u1. Very small (1–2 line YAML diff).

### bb-7i84 — Investigate proper KMP → `com.android.kotlin.multiplatform.library` migration for `:compose-resources` (post-#1171 revert)

PR #1171 migrated `:compose-resources` off legacy `com.android.library`; it
compiled and passed unit/lint/detekt/build, but
`:compose-app:pixel5api34DebugAndroidTest` failed at runtime with
`MissingResourceException` for `strings.commonMain.cvr`. Root cause: the new
plugin generates the `.cvr` files into its own intermediates but does NOT publish
them as a consumable Android assets artifact for downstream consumers; the legacy
plugin's `sourceSets.main.assets.srcDirs` hook did. Reverted in PR #1172.
`:compose-resources` is the last KMP module still on legacy `com.android.library`.

**Next steps:** (1) check the Compose Multiplatform changelog (currently 1.10.0)
for `androidKotlinMultiplatformLibrary` asset-publication support. (2) try explicit
asset wiring in the migrated `build.gradle.kts` (point `assets.srcDirs` at
`generated/assets/copyDebugComposeResourcesToAndroidAssets`). (3) if neither works,
close as "blocked upstream — needs CMP support" and link the JetBrains youtrack
ticket. **DoD:** migrated AND `pixel5api34DebugAndroidTest` passes 5/5 locally AND
`validation_instrumented` green on PR + post-merge. See
`feedback_kmp_android_plugin_migration` memory and PR #1171/#1172.

### bb-bpbw — `List<T>` → `ImmutableList<T>` for hot-path collections

Follow-up to PR #1107 (Compose stability config). `presentation-model.**` and
`domain.model.**` are marked stable, but `List<T>` is still flagged unstable by the
Compose Compiler (mutable from the JVM's view). For hot-path collections crossing
composable boundaries (LazyColumn/LazyGrid item lists, `groupedDevices`,
`groupedTypes`), switching to `kotlinx.collections.immutable.ImmutableList<T>`
(already stable in the config) removes the per-recomposition skip check.

**Investigate:** dump Compose metrics (`-Pkotlinx.compose.metricsDestination`),
find parameters reporting unstable due to `List<T>`, convert at the data-source
boundary (ViewModels where state is constructed). Medium effort.

> ⚠️ Risky unattended — every candidate state class is consumed by Swift
> Kotlin/Native, and `validation_ios_ui` is dev-mode-skipped on PRs. Needs
> release-mode CI or local `xcodebuild` verification. (See
> `feedback_bb_bpbw_kn_interop_risk` memory.)

### bb-k4sk — Unblock & re-enable Kotlin 2.4.0+ when SKIE adds support

Dependabot's kotlin group bump to 2.4.0 (PR #1222) was closed 2026-06-15 because
SKIE 0.10.9 hard-fails the build: `Error: SKIE 0.10.9 does not support Kotlin
2.4.0. Supported versions are: [..., 2.3.0].` A version-scoped ignore
(`org.jetbrains.kotlin:* >= 2.3.20`; `kotlinx-*` deliberately left free) was added
in PR #1243.

**When a SKIE release supports the target Kotlin** (check Touchlab SKIE releases vs
the `skie` pin in `libs.versions.toml`, currently `0.10.9`): (1) bump `skie`,
(2) delete the kotlin block in `.github/dependabot.yml` → `ignore:`, (3) remove the
`# Kotlin 2.3.20 blocked: SKIE …` comment in `libs.versions.toml` and bump
`kotlin` (and let Dependabot re-propose / bump `kotlinx-coroutines` etc.).
External-dependency-gated; closes when Kotlin is bumped past 2.3.0 with green CI.

### bb-gr79 — Unblock & re-enable gRPC 1.79+ / Wire 6.0+ (regenerate protos)

Dependabot's grpc group bump (grpcJava 1.63→1.81, grpc-kotlin 1.4.1→1.5.0, wire
5.0→6.4; PR #1223) was closed 2026-06-15: gRPC ≥1.79 removed the codegen APIs the
generated stubs call (`BlockingClientCall`, `blockingV2UnaryCall`, …), producing 5
compile errors in `server/app` (generated `*Grpc.java`). Version-scoped ignores
(`io.grpc:* >= 1.79.0`, `com.squareup.wire:* >= 6.0.0`) were added in PR #1243.

**To unblock:** regenerate the protos with a codegen plugin version matching the new
gRPC (`protobufPlugin` / gRPC codegen in `server/app/build.gradle.kts`), confirm
`server/app` compiles, then delete the grpc/wire block in `.github/dependabot.yml`
→ `ignore:`, remove the `# gRPC 1.79+ blocked` / `# Wire 6.0+ blocked` comments in
`libs.versions.toml`, and bump `grpcJava` / `grpc-kotlin` / `wire`. Verify
`validation_compile_tests` + `build_server` (release-mode) before merge.

### bb-ovm1 — Spike: KMP-ObservableViewModel to kill the 16 Swift ViewModelWrapper boilerplate

Evaluate adopting rickclephas **KMP-ObservableViewModel** to replace the ~16
hand-written `ios-app-swift-ui/**/*ViewModelWrapper.swift` files (each mirrors a
shared `StateFlow` into `@Published` via `Task { for await }` + manual
`KmpViewModelStore`/`deinit`). 5-agent investigation 2026-06-17 — findings:

- **Coexists with SKIE (HIGH confidence):** documented, regression-tested, shipped
  in production (rickclephas/KMP-ObservableViewModel#93). KEEP SKIE — our **enum
  interop is load-bearing** (compiler-exhaustive Swift `switch` w/ no `default` over
  `SortOption`/`AiRole`/… in `HomeScreen.swift`, `DeviceTypeListScreen.swift`,
  `SettingsViewModelWrapper.swift`); rickclephas provides no enum bridging, so full
  SKIE removal is OFF the table (also keeps the bb-k4sk pin a separate concern).
- **Pin impact NEUTRAL:** both rickclephas libs already support Kotlin 2.3.20+ and
  ship faster than SKIE; SKIE (cap 2.3.10) stays the sole 2.3.20 blocker (bb-k4sk).
- **Pure flow swap:** ZERO suspend funcs consumed from Swift; async surface = 32
  `StateFlow` observations across the 16 wrappers.
- **ObservableViewModel does NOT require KMP-NativeCoroutines** → two options:
  - **Option A (recommended first):** ObservableViewModel + SKIE as-is (no
    NativeCoroutines, no SKIE config change). Get `@StateViewModel` auto
    lifecycle/`onCleared`/cancellation; expose state to Swift via hand-written iOS
    extension properties.
  - **Option B (graduate to if A's binding is clunky):** add NativeCoroutines for
    `@NativeCoroutinesState` ergonomics, then scope-disable SKIE async on the VM
    package only: `skie { features { group("…viewmodel") {
    SuspendInterop.Enabled(false); FlowInterop.Enabled(false) } } }` — safe,
    documented; leaves Sealed/Enum/DefaultArgument interop untouched.

**Make-or-break unknown to resolve in the spike (DeviceDetail):** does
`com.rickclephas.kmp.observableviewmodel.ViewModel` interoperate, on ALL KMP
targets, with our existing usages — `KmpViewModelStore.put(key, vm:
androidx.lifecycle.ViewModel)`, Compose `uiState.collectAsStateWithLifecycle()`
(`DeviceDetailScreen.kt`), and `viewModelScope` (`safeStateIn(scope =
viewModelScope)` + `viewModelScope.launch` in `DeviceDetailViewModel.kt`)? The
shared VMs back the **Compose** UI on Android/Desktop/iOS-Compose, not just SwiftUI,
so the base-class swap must not break Compose-MP. Also confirm a rickclephas release
exists for our exact pinned Kotlin.

**Spike steps (DeviceDetail only):** (1) add `com.rickclephas.kmp:kmp-observableviewmodel-core`
to `:viewmodel` commonMain + `KMPObservableViewModelSwiftUI` SPM to the iOS app;
(2) swap `DeviceDetailViewModel` base class + retarget `viewModelScope` to the
rickclephas scope (`.coroutineScope` for `safeStateIn`/`launch`); (3) add the
one-time `extension Kmp_observableviewmodel_coreViewModel: @retroactive ViewModel {}`
file; replace `@StateObject DeviceDetailViewModelWrapper` with `@ObservedViewModel`
over the DI-injected VM; delete the wrapper. **DoD:** Android `assembleDebug` +
Desktop + iOS-Compose all build & DeviceDetail still renders/updates (proves
Compose-MP intact) AND iOS SwiftUI DeviceDetail works via **release-mode local
`xcodebuild`** (dev-mode PR CI skips iOS — see bb-bpbw / `feedback_bb_bpbw_kn_interop_risk`).
**Kill if:** rickclephas VM can't satisfy KmpViewModelStore/Compose without
per-platform forking, or no build exists for our pinned Kotlin. Then roll out
across the other 15 wrappers (medium, mechanical).

**Spike result 2026-06-17 (branch `spike/bb-ovm1-observableviewmodel`) — GATE PASSED, GO:**
Wired `kmp-observableviewmodel-core:1.0.1` into `:viewmodel`, swapped
`DeviceDetailViewModel` to extend `com.rickclephas.kmp.observableviewmodel.ViewModel`
(`viewModelScope` → `viewModelScope.coroutineScope` for `safeStateIn`/`launch`).
Empirically verified:
- rickclephas's `androidx` hierarchy group covers Android + iOS + JVM/Desktop +
  macOS (exactly our targets) → its `ViewModel` **extends `androidx.lifecycle.ViewModel`**
  on every platform we ship; standalone `nonAndroidx` variant is only JS/wasm/
  watchOS/tvOS (unused).
- No duplicate-class clash between our JetBrains-fork lifecycle
  (`org.jetbrains.androidx.lifecycle` 2.10.0-beta01) and rickclephas's
  `androidx.lifecycle` — resolves/links on JVM **and** K/N.
- Compiles: `:viewmodel` Desktop + iOS-SimArm64 + Android; `:compose-app` Desktop +
  Android (Compose `collectAsStateWithLifecycle` intact). `DeviceDetailViewModelTest`
  passes. **SKIE links `:ios-swift-di` debug framework** with the new ViewModel
  surface — SKIE + ObservableViewModel coexist in our build.
- Base-class swap is **backward-compatible**: existing Swift `*Wrapper` still
  compiles (uiState still a SKIE-bridged StateFlow; VM still IS-A androidx ViewModel
  so `KmpViewModelStore.put` accepts it) → can land the Kotlin base-class change
  across all 16 VMs first (CI-verifiable), migrate Swift wrappers to `@ObservedViewModel`
  incrementally after.
**SwiftUI phase 2026-06-18 — VERIFIED, full `xcodebuild` BUILD SUCCEEDED (Option A):**
- `uiState` routed through the new `safeStateIn(viewModelScope, …)` overload
  (`ViewModelExtensions.kt`) → observable `ObservableStateFlow` that drives the SwiftUI
  change publisher. This is what makes `@StateViewModel`/`@ObservedViewModel` auto-update
  WITHOUT KMP-NativeCoroutines. (Needed `languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")`.)
- iOS framework: `ios-swift-di` `export(libs.kmp.observableviewmodel.core)` (+ `api`).
  Harmless `-Xexport-library` warning: the SPM package vendors its OWN
  `KMPObservableViewModelCoreObjC` target, so the cinterop need not be re-exported.
- Swift: added SPM package `KMP-ObservableViewModel` @ **1.0.1** (pinned to match the
  Kotlin artifact) via `xcodeproj` gem (products `KMPObservableViewModelCore` +
  `…SwiftUI` on the app target). `Core/ObservableViewModelBridge.swift` =
  `extension shared.ViewModel: @retroactive KMPObservableViewModelCore.ViewModel {}`
  (SKIE exports the base class as `shared.ViewModel`; conformance compiles — SKIE-type
  alignment risk RESOLVED) + manual `uiStateValue` accessor. `DeviceDetailScreen` now
  `@StateViewModel` (auto lifecycle/clear), **`DeviceDetailViewModelWrapper.swift` deleted**.
- Build gotcha (env, not the spike): proto gen failed via stale Bazel Xcode config after
  an Xcode update → `bazel shutdown` re-resolves; then `xcodebuild` clean.
**Net:** wrapper boilerplate (Task/for-await/deinit/KmpViewModelStore) gone on one screen,
proven end-to-end. ObservableViewModel is Kotlin-pinned → bump in lockstep with Kotlin
(bb-k4sk); builds exist 2.3.0→2.4.0. **Next:** roll out to the other 15 wrappers (mechanical).

## P4

### bb-fa11 — Evaluate adding `validation_lint` to the release sentinel-set gate

PR #1197 (server verify-ci) and PR #1200 (local `release-android.sh`) both use a
6-job sentinel set: `validation_ios_ui`, `validation_instrumented`,
`build_android`, `build_ios_compose`, `build_ios_native`, `build_server` (kept at
6 for client/server symmetry; the original proposal had 8). Question: add
`validation_lint`? For: catches lint regressions before tagging. Against: lint is
fast and non-flaky, so it may not change observed reliability. Skip `build_desktop`
(irrelevant to Android releases). If adding, update BOTH
`.github/workflows/release-android.yml` and `scripts/release-android.sh` sentinel
arrays + the SKILL doc list. Small judgment call, not urgent.

### bb-5ceu — Understand asymmetric flow re-emit (`HomeViewModel` vs `DeviceTypeListViewModel`) after `restoreFromLegacy`

Diagnostic follow-up from bb-lg42. The symptom was asymmetric: after restore,
HomeViewModel (Devices) populated but DeviceTypeListViewModel (Device Types) and
HistoryListViewModel stayed stuck until restart — even though Home and DeviceType
both consume the SAME `getDeviceTypesUseCase()` flow. PR #1190's explicit
`rebindSignal` masks the symptom universally, but the asymmetry was never explained.
Worth understanding so (1) a residual root cause doesn't bite a different path, and
(2) future agents don't delete the `rebindSignal` thinking the StateFlow swap is
sufficient.

**Hypotheses to try:** log whether the new DB instance has identity-different DAO
instances vs the closed-DB DAOs; reproduce on a JVM integration test subscribing
before vs after restore with `WhileSubscribed` timeout; read Room's `@Query` Flow
behavior on subscribe shortly after close+reopen of a raw-file-copied DB. Low
priority — user-visible fix is in; this is understanding/documentation work.
Closes when a written explanation lands in `AGENTS.md` or a workflow doc.

### bb-gac9 — Investigate google-api-client 2.9.0 transitive `NoClassDefFoundError`

Dependabot bump google-api-client 2.2.0 → 2.9.0 (PR #1226) was closed 2026-06-15:
it broke `LocalGrpcValidationTest` (`server/app`) with `NoClassDefFoundError` /
`ClassNotFoundException` (a transitive dependency the 2.9.0 line pulls conflicts
with the gRPC test stack). It was also ~1 month stale. **No** dependabot ignore was
added (the block isn't structural), so Dependabot will re-propose on a future
version.

**If/when we want this upgrade:** diff `./gradlew :server:app:dependencies` across
2.2.0 → 2.9.x to find the conflicting transitive (likely a guava / protobuf /
grpc-context clash), align/force it, then bump `googleApiClient` in
`libs.versions.toml`. Low priority — current 2.2.0 works fine.

## Done

### bb-16u1 — Auto-generate inline CI trigger fails: expired `BOT_PAT` — RESOLVED 2026-06-10
`BOT_PAT` rotated by the maintainer and verified working: a manually dispatched
`Auto-Generate Content` run created auto-PR #1237, which picked up `Battery Butler
CI` with no `401 Bad credentials` (the inline close/reopen trigger fired). The
loud-failure follow-up is already in `auto-generate.yml` (`if ! gh pr close ...; then
… exit 1`, no `|| true`). Anchor kept here because `auto-generate.yml` error messages
reference bb-16u1. Related cleanup still open: [bb-j6td](#p3) (`GITHUB_TOKEN` fallback).

_(Move completed tasks here with a one-line outcome, or delete them.)_
