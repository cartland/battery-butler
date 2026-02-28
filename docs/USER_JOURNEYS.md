# User Journeys

This document maps every user-reachable path through Battery Butler. Each journey includes the entry point, tap sequence, Screen.kt references, prerequisite journeys, and edge cases.

Use this alongside [FEATURES.md](FEATURES.md) (feature inventory) and [TESTING.md](TESTING.md) (coverage matrix) for a complete picture of what the app does and how it's tested.

---

## J1: First Launch / Login

**Entry point:** App cold start (unauthenticated)
**Screen:** `Screen.Login`

1. User opens the app for the first time.
2. Login screen displays with sign-in options.
3. User authenticates.
4. App navigates to `Screen.Devices` (home).

**Edge cases:**
- Network unavailable during login attempt
- Token refresh failure on subsequent launches

**Prerequisites:** None

---

## J2: Browse Devices

**Entry point:** Devices tab (home screen)
**Screen:** `Screen.Devices`

1. User sees a grouped/sorted list of devices with icons, names, types, and locations.
2. User can change sort order (name, location, battery age, device type) and grouping (none, type, location).
3. Sync status indicator shows current sync state.
4. If no devices exist, an empty state prompt is shown.

**Edge cases:**
- Empty state (no devices yet) — friendly prompt
- Sync failure — error message in snackbar

**Prerequisites:** J1 (authenticated)

---

## J3: Add Device

**Entry point:** FAB on Devices tab, or "Add New Device" from event form
**Screen:** `Screen.Devices` -> `Screen.AddDevice`

1. User taps the "+" FAB on the Devices tab.
2. Add Device form appears with fields: Name (required), Location, Device Type dropdown.
3. User fills in name and optionally selects a type and location.
4. User taps Save.
5. Device is created and appears in the device list.

**Alternative: AI Batch Import** (see J14)
- User taps the AI batch input area and types natural language (e.g., "bedroom alarm clock, kitchen smoke detector").
- AI parses and creates multiple devices.

**Edge cases:**
- Blank name — save button disabled
- "Manage Device Types" link navigates to Types tab

**Prerequisites:** J1

---

## J4: View Device Detail

**Entry point:** Tap a device in the Devices list
**Screen:** `Screen.Devices` -> `Screen.DeviceDetail(deviceId)`

1. User taps a device in the list.
2. Device detail screen shows: profile header (large icon, name, type, location), battery info cards (type, quantity), "Record Replacement" button, and recent event history.
3. "View All" link navigates to History filtered by device.

**Edge cases:**
- Device with no events — event history section is empty
- Device type deleted — shows "Unknown" type

**Prerequisites:** J2 (at least one device exists)

---

## J5: Edit Device

**Entry point:** Edit button on Device Detail screen
**Screen:** `Screen.DeviceDetail(deviceId)` -> `Screen.EditDevice(deviceId)`

1. User taps Edit on the device detail screen.
2. Edit form shows current name, location, and device type.
3. User modifies fields and taps Save.
4. Device is updated and user returns to device detail.

**Delete flow:**
1. User taps Delete on the edit screen.
2. Confirmation dialog appears.
3. User confirms — device is deleted and user returns to device list.

**Edge cases:**
- Device deleted by sync while editing — NotFound state

**Prerequisites:** J4

---

## J6: Record Battery Replacement

**Entry point:** "Record Replacement" button on Device Detail, or FAB on History tab
**Screen:** `Screen.DeviceDetail(deviceId)` -> `Screen.AddBatteryEvent` or `Screen.History` -> `Screen.AddBatteryEvent`

1. User taps "Record Replacement" on device detail (or FAB on History tab).
2. Add Battery Event form shows: Device dropdown (pre-selected if from detail), Date (defaults to today).
3. User confirms device and date, taps Save.
4. Event is created, device's `batteryLastReplaced` timestamp is updated.

**Alternative: AI Batch Import** (see J14)

**Edge cases:**
- "Add New Device" option in device dropdown — navigates to Add Device flow
- Future date selected — allowed (user may pre-record)

**Prerequisites:** J1 (device optional — can create inline)

---

## J7: Browse Device Types

**Entry point:** Types tab
**Screen:** `Screen.Devices` -> `Screen.Types`

1. User taps the Types tab.
2. Types list shows icon, name, and battery specs for each type.
3. User can sort by name or battery type, group by none or battery type.

**Edge cases:**
- Empty state (no types) — prompt to add types
- Preloaded common types available on first launch

**Prerequisites:** J1

---

## J8: Add Device Type

**Entry point:** FAB on Types tab, or "Manage Device Types" link from Add Device
**Screen:** `Screen.Types` -> `Screen.AddDeviceType`

1. User taps "+" FAB on Types tab.
2. Add Device Type form shows: Icon grid (40+ icons), Name, Battery Type, Battery Quantity (+/- buttons).
3. User enters a name — AI auto-suggests an icon.
4. User taps Save.
5. New device type appears in the types list.

**Alternative: AI Batch Import** (see J14)

**Edge cases:**
- Blank name — save disabled
- Duplicate name — allowed (no uniqueness constraint)

