# Feature Parity Mapping

This document tracks the feature parity between the **Compose Multiplatform (CMP)** app and the **native iOS SwiftUI** app at the **per-feature** level. Screen-level parity is 100% (all 15 screens exist in both), but feature-level parity is approximately **40%**.

## Overview

The architecture shares ViewModels, UseCases, Repositories, and domain state across all platforms:
- **Android CMP & iOS CMP** use shared `@Composable` UI code via `presentation-feature`.
- **iOS SwiftUI** uses native SwiftUI views observing the same Kotlin ViewModels via SKIE plus KMP-ObservableViewModel (`@StateViewModel`); see bb-ovm1.

Most SwiftUI screens are minimal implementations that render basic data but lack sorting, grouping, error handling, empty states, icons, and interactive features that Compose provides.

## Summary

| Metric | Value |
|--------|-------|
| Screen parity (screen exists in both) | **15 / 15 (100%)** |
| Feature parity (per-feature match) | **~50%** |
| Screens at FULL parity | 0 |
| Screens at PARTIAL parity | 12 |
| Screens at MINIMAL parity | 3 |
| Screens at NONE parity | 0 |

## Rating Key

| Rating | Meaning |
|--------|---------|
| **FULL** | All features present; minor style differences only |
| **PARTIAL** | Core flow works but missing notable features |
| **MINIMAL** | Screen exists but missing most features beyond basic data display |
| **NONE** | Screen does not exist |

## Tracking

iOS SwiftUI feature-parity gaps are tracked in `TODO.md` (repo root).

## Cross-Cutting Gaps

These issues affect all SwiftUI screens, not just individual ones:

- **Localization**: All SwiftUI screens use hardcoded English strings. Compose uses full i18n string resources. This is a systemic gap that should be addressed holistically.
- **"Add New..." picker options**: Several Compose type pickers include inline "Add New Device Type..." or "Add New Device..." options. SwiftUI pickers lack these shortcuts across Add Device, Edit Device, and Add Battery Event screens.
- **~~Icon mapping~~**: ✅ Fixed in PR #873. `SFSymbolMapper` maps all 37 device type icon names to SF Symbols. `ButlerIconBox` wraps icons with themed sage green containers.

---

## Per-Screen Feature Assessment

### 1. Navigation / Tabs — PARTIAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Bottom tab bar (3 tabs) | ✅ | ✅ | — |
| Persistent AI input bar | ✅ | ❌ | SwiftUI has AI as a separate tab, not an always-visible input |
| AI overlay (split-screen) | ✅ | ❌ | No split-screen overlay; AI is a standalone screen |
| Tab transition animations | ✅ | ✅ | Different animation styles (both acceptable) |

**Compose:** `MainScreenShell` wraps tabs with persistent top bar, AI input bar, and bottom nav. AI expands as split-screen overlay.
**SwiftUI:** `MainScreen.swift` uses native `TabView`. AI is a separate tab.
**Tracking:** `bb-ke1y`

