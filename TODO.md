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

### bb-data-location-rename — Rename `NetworkMode` → `DataMode` throughout the codebase — DONE 2026-07-07

**Deferred 2026-07-06** from the `NetworkMode`-local-database-isolation fix (see the Done section
entry for that PR), floated then as "data location". **Superseded 2026-07-07**: the user gave an
explicit instruction to call it **"Data Mode"** instead, plus two related behavior changes bundled
into the same PR:

- Renamed `NetworkMode` → `DataMode` (sealed interface, repository, use case, `DataModeKeyedState`,
  DI bindings, strings, docs, iOS Swift bindings) across ~90 files. The sealed variant names
  themselves (`None`, `Mock`, `GrpcLocal`, `GrpcAws`, `GrpcDev`, `LabsStaging`, `LabsProd`) were
  deliberately left unchanged — renaming `LabsStaging`/`LabsProd` would have cascaded into the
  entire Labs auth subsystem (`LabsAuthRepository`, `LabsSessionStorage`, `apiKeyForMode`, etc.),
  which was out of scope. The on-disk DataStore preference key literal (`"network_mode"`) was also
  deliberately left unchanged, so existing installs keep their saved selection across the update.
- Reordered + relabeled the visible Settings picker to **Device only / Production / Staging /
  Mock** (`None` / `LabsProd` / `LabsStaging` / `Mock`), per the user's requested ordering.
- Added `FeatureFlag.LEGACY_DATA_MODES` (disabled by default in both `AppComponent` and
  `NativeComponent`) to hide the legacy own-backend modes (`GrpcLocal`, `GrpcAws`, `GrpcDev` —
  AWS infrastructure is hibernated) from the picker unless explicitly enabled.

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

### bb-labs-signout-clear — Clear local Labs data on sign-out, resync on sign-in — DONE 2026-07-06 (structural fix, `SignOutLabsUseCase`/`SignInToLabsUseCase`)

**User report**: "when I am signed out and change network, [both Labs environments] show data" — read as an isolation regression, but investigation showed the per-environment file isolation (bb PR #1324) was working correctly; the actual gap was that the Devices/Types/History tabs read local Room data with zero awareness of Labs auth state (by original offline-first design) and nothing had ever cleared local data on sign-out, so a previously-synced environment's cached devices stayed visible indefinitely after signing out.

**Fix**: `DeviceRepository.clearAllLocalData()` (new) wired through two new UseCases — `SignOutLabsUseCase` (signs out + clears local data) and `SignInToLabsUseCase` (signs in + triggers an immediate `resync()` on success) — composed at the usecase layer rather than inside `DefaultLabsAuthRepository` itself, since that class takes an unfakeable platform `expect class` (`GoogleSignInBridge`) and is exempted from direct unit testing. `LoginViewModel`/`SettingsViewModel` now call these UseCases instead of the repository sign-in/out methods directly. Verified end-to-end on a real emulator against real Labs Prod data: sign-out immediately empties all three tabs; sign-in repopulates them.

**Follow-up finding (not fixed, tracked separately below)**: on a *cold* Labs backend (first request after being idle), the immediate resync can exceed its 15s timeout — see `bb-labs-cold-resync`. This isn't a correctness bug (the ambient background sync loop still eventually recovers), just a latency characteristic worth improving.

**Deferred**: the app's own gRPC `AuthRepository.signOut()` has the same gap (never clears local data) but is architecturally different (one global account spans all gRPC modes, vs. Labs's per-environment sessions) — not fixed here, flagged as a fast, low-risk follow-up if wanted.

### bb-labs-persist-signin-belief — Persist "believed signed in to Labs" across process restarts — DONE 2026-07-07

**Follow-up to `bb-labs-signout-clear`**: after that fix shipped (android/44), user asked whether more protection was needed. Investigation found a remaining gap: `labsAuthState` is in-memory only and always resets to `Unauthenticated` on process launch, so a user who is genuinely signed in but whose app process gets killed (common on Android under memory pressure) sees "Sign in to Labs" at the same moment the Devices/Types/History tabs still show their real, legitimately-cached data — the same contradictory symptom as the original bug, just triggered by a process restart instead of an explicit sign-out. A naive "clear local data whenever Unauthenticated is observed" fix was rejected — every cold start starts `Unauthenticated`, so this would wipe the cache on every restart and defeat offline-first caching.