**Prerequisites:** J1

---

## J9: Edit Device Type

**Entry point:** Tap a device type in the Types list
**Screen:** `Screen.Types` -> `Screen.EditDeviceType(typeId)`

1. User taps a device type in the list.
2. Edit form shows current name, icon, battery type, and quantity.
3. User modifies fields and taps Save.
4. Type is updated.

**Delete flow:**
1. User taps Delete.
2. Confirmation dialog appears.
3. User confirms — type is deleted.

**Edge cases:**
- Type is used by devices — devices retain `typeId` but show "Unknown" if type is deleted
- Type not found (deleted by sync) — NotFound state

**Prerequisites:** J7 (at least one type exists)

---

## J10: Browse History

**Entry point:** History tab
**Screen:** `Screen.Devices` -> `Screen.History`

1. User taps the History tab.
2. Event list shows all battery replacements across all devices.
3. Each event shows device name, type, location, and relative time.

**Edge cases:**
- Empty state (no events) — prompt to record a replacement
- Events for deleted devices show "Unknown" device name

**Prerequisites:** J1

---

## J11: View/Edit Event

**Entry point:** Tap an event in the History list
**Screen:** `Screen.History` -> `Screen.EventDetail(eventId)`

1. User taps an event in the history list.
2. Event detail shows date (with date picker to change) and event info.
3. User can edit the date and save, or delete the event.

**Edge cases:**
- Event not found (deleted by sync) — NotFound state

**Prerequisites:** J10 (at least one event exists)

---

## J12: Configure Settings

**Entry point:** Settings icon in top app bar
**Screen:** Any screen -> `Screen.Settings`

1. User taps the settings icon.
2. Settings screen shows: Network Mode selector, Export Data button, App Version.
3. User can change network mode (Prod Server, Dev Server, gRPC Local, Mock, None/Offline).

**Edge cases:**
- Switching network mode triggers sync reconnection

**Prerequisites:** J1

---

## J13: AI Chat

**Entry point:** AI input field in bottom bar (any tab)
**Screen:** AI overlay (not a separate Screen — overlays current tab via `AiChatViewModel` in App scope)

1. User types a message in the always-visible AI input field at the bottom of any tab.
2. User taps Send — AI overlay expands upward showing chat history.
3. AI processes the message with full device context (inventory, types, events).
4. AI response streams in with tool execution results (add device, record event, etc.).
5. User can continue chatting or collapse the overlay.

**Edge cases:**
- Blank input — ignored
- Input while processing — ignored (debounced)
- AI engine unavailable — error message shown
- Back press while overlay is open — collapses overlay (does not navigate back)
- Tab switching while chat is open — collapses overlay, chat state preserved

**Prerequisites:** J1

---

## J14: AI Batch Import

**Entry point:** AI batch input on Add Device, Add Device Type, or Add Battery Event screens
**Screens:** `Screen.AddDevice`, `Screen.AddDeviceType`, `Screen.AddBatteryEvent`

1. User navigates to any Add screen (J3, J8, or J6).
2. User enters natural language in the batch input area:
   - Devices: "bedroom alarm clock, kitchen smoke detector"
   - Types: "smoke alarms with 2 AAA batteries"
   - Events: "Replaced remote batteries today"
3. AI parses the input and calls appropriate tools:
   - `addDevice` — creates devices with parsed names/locations/types
   - `addDeviceType` — creates types with parsed specs (deduplicates by name)
   - `recordBatteryReplacement` — creates events with inferred dates
4. Progress messages show with spinner, success with checkmarks, errors with details.

**Edge cases:**
- AI fails to parse — error result shown
- Duplicate type names — skipped with "already exists" message
- Missing required fields (e.g., device name) — individual tool call returns error

**Prerequisites:** J1

---

## J15: Data Export

**Entry point:** Export button in Settings
**Screen:** `Screen.Settings`

1. User taps "Export Data" in Settings.
2. App generates a JSON file containing all devices, device types, and battery events.
3. Platform file-save dialog appears.
4. User saves `Battery_Butler_Backup_YYYYMMDD_HHMMSS.json`.

**Edge cases:**
- Empty database — exports valid JSON with empty arrays
- Export during sync — captures current local state

**Prerequisites:** J12

---

## J16: Sync Lifecycle

**Entry point:** Background process, triggered by CRUD operations and network state changes
**Screens:** All screens (sync status visible on Devices tab)

1. User performs any CRUD operation (add/edit/delete device, type, or event).
2. Change is persisted locally immediately (Room database).
3. When network is available, SyncManager pushes changes to server.
4. Server responds with latest state; SyncManager merges into local database.
5. Sync status indicator on Devices tab reflects current state: Idle, Syncing, Success (auto-dismiss), Failed.

**Edge cases:**
- Network unavailable — changes queue locally, sync on reconnect
- Server conflict — server state wins (last-write-wins)
- Sync failure — error shown in snackbar, auto-retry with backoff
- App backgrounded during sync — sync continues to completion

**Prerequisites:** J1, network mode set to a server option (J12)
