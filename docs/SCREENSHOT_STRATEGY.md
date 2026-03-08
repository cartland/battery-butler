# Screenshot Test Strategy

This document defines **what** we screenshot-test, **why**, and **how** the two platforms should stay in sync.

## Purpose & Philosophy

Screenshots are **visual regression baselines**, not correctness tests. ViewModel unit tests cover logic; screenshots catch unintended visual changes across every user-reachable screen state.

Following the strategy matters more than maximizing count — untested states should be deliberate, not accidental.

## Priority Tiers

### Tier 1 — Required (every screen, every key state)

Each screen in `Screen.kt` must have at least one screenshot per **meaningful UI state** — states that render visually distinct layouts:

- **Success / With Data** — the primary populated view
- **Empty** — no items, shows empty-state messaging
- **Loading** — spinner or skeleton
- **NotFound / Error** — entity missing or request failed

Light and dark mode for each state (Android has this today; iOS does not yet — see [Platform Parity Rules](#platform-parity-rules)).

### Tier 2 — Recommended (data variations within a state)

- Populated vs. empty lists (already covered for most screens)
- Edge cases that affect layout: deleted-device references, very old battery age colors, long text truncation
- AI overlay states (collapsed on each tab, expanded, full-height)
- Settings variants (network mode options)

### Tier 3 — Optional (components and form factors)

- Shared components in isolation (ButlerListItemCard, ButlerIconBox, DeviceRow, etc.)
- Tablet / alternate device form factors (currently only Play Store screenshots)
- Play Store marketing screenshots (light-only is intentional)

## Platform Parity Rules

For each screen, both platforms should cover the same set of **Tier 1** states unless there is a documented reason for divergence. Valid reasons:

| Reason | Example |
|--------|---------|
| **Framework limitation** | iOS dark mode snapshots require `.preferredColorScheme` wiring — not yet implemented |
| **Platform-specific UI** | AI overlay is Android-only (split-screen panel in Compose, not yet built in SwiftUI) |
| **Deliberate deferral** | Low-risk state deferred with tracking issue |

When adding a screenshot test to one platform, check the parity matrix below. If the other platform is missing the same state, either add it or document the reason.

## Current State — Parity Matrix

**Counts:** Android 131 PNGs (74 test functions, 14 files) · iOS 46 PNGs (46 test functions, 19 files)

**Key structural differences:**
- Android tests every state in both light and dark mode; iOS is light-only throughout
- Android has Play Store marketing screenshots (phone + 7" tablet + 10" tablet); iOS does not
- Android has component gallery tests (12 components); iOS tests a smaller set of row components

### Screen-Level Coverage

| Screen | State | Android | iOS | Gap Reason |
|--------|-------|---------|-----|------------|
| **Login** | Unauthenticated | ✓ L/D | ✓ | iOS: no dark mode snapshots yet |
| | Authenticating | ✓ L/D | ✓ | iOS: no dark mode |
| | NotConfigured | ✓ L/D | — | iOS: state not implemented in SwiftUI snapshot tests |
| | Error | ✓ L/D | — | iOS: error alert not snapshot-tested |
| **Devices** | With data | ✓ L/D | ✓ | iOS: no dark mode |
| | Empty | ✓ L/D | ✓ | iOS: no dark mode |
| **History** | With data | ✓ L/D | ✓ | iOS: no dark mode |
| | Empty | ✓ L/D | ✓ | iOS: no dark mode |
| | Loading | ✓ L/D | ✓ | — |
| **Types** | With data | ✓ L/D | ✓ | iOS: no dark mode |
| | Empty | ✓ L/D | ✓ | iOS: no dark mode |
| | Loading | ✓ L/D | ✓ | — |
| **Settings** | Default | ✓ L/D | ✓ | iOS: no dark mode |
| | All network modes | ✓ L/D | — | iOS: variant not tested |
| **AddDevice** | Empty form | ✓ L/D | ✓ | iOS: no dark mode |
| | Filled form | — | ✓ | Android: only tests empty form |
| **AddBatteryEvent** | With devices | ✓ L/D | ✓ | iOS: no dark mode |
| | Empty (no devices) | ✓ L/D | ✓ | — |
| **AddDeviceType** | Empty form | ✓ L/D | ✓ | iOS: no dark mode |
| | Filled form | — | ✓ | Android: only tests empty form |
| | Error (duplicate) | — | ✓ | Android: error state not previewed |
| **DeviceDetail** | Success | ✓ L/D | ✓ | iOS: no dark mode |
| | Loading | ✓ L/D | ✓ | iOS: no dark mode |
| | NotFound | ✓ L/D | ✓ | iOS: no dark mode |
| **DeviceTypeDetail** | Success | ✓ L/D | ✓ | iOS: no dark mode (PR #911) |
| | Loading | ✓ L/D | ✓ | iOS: no dark mode (PR #911) |
| | NotFound | ✓ L/D | ✓ | iOS: no dark mode (PR #911) |
| **EditDevice** | Loaded form | ✓ L/D | ✓ | iOS: no dark mode |
| | Loading | ✓ L/D | ✓ | iOS: no dark mode |
| | NotFound | ✓ L/D | ✓ | iOS: no dark mode |
| **EditDeviceType** | Loaded form | ✓ L/D | ✓ | iOS: no dark mode |
| | Loading | ✓ L/D | ✓ | — |
| | NotFound | ✓ L/D | ✓ | — |
| **EventDetail** | Success | ✓ L/D | ✓ | iOS: no dark mode |
| | Deleted device ref | ✓ L/D | — | iOS: edge case not tested |
| | NotFound | ✓ L/D | ✓ | iOS: no dark mode |
| | Loading | ✓ L/D | ✓ | — |
| **EditBatteryEvent** | Loaded form | ✓ L/D | ✓ | iOS: no dark mode (PR #907) |
| | Loading | — | ✓ | Android: state not previewed |
| | NotFound | ✓ L/D | ✓ | iOS: no dark mode (PR #907) |
| **AI Chat** | With messages | ✓ L/D | ✓ | iOS: no dark mode |
| | Empty | ✓ L/D | ✓ | iOS: no dark mode |
| **AI Overlay** | Collapsed (Devices) | ✓ L/D | — | Platform-specific: Android-only split-screen panel |
| | Collapsed (Types) | ✓ L/D | — | Platform-specific |
| | Collapsed (History) | ✓ L/D | — | Platform-specific |
| | Expanded | ✓ L/D | — | Platform-specific |
| | Full height | ✓ L/D | — | Platform-specific |

### Component-Level Coverage

| Component | Android | iOS | Notes |
|-----------|---------|-----|-------|
| ButlerListItemCard | ✓ L/D | — | Tier 3 |
| ButlerIconBox | ✓ L/D | — | Tier 3 |
| DeviceListItem | ✓ L/D | — | iOS tests `DeviceRow` instead (3 variants) |
| DeviceListItem (old battery) | ✓ L/D | — | Tier 2: battery age color edge case |
| DeviceListItem (very old battery) | ✓ L/D | — | Tier 2: battery age color edge case |
| DeviceTypeListItem | ✓ L/D | — | iOS tests `DeviceTypeRow` instead (2 variants) |
| DeviceTypeIconItem | ✓ L/D | — | Tier 3 |
| HistoryListItem | ✓ L/D | — | Tier 3 |
| AddItemCard | ✓ L/D | — | Tier 3 |
| CompositeControl | ✓ L/D | — | Tier 3 |
| EmptyStateContent | ✓ L/D | — | Tier 3 |
| ExpandableSelectionControl | ✓ L/D | — | Tier 3 |
| DeviceIcons gallery | ✓ L/D | — | Tier 3 |
| TopAppBar (default) | ✓ L/D | — | Tier 3 |
| TopAppBar (with actions) | ✓ L/D | — | Tier 3 |
| DeviceRow | — | ✓ (3) | iOS-specific component name |
| DeviceTypeRow | — | ✓ (2) | iOS-specific component name |
| MessageRow | — | ✓ (2) | iOS-specific component name |

### Marketing / Play Store

| Form Factor | Android | iOS | Notes |
|-------------|---------|-----|-------|
| Phone (Pixel 5) | ✓ Light (5) | — | Tier 3: Play Store listing |
| 7" Tablet | ✓ Light (5) | — | Tier 3: Play Store listing |
| 10" Tablet | ✓ Light (5) | — | Tier 3: Play Store listing |

## Gap Summary

### Systemic gaps

1. **iOS has no dark mode snapshots** — all 46 snapshots are light-only. Android has light + dark for every non-marketing state. Adding `.preferredColorScheme(.dark)` variants would roughly double iOS snapshot count.
2. **iOS Tier 1 complete** — all screens now have snapshot coverage for their meaningful states. DeviceTypeDetail was the last gap, closed by PR #911.
3. **Android Tier 1 gaps closed** — History, Types, EventDetail, EditDeviceType Loading states and EditDevice Loading/NotFound states now have coverage.

### Unique strengths

- **Android**: Comprehensive dark mode coverage, component gallery, Play Store form factor screenshots, AI overlay states, battery age color edge cases
- **iOS**: More form-state variants (filled forms, error states for AddDeviceType), Loading states for several screens Android lacks

## Adding a New Screenshot Test

### Checklist

1. **Check the parity matrix** — does this screen/state need coverage?
2. **Both platforms?** If only one, document the reason in the matrix
3. **Which tier?** Tier 1 = required before merge. Tier 2/3 = nice to have
4. **Android**: Add `@Preview` composable + `@PreviewScreenshots` test wrapper. Both light and dark variants are generated automatically from a single preview
5. **iOS**: Add XCTest function with `assertSnapshot(of:as:)` using `ViewImageConfig.iPhoneX` (or appropriate device config)
6. **Keep test files ≤ 10 tests** to avoid OOM on CI runners (see [ADR: Split files, not heap](../docs/architecture/adr-005-screenshot-oom-split-files.md))
7. **Regenerate reference images**: Android: `./scripts/generate-android-screenshots.sh` · iOS: `./scripts/record-ios-snapshots.sh`
8. **Update this matrix** when adding or removing screenshot tests

### Android file template

```kotlin
// android-screenshot-tests/src/screenshotTest/kotlin/.../NewScreenScreenshotTest.kt
class NewScreenScreenshotTest {
    @PreviewScreenshots
    @Preview
    @Composable
    fun NewScreenSuccessPreview() {
        BatteryButlerTheme {
            NewScreenContent(uiState = NewScreenUiState.Success(data))
        }
    }
}
```

### iOS file template

```swift
// ios-app-swift-ui/iosAppSwiftUITests/NewScreenTests.swift
final class NewScreenTests: XCTestCase {
    func testNewScreenContentView_Success() {
        let view = NewScreenContentView(/* ... */)
        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhoneX)))
    }
}
```

## Removing Screenshot Tests

- **Never remove a Tier 1 test** without replacing it (screen refactored → new test for new screen)
- **Component tests (Tier 3)** can be removed if the component is deleted
- **Update the parity matrix** when adding or removing tests
