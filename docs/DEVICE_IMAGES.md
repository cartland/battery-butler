# Device Images — client implementation spec

**Status:** spec, not yet built. The **Labs backend already serves this** (the endpoints and the
new snapshot field below are live); the **client has not implemented it**. This document is the
plan for the client (app) work, grounded in the current code so it can be picked up in a future
session.

**Scope — Labs backend only.** The app is multi-backend (`NetworkMode.Mock / None / Grpc* /
LabsStaging / LabsProd`). **Blob image storage exists only on the Labs backend.** The app's own
gRPC/server backend carries the `imagePath` *string* column but has no image-bytes endpoint. So
device photos are a **Labs-mode capability**: gate the photo UI on "the current backend supports
images" (§6). In Mock/None/gRPC modes, behavior is unchanged (icon only) — that's scope, not a
regression.

> **Config stays injected — never hardcode host/keys.** As everywhere in this repo, the Labs
> server URL and keys come from `BuildConfig`/`AppConfig` (the `LABS_*` properties), never from
> source. This doc names none of them; it only describes the contract *shape* the client codes
> against.
>
> **What's not in this repo (get it from the project maintainer):** the `LABS_*` values (host +
> keys) and an account with **device-edit access** to the Labs **staging** backend. Inject the
> values the usual way (`local.properties` locally, CI config in workflows) — the same as any other
> Labs-backed build; they are an external ask, not a missing file here. You can build and unit-test
> workstreams **A/B/D with no backend access at all**; you only need staging to run the end-to-end
> checks in §9.

---

## 1. The ask

Each device can optionally have **one photo**, shown next to its name/location. The user picks a
photo (camera roll / files), it uploads, and it replaces any previous photo. Photos are optional
and removable. Capture is from the app; byte storage + serving already exist on the backend.

---

## 2. What already exists on the client — **do not rebuild it**

A per-device *string* image field is **already wired end to end and contract-pinned** — it is just
**dormant in the UI**. Reuse this plumbing; the gap is bytes + capture + display, not the data
model.

| Layer | Symbol | File | State |
|---|---|---|---|
| Domain | `Device.imagePath: String? = null` | `domain/…/domain/model/Device.kt` | live in model, **never displayed / never set from UI** |
| Proto | `ProtoDevice.image_path = 7` | `protos/com/chriscartland/batterybutler/protos/model.proto` | compiled via Wire |
| REST wire DTO | `DeviceWire.imagePath: String = ""` | `data-network/…/rest/SyncDto.kt` | pinned by the wire-contract test |
| Mapping | empty⇄null (`imagePath.takeIf { it.isNotEmpty() }`) | `data-network/…/rest/RestSyncMapper.kt` | live |
| Local DB | `DeviceEntity.imagePath: String?` | `data-local/…/room/entity/DeviceEntity.kt` | live, migrated |
| Create/edit plumbing | `DeviceInput.imagePath`, threaded through `AddDeviceViewModel`/`EditDeviceViewModel` | `domain/…/model/DeviceInput.kt` etc. | accepted end-to-end, **but the forms never populate it** |

**Absent entirely** (confirmed by exhaustive grep — zero matches in production code):

- **No image-loading library** — no Coil3, Kamel, or any multiplatform loader in
  `gradle/libs.versions.toml`. `imagePath` is a bare string; nothing decodes bytes.
- **No image picker / camera** — no Android Photo Picker (`PickVisualMedia`), no iOS `PHPicker`, no
  desktop file chooser wired for images.
- **No HEIC / EXIF / downscale / resize** code of any kind.

So the device UI renders a **Material vector icon** wherever a photo would go (the detail header's
112.dp circular avatar and the list item), driven by `deviceType.defaultIcon` — **not** any
per-device image.

---

## 3. The load-bearing distinction: `imageEtag` (new, server) vs `imagePath` (old, client)

The backend deliberately **does not** repurpose `imagePath`. Mirror that separation exactly:

- **`imageEtag`** — *new, server-managed, read-only for the client.* An opaque content hash of the
  stored bytes (`""` = no image). It appears on the **GET snapshot's** devices only. It is the
  client's **cache key and change signal**: cache bytes keyed by the etag, and re-fetch the image
  **only when the snapshot's etag differs from what you cached**. The snapshot the app already
  fetches tells it whether an image exists and whether it changed — no polling, no extra round-trip
  to discover it.