### 2. Home (Device List) — PARTIAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Device list display | ✅ | ✅ | — |
| Device icons (mapped by type) | ✅ | ✅ | `SFSymbolMapper` + `ButlerIconBox` (PR #873) |
| Battery age colors (gray/amber/red) | ✅ | ✅ | `BatteryAgeHelper` 3-tier coloring (PR #873) |
| Sort controls (name/type/age/location) | ✅ | ❌ | No sort UI |
| Group by location | ✅ | ❌ | Flat list only |
| Sync status indicator | ✅ | ❌ | No sync status display |
| Error state handling | ✅ | ❌ | No error UI |
| Empty state | ✅ | ❌ | No empty state message |
| Add device card | ✅ | ✅ | — |

**Compose:** `HomeScreenContent.kt` — full-featured with `DeviceIconMapper`, `StatCard`, sort/group dropdowns, battery age colors.
**SwiftUI:** `HomeScreen.swift` — themed list with `ButlerIconBox`, resolved type/location subtitle, battery age trailing with 3-tier coloring (PR #873).
**Tracking:** `bb-847x` (sort/group), `bb-wddv` (sync status)

### 3. History — PARTIAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Event list display | ✅ | ✅ | — |
| Device name on each event | ✅ | ✅ | PR #873 |
| Device type display | ✅ | ✅ | PR #873 |
| Location display | ✅ | ✅ | PR #873 |
| Days-ago relative date | ✅ | ✅ | PR #873 |
| Empty state | ✅ | ❌ | No empty state message |
| Tap to event detail | ✅ | ❌ | Items are not tappable — no navigation |
| Add event card | ✅ | ❌ | No add-event shortcut |
| Date badge visual (month/day) | ✅ | ✅ | Calendar badge with month/day (PR #873) |
| Sync status | ✅ | ❌ | — |

**Compose:** `HistoryListContent.kt` — enriched items with device context, relative dates, icons.
**SwiftUI:** `HistoryListScreen.swift` — enriched rows with calendar badge (month/day), device name, type/location subtitle, days-ago trailing (PR #873).
**Tracking:** `bb-wddv`

### 4. AI Chat — PARTIAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Chat message display | ✅ | ✅ | — |
| Send message | ✅ | ✅ | — |
| Chat bubble styling | ✅ | ✅ | Sage green user / steel blue AI bubbles (PR #873) |
| Split-screen overlay mode | ✅ | ❌ | SwiftUI is standalone screen only |
| Cross-tab persistence | ✅ | ❌ | Chat only visible in AI tab |
| Context hints | ✅ | ❌ | No contextual suggestions |
| Clear chat | ✅ | ✅ | — |
| Loading indicator | ✅ | ✅ | — |

**Compose:** `MainScreenShell` bottom bar + `AiTabContent` split-screen overlay; `AiChatScreen.kt` standalone.
**SwiftUI:** `AiChatScreen.swift` — standalone tab with themed `ChatBubbleShape` (sage green user, steel blue AI bubbles, PR #873).
**Tracking:** `bb-ke1y`

### 5. Settings — MINIMAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| App version display | ✅ | ✅ | — |
| Sign-out button | ✅ | ❌ | No sign-out option |
| Account info display | ✅ | ❌ | — |
| Network mode selector | ✅ | ❌ | No network mode switching |
| AI engine selector | ✅ | ❌ | — |
| Check for updates | ✅ | ❌ | — |
| Export data | ✅ | ❌ | — |
| Dynamic version (BuildConfig) | ✅ | ❌ | Hardcoded version string |

**Compose:** `SettingsScreen.kt` — full settings with network mode, AI engine, export, update check, sign-out.
**SwiftUI:** `SettingsScreen.swift` — shows app version only.
**Tracking:** `bb-abn6`

### 6. Add Device — PARTIAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Device name input | ✅ | ✅ | — |
| Device type picker | ✅ | ✅ | — |
| Location field | ✅ | ❌ | Hardcoded to empty string |
| Loading indicator during save | ✅ | ❌ | No loading state |
| AI batch import | ✅ | ❌ | — |
| Validation errors | ✅ | ✅ | — |

**Compose:** `AddDeviceScreen.kt` — name, type picker, location, loading state.
**SwiftUI:** `AddDeviceScreen.swift` — name and type picker only, no location.
**Tracking:** `bb-rrs4.2`

### 7. Device Detail — PARTIAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Device name display | ✅ | ✅ | — |
| Device type display | ✅ | ✅ | — |
| Location display | ✅ | ❌ | No location shown |
| Mapped device icon | ✅ | ✅ | `SFSymbolMapper` + `ButlerIconBox` (PR #873) |
| Battery stat cards (age, last replaced) | ✅ | ❌ | No stat cards |
| Battery age color | ✅ | ✅ | `BatteryAgeHelper` in DeviceRow trailing (PR #873) |
| Battery history list | ✅ | ✅ | — |
| Edit button | ✅ | ✅ | — |
| Navigation to event detail | ✅ | ✅ | — |

**Compose:** `DeviceDetailScreen.kt` — `DeviceIconMapper`, `LocationOn` icon, `StatCard` components, age colors.
**SwiftUI:** `DeviceDetailScreen.swift` — themed header with `ButlerIconBox`, sage green CTA button, resolved type name. Still missing stat cards and location display (PR #873).
**Tracking:** `bb-rrs4.1`

### 8. Edit Device — PARTIAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Edit device name | ✅ | ✅ | — |
| Edit device type | ✅ | ✅ | — |
| Edit location | ✅ | ✅ | — |
| Delete device button | ✅ | ✅ | — |
| Delete confirmation dialog | ✅ | ❌ | Deletes immediately without confirmation |
| Save changes | ✅ | ✅ | — |

**Compose:** `EditDeviceScreen.kt` — full edit form with `AlertDialog` delete confirmation.
**SwiftUI:** `EditDeviceScreen.swift` — edit form works, but delete has no confirmation.
**Tracking:** `bb-rrs4.4`

### 9. Add Battery Event — PARTIAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Device picker | ✅ | ✅ | — |
| Event type picker | ✅ | ✅ | — |
| Date picker | ✅ | ✅ | Different style (iOS interactive wheel picker, arguably better UX) |
| Notes field | ✅ | ✅ | — |
| Save with validation | ✅ | ✅ | — |
| "More Details" expandable toggle | ✅ | ❌ | Compose hides battery type/notes behind expandable; SwiftUI shows all fields always |
| "Add New Device" option in picker | ✅ | ❌ | Compose dropdown includes "Add New Device..." option |

**Compose:** `AddBatteryEventScreen.kt` — has expandable "More Details" section and inline "Add New Device" picker option.
**SwiftUI:** `AddBatteryEventScreen.swift` — core flow works but missing progressive disclosure and add-device shortcut.

### 10. Event Detail — NEAR FULL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Event data display | ✅ | ✅ | — |
| Device name display | ✅ | ✅ | — |
| Edit button (toolbar) | ✅ | ✅ | — (PR #907) |
| Edit event form (sheet) | ✅ | ✅ | — (PR #907) |
| Delete capability | ✅ | ✅ | — (PR #907, via edit screen) |
| Navigate to device | ✅ | ❌ | No device navigation link |

**Compose:** `EventDetailScreen.kt` — edit action in toolbar, device navigation.
**SwiftUI:** `EventDetailScreen.swift` — edit action opens `EditBatteryEventScreen` as sheet (PR #907). Device navigation not yet implemented.
**Tracking:** `bb-rrs4.5`

### 11. Add Device Type — MINIMAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Type name input | ✅ | ✅ | — |
| Battery type picker | ✅ | ✅ | — |
| Icon picker grid | ✅ | ❌ | No icon selection |
| Battery quantity (+/-) | ✅ | ❌ | No quantity controls |
| AI icon suggestion | ✅ | ❌ | — |
| Batch import | ✅ | ❌ | — |
| Load common types | ✅ | ❌ | — |
| Save with validation | ✅ | ✅ | — |

**Compose:** `AddDeviceTypeScreen.kt` — full-featured with icon picker, quantity, AI suggestion, batch import.
**SwiftUI:** `AddDeviceTypeScreen.swift` — name and battery type only.
**Tracking:** `bb-rrs4.3`

### 12. Edit Device Type — PARTIAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Edit type name | ✅ | ✅ | — |
| Edit battery type | ✅ | ✅ | — |
| Icon picker | ✅ | ❌ | No icon selection |
| Battery quantity | ✅ | ❌ | No quantity controls |
| Delete type | ✅ | ✅ | — |

**Compose:** `EditDeviceTypeScreen.kt` — full edit with icon picker and quantity.
**SwiftUI:** `EditDeviceTypeScreen.swift` — name and battery type only.
**Tracking:** `bb-57ln` (partial)

### 13. Device Types List — MINIMAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Type list display | ✅ | ✅ | — |
| Icon display per type | ✅ | ❌ | No icons shown |
| Battery quantity display | ✅ | ❌ | No quantity shown |
| Sort dropdown | ✅ | ❌ | No sort UI |
| Sort direction toggle | ✅ | ❌ | No direction control |
| Group dropdown | ✅ | ❌ | No group UI |
| Group direction toggle | ✅ | ❌ | No direction control |
| Load common types button | ✅ | ❌ | No button in empty state |
| Empty state (icon + buttons) | ✅ | ❌ | Single text line only |
| Add type card | ✅ | ✅ | Different placement (toolbar vs inline) |
| Tap to detail/edit | ✅ | ✅ | — |

**Compose:** `DeviceTypeListContent.kt` — icons, quantity, sort/group dropdowns.
**SwiftUI:** `DeviceTypeListScreen.swift` — basic list with name and battery type.
**Tracking:** `bb-847x`

### 14. Login — PARTIAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Google Sign-In button | ✅ | ✅ | — |
| Loading state | ✅ | ✅ | — |
| Typed error messages (7+ types) | ✅ | ❌ | SwiftUI shows generic "Failed to sign in" |
| Error state display | ✅ | ✅ (generic) | — |

**Compose:** `LoginScreen.kt` — maps `AuthError` subtypes to specific user-facing messages.
**SwiftUI:** `LoginScreen.swift` — shows a single generic error string.
**Tracking:** `bb-omvv`

### 15. Device Type Detail — PARTIAL

| Feature | Compose | SwiftUI | Gap |
|---------|---------|---------|-----|
| Type name + icon header | ✅ | ✅ | — |
| Battery type info | ✅ | ✅ | — |
| Battery quantity info | ✅ | ✅ | — |
| Devices list with navigation | ✅ | ✅ | — |
| Edit button (toolbar → sheet) | ✅ | ✅ | — |
| Empty devices state | ✅ | ✅ | — |
| Loading state | ✅ | ✅ | — |
| Not found state | ✅ | ✅ | — |

**Compose:** `DeviceTypeDetailContent.kt` — read-only detail with device list and edit navigation.
**SwiftUI:** `DeviceTypeDetailScreen.swift` — two-layer Screen + ContentView matching Compose flow (PR #911).
**Tracking:** `bb-tgd6` (closed)

---

## Task Tracking Summary

> Historical record. These IDs were tracked in Beads (now removed); most are
> closed (see PR links). Live parity work, if any, is in `TODO.md`.

| Task | Area | Priority | Status |
|------|------|----------|--------|
| `bb-rrs4` | Epic: iOS Feature Parity | P2 | Open |
| `bb-57ln` | EditBatteryEvent + DeviceTypeDetail | P2 | Closed (EditBatteryEvent done PR #907; DeviceTypeDetail → `bb-tgd6`) |
| `bb-ke1y` | AI chat tab/overlay parity | P2 | Open |
| `bb-847x` | Sort/group controls (Home + Types) | P2 | Open |
| `bb-abn6` | Settings (network, AI engine, account) | P2 | Open |
| `bb-omvv` | Login error handling | P3 | Open |
| `bb-wddv` | History enrichment + sync status | P3 | Open |
| `bb-rrs4.1` | Device Detail: location, stat cards, icons | P2 | Open |
| `bb-rrs4.2` | Add Device: location field, loading | P2 | Open |
| `bb-rrs4.3` | Add Device Type: icon picker, quantity, AI | P2 | Open |
| `bb-rrs4.4` | Edit Device: delete confirmation dialog | P3 | Open |
| `bb-rrs4.5` | Event Detail: Edit button, device nav | P2 | Open |
