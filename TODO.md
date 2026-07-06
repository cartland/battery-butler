# TODO

Project task tracking for Battery Butler.

> Migrated from Beads (`bd`) on 2026-06-08. Task IDs (`bb-xxxx`) are preserved as
> stable anchors because other docs and workflow comments cross-reference them.
> Working convention: edit this file directly. Move a task to **Done** (or delete
> it) when complete. Add new tasks under the appropriate priority heading.

## P2

### bb-android42-release — Finish the in-progress android/42 release

**In progress 2026-07-06.** PR #1324 (data-location isolation fix, merged, main
HEAD `f44313ce`) needs to ship as android/42. Sequence so far:
1. `./scripts/release-android.sh --check` on `f44313ce` found the sentinel jobs
   had no completed run (the push-to-main CI ran in `development` mode per
   `.github/ci-mode.txt`, so build/instrumented/iOS jobs were skipped).
2. Dispatched release-mode CI manually: `gh workflow run "Battery Butler CI" --ref main -f ci_mode=release`
   → run `28782166382`. As of this note, most jobs passed
   (`build_android`, `build_server`, `validation_instrumented`, `build_desktop`);
   only `validation_ios_ui`/`build_ios_native`/`build_ios_compose` were still running.

**Next steps for whoever picks this up:** confirm run `28782166382` finished
successfully (`gh run view 28782166382 --json status,conclusion`), re-run
`./scripts/release-android.sh --check` to confirm the sentinel gate now passes,
then run the actual release **only after the user explicitly confirms the tag
name** (`android/42` expected, but re-derive via `--check`'s "Next tag" line in
case another release landed first) — per `.agent/AGENTS.md` Critical Rule 2,
never add a release-script override flag or run the tagging step without that
explicit confirmation.

There's also a separate PR #1326 (Devices default-sort-order feature, CI green
as of this note, not yet merged) that the user may want folded into this same
release once merged — check its status too.

### bb-data-location-rename — Rename `NetworkMode` → `DataLocation` throughout the codebase (deferred follow-up)

**Deferred 2026-07-06** from the `NetworkMode`-local-database-isolation fix (see the
Done section entry for that PR). The user's original ask was to review data
isolation across network switches and floated calling the concept "data
location" instead of "network mode," since selecting a mode is really selecting
an entire local dataset, not just a remote backend. The isolation *guarantee*
was fixed separately and does not require this rename — this task is purely
about renaming the concept everywhere it appears, for clarity:

- `domain/model/NetworkMode.kt` (the sealed interface itself) and its 7
  variants (`None`, `Mock`, `GrpcLocal`, `GrpcAws`, `GrpcDev`, `LabsStaging`,
  `LabsProd`) — decide whether `Mock`/`None` (not really "locations") stay as-is
  or get folded differently.
- `NetworkModeRepository`/`DataStoreNetworkModeRepository`, `DelegatingRemoteDataSource`,
  and every other repository/class name built on "NetworkMode".
- User-facing Settings UI copy — currently literally labeled **"Network Mode"**
  (`presentation-core/.../ExpandableSelectionControl.kt`, `SettingsContent.kt`'s
  "Network Mode Card").
- ~94 Kotlin files reference `NetworkMode` repo-wide (per a 2026-07-06 grep) —
  this is a large, high-file-count rename that touches user-visible strings, so
  give it its own explicitly-scoped PR (or a small number of PRs) rather than
  bundling it with unrelated work. Not urgent; purely a clarity/naming
  improvement, no behavior change implied.

### bb-play-pub-stale — `docs/GOOGLE_PLAY_PUBLISHING.md` is stale relative to the actual release flow

**Found 2026-07-04** while debugging Labs sign-in (`bb-gsi-sha1`) and releasing
android/36–38. The doc describes a `publish-android.yml` workflow that no longer
exists (`.github/workflows/` only has `release-android.yml`) and a simplified
"just push a tag" flow with no mention of the sentinel-CI-gate / `--check` /
`--confirm-tag` wrapper (`scripts/release-android.sh`) that's actually required
today — see the `release-android` skill and `.agent/ci.md` § Pre-Release CI Gate.
Following the doc as written (a raw `git tag android/N && git push origin android/N`)
would bypass the sentinel gate entirely.