**Fix**: mirrored the existing pattern already used by the app's own gRPC `DefaultAuthRepository` (which persists via `AuthTokenStorage` and starts `AuthState.Unknown`, resolving from storage once). Added `LabsSessionStorage`/`DataStoreLabsSessionStorage` (`data-local/.../auth/`) — a lightweight, per-Labs-environment (staging/prod, keyed the same way as the existing in-memory `DataModeKeyedState`) persisted "believed signed in" flag storing only profile info (`User`), no tokens, no expiry. `DefaultLabsAuthRepository`'s `authStateByMode` now defaults to `AuthState.Unknown` and resolves to `Authenticated`/`Unauthenticated` from persisted storage in an `init` block, guarded by a new generic `DataModeKeyedState.compareAndSet` so it never clobbers a real sign-in/out that happens first. Sign-in saves the belief; sign-out (already called via `SignOutLabsUseCase`) clears it alongside the local data clear. Deliberately does not re-validate a real session or track expiry — per explicit user scoping ("I don't actually care about expired auth... I just care that we remember that we think we are signed in"). Verified end-to-end on a real emulator: sign in, `adb shell am force-stop` + relaunch → Settings shows the signed-in account immediately (no false "Sign in to Labs"), Devices tab still shows cached data; sign out, force-stop + relaunch → correctly shows signed-out with an empty Devices tab.

### bb-labs-cold-resync — Immediate post-sign-in resync can time out on a cold Labs backend — Partially DONE 2026-07-07

**Found 2026-07-06** while manually verifying `bb-labs-signout-clear` on a real emulator: the first sign-in after the Labs backend had been idle for a while took about 4 minutes for devices to actually populate (the new `SignInToLabsUseCase`'s immediate `resync()` call has a 15s timeout inherited from `DefaultSyncManager.RESYNC_TIMEOUT`, likely tied to a cold-start delay on the Labs backend's hosting). A second sign-in shortly after (backend already warm) synced within ~15 seconds, confirming this is backend cold-start latency, not a logic bug — the resync silently times out (caught, logged, `SyncStatus.Failed`), and the pre-existing ambient `subscribeWithRetry()` background loop is what eventually recovers, exactly as it did before this session's fix.

