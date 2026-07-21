# Labs Auth — architecture, lifecycle, and decision record

**Status:** shipped (this document tracks the code as of the auth reliability wave that
introduced it: #1379 → #1380 → #1382 → the PR C close-out, which added the session-expired UI
reaction, Labs-specific sign-in failure copy, and the Firebase-uid user id). Covers the **Labs
backend** sign-in only (`NetworkMode.LabsStaging` / `LabsProd`); the app's own gRPC backend has
its own `AuthRepository` and is out of scope here.

> **Config stays injected — never hardcode host/keys.** The Labs URLs, Firebase Web API keys, and
> OAuth client ids come from `BuildConfig` (the `LABS_*` properties). This doc names none of
> them; it describes the contract shape only.

---

## 1. The chain, end to end

```
Google Sign-In (per-env Labs OAuth client)          interactive, once
        │  Google ID token (aud = Labs client)
        ▼
accounts:signInWithIdp  (Firebase Auth REST)        FirebaseIdTokenProvider.signInWithGoogle
        │  Labs ID token (~1h) + refresh token (long-lived)
        ▼
in-memory Session (per env)  ──persist──►  LabsRefreshTokenPersistence (refresh token, per env)
        │                                   LabsSessionStorage        (believed user, per env)
        ▼
securetoken /v1/token                                silent refresh: single-flight, off-mutex,
        │  fresh ID token (+ maybe rotated           proactive (5-min buffer), restore-on-demand
        ▼   refresh token → re-persisted on change)
Authorization: Bearer <ID token>
        ▼
GET/POST /v1/battery-butler/sync                     RestRemoteDataSource (retry-once policy)
```

Layers and owners:

| Concern | Owner | File |
|---|---|---|
| Session bytes (tokens, expiry), refresh, proactive refresh | `FirebaseIdTokenProvider` (one per env) | `data-network/…/rest/FirebaseIdTokenProvider.kt` |
| Per-env provider map, persistence write-through, session-loss events | `DefaultLabsAuthGateway` (DI singleton) | `data-network/…/DefaultLabsAuthGateway.kt` |
| Wire retry-once policy, typed 401/transient surfacing | `RestRemoteDataSource` | `data-network/…/rest/RestRemoteDataSource.kt` |
| UI auth state, believed-user belief, cold-start restore gate | `DefaultLabsAuthRepository` | `data/…/repository/auth/DefaultLabsAuthRepository.kt` |
| Sync cadence + the cold-start gate consumer | `DefaultSyncManager` | `data/…/repository/DefaultSyncManager.kt` |
| Sign-in / sign-out orchestration | `SignInToLabsUseCase` / `SignOutLabsUseCase` | `usecase/…` |

## 2. Per-environment partitioning

Staging and prod are **separate Firebase projects**; a staging token is meaningless to the prod
backend. Every piece of session state is therefore keyed by the environment's Firebase Web API
key (`apiKeyForMode`):

- `DefaultLabsAuthGateway.providersByApiKey` — one `FirebaseIdTokenProvider` (one in-memory
  session) per env, map guarded by a mutex (an unguarded `getOrPut` could construct two and
  silently drop one session).
- `DataModeKeyedState<AuthState>` — one UI auth state per env; the *current* env's state is
  re-derived reactively from the live `dataMode` flow, so switching modes can never show a stale
  env's "signed in" (see `bb-labs-mode-auth-state`).
- `LabsRefreshTokenPersistence` / `LabsSessionStorage` — refresh token and believed user, per env.
- Session-loss events (`LabsSessionInvalidation`) carry their env key, so a staging invalidation
  arriving after the user switched to prod still flips *staging's* state.

## 3. The auth state machine

`AuthState` (shared with the own-backend card for UI parity):
`Unknown → (believed user resolved) → Authenticated | Unauthenticated`, plus `Authenticating`
and `Failed(error)` around interactive sign-in.

`Unauthenticated` carries a **cause** — `SignedOutCause.SIGNED_OUT` (default: never signed in,
or explicit sign-out) vs `SignedOutCause.SESSION_EXPIRED` (reactive: the backend authoritatively
rejected the stored session). Every existing `is Unauthenticated` check treats both as signed
out. The UI reacts to the cause (PR C): the front-door login shows calm inline "your Labs session
expired — sign in again" copy next to the normal sign-in button (never an error dialog — the user
did nothing wrong), and the Settings Labs card's signed-out line says "Session expired — sign in
again" instead of the first-run sign-in pitch (`isLabsSessionExpired` +
`presentation-feature/…/auth/LabsAuthText.kt`). Labs sign-in *failures* map through
`labsAuthErrorText` to Labs-specific strings (`labs_auth_error_*`) rather than the legacy
own-backend `auth_error_*` copy. Home keeps #1380's `SyncStatus.AuthRequired` status string
("Sign in required. Your data is safe on this device."), which the session-expired terminal 401
already produces; a tap-through affordance from that snackbar was deliberately not added (it
would thread a navigation callback through the Home scaffolding — the sign-in path is the front
door / Settings card).