- **`imagePath`** — *old, client-owned, opaque to the server.* Leave it alone. It is a proto field
  the app has always synced (a local path in its own storage); the server assigns it no meaning and
  never writes it. **Do not** carry the server image through it, and **do not** treat it as a URL to
  load — server image bytes come only through the authenticated GET endpoint below. (Reason the
  backend refused to reuse it: a sync push *replaces the whole device record*, so server state living
  inside the device would be wiped by the next push; and installs in the field already sync their own
  `imagePath`. A separate read-only field sidesteps both.)

You may keep using `imagePath` for whatever local purpose you like, or ignore it. This spec touches
it **not at all**.

---

## 4. The backend contract the client codes against

The Labs backend exposes this **additively** — an app in the field that predates it keeps working
(it parses leniently and ignores what it doesn't know). The client's job is to consume it.

### 4a. The snapshot gains one field (GET side only)

`GET /v1/battery-butler/sync` — each device now carries:

```
imageEtag: string     // opaque content hash of the stored bytes; "" = no image
```

`POST /v1/battery-butler/sync` devices do **not** carry `imageEtag`, and a client that echoes it
back on push has it **stripped** server-side — so **a push can never change image state.** The image
lifecycle is controlled solely by the three routes below.

### 4b. Three image routes

| Route | Request | Success | Errors the client must handle |
|---|---|---|---|
| `PUT /v1/battery-butler/devices/{id}/image` | raw bytes, `Content-Type: image/jpeg\|png\|webp`, ≤ 10 MB | `200 {"imageEtag":"<hash>"}` | `400` bad type / bytes-mismatch; `404` unknown device; `413` over cap |
| `GET /v1/battery-butler/devices/{id}/image` | — | `200` raw bytes, stored `Content-Type`, `ETag` header | `404` no image / no access |
| `DELETE /v1/battery-butler/devices/{id}/image` | — | `200 {"success":true,"message":""}` | idempotent — removing a missing image still `200`s |

**Auth** is unchanged from sync: the same `Authorization: Bearer <Labs ID token>` the client already
attaches in `RestRemoteDataSource` (via `bearerAuth`, token from `LabsAuthGateway` /
`FirebaseIdTokenProvider`). No new credential, host, or SDK. GET needs the same **read** permission
as `GET /sync`; PUT/DELETE need the same **write** permission as `POST /sync` — a user who can push
devices can already manage their photos.

**Rules the client must honor:**

- **PUT = replace.** Same route for first upload and replacement; the returned etag changes. There is
  no separate "create".
- **The device must already exist server-side** (be synced) or PUT returns `404`. **On Add-device:
  push the device first, then upload its photo** (sequencing in §6E).
- **`Content-Type` must match the actual bytes** — the server sniffs magic bytes and rejects a
  mismatch with `400`. Send the true type of what you upload (you'll be sending `image/jpeg` after
  normalization, §6C).
- **JPEG / PNG / WebP only. HEIC is rejected (`400`).** iPhone camera output is HEIC → the client
  **must** transcode to JPEG before upload (§6C).
- **10 MB cap** (`413`) — a backstop, not a target; a downscaled JPEG (~2048px) is ≈ 200–600 KB.
- **DELETE is idempotent**, and **deleting a device cascades** (its image is removed server-side) —
  the client need not delete the image separately when it deletes a device.
- **Permission / existence:** an unauthenticated call returns `401`; an unknown device or a caller
  without access both return `404` (indistinguishable) — handle both as "not available".

---

## 5. Contract mirror state (good news: no coordinated backend change)

`imageEtag` is **already on the backend**, including its contract fixtures. The client's mirror is
therefore **currently green but drifted** — its embedded snapshot golden constant has no `imageEtag`
yet, and its wire-contract test pins the old field set. The client PR simply **re-aligns** the
mirror; it needs **no** coordinated backend change and can proceed against the Labs staging backend
independently. Keep the mirror **in lockstep with the backend contract fixtures**:

- `data-network/src/commonTest/…/rest/SyncWireContractTest.kt` — today there is a **single** `DeviceWire`
  field-set pin (both sides share it). **Add a new pin** for the snapshot device shape (the `DeviceWire`
  fields **plus** `imageEtag`) and **leave the existing `DeviceWire` (push) pin unchanged** — don't just
  widen the current one, or the push side drifts.
- `data-network/src/commonTest/…/rest/SyncGoldenFixtureTest.kt` — the snapshot golden gains **one key**,
  `"imageEtag"`, on its device(s); the push-request golden is **unchanged** (the split is real; see §6A).
  *"No coordinated backend change" means no backend **code** change* — but the golden is byte-shared with
  the backend repo's fixture, so use the exact `imageEtag` value the backend emits for that fixture device
  (get it from the maintainer, or read it off a live staging snapshot). Structurally it is just the one
  added key.

---

## 6. Workstreams

Recommended order **A → B → D → C → E → F**, so transport + display are testable (Ktor `MockEngine`
+ a fixed etag) before the platform-specific capture work.

### A. Wire-contract mirror (`data-network`) — additive, edit in lockstep

- In `SyncDto.kt`, mirror the backend's **two-shape split**: add a snapshot-only device DTO carrying
  `imageEtag`, used by `SyncSnapshotWire.devices`; **keep the push `DeviceWire` without it** for
  `SyncPushRequestWire.devices`. (Recommended: `DeviceSnapshotWire = DeviceWire + imageEtag: String =
  ""`. Do **not** just add `imageEtag` to the single `DeviceWire` — with `encodeDefaults = true` that
  would emit `imageEtag` on push too and drift the push golden.)
- Carry it into the domain: add `Device.imageEtag: String? = null` (empty⇄null, same convention as
  `imagePath`). Snapshot mapping (`RestSyncMapper.toDomain`) populates it; **push mapping
  (`Device.toWire`) drops it** naturally by producing the push `DeviceWire`.
- Persist it: add a nullable `DeviceEntity.imageEtag: String?` Room column (additive nullable → a
  simple migration). Persisting the etag lets the UI know offline whether a device has a photo and
  keeps the cache key stable across restarts.
- Update the pins in lockstep (§5).

### B. Binary image transport (`data-network`) — genuinely new

- Add an **image capability interface** (recommended over widening `RemoteDataSource`, so
  Mock/gRPC/None don't need no-op impls):

  ```kotlin
  interface DeviceImageDataSource {
      suspend fun upload(deviceId: String, bytes: ByteArray, contentType: String): String // etag
      suspend fun fetch(deviceId: String): DeviceImageBytes?   // bytes + contentType, null on 404
      suspend fun delete(deviceId: String)                      // idempotent
  }
  ```

- Implement it in `RestRemoteDataSource` (or a sibling), reusing the existing `httpClient` +
  `bearerAuth(token)` + base-URL pattern (`syncUrl()`), with new path constants
  `…/devices/{id}/image`. Ktor `put { setBody(bytes); contentType(...) }`, `get { }.body<ByteArray>()`
  (read the response `Content-Type`), `delete { }`. Map `404` on fetch → `null`; surface `400`/`413`/
  `404` on upload as typed errors the ViewModel can message.
- Expose "images supported?" on `DelegatingRemoteDataSource` (true only in `LabsStaging`/`LabsProd`
  modes) so §6F can gate the UI.

### C. Capture / pick / normalize (new — per-platform `expect/actual`)

The hardest slice; none exists today. Produce a **≤10 MB JPEG** from a user-picked image:

- **Pick** — `expect fun pickDeviceImage(): ByteArray?` (or Flow/callback), actuals:
  - **Android** — `ActivityResultContracts.PickVisualMedia` (Photo Picker; no storage permission).
  - **iOS** — `PHPickerViewController` (**both** front-ends — see §7).
  - **Desktop** — a file chooser; **no camera** on desktop.
- **Normalize to JPEG** (the contract rejects HEIC and requires type⇄bytes agreement):
  - **Android** — `ImageDecoder`/`BitmapFactory` → downscale (long edge ~2048px) → `Bitmap.compress(JPEG, ~80)`; honor EXIF orientation.
  - **iOS** — `UIImage`/ImageIO: **HEIC → JPEG** here specifically; respect `imageOrientation`; downscale; JPEG re-encode.
  - **Desktop** — `javax.imageio` (input realistically JPEG/PNG; no camera).
- Enforce client-side before upload: type ∈ {jpeg,png,webp} (you emit jpeg), size ≤ 10 MB (trivially
  met post-downscale). Client downscaling also strips EXIF/GPS as a side effect.
- **Fit the version catalog:** any new dependency must slot under the pinned **Kotlin / Compose MP /
  Ktor** versions in `gradle/libs.versions.toml` (full of "do-not-bump" comments). Prefer
  platform-native APIs over a heavyweight new dep where practical.

### D. Display + byte cache (new — `presentation` + `data`)

- **Fetch orchestration:** after a snapshot, for each device with a non-empty `imageEtag` whose bytes
  aren't cached, call `DeviceImageDataSource.fetch(deviceId)` and store bytes **keyed by the etag**.
  Content-addressed ⇒ immutable ⇒ cache-forever; drop entries for etags no longer referenced.
- **Decode + render:** show the cached bytes as an `ImageBitmap` in the **112.dp circular avatar**
  (`presentation-feature/…/devicedetail/DeviceDetailContent.kt`, the `Box(CircleShape)`) and the
  **list item** (`presentation-core/…/components/DeviceListComponents.kt`), falling back to the current
  Material icon when `imageEtag` is `""`/null or bytes aren't cached yet.
- **Loader choice — decide (§8):** (i) **manual `ImageBitmap` decode** of the cached bytes
  (`expect/actual` decoder) — no new dependency, and our bytes arrive through an *authenticated*
  endpoint (not a plain URL a generic loader would GET); or (ii) **Coil3 multiplatform** with a custom
  fetcher that adds the Bearer token — more machinery, must fit the pinned versions. **(i) is
  recommended.**

### E. Upload / replace / delete orchestration (`usecase` + `viewmodel`)

- **Add device:** create the device first (existing push path), obtain its id, **then** `upload(...)`
  — the backend `404`s a PUT for an unknown device (§4). Optimistically set the device's `imageEtag`
  from the response and cache the bytes.
- **Edit device / detail:** "Change photo" → pick → normalize → `upload` → update etag + cache;
  "Remove photo" → `delete` → clear etag + evict cache.
- `DeviceInput.imagePath` already threads through the Add/Edit VMs, but that is the **client-owned**
  field — don't overload it for the server image (§3). The server image round-trips solely via the
  capability interface + `imageEtag`.
- Failure UX: message `413`/`400` (shouldn't happen post-normalize, but guard) and `404` (device not
  yet synced — retry after push). A failed upload leaves the device without a photo; never corrupts
  local state.

### F. UI surfaces

- **Add/Edit forms** — a "Choose photo" / "Change photo" / "Remove photo" control in
  `presentation-feature/…/adddevice/AddDeviceContent.kt` and `…/editdevice/EditDeviceContent.kt` (the
  forms that today build `DeviceInput(...)` with only name/location/typeId).
- **Detail + list** — swap the Material-icon avatar for the photo when present (files in §6D).
- **Gate on capability** (§6B): show photo affordances only when the current backend supports images
  (Labs modes).
- **Screenshot tests** to update when the avatar changes: `android-screenshot-tests/…/DeviceDetailScreenshotTest.kt`, `…/DeviceIconsScreenshotTest.kt` — regenerate reference images (`./scripts/generate-android-screenshots.sh`) and commit them alongside the test change.

---

## 7. Platform caveats

1. **Two iOS front-ends** — `ios-app-swift-ui/` (SwiftUI over the KMP framework; its tests hard-code
   `imagePath: nil`) **and** `ios-app-compose-ui/` (Compose-on-iOS). A photo UI added in shared Compose
   covers the Compose-iOS app; the SwiftUI app needs its own picker/render pass (or an explicit
   decision to ship it Compose-only first).
2. **iOS HEIC is the real work** — the camera default; must transcode to JPEG (§6C). Nothing in the
   repo handles it today.
3. **Desktop has a file picker but no camera** — pick-from-file only; fine.
4. **No web/wasm target** — targets are `androidTarget`, `iosX64/Arm64/SimulatorArm64`, `jvm("desktop")`.
5. **Do-not-bump catalog** — a new image dep must slot under the pinned Kotlin/Compose/Ktor versions
   (`gradle/libs.versions.toml`), and per `.agent/AGENTS.md` add it to `.github/dependabot.yml`
   `ignore:` if pinned.

---

## 8. Open decisions for the implementer

Small local calls — none blocks starting; recommendations in parens:

1. **Loader:** manual `ImageBitmap` decode of authenticated bytes *(recommended)* vs Coil3 + token
   fetcher. (§6D)
2. **Capability surface:** separate `DeviceImageDataSource` interface *(recommended)* vs widening
   `RemoteDataSource` with no-op gRPC/Mock impls. (§6B)
3. **Cache location/eviction:** in-memory + on-disk keyed by etag; *evict on snapshot when an etag is
   no longer referenced*.
4. **SwiftUI parity now or later** *(Compose-iOS first; SwiftUI as a fast-follow — call it out in the
   PR)*. (§7)
5. **`Device.imageEtag` domain type:** `String?` empty⇄null *(recommended, matches `imagePath`)* vs
   non-null `""`.

---

## 9. Success criteria

Against the **Labs backend** (staging first), signed in as a user who can edit devices:

1. **Round-trip.** Pick a photo on a device → it uploads → the detail avatar and list item show it,
   not the Material icon. Kill and reopen the app → the photo is still shown (persisted etag + cache).
2. **Replace.** Pick a different photo → the new one shows; `imageEtag` changed; the old bytes are no
   longer fetched.
3. **Remove.** "Remove photo" → the icon returns; a later snapshot reports `imageEtag = ""`.
4. **Second install.** A photo uploaded on one install appears on another install of the same account
   after its next sync — proving the etag-in-snapshot signal drives the fetch (no local-only state).
5. **HEIC.** An iPhone-camera (HEIC) pick uploads successfully (transcoded to JPEG) — never a `400`.
6. **Guardrails.** Oversized or non-image input is rejected client-side with a clear message; the
   server `400`/`413` paths are covered by a transport test but never reached in normal use.
7. **Cascade.** Deleting a device removes its photo server-side (no separate image delete); no orphan
   bytes are re-fetched.
8. **Old-mode safety.** In Mock/gRPC/None modes the app behaves exactly as before (icon-only, no photo
   affordances) — no crashes, no failed image calls.
9. **Contract green + aligned.** `SyncWireContractTest` + `SyncGoldenFixtureTest` pass and the snapshot
   golden matches the backend byte-for-byte (the `imageEtag` value to use is per §5); the push golden is
   unchanged.

---

## 10. Rollout / sequencing

1. **Backend:** already live — no backend work for this feature.
2. **Client PR(s)**, staging-first: land A+B+D behind the capability gate (testable with `MockEngine`
   + a fixed etag), then C (per-platform capture), then E+F (UX), then screenshot updates. Keep PRs
   small per `.agent/AGENTS.md`; each on its own `agent/…` branch. Verify each against the **Labs
   staging** backend (`NetworkMode.LabsStaging`); your account needs edit access there (how to obtain
   access + the `LABS_*` values is in the config note at the top).
3. **Prove it on a device** against staging using §9 — especially the iOS HEIC path and second-install
   propagation.
4. **Prod:** no backend release needed — point the app at `NetworkMode.LabsProd` (config via the
   injected `LABS_*` values). Same server code and behavior as staging.
5. **Rollback:** the feature is additive and capability-gated. A build without it shows icons (the new
   field is ignored); a build with it, pointed at any pre-photo state, shows icons until a photo is
   uploaded. Nothing to migrate.

---

## 11. Anchor file index (verify before editing — the tree moves)

| Purpose | Path |
|---|---|
| Domain device (`imagePath`; add `imageEtag`) | `domain/…/domain/model/Device.kt` |
| Device create input | `domain/…/domain/model/DeviceInput.kt` |
| Proto | `protos/com/chriscartland/batterybutler/protos/model.proto` |
| REST wire DTOs (add snapshot `imageEtag`) | `data-network/…/datanetwork/rest/SyncDto.kt` |
| REST mapper (empty⇄null) | `data-network/…/datanetwork/rest/RestSyncMapper.kt` |
| Sync call site (add image transport) | `data-network/…/datanetwork/rest/RestRemoteDataSource.kt` |
| Ktor client + per-platform engines | `data-network/…/datanetwork/rest/SyncHttpClient.{,android,ios,desktop}.kt` |
| Auth gateway / token provider | `data-network/…/datanetwork/{LabsAuthGateway,DefaultLabsAuthGateway}.kt`, `…/rest/FirebaseIdTokenProvider.kt` |
| Backend router / modes | `data-network/…/datanetwork/DelegatingRemoteDataSource.kt`, `domain/…/model/NetworkMode.kt` |
| Contract pin | `data-network/src/commonTest/…/rest/SyncWireContractTest.kt` |
| Golden fixtures (keep in lockstep w/ backend) | `data-network/src/commonTest/…/rest/SyncGoldenFixtureTest.kt` |
| Local DB entity (add `imageEtag` column) | `data-local/…/datalocal/room/entity/DeviceEntity.kt` |
| Detail avatar (render site) | `presentation-feature/…/presentationfeature/devicedetail/DeviceDetailContent.kt` |
| List item (render site) | `presentation-core/…/presentationcore/components/DeviceListComponents.kt` |
| Add/Edit forms | `presentation-feature/…/adddevice/AddDeviceContent.kt`, `…/editdevice/EditDeviceContent.kt` |
| Add/Edit view models | `viewmodel/…/adddevice/AddDeviceViewModel.kt`, `…/editdevice/EditDeviceViewModel.kt` |
| Screenshot tests (+ reference PNGs) | `android-screenshot-tests/…/DeviceDetailScreenshotTest.kt`, `…/DeviceIconsScreenshotTest.kt` |
| Version catalog (fit new deps under pins) | `gradle/libs.versions.toml` |
