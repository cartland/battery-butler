# TODO

Project task tracking for Battery Butler.

> Migrated from Beads (`bd`) on 2026-06-08. Task IDs (`bb-xxxx`) are preserved as
> stable anchors because other docs and workflow comments cross-reference them.
> Working convention: edit this file directly. Move a task to **Done** (or delete
> it) when complete. Add new tasks under the appropriate priority heading.

## P2

### bb-gsi-staging — Labs staging sign-in only testable via local debug builds, not Play Store

**Context:** `bb-gsi-sha1` (below, in Done) fixed Labs **prod** sign-in on Play Store
release builds. Staging cannot share that fix: Google only lets one (package name +
signing certificate) pair be verified as an Android OAuth client under a single Google
Cloud project, globally — and since Battery Butler is one APK toggling between Labs
staging/prod via a Settings dropdown (not separate installs), only one project
(`cartland-labs`, prod) can own that verification for any Play-Store-signed build.

Pointing staging's client ID at prod's (tried on android/37) got past Credential
Manager but failed the token exchange: `signInWithIdp HTTP 400` — Firebase Auth's
built-in Google provider only trusts ID tokens whose audience is a client registered
under that *same* project, and there's no console setting to add a foreign project's
client ID as a trusted audience. `LABS_STAGING_GOOGLE_OAUTH_CLIENT_ID` was reverted to
its own value (`247996361369-...`, the `cartland-labs-staging` project's real client).

**To make Labs staging sign-in testable at all:** register the **debug keystore's**
SHA-1 (via `./gradlew signingReport`, a certificate not claimed by prod) as an Android
OAuth client under `cartland-labs-staging`'s project, then test staging sign-in only
via local debug builds — never via a Play-Store-installed release (those all share
prod's identity now). Not yet done; low priority since staging is a dev-only backend.

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

### bb-emult — `validate.sh` hangs on `pixel5api34DebugAndroidTest` when no emulator can boot

`./scripts/validate.sh` step 4 runs `:compose-app:pixel5api34DebugAndroidTest`
(Gradle Managed Device). In a headless / sandboxed environment where the GMD
emulator can't boot (no KVM/hardware accel, missing system image), the task
**hangs indefinitely** rather than failing fast — blocking the rest of local
validation (steps 5–8 never run). Observed repeatedly while landing the REST sync
work (PRs #1272 / #1275): each PR had to substitute a targeted gate set
(`spotless` + `detekt detektAndroidMain` + `check{Architecture,NamingConventions,HardcodedStrings,ImportBoundary}`
+ cross-platform compile) for the full run.

Dev-mode PR CI skips `validation_instrumented`, so this only bites **local**
validation — but an indefinite hang is worse than a skip.

**Options to evaluate:** (a) detect emulator/accel availability in `validate.sh`
and skip-with-warning (or add a `--skip-instrumented` flag) instead of hanging;
(b) add a timeout to the GMD task; (c) document the manual substitute-gate set for
machines without a bootable emulator. Pick whichever keeps "validate before push"
honest without an indefinite hang.

### bb-syncit — Live integration test for the Labs `/sync` wire contract (credential-gated)

The REST sync contract (`data-network/.../rest/SyncDto.kt`, PR #1272) is pinned two
ways today: per-side field tests (`SyncWireContractTest`) and a byte-shared golden
fixture (`SyncGoldenFixtureTest`, mirrored by the Labs backend's
`test/fixtures/battery-butler/` golden JSON). Together those catch a *transcription*
mismatch — but NOT future cross-repo *drift*: the two repos hold separate copies of
the fixture, and nothing fails CI if one is edited and the other isn't.

The hard guard is a live integration test exercising the real round-trip: client
builds a push → `POST /v1/battery-butler/sync` → `GET /sync` → assert the snapshot
matches. Add it once the `RestRemoteDataSource` lands (the follow-up PR) AND the Labs
auth credentials are set up (see the Labs backend's `BATTERY-BUTLER.md` → "Owner
setup: desktop client auth"). Gated on both.

Until then: **edit the golden fixtures in lockstep** across both repos on any contract
change. (An emulator-backed producer test on the Labs side is a separate, no-creds
option tracked there.)

### bb-labs-signin — Trigger the Labs sign-in (FirebaseIdTokenProvider.signInWithGoogle) — credential-gated

Workstreams D + E landed the Labs REST sync auth *structure*: PR #1285 (the `FirebaseIdTokenProvider`
core — Google ID token → Labs Firebase ID token via REST IdP, refresh, MockEngine-tested) and PR
#1286 (the wiring + config). `DelegatingRemoteDataSource` already uses `getIdToken` as the Bearer
provider, but nothing yet calls `signInWithGoogle()`, so `getIdToken` returns null and a Labs sync
runs unauthenticated (401). Follow-up: trigger the interactive Google→Labs exchange from a sign-in
flow — the UX was deliberately deferred (e.g. auto-exchange on the app's existing Google sign-in, or
an explicit "Connect to Labs" action in Settings when a Labs mode is selected). Functional only after
bb-labs-owner.

### bb-labs-owner — Owner setup: Labs OAuth client + Firebase Web API key + ORG_GRADLE_PROJECT_LABS_* secrets

For Labs REST sync to authenticate, the owner must: (1) create the Labs OAuth client (or whitelist
battery-butler's existing client) and obtain the Labs Firebase **Web API key** — see the Labs
backend's `BATTERY-BUTLER.md` → "Owner setup: desktop client auth"; (2) set
`ORG_GRADLE_PROJECT_LABS_FIREBASE_API_KEY` / `LABS_STAGING_URL` / `LABS_PROD_URL` (GitHub Actions
secrets and/or `local.properties`), which data-network's `BuildConfig` consumes (#1286); (3) grant
the user access on the Labs backend (`grant-access` by email). Unblocks bb-labs-signin and bb-syncit.

### bb-fbai-setup — Owner setup: real Firebase project for the AI chat (firebase-ai)

The cloud AI engine migrated from the deprecated `com.google.ai.client.generativeai` SDK to
`firebase-ai` (PR #1284) to remove a Ktor 2.x/3.x conflict that crashed the app on launch.
`compose-app/google-services.json` is currently a mock (`project_id: mock-project-id`), so the app
launches fine but the cloud AI chat reports unavailable. To enable it: create a Firebase project
with the **Gemini Developer API** enabled and add its real `google-services.json` (package
`com.chriscartland.batterybutler`). The on-device mlkit engine is unaffected.

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
the `skie` pin in `libs.versions.toml`, now `0.10.12`): (1) bump `skie`,
(2) delete the kotlin block in `.github/dependabot.yml` → `ignore:`, (3) remove the
`# Kotlin 2.3.20+ deferred …` comment in `libs.versions.toml` and bump
`kotlin` (and let Dependabot re-propose / bump `kotlinx-coroutines` etc.).
External-dependency-gated; closes when Kotlin is bumped past 2.3.0 with green CI.

**2026-06-20 update (PR #1256):** SKIE was bumped `0.10.9` → `0.10.12`, which supports
Kotlin up to **2.3.21** — so SKIE no longer blocks a Kotlin 2.3.x bump. The remaining
gate is **KMP-ObservableViewModel** (`1.0.1` pins Kotlin 2.3.0; see `observableviewmodel`
in `libs.versions.toml`) — bump it in lockstep. Note: Kotlin **2.4.0** still needs a
newer SKIE (0.10.12 maxes at 2.3.21), so the dependabot `org.jetbrains.kotlin:* >= 2.3.20`
ignore stays until BOTH SKIE and KMP-ObservableViewModel support the target Kotlin.

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

### bb-ncst — (Deferred) Evaluate KMP-NativeCoroutines `@NativeCoroutinesState` to remove manual `xxxValue` accessors

Option A (current) exposes KMP state to SwiftUI via KMP-ObservableViewModel + hand-written
per-VM `xxxValue` extension accessors (`var uiStateValue: … { uiState.value }`).
KMP-NativeCoroutines' `@NativeCoroutinesState` would KSP-generate those accessors instead,
removing the boilerplate.

**Deferred — adopt only if the manual accessors become a real maintenance drag**, because the
investigation (2026-06-20) found:
- It adds a **4th** Kotlin-version-pinned dependency (+ its KSP) — NOT already present
  transitively (KMP-ObservableViewModel 1.0.1 depends only on stdlib / coroutines-core /
  lifecycle-viewmodel; there are zero `nativecoroutines` refs in the build today).
- It does **NOT** fix the silent-non-observable footgun — that's already closed by
  `ExposedStateObservabilityConventionTest` in `:viewmodel:desktopTest` (PR #1255). So
  NativeCoroutines is now pure ergonomic convenience, not a correctness fix.
- It requires SKIE `FlowInterop` to be **disabled** (NativeCoroutines and SKIE flow interop
  conflict), but `FlowInterop` is load-bearing here — it provides the strong Swift typing for
  `StateFlow.value` that the current `.value` accessors rely on (PR #1256). So adoption is
  all-or-nothing across every screen, not incremental.

**If adopted:** wire the NativeCoroutines KSP, annotate exposed state with `@NativeCoroutinesState`,
disable SKIE `FlowInterop`, regenerate, and verify `@StateViewModel` re-render still fires when
Swift reads the generated `xValue` — with a local `./scripts/build-ios.sh` (dev-mode PR CI skips iOS).
Related: bb-k4sk (Kotlin/SKIE version coupling), `.agent/ios.md` (Option A pattern).

## Done

### bb-gsi-sha1 — Google Sign-In fails on Play Store release build ("No Google account found" / `NoCredentialException`) — DONE 2026-07-04 (PR #1312, android/36, android/37)
Reported on android/35: "Sign in to Labs" failed with "No Google account found" despite
real Google accounts on-device. `GoogleSignInBridge.android.kt`'s generic error catch was
silently swallowing the real exception, and Settings only showed a generic bucket
message. PR #1312 (android/36) added logging and switched Settings to show `error.cause`
instead — the next repro surfaced the real error: `[28444] Developer console is not set
up correctly`. Root cause: `LABS_PROD_GOOGLE_OAUTH_CLIENT_ID` (GitHub Actions variable,
wired into `release-android.yml`) held the **wrong** Web client ID —
`247996361369-88u7lkclu151dp0psg4gprnv5dvglu0b...`, actually Labs **staging**'s client ID,
apparently copy-pasted into prod's slot by mistake. Prod's real client ID (Firebase
Console → `cartland-labs` → Authentication → Sign-in method → Google → Web SDK
configuration) is `604157815175-a5t5fec1mqlo4u44skoa9rmfeb72bvts...`. Updated the
variable and released android/37; confirmed working on a Play-Store-installed build.
(The Play App Signing vs upload-keystore SHA-1 theory from the initial write-up was a
red herring — both were already registered correctly.) Attempting to share this fix for
Labs staging too (pointing its client ID at prod's) failed with `signInWithIdp HTTP 400`
— Firebase Auth's Google provider won't trust a token audience from a different project.
Staging's variable was reverted to its own value; follow-up tracked as `bb-gsi-staging`.

### bb-iosv — Fix build_ios_native (CONFIGURATION_BUILD_DIR broke SwiftPM) — DONE 2026-06-28 (PR #1270)
`build_ios_native` was red on `main` from the migration (#1250). Root cause: the job set
`CONFIGURATION_BUILD_DIR=build/`, which makes SwiftPM package products land in a flat dir the app's
`swiftc` doesn't search → "Unable to find module dependency: 'KMPObservableViewModelCore'" (only
`iosAppSwiftUI` consumes an SPM package, so the other iOS apps were fine). #1260
(`-resolvePackageDependencies`) and #1265 (drop DerivedData cache + `clean build`) mis-diagnosed it as
a stale cache and did NOT fix it. #1270 dropped `CONFIGURATION_BUILD_DIR` and builds into
`-derivedDataPath build/ios-build` (matching the passing `validation_ios_ui` job + `scripts/build-ios.sh`).
Reproduced + fixed locally, then **confirmed green on the f6e71369 push run**.

### bb-cidsp — Add ci_mode override to workflow_dispatch — DONE 2026-06-28 (PR #1267)
`workflow_dispatch` now takes a `ci_mode` (development/release) input that overrides
`.github/ci-mode.txt`, and a manual run forces `code=true`, so
`gh workflow run "Battery Butler CI" --ref main -f ci_mode=release` runs the full suite (incl. the iOS
jobs) on demand — which is what let us finally run + diagnose build_ios_native (bb-iosv).

### bb-ovm1 — Migrate all 16 iOS ViewModels to KMP-ObservableViewModel — DONE 2026-06-20 (PR #1250)
Replaced every hand-written Swift `*ViewModelWrapper` (Task/for-await/`KmpViewModelStore`/deinit)
with KMP-ObservableViewModel `@StateViewModel`, keeping SKIE (Option A — no NativeCoroutines).
KMP VMs extend `com.rickclephas.kmp.observableviewmodel.ViewModel`; state via observable
`safeStateIn(viewModelScope,…)` / `retryableStateIn(…)` / `MutableStateFlow(viewModelScope,…)`;
screens read `xxxValue` accessors. Deleted orphaned `KmpViewModelStore`; relocated
`LoginErrorInfo`/`SettingsDisplay`; synced `.agent/ios.md`. ObservableViewModel is
Kotlin-version-pinned → bump in lockstep with Kotlin (bb-k4sk). Counter `appCounterRunning` kept
direct-forwarded (observable `stateIn` broke a synchronous-`.value` test). Pattern lives in
`.agent/ios.md`; the dev-mode-CI pre-merge lesson is in `.agent/AGENTS.md`.

### bb-16u1 — Auto-generate inline CI trigger fails: expired `BOT_PAT` — RESOLVED 2026-06-10
`BOT_PAT` rotated by the maintainer and verified working: a manually dispatched
`Auto-Generate Content` run created auto-PR #1237, which picked up `Battery Butler
CI` with no `401 Bad credentials` (the inline close/reopen trigger fired). The
loud-failure follow-up is already in `auto-generate.yml` (`if ! gh pr close ...; then
… exit 1`, no `|| true`). Anchor kept here because `auto-generate.yml` error messages
reference bb-16u1. Related cleanup still open: [bb-j6td](#p3) (`GITHUB_TOKEN` fallback).

_(Move completed tasks here with a one-line outcome, or delete them.)_