**Fix (partial)**: `DeviceRepository`/`SyncManager.resync()` now takes an optional `timeout: Duration` param (default `DEFAULT_RESYNC_TIMEOUT` = 15s, unchanged for pull-to-refresh's interactive spinner). `SignInToLabsUseCase` now passes a longer 60s timeout for its post-sign-in resync, since that call already has a natural loading moment and is more likely to hit a cold backend. **Not fully closed**: 60s still doesn't cover the observed ~4-minute worst case, and no distinct "still connecting" UI state was added (the generic `SyncStatus.Failed` still shows if 60s isn't enough) — left as-is since the ambient background loop recovers regardless; revisit only if the 60s bump proves insufficient in practice.

### bb-labs-silent-reauth — Opportunistic silent Labs re-auth on process restart — DONE 2026-07-07

**Follow-up to `bb-labs-persist-signin-belief`**: that fix persisted the *belief* that the user is signed in to Labs so the UI doesn't contradict itself after a process restart, but deliberately left the *real* gateway session unrestored — meaning background sync could keep failing silently until the user explicitly re-signed in (whose entry point was now hidden, since the UI shows Authenticated). Flagged as the most concrete remaining gap when asked "do we need more protections."

**Fix**: added `GoogleSignInBridge.signInSilentlyWithClient(clientId, clientSecret)` to the expect/actual (`data-network/.../auth/`). Android actual reuses the existing Credential Manager `performSignIn` path with `setFilterByAuthorizedAccounts(true)` instead of `false` (succeeds only if the account was previously authorized, no UI). iOS/Desktop actuals always return `Result.Error` immediately — neither uses a native Sign-In SDK (both are hand-rolled interactive OAuth flows with no persisted, silently-restorable session), so there's nothing to check without showing UI. `DefaultLabsAuthRepository`'s `init` block now calls a new `attemptSilentReauth()` right after resolving `AuthState.Unknown` from the persisted belief (guarded by `DataModeKeyedState.compareAndSet`'s new boolean return, so it only fires once per belief-resolution, not on every subsequent explicit sign-in). Best-effort by design: on failure it changes nothing (no state rollback, no UI impact) — background sync just keeps failing as it would without this attempt, exactly matching the tolerance already established for `bb-labs-persist-signin-belief`. Verified end-to-end on a real emulator: sign in, `adb shell am force-stop` + relaunch → logcat confirms `"Silent Labs re-auth succeeded; real session re-established"`, Settings' "Copy Labs ID Token" reflects a live token, no dialog ever shown.

**Deferred**: real silent re-auth for iOS/Desktop would require adopting a native Sign-In SDK or building refresh-token persistence for the existing PKCE flows — a materially bigger feature, not done here. Those platforms rely solely on the persisted-belief UI fix; their real session still needs an explicit re-sign-in after a process restart if the ambient in-memory session was lost.

### bb-auth-session-length — Own-backend session forced the interactive Google account picker every ~1h — DONE 2026-07-08

**User report**: "when I return to the app after a while I get a popup to select my Google account" every "few hours." Investigation found the OWN-backend (gRPC) session token — separate from the Labs session above — expired after 24h if the server verified it, or a hardcoded **1 hour** (`DefaultAuthRepository.LOCAL_TOKEN_EXPIRY_MS`) if only locally-verified (e.g. server unreachable at sign-in). `refreshToken()` was a stub that always errored ("user must re-authenticate"), and neither the in-memory expiry timer nor a cold start finding an already-expired stored token ever attempted anything before dropping straight to `AuthState.Unauthenticated` — the only path back in was the fully interactive account picker (`GoogleSignInBridge.signIn()`, `filterByAuthorizedAccounts=false`).

**Fix**: added `GoogleSignInBridge.signInSilently()` to the expect/actual (mirrors the Labs-specific `signInSilentlyWithClient` above, but uses the bridge's already-`initialize()`d default client instead of an explicit Labs client — so no new client-id plumbing needed). Android reuses `performSignIn(clientId, filterByAuthorizedAccounts = true)`; iOS/Desktop always return `Result.Error` (same reasoning as Labs — hand-rolled interactive flows, nothing to check silently). `DefaultAuthRepository`'s `scheduleTokenExpiry` and cold-start init path both now call a new `attemptSilentRefresh()` instead of clearing the token outright: on success it calls the existing `verifyWithServer(...)` exactly as an explicit sign-in would (re-arming the next expiry itself, so the loop self-perpetuates); on failure it falls back to the original clear-and-sign-out behavior. Cold start with an already-expired stored token now shows the believed-signed-in user optimistically (`toAuthState()` no longer checks expiry) while the silent refresh runs in the background, instead of flashing signed-out first — same rationale as `bb-labs-persist-signin-belief`.

**Result**: on Android, the account picker should now only ever appear on a genuinely first sign-in or after the user explicitly signs out / revokes access on-device — not on a routine ~1h/24h expiry. iOS/Desktop are unchanged (same deferred-scope reasoning as `bb-labs-silent-reauth` above).

## P3

### bb-dimg — Device photos: capture, upload, and display a per-device image (Labs backend)

**Status (2026-07-19): workstreams A–F implemented AND live-verified end-to-end against
Labs staging; branch `device-images-a`, draft PR #1359 ready for review/merge.** On an
Android emulator signed in as `chriscartlanddemo@gmail.com` in Staging mode: picked a
real image via the Android Photo Picker → normalized → uploaded (HTTP 200, real etag
returned) → displayed correctly on Edit Device, Device Detail, and the device list →
removed cleanly (buttons revert, avatar reverts to the fallback icon). Root-caused two
false alarms during that pass rather than shipping around them: (1) an "Unable to check
Uri permission... WM lock" system log that looked causal but was unrelated noise: the
picker→bytes→normalize→upload chain works fine regardless; (2) intermittent HTTP 401s
on upload traced to `DefaultLabsAuthGateway`'s best-effort, already-documented
refresh-token restore race on a cold process start (`bb-labs-refresh-token-persistence`)
— not a regression from this work, reproduces only when acting faster than a human
right after launch. Remaining follow-ups noted below (Add Device photo, Detail-screen
tap-to-change) are unchanged.

**Full-diff review pass (2026-07-20)** before recommending release found and fixed three
real defects (independently corroborated by a second reviewer pass, not just self-review):
Android decoded picked photos at full resolution before downscaling (a 50MP camera photo
could OOM on a typical heap — now decodes at a memory-bounded `inSampleSize` first);
Desktop/JVM never read EXIF orientation at all, so a portrait phone photo picked on
Desktop shipped permanently sideways (normalization strips EXIF on re-encode, so there's
no recovering it downstream — added a dependency-free JPEG/EXIF reader + transform,
verified empirically via pixel-sampling tests across all 8 orientation values, which
caught a real sign error in the "transverse" case on the first pass); and a picked image
that fails local normalization (corrupt/unsupported) silently did nothing in the UI —
now surfaces a proper error. Two lower-severity items were deliberately deferred rather
than fixed in this pass: no loading indicator / double-tap guard during
upload-or-remove (idempotent PUT/DELETE, so a duplicate request just wastes bandwidth,
doesn't corrupt anything), and `HomeViewModel`'s per-etag image map re-queries Room on
every sort/group change, not just when the device set's etags actually change (extra
local reads, not a correctness issue).

Let each device optionally have one photo, shown next to its name/location: pick →
upload → replace/remove, displayed in the detail avatar + list item. **Full spec:
[`docs/DEVICE_IMAGES.md`](docs/DEVICE_IMAGES.md)** — read it first; this is the summary.

**The backend half is already live** — the Labs backend serves an emit-only
`imageEtag` on each snapshot device plus three `PUT/GET/DELETE
/v1/battery-butler/devices/{id}/image` routes (10 MB cap; JPEG/PNG/WebP only; the
device must be synced first). No coordinated backend change is needed; the client
can build against Labs staging independently.

**Key facts that shape the work** (details + anchor files in the spec):
- `imagePath` is **already wired end-to-end** (domain, proto, wire DTO, Room, sync)
  but **dormant in the UI** — reuse that plumbing; don't rebuild it. `imageEtag`
  (new, server-managed) is the cache key + change signal, kept **separate** from the
  client-owned `imagePath`.
- **No image library, picker, camera, or HEIC/downscale code exists anywhere** —
  capture + normalize-to-JPEG (iOS HEIC→JPEG especially) + display are new, per
  platform (Android Photo Picker / iOS PHPicker / desktop file chooser).
- **Labs-mode only** — blob images exist only on the Labs backend; gate the photo UI
  on backend capability (Mock/gRPC/None stay icon-only).

**Workstreams** (spec §6, recommended order A→B→D→C→E→F, small PRs): (A) mirror
`imageEtag` in `SyncDto.kt` + contract/golden tests in lockstep; (B) binary transport
via a `DeviceImageDataSource` on `RestRemoteDataSource`; (C) per-platform pick +
JPEG normalize; (D) etag-keyed byte cache + `ImageBitmap` display; (E) upload/replace/
delete orchestration (push device *before* first upload); (F) UI in Add/Edit + detail/
list + screenshot baselines. Success criteria in §9.

**Deliberate scope cut for (E)/(F):** photo upload/replace/remove UI landed on the
**Edit Device screen only**. Add Device was skipped because the backend 404s an
image PUT for a not-yet-synced device (would need to hold picked bytes across the
create→sync→upload boundary — real complexity, deferred). Tap-to-change from the
Device Detail screen (which already *displays* the photo, from workstream D) was
skipped purely to bound scope. Both are natural small follow-ups once E2E-verified.
Android/iOS picker + normalizer code is compile-verified only (no real device/
simulator manual testing in this environment) — same accepted gap as other
unfakeable platform bridges (e.g. `GoogleSignInBridge`).

### bb-anim-ios-record-flight — SwiftUI parity for the record-replacement flight animation

The Compose Device Detail screen animates a newly recorded battery event flying
from the "Record replacement" button into its spot in the history list, with the
list auto-scrolling to keep the landing visible (`presentation-feature/.../devicedetail/RecordReplacementFlight.kt`
+ `DeviceDetailContent.kt`). The native SwiftUI `DeviceDetailScreen.swift` has no
equivalent. SwiftUI building blocks: `matchedGeometryEffect` for the button→row
morph, `ScrollViewReader.scrollTo` for the scroll, `withAnimation`/`transition`
for the list insertion. Tracked in `docs/UI_SCREENS_MAPPING.md` gap table.

### bb-anim-ideas — Animation backlog: motion polish across the app

Breadth survey done 2026-07-09 (deep dive landed as the record-replacement
flight above). The app is otherwise almost motion-free — only the sync-status
fade (`HomeScreenContent.kt`), the AI chat panel height (`MainScreen.kt`), and a
chevron rotation (`ExpandableSelectionControl.kt`). Candidate animations, roughly
ordered by payoff/effort:

- [ ] **`Modifier.animateItem()` on the three tab lists** — Home
  (`HomeScreenContent.kt:351`), Types (`DeviceTypeListContent.kt:165`), History
  (`HistoryListContent.kt:102`). All already use stable `key = { it.id }`, so
  insert/remove fades and — the big one — items gliding to their new positions
  when sort/group options change are nearly free. Do this one first.
- [ ] **Animated sort/group transitions** — pairs with the above: when
  `sortOption`/`isSortAscending` changes, rows glide instead of snapping.
  Also rotate the sort-direction arrow icon (Home/Types filter rows).
- [ ] **Staggered first-load entrance** — items fade+slide up with a small
  per-index delay on first composition of a tab list. Guard so it runs once per
  screen entry, not on every recomposition.
- [ ] **AI chat message entrance + typing indicator** — new chat bubbles slide
  in (`AiTabContent.kt`); replace the static "thinking" state with a pulsing
  three-dot indicator.
- [ ] **Battery icon fill on record** — after a replacement is recorded, animate
  the `BatteryFull` icon on the new row (or the stat card) filling from empty —
  reinforces "fresh battery".
- [ ] **Shared-element list→detail transition** — device card icon/name morphs
  into the Device Detail header (`SharedTransitionLayout` + NavDisplay).
  Highest wow, highest risk; prototype behind a small scope first.
- [ ] **Pull-to-refresh custom indicator** — replace the stock spinner with a
  battery outline that fills while refreshing (`PullToRefreshBox` custom
  `indicator` slot; Home/Types/History).
- [ ] **Press-scale micro-interaction as a shared modifier** — extract the
  record button's press-scale (added with the flight PR) into a
  `Modifier.pressScale()` helper in `presentation-core` and apply to list cards
  and primary buttons app-wide.
- [ ] **Bottom-nav selection indicator** — animate the active-tab pill/icon
  (scale or sliding indicator) in `MainScreenShell`'s NavigationBar.
- [ ] **Count-up stats** — Device Detail stat cards and "N days" labels animate
  from 0/previous value on first show (`animateIntAsState`).
- [ ] **Empty-state breathing** — idle scale/opacity loop on `EmptyStateContent`
  illustrations; must respect reduced-motion once a platform signal is available.
- [ ] **iOS parity decision per item** — each shipped Compose animation should
  get a row in `docs/UI_SCREENS_MAPPING.md` (see bb-anim-ios-record-flight).

### bb-uxsync — Re-sync iOS palette + design doc to the revamped Android theme

The Android theme (`presentation-core/.../theme/Color.kt` + `Theme.kt`) was overhauled:
every Material 3 color role is now defined for both light and dark (previously the
surface-container / surface-variant / inverse / outline-variant roles were unset and
fell back to Material's cold purple baseline, which looked bad in dark mode). Several
brand tones were also adjusted for WCAG AA contrast (primary/secondary/tertiary light
darkened slightly; dark secondary/tertiary switched from the shared mid-tones to proper
lighter dark-mode tones). `docs/design/IOS_DESIGN_LANGUAGE.md` §2 and the iOS SwiftUI
`Color` constants still carry the OLD hex values — re-sync them so the two platforms
keep a shared identity. Source of truth is the new `Color.kt`.

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

**Related symptom found 2026-07-07**: on a machine where the emulator *can* boot,
running `validate.sh` concurrently in two+ git worktrees makes both try to boot
the same Gradle Managed Device AVD name (`dev34_google_apis_arm64-v8a_Pixel_5`),
which errors with "Running multiple emulators with the same AVD is an
experimental feature. Please use -read-only flag to enable this feature." See
`bb-worktree-validate-collision` for the full writeup (this is one symptom of a
broader "don't run `validate.sh` concurrently across worktrees" problem, now also
documented in `.agent/AGENTS.md`).

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

### bb-screenshot-flake — Intermittent pixel-level screenshot test flakiness unrelated to actual UI changes

**Found 2026-07-06** while updating Home-screen screenshot baselines for the
default-sort-order change. Running `validateDebugScreenshotTest`/`updateDebugScreenshotTest`
twice on an otherwise-unchanged commit produced pixel-different (but *visually
identical*, confirmed by side-by-side image comparison) renders for
`TabletHistoryTest_Light` (`PlayStoreTabletScreenshotTestKt`) and
`Tablet10DeviceDetailTest_Light` (`PlayStoreTablet10ScreenshotTestKt`) — neither
screen was touched by the change being validated. A separate run also showed
`AddDeviceTypeScreenPreviewTest`/`AddBatteryEventScreenPreviewTest`/
`EditDeviceTypeScreenPreviewTest` (`ScreensScreenshotTestKt`) failing for what
looked like the same reason. Likely font-hinting/anti-aliasing nondeterminism in
the Android Studio preview renderer between separate JVM invocations, not a real
regression. Left these baselines untouched (reverted after confirming
byte-near-identical, visually-identical diffs) rather than blindly accepting
whatever a given run happened to render.

**Worth investigating:** whether this also causes spurious `validation_screenshots`
CI failures unrelated to a PR's actual diff (would explain otherwise-mysterious
red screenshot checks). If confirmed, look at pinning renderer/font versions or
adding a tolerance threshold to the image comparison.

### bb-worktree-validate-collision — Running `validate.sh` concurrently across git worktrees corrupts shared toolchain state

**Found 2026-07-07/08** while landing 4 independent PRs in parallel (one main
session + 3 background agents, each in its own `git worktree`, each running the
full `./scripts/validate.sh`). Running more than one at a time on the same
machine hits three distinct collisions, all now documented as a workflow rule in
`.agent/AGENTS.md` ("Parallel Worktrees & `validate.sh`"):

1. **Shared Gradle daemon registry**: `validate.sh`'s iOS Checks section runs
   `./gradlew --stop` ("Reclaim heap before memory-intensive iOS builds") before
   the memory-intensive iOS build steps. `--stop` targets *every* compatible
   daemon in the shared registry (`~/.gradle/daemon/`), not just the caller's own
   — so it kills every other worktree's in-flight Gradle build with
   `org.gradle.launcher.daemon.server.api.DaemonStoppedException`. Confirmed
   `GRADLE_OPTS="-Dorg.gradle.daemon=false"` does **not** fix this (that property
   is only read from `gradle.properties`/CLI, not JVM system properties passed
   via `GRADLE_OPTS`) — even a local `gradle.properties` override just spawns a
   "single-use" daemon that's still registered in the same shared registry and
   still gets killed. The fix that actually works:
   `GRADLE_OPTS="-Dorg.gradle.daemon.registry.base=<unique-dir-per-worktree>"`,
   which *is* honored as a system property and gives each worktree's run its own
   private daemon pool, immune to other worktrees' `--stop` calls.
2. **Android emulator/AVD collision**: two worktrees' `pixel5api34Setup`
   Gradle Managed Device tasks try to boot the same AVD name
   (`dev34_google_apis_arm64-v8a_Pixel_5`) simultaneously — see `bb-emult` for
   the exact error.
3. **Docker Hub anonymous-pull rate limiting**: multiple concurrent
   `:server:app:jibBuildTar` tasks pulling the same `eclipse-temurin:21-jre-alpine`
   base image from one IP can exhaust the anonymous pull quota, manifesting as
   an apparent hang ("The base image requires auth. Trying again...", near-zero
   CPU time, no forward progress) rather than a clean error.

**Separately discovered while debugging the above**: a stale local
`~/Library/Developer/Xcode/DerivedData/iosAppSwiftUI-*` can break the
`iosAppSwiftUI` xcodebuild step with `Unable to resolve module dependency:
'KMPObservableViewModelCore'` even though `xcodebuild -resolvePackageDependencies`
reports the package resolved fine. Confirmed via `git stash` that this
reproduces identically on unmodified `main` — a local dev-machine artifact, not
a code regression, and CI runners (fresh `DerivedData` every run) shouldn't hit
it. Workaround (`rm -rf` the stale dir) documented in `.agent/ios.md`; root
cause not investigated further since it's non-blocking once known.

**Not yet done:** actually fixing `validate.sh`/`ci.yml` so concurrent runs are
safe by default (e.g. always isolating the daemon registry, or documenting a
`--worktree-safe` flag) — the current state is "documented gotcha + manual
workaround," not a structural fix.

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

### bb-labs-refresh-token-persistence — Google Sign-In dialog still appeared on 1.0.0-49 after `bb-silent-reauth-cooldown` — DONE 2026-07-18

User-reported (1.0.0-49, the release containing `bb-silent-reauth-cooldown`): the Google Sign-In
dialog still appeared when returning to the app "later in the day." Root cause: the 6h cooldown
added by `bb-silent-reauth-cooldown` only *throttled* `attemptSilentReauth()`'s use of
`GoogleSignInBridge.signInSilentlyWithClient` (Android Credential Manager's `getCredential()` call,
never guaranteed headless) — a normal "open app in the morning, come back in the evening" gap
exceeds 6h, so the cooldown had already elapsed and the same UI-risky call fired again. Throttling
treated the symptom (frequency); it never removed the OS-UI risk itself.

**Redesign**: separate *obtaining* a Google identity assertion (interactive, OS-owned, UI is
expected — only [`signInToLabs`]) from *maintaining* a session (must never risk OS UI). The Labs
path already had a real, working, non-interactive renewal mechanism —
`FirebaseIdTokenProvider`'s Firebase refresh token, refreshed via a plain `securetoken.googleapis
.com` REST call — it was just held in-memory only (`FirebaseIdTokenProvider.session`) and lost on
every process death, which is *why* the app fell back to Credential Manager on every cold start.

**Fix**:
1. New `domain.repository.LabsRefreshTokenPersistence` port (thin key-value store, one per Labs
   environment) — defined in `domain` (not `data-local`) so `data-network`'s
   `DefaultLabsAuthGateway` can depend on it without a new inter-module edge; implemented by
   `data-local`'s `DataStoreLabsRefreshTokenPersistence` (DataStore-backed, same shared store as
   `DataStoreLabsSessionStorage`, new key prefix).
2. `FirebaseIdTokenProvider` gained `restoreSession(refreshToken)` and `currentRefreshToken()`.
   `restoreSession` classifies failures: HTTP 4xx (`RefreshOutcome.Invalid`) means the refresh
   token was actually rejected — authoritative, maps to `AuthError.Token.Invalid`; anything else
   (network exception, 5xx) is `RefreshOutcome.Transient`, maps to `AuthError.SignIn.NetworkError`
   and should **not** be treated as a sign-out.
3. `LabsAuthGateway` gained `restoreSession(): Result<Unit, AuthError>` (no-arg — the gateway owns
   reading/writing the persisted token itself). `DefaultLabsAuthGateway` write-through persists the
   refresh token on sign-in success and on `restoreSession` success, and clears it on `Token.Invalid`
   or explicit `signOutLabs()`.
4. `DefaultLabsAuthRepository.attemptSilentReauth()` → `restoreLabsSession(key)`: calls
   `labsAuthGateway.restoreSession()` (pure network call now, so **no cooldown needed** — safe on
   every process start). Only `AuthError.Token.Invalid` clears the believed-signed-in belief and
   flips to `Unauthenticated`; a transient error is left alone exactly like the old best-effort
   design (background sync keeps failing quietly until the next attempt or an explicit sign-in).
5. Removed the now-dead `SilentReauthCooldown` (`SilentAuthRecoveryPolicy.kt`) and its
   `LabsSessionStorage.getLastSilentReauthAttemptMs`/`recordSilentReauthAttempt` backing fields.

**Side effect**: this also fixes iOS/Desktop's Labs silent-restore gap noted as a "materially bigger
feature" deferral in `bb-labs-silent-reauth` — `FirebaseIdTokenProvider` is `commonMain` and
platform-agnostic, unrelated to `GoogleSignInBridge`'s platform-specific silent methods (which stay
interactive-only on iOS/Desktop). Persisting its refresh token benefits all three platforms; they no
longer need a full interactive OAuth sheet/browser after every process restart for Labs mode.

**Deliberate design choice — client-held refresh token, not a server-mediated session**: the
refresh token is persisted on-device (DataStore), matching RFC 8252 ("OAuth 2.0 for Native Apps")
and exactly how the official Firebase Auth SDK behaves internally. This is the standard pattern for
native/mobile OAuth clients (unlike browser-based confidential clients, the OS gives real secure
storage). The tradeoff vs. a server-mediated session: revocation propagates on the *next* refresh
attempt, not instantly. A backend-mediated session (this repo's own gRPC server holding the token,
issuing a short-lived session cookie to the client) would give instant revocation but requires a
backend in the loop for every renewal — not available here: the Labs backend is external/third-party
(can't add endpoints to it), and this repo's own gRPC server is legacy, off by default
(`LEGACY_DATA_MODES`), and hibernated on AWS per `.agent/server.md`. Scoped out as a separate,
much larger project if ever revisited.

**Deferred / accepted scope limits**:
- Own-backend path (`DefaultAuthRepository`, legacy, `LEGACY_DATA_MODES` off by default) untouched.
  It has no server-side infra for this pattern at all: session token is a random UUID in an
  in-memory `ConcurrentHashMap` (`AuthService.kt`, wiped on server restart), fixed 24h expiry, no
  `RefreshToken`/`SignOut` RPC in the proto, and `signOut()` never notifies the server (the token
  stays valid until natural expiry even after "sign out"). Fixing this needs a persisted session
  store and two new RPCs — a real follow-up project, not done here since the path is off by default.
- `GoogleSignInBridge.signInSilentlyWithClient` is now unused (only `DefaultLabsAuthRepository`
  called it), but left in the interface + all three platform actuals rather than removed — a
  same-shaped method (`signInSilently()`, own-backend) is still used, and removing an expect/actual
  method across Android/iOS/Desktop is a separate, higher-risk cleanup not bundled with this fix.
- `FirebaseIdTokenProvider.currentRefreshToken()` is re-persisted after `signInWithGoogle` and
  `restoreSession`, but *not* after an in-session `getIdToken()` refresh (which also updates
  `session.refreshToken` if Firebase ever rotates it). Google's classic OAuth2 token endpoint
  typically doesn't rotate refresh tokens on `securetoken.googleapis.com` (the pre-existing
  `body.refreshToken.ifBlank { refreshToken }` fallback is defensive, not confirmed rotation
  behavior); if it ever does, a persisted-but-now-stale refresh token would only be discovered the
  *next* time `restoreSession` runs (transient failure, not incorrectly treated as revocation,
  since only HTTP 4xx maps to `Invalid`) — degrades gracefully, not a correctness bug.

### bb-silent-reauth-cooldown — Google Sign-In dialog appeared frequently on app resume — DONE 2026-07-11

User-reported: "the Android app frequently has the Google sign-in dialog appear when I return to
the app after some time." The app defaults to Labs mode (`LoginViewModel.kt`), so
`DefaultLabsAuthRepository` is the relevant path for almost every user, not the own-backend
`DefaultAuthRepository` (gated behind the disabled-by-default `LEGACY_DATA_MODES` flag).

**Root cause**: `DefaultLabsAuthRepository`'s `init` block calls `attemptSilentReauth()` once per
process (re)start, right after resolving the persisted "believed signed in" belief
(`bb-labs-silent-reauth`, done 2026-07-07). Since the repository is a DI singleton, it's recreated
fresh — and `attemptSilentReauth()` fires again — every time Android kills the backgrounded process
and the user reopens the app, which happens often. That call
(`GoogleSignInBridge.signInSilentlyWithClient`) was assumed to be headless because it passes
`filterByAuthorizedAccounts(true)`, but on Android it uses the *exact same* Credential Manager
`getCredential()` call as the interactive picker — that flag narrows candidates, it doesn't
guarantee zero UI. With multiple Google accounts on-device, or Play Services deciding a credential
needs re-confirmation, the "silent" call can still surface a chooser/bottom-sheet, landing right
when the user reopens the app. The original fix's doc comment ("no UI") was based on a single-account
emulator test, not a real API guarantee.

**Fix** (`data/.../repository/auth/SilentAuthRecoveryPolicy.kt`, new file):
1. `SilentReauthCooldown.shouldAttempt(lastAttemptAtMs, nowMs)` — pure, unit-tested gate with a
   6-hour cooldown. `DefaultLabsAuthRepository.attemptSilentReauth()` now checks a persisted
   per-environment `LabsSessionStorage.getLastSilentReauthAttemptMs()` (new DataStore-backed key,
   `DataStoreLabsSessionStorage`) before attempting, and records the attempt before calling
   `signInSilentlyWithClient`. Safe to skip: both callers already tolerate the pre-existing session
   staying unrestored until the next attempt or an explicit sign-in (background sync just keeps
   failing quietly, exactly as documented for the original best-effort design).
2. Secondary, own-backend defensive fix: `DefaultAuthRepository.verifyWithServer` gained an
   `isExplicitAttempt` param. Previously, if a *background* `attemptSilentRefresh()` succeeded at the
   Credential Manager step but the **server** rejected the token, `_authState` unconditionally
   dropped to `AuthState.Failed` — which `LoginContent.kt` auto-shows as an `ErrorDialog` with
   "Try Again" wired straight to the interactive `onGoogleSignIn`. A background-triggered rejection
   now falls back to `AuthState.Unauthenticated` quietly instead, matching how a failed
   `signInSilently()` call was already handled. Extracted as pure `authStateForServerRejection()`
   for direct unit testing (`SilentAuthRecoveryPolicyTest.kt`, 8 tests total across both helpers).
   This path is gated behind `LEGACY_DATA_MODES` (off by default) so it's a smaller-blast-radius fix
   than the Labs one, but the bug was real regardless of reachability.

**Deferred**: did not add the same cooldown to `DefaultAuthRepository`'s cold-start
`attemptSilentRefresh()` call (when a stored token is found already-expired at process restart) —
that method is also called from the natural `scheduleTokenExpiry()` timer within a single process
life, and a uniform cooldown longer than `LOCAL_TOKEN_EXPIRY_MS` (1h) would incorrectly suppress
that legitimate self-perpetuating refresh loop. Would need a cooldown scoped only to the cold-start
call site, not inside the shared `attemptSilentRefresh()` — skipped since this path is unreachable
for a default-config user (`LEGACY_DATA_MODES` off). Revisit if `DefaultAuthRepository` is ever used
by more than test/legacy configurations.

**Both repositories remain exempt from direct unit tests** (`GoogleSignInBridge` expect class,
unfakeable — see `test-coverage-exemptions.txt`); the new decision logic was extracted into pure,
directly-testable functions instead of relying on integration coverage.

### bb-labs-mode-auth-state — Labs Sign-In showed a stale account after switching DataMode — DONE 2026-07-05 (structural fix, `DataModeKeyedState`)
Found while auditing whether the app isolates credentials correctly across Labs staging/prod.
Confirmed live on-device: sign in to Labs Prod, switch Data Mode to Labs Staging — Settings
kept showing the **prod** account as "signed in" even though staging was never authenticated;
tapping "Copy Labs ID Token" in that state silently did nothing (the token really was null for
staging, but the UI gave no feedback). No credential leakage — `DefaultLabsAuthGateway` already
correctly partitions **token** sessions per Firebase API key — but `DefaultLabsAuthRepository`'s
`_labsAuthState` was a single unpartitioned `MutableStateFlow`, so switching modes never reset or
re-evaluated it.

**Structural fix, not just a patch:** added `domain/model/DataModeKeyedState.kt` — a small,
directly-unit-tested (`DataModeKeyedStateTest.kt`, 3 tests, no fakes needed) class that holds
one value per data-mode-derived key and reactively exposes whichever one is current, so
there's no unpartitioned field left to go stale. `DefaultLabsAuthRepository.labsAuthState` now
uses it (keyed the same way `DefaultLabsAuthGateway` already keys token sessions, via
`apiKeyForMode`). `LabsAuthRepository.clearError()` became `suspend fun` as part of this (needs
to read the current data mode). Intent: any *future* per-environment state should reach for
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