The secrets it lists (`KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD`,
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`) do still match what `release-android.yml`
actually reads — only the workflow name and release procedure are stale.

**Fix:** update `docs/GOOGLE_PLAY_PUBLISHING.md` to reference `release-android.yml` /
`scripts/release-android.sh` and describe the sentinel-gate flow (or fold its
secrets-setup content into the `release-android` skill / README and retire this
file). Small, docs-only; not urgent since the secrets info is still correct and the
skill doc is the actual source of truth agents follow.

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

### bb-cli-backup-import — `:cli push` can't consume the app's own Export Data backup format

**Found 2026-07-06** while clearing + restoring Labs staging and prod from a
Google-Drive-hosted `Settings → Export Data` backup file. `cli push <file>` (see
`cli/src/main/kotlin/.../cli/Main.kt`) decodes the file straight into
`SyncPushRequestWire` (flat `deviceTypes`/`devices`/`events`/`deleted*Ids`, epoch-ms
timestamps). The app's own backup format (`usecase/.../BackupDto.kt`) is a
different, wrapped shape (`{"data":{"devices":[...],...}}`, ISO-8601 date strings,
different optional fields — see `DeviceDto`/`DeviceTypeDto`/`BatteryEventDto`).
Feeding a real export file to `push` today doesn't error: every `SyncPushRequestWire`
field has a default, so it silently pushes empty arrays (a no-op that looks like
success). Had to hand-write a throwaway Python conversion script for the actual
restore this session (not committed — one-off).

**Fix options:** (a) add a `restore-backup <file>` subcommand to `:cli` that parses
the `BackupContainer` shape and maps each DTO to its Wire equivalent (mirroring
`ImportDataUseCase`'s mapping, including the ISO-string → epoch-ms conversion), or
(b) at minimum, make `push` reject/error on an unrecognized top-level shape instead
of silently defaulting to empty. (a) is more useful; (b) is the safety-net minimum.

### bb-labs-scope-editors — Document/handle the Labs backend's `editors` scope requirement for prod writes

**Found 2026-07-06**: `POST /v1/battery-butler/sync` against Labs **prod**
(`cartland-labs`) rejected an authenticated write with `HTTP 403
{"error":{"code":"forbidden","message":"requires scope 'editors'"}}` even though
the identical request against **staging** succeeded for the same user. This is
authorization enforced by the Labs backend itself (a separate service, not in this
repo) — granting the account `editors` on the backend's admin side resolved it.
Noted in README's Labs CLI section so a future `cli push`/restore against prod
isn't blocked by a mystery 403. No repo-side code change identified yet; revisit
if the backend's admin/scope-granting process itself needs documenting here.

## P4

### bb-cli-test-data — Clean up leftover `cli-test-type-1` test data on Labs staging

While building and testing the `:cli` module's `push` subcommand this session, a
test device type (`cli-test-type-1`, "CLI Push Test") was pushed to the **real**
Labs staging backend. It's harmless (not real user data, staging only), but it
should either be deleted via `cli push` with a tombstone/delete payload, or left
with a note so a future session doesn't mistake it for genuine staging data. Not
urgent — cosmetic cleanup only.

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

### bb-labs-mode-auth-state — Labs Sign-In showed a stale account after switching NetworkMode — DONE 2026-07-05 (structural fix, `NetworkModeKeyedState`)
Found while auditing whether the app isolates credentials correctly across Labs staging/prod.
Confirmed live on-device: sign in to Labs Prod, switch Network Mode to Labs Staging — Settings
kept showing the **prod** account as "signed in" even though staging was never authenticated;
tapping "Copy Labs ID Token" in that state silently did nothing (the token really was null for
staging, but the UI gave no feedback). No credential leakage — `DefaultLabsAuthGateway` already
correctly partitions **token** sessions per Firebase API key — but `DefaultLabsAuthRepository`'s
`_labsAuthState` was a single unpartitioned `MutableStateFlow`, so switching modes never reset or
re-evaluated it.

**Structural fix, not just a patch:** added `domain/model/NetworkModeKeyedState.kt` — a small,
directly-unit-tested (`NetworkModeKeyedStateTest.kt`, 3 tests, no fakes needed) class that holds
one value per network-mode-derived key and reactively exposes whichever one is current, so
there's no unpartitioned field left to go stale. `DefaultLabsAuthRepository.labsAuthState` now
uses it (keyed the same way `DefaultLabsAuthGateway` already keys token sessions, via
`apiKeyForMode`). `LabsAuthRepository.clearError()` became `suspend fun` as part of this (needs
to read the current network mode). Intent: any *future* per-environment state should reach for
this class instead of a bare `MutableStateFlow`, making this bug category structurally harder to
reintroduce. Verified live: prod→staging now correctly shows unauthenticated; staging→prod
correctly restores prod's own session (not just reset to default).

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
red herring — both were already registered correctly.) The product goal is for one
production APK to sign in against **either** Labs backend, but Google only lets one
(package name + signing certificate) pair be verified as an Android OAuth client under
one Google Cloud project globally — so only prod's project can own Credential Manager
verification for the Play-distributed app. First attempt to share prod's client ID for
staging too failed at the token exchange (`signInWithIdp HTTP 400` — Firebase Auth's
built-in Google provider only trusts audiences registered under that same project).
Fix: **Identity Platform** (the advanced admin surface behind Firebase Auth) has an
"Allowed client IDs" list on the Google provider config, for exactly this — added
prod's Web client ID there under `cartland-labs-staging`'s project (`console.cloud.google.com`
→ staging project → Identity Platform → Providers → Google → Allowed client IDs → Add),
then pointed `LABS_STAGING_GOOGLE_OAUTH_CLIENT_ID` at prod's client ID too (android/38).
Confirmed both Labs prod and Labs staging sign-in work from the same production build.

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