**The signed-in `User.id` is the Firebase uid.** The token exchange surfaces
`signInWithIdp`'s `localId` + `email` as `LabsSignInIdentity`, and the repository keys the
believed user on that uid (`labsUserFrom`) — the id the backend authorizes and attributes writes
with (e.g. device-image `uploadedByUid`) — falling back to the old email-shaped derivation only
if the wire response omitted the uid. Installs from before this change persist an email-shaped id
in `LabsSessionStorage`; that value is display-time only and the next successful sign-in
overwrites it (no migration). Nothing client-side keys data off `User.id`: env partitioning uses
the Firebase API key, local-DB isolation uses the `DataMode` identity.

**Reactive session loss keeps local data.** The reactive path clears *credentials* (in-memory
session, persisted refresh token, believed user) and flips the state — it never touches the
device DB. The Home list stays visible next to the "sign in required" sync status. Only the
explicit `SignOutLabsUseCase` clears local data.

### Cold-start: restore + the sync gate

On process start the believed user resolves `Unknown → Authenticated` optimistically (so the UI
doesn't flash a sign-in prompt next to cached data), and the *real* session is restored
non-interactively: persisted refresh token → `securetoken` (never Credential Manager — its
"silent" path can show OS UI; see `bb-silent-reauth-cooldown`).

The restore is **single-flight per env** and its resolution gates sync:
`LabsAuthRepository.awaitLabsSessionRestore()` suspends until the env resolves
(`RESTORED | NO_SESSION | INVALID | TRANSIENT_FAILURE`), triggering the restore lazily if the
init-time trigger didn't run (e.g. cold start in Mock, then a switch to Labs).
`DefaultSyncManager` awaits it (Labs modes only, with a 30s belt-and-braces bound) before its
first request — pre-gate, the loop's first iterations raced the restore and churned
`AuthRequired(NO_SESSION)` on every believed-signed-in launch. A **transient** restore failure
resolves (never blocks): sync proceeds and surfaces *network* errors, and the token path
re-attempts the restore on demand, so a recovered network heals without user action.

### Retry-once, then reactive session loss

On a Labs request 401 (reason parsed leniently from the error envelope's `details.reason`;
prod may not emit reasons yet):

| 401 reason | Token was | Action |
|---|---|---|
| `expired` | anything | force one refresh, retry the request once |
| unknown | served from cache (locally looked unexpired) | force one refresh, retry once |
| unknown | just minted | terminal — another mint can't do better |
| `invalid` | anything | terminal |
| retry still 401s | — | terminal |

Terminal → `reportSessionRejected` → the gateway clears the session + persisted token and emits
`LabsSessionInvalidation` → the repository flips that env to
`Unauthenticated(SESSION_EXPIRED)` and clears the believed user. One reaction path for every
invalidation source (terminal 401, rejected refresh during a token read, rejected restore).

### Transient ≠ auth-dead

`LabsAuthGateway.getLabsToken()` returns a typed `LabsTokenResult` — `Token(servedFromCache)` /
`NoSession` / `SessionInvalidated` / `TransientFailure` — replacing the old nullable token whose
`null` conflated "must sign in" with "refresh failed on a flaky network". `TransientFailure`
surfaces as `RemoteSyncException.TokenUnavailable` → a *network* sync status. A flaky network
never shows "sign in required".

## 4. Token freshness (why syncs no longer pay 3.4s)

- **5-minute expiry buffer** (was 60s) — a token within 5 minutes of expiry is treated as stale.
- **Proactive background refresh** — while an env's session is live *and it is the selected
  env*, an app-scope task sleeps until `expiresAt − buffer` and refreshes then, rescheduling off
  every applied refresh (transient failures retry on a 60s cadence). User-facing requests
  essentially never trigger a refresh themselves; the measured 3.4s mid-request refresh syncs
  disappear.
- **Single-flight, off-mutex refresh** — the provider's mutex guards only in-memory state, never
  the network call (it used to be held across `securetoken`, serializing all callers behind the
  network). Concurrent callers share one refresh `Deferred`; a session epoch keeps a landing
  refresh from resurrecting a signed-out session or clobbering a newer sign-in.
- **Rotated refresh tokens are persisted** — every applied refresh reports its refresh token;
  the gateway persists it iff it changed (Firebase rotates occasionally; an unpersisted rotation
  meant the next cold-start restore presented a stale token and failed).

## 5. Sync cadence (`bb-sync-loop-starvation`)

`DefaultSyncManager` owns the cadence: `dataMode.distinctUntilChanged().collectLatest { mode →
loop }`. Labs (request/response) modes do one fetch per iteration then sleep 60s (pull-to-refresh
/ post-edit pushes / sign-in resync cover immediacy); Mock/gRPC (stream) modes collect until the
stream ends, then reconnect. Failures back off 1s→…→30s. `DelegatingRemoteDataSource.subscribe()`
reads the mode **once per collection** and completes when the source completes — mode reactivity
lives only in the manager. The previous double-`flatMapLatest` shape starved in production: the
one-shot REST fetch's completion was swallowed, and once #1379's `distinctUntilChanged` stopped
the accidental DataStore-ripple cadence, one successful sync was followed by zero requests
forever ("Syncing…" stuck on screen).

Post-sign-in resync runs on the **app scope** (was: the caller's viewModelScope, which sign-in
navigation tore down mid-flight — the documented #1379 residual); its outcome reaches the UI via
`syncStatus`.

## 6. Decision record: on-device Firebase refresh token, not the `__session` cookie

The cartland-labs server supports two session models: the Firebase **refresh-token** flow this
app uses (client holds a long-lived refresh token; every request carries a short-lived ID token
the server verifies), and the **server-managed `__session` cookie** the labs web portal and the
cartland-labs mobile app use (sign-in mints a ≤14-day server session; nothing token-shaped on the
device).

Battery-butler deliberately keeps the **refresh-token model**:

1. **iOS has no silent re-auth path.** The iOS Google sign-in is a hand-rolled
   `ASWebAuthenticationSession` PKCE flow — it *always* shows UI. When a ≤14-day cookie expired,
   iOS users would get a visible re-auth prompt every two weeks, forever.
2. **Android's silent re-auth was removed.** Credential Manager's "silent" sign-in is the same
   `getCredential` call as the picker and can surface OS UI on any process restart; it was
   throttled and then removed (#1350, `bb-silent-reauth-cooldown`). So Android would pay the same
   periodic visible re-auth.
3. The refresh token gives **indefinite sessions with per-request server verification** — the
   server still verifies a fresh, signed, ~1h ID token on every request and can revoke the
   refresh token server-side at any time.

Cost of this choice: a long-lived credential lives on the device (mitigated: never logged, per-env
storage, cleared on sign-out and on any authoritative rejection) and the client owns refresh
machinery (#1382). The labs server accepts both models, so this stays **revisitable** — if both
platforms ever gain genuinely-silent re-auth, the cookie model removes all client token handling.

## 7. Defect inventory → which PR fixed what

| Defect | Fixed in |
|---|---|
| Spurious `dataMode` re-emissions cancelled in-flight syncs (DataStore ripple) | #1379 (`distinctUntilChanged`) |
| Torn snapshot apply on cancellation emptied the device list | #1379 (`NonCancellable` apply) |
| 401 error body parsed as an empty snapshot ("synced" while signed out) | #1380 (status-aware wire, typed `RemoteSyncException`) |
| Null token fired a guaranteed-401 request | #1380 (client-side `NO_SESSION` refusal) |
| Sync loop starvation: one sync, then zero requests forever ("Syncing…" stuck) | #1382 (manager-owned cadence + mode-once `subscribe()`) |
| Cold start raced the session restore → `AuthRequired(NO_SESSION)` churn | #1382 (restore gate) |
| Transient refresh failure showed "sign in required" | #1382 (`LabsTokenResult` / `TokenUnavailable`) |
| Stale token 401 had no retry; no reaction to a dead session (state stayed "signed in") | #1382 (retry-once + reactive session loss) |
| Provider map race could split-brain an env's session | #1382 (mutex) |
| Rotated refresh token never persisted → cold-start restore failed | #1382 (write-through on change) |
| Mid-request refresh latency (3.4s syncs); mutex held across the refresh network call | #1382 (proactive refresh, 5-min buffer, off-mutex single-flight) |
| Post-sign-in resync died with the login screen's scope | #1382 (app-scope resync) |
| uiState withheld behind image-query first emissions (device list invisible with a full DB) | #1382 (image-map seed) |
| UI copy/flows reacting to `SESSION_EXPIRED` (re-auth prompt, etc.) | PR C (cause-aware login + Settings copy) |
| Labs sign-in failures reused the own-backend gRPC-era copy ("Coming Soon", …) | PR C (`labsAuthErrorText` + `labs_auth_error_*` strings) |
| `User.id` was the Google email, not the Firebase uid the backend authorizes with | PR C (`LabsSignInIdentity` threading `localId`) |
