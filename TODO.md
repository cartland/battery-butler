# TODO

Project task tracking for Battery Butler.

> Migrated from Beads (`bd`) on 2026-06-08. Task IDs (`bb-xxxx`) are preserved as
> stable anchors because other docs and workflow comments cross-reference them.
> Working convention: edit this file directly. Move a task to **Done** (or delete
> it) when complete. Add new tasks under the appropriate priority heading.

## P2

### bb-16u1 — Auto-generate inline CI trigger fails: expired `BOT_PAT` (USER ACTION REQUIRED)

> ⚠️ Still unfixed as of 2026-06-08, ~4 weeks after first report. The
> `BOT_PAT`-authenticated inline CI trigger in `auto-generate.yml` (and the
> `ci-trigger-auto-prs.yml` fallback) returns `HTTP 401: Bad credentials` on
> every run, so auto-generated PRs (diagrams, screenshots) get no CI until a
> human kicks them. Recurs weekly — observed `CI for Auto PRs` → `trigger-ci`
> failures on 2026-06-01 and 2026-06-08.

**Root cause:** `secrets.BOT_PAT` is expired/invalid. The `gh pr close || true` /
`gh pr reopen || true` trigger step swallows the failure, so the workflow goes
green while the trigger does nothing. The `secrets.BOT_PAT || secrets.GITHUB_TOKEN`
fallback in `ci-trigger-auto-prs.yml` doesn't help — `GITHUB_TOKEN` can't trigger
other workflows by design.

**Fix required (user action — an agent cannot edit repo secrets via CLI):**
1. Generate a new fine-grained PAT for `cartland/battery-butler` with
   `Pull requests: Read and write` + `Workflows: Read and write` (or a classic
   PAT with `repo` + `workflow` scopes).
2. Update the `BOT_PAT` secret in Settings → Secrets and variables → Actions.
3. Verify: `gh workflow run "Auto-Generate Content"` and confirm the resulting
   auto-PR gets full CI checks (not just GitGuardian) within ~1 min.

**Agent-doable follow-up:** remove `|| true` from the trigger step so the failure
becomes visible, or `continue-on-error: true` + a step that posts a PR comment on
failure. Manual workaround meanwhile: `gh pr close <N> && gh pr reopen <N>` from a
session whose token is valid.

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

## Done

_(Move completed tasks here with a one-line outcome, or delete them.)_
