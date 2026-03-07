# iOS SwiftUI Design Language

A complete visual specification for the Battery Butler iOS app. This document defines the
shared "Sage & Linen" design identity expressed through iOS-native SwiftUI idioms.

> **Status:** Documentation only — no code changes. Serves as the reference for follow-up
> implementation PRs.

---

## 1. Design Philosophy

- **Shared identity, native expression.** Both platforms use the same color palette, spacing
  scale, and component hierarchy. iOS uses SwiftUI idioms (SF Symbols, system fonts,
  `List`/`Form`), not a Material Design port.
- **Same rhythm, different feel.** A user switching between Android and iOS should recognize
  the same app — same sage greens, same card structure, same battery age coloring — but each
  platform should feel native.
- **Design tokens over hardcoded values.** All colors, spacing, radii, and icon sizes are
  defined as named constants. No magic numbers in views.

---

## 2. Color Palette

All hex values sourced from `presentation-core/.../theme/Color.kt`. The `FF` alpha prefix
is omitted — all colors are fully opaque unless noted.

### 2.1 Semantic Roles

| Role | Light | Dark | Usage |
|------|-------|------|-------|
| **Primary** | `#537A66` Deep Sage | `#7CA38F` Lighter Sage | Buttons, tint, active states |
| **OnPrimary** | `#FFFFFF` | `#FFFFFF` | Text/icons on primary backgrounds |
| **PrimaryContainer** | `#CCE8D7` Pastel Sage | `#3A5C4A` Dark Sage | Icon containers, chips |
| **OnPrimaryContainer** | `#0D3322` Deep Forest | `#CCE8D7` Pastel Sage | Text/icons in containers |
| **Secondary** | `#8B7355` Warm Walnut | `#8B7355` | Secondary actions, accents |
| **OnSecondary** | `#FFFFFF` | `#FFFFFF` | Text/icons on secondary |
| **SecondaryContainer** | `#EBDFC8` Warm Cream | `#5B4933` Dark Walnut | Secondary chips, tags |
| **OnSecondaryContainer** | `#2B1E0D` Deep Espresso | `#EBDFC8` Warm Cream | Text in secondary containers |
| **Tertiary** | `#5E7A91` Steel Blue | `#5E7A91` | AI/chat elements, special accents |
| **OnTertiary** | `#FFFFFF` | `#FFFFFF` | Text/icons on tertiary |
| **TertiaryContainer** | `#D5E3EC` Light Blue-Grey | `#3A5163` Dark Steel Blue | AI chat bubbles (model) |
| **OnTertiaryContainer** | `#19333F` | `#D5E3EC` | Text in tertiary containers |
| **Background** | `#F7F5EF` Warm Linen | `#191C1A` Warm Dark | Screen background |
| **OnBackground** | `#2D2926` Soft Black | `#E3E2E6` | Primary text |
| **Surface** | `#FEFCF8` Off-White | `#252927` | Cards, elevated surfaces |
| **OnSurface** | `#2D2926` | `#E3E2E6` | Text on surfaces |
| **Error** | `#BA1A1A` | `#FFB4AB` | Error states, 365+ day batteries |
| **OnError** | `#FFFFFF` | `#690005` | Text/icons on error |
| **ErrorContainer** | `#FFDAD6` | `#93000A` | Error backgrounds |
| **OnErrorContainer** | `#410002` | `#FFDAD6` | Text in error containers |
| **Outline** | `#79756C` Warm Grey | `#928F86` Lighter Warm Grey | Borders, dividers |

### 2.2 Battery Age Warning Colors

| Threshold | Light | Dark | Weight |
|-----------|-------|------|--------|
| 0–179 days | `onSurfaceVariant` (default gray) | `onSurfaceVariant` | Normal |
| 180–364 days | `#956D00` Dark Amber | `#E5A100` Bright Amber | Normal |
| 365+ days | `error` (`#BA1A1A` / `#FFB4AB`) | `error` | **Bold** |
| Unknown (nil) | `onSurfaceVariant` | `onSurfaceVariant` | Normal |

### 2.3 SwiftUI Implementation Guidance

Define colors as a `Color` extension:

```swift
extension Color {
    // Do NOT use Color("name") from asset catalog — define programmatically
    // for light/dark support, use UIColor with trait collection or
    // Color(light:dark:) pattern

    static let butlerPrimary = Color(light: Color(hex: 0x537A66),
                                     dark: Color(hex: 0x7CA38F))
    static let butlerBackground = Color(light: Color(hex: 0xF7F5EF),
                                        dark: Color(hex: 0x191C1A))
    // ... etc.
}
```

### 2.4 Anti-Patterns

- **NEVER** use `.blue`, `.green`, or `.accentColor` for themed UI elements.
- **NEVER** use system tint colors (`.tint(.blue)`) — use `.tint(.butlerPrimary)`.
- **NEVER** hardcode hex values in views — always reference named color constants.

---

## 3. Spacing Scale

Matches `presentation-core/.../theme/Padding.kt` exactly.

| Token | Value | Usage |
|-------|-------|-------|
| `extraSmall` | 4 pt | Tight spacing within components (icon-to-label in compact layouts) |
| `small` | 8 pt | Compact layouts, icon-to-text gaps, inline padding |
| `medium` | 12 pt | Related elements, chat bubble internal padding |
| `standard` | 16 pt | Default content padding, list row horizontal insets |
| `large` | 24 pt | Section separation, vertical gaps between groups |
| `extraLarge` | 32 pt | Major section breaks, screen-level top/bottom margins |

### SwiftUI Implementation

```swift
enum ButlerSpacing {
    static let extraSmall: CGFloat = 4
    static let small: CGFloat = 8
    static let medium: CGFloat = 12
    static let standard: CGFloat = 16
    static let large: CGFloat = 24
    static let extraLarge: CGFloat = 32
}
```

---

## 4. Corner Radius Scale

Matches `presentation-core/.../theme/Padding.kt` `CornerRadius` object.

| Token | Value | Usage |
|-------|-------|-------|
| `extraSmall` | 4 pt | Subtle rounding on small elements |
| `small` | 8 pt | Icon containers (IconBox) |
| `medium` | 12 pt | Cards, list item containers |
| `large` | 16 pt | Chat bubbles, prominent interactive elements |
| `extraLarge` | 28 pt | Pills, fully rounded buttons, capsule shapes |

### SwiftUI Implementation

```swift
enum ButlerCornerRadius {
    static let extraSmall: CGFloat = 4
    static let small: CGFloat = 8
    static let medium: CGFloat = 12
    static let large: CGFloat = 16
    static let extraLarge: CGFloat = 28
}
```

---

## 5. Icon Sizes

Matches `presentation-core/.../theme/IconSize.kt`.

| Token | Value | Usage |
|-------|-------|-------|
| `extraSmall` | 16 pt | Small indicators, secondary icons |
| `small` | 18 pt | Dropdown indicators, minor actions |
| `medium` | 24 pt | Default icon size — list items, buttons, navigation |
| `large` | 32 pt | Featured icons, selection states, hero elements |

### SwiftUI Implementation

```swift
enum ButlerIconSize {
    static let extraSmall: CGFloat = 16
    static let small: CGFloat = 18
    static let medium: CGFloat = 24
    static let large: CGFloat = 32
}
```

Use `.font(.system(size: ButlerIconSize.medium))` on `Image(systemName:)` for consistent
SF Symbol sizing.

---

## 6. Component Patterns

Six core components, each adapting an Android equivalent to SwiftUI idioms.

### 6.1 ButlerIconBox

**Android source:** `ButlerIconBox` — 48dp Box, primaryContainer background, shapes.small.

**SwiftUI spec:**

| Property | Value |
|----------|-------|
| Frame | 44–48 pt × 44–48 pt (44 preferred for iOS touch targets) |
| Background | `primaryContainer` (sage green) |
| Corner radius | `CornerRadius.small` (8 pt) — `RoundedRectangle(cornerRadius: 8)` |
| Icon size | `IconSize.medium` (24 pt) |
| Icon color | `onPrimaryContainer` (deep forest / pastel sage) |
| Alignment | Center |

```swift
// Conceptual structure
struct ButlerIconBox: View {
    let systemName: String
    var containerColor: Color = .butlerPrimaryContainer
    var contentColor: Color = .butlerOnPrimaryContainer

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: ButlerIconSize.medium))
            .foregroundStyle(contentColor)
            .frame(width: 44, height: 44)
            .background(containerColor, in: RoundedRectangle(cornerRadius: ButlerCornerRadius.small))
    }
}
```

**Key difference from current iOS:** DeviceRow uses bare `Image(systemName: "cpu")` with
`.foregroundColor(.blue)`. The icon box adds visual weight, branded color, and consistent
sizing.

### 6.2 ButlerListItemCard

**Android source:** `ButlerListItemCard` — 1dp outlineVariant border (0.5 alpha),
shapes.medium, standard padding.

**SwiftUI spec (two contexts):**

**In `List` context:** Use native `List` row styling. Add a subtle border overlay:

```swift
.overlay(
    RoundedRectangle(cornerRadius: ButlerCornerRadius.medium)
        .stroke(Color.butlerOutline.opacity(0.5), lineWidth: 1)
)
```

**In `ScrollView` context:** Full custom card:

| Property | Value |
|----------|-------|
| Background | `surface` |
| Corner radius | `CornerRadius.medium` (12 pt) |
| Border | 1 pt `outline` at 0.5 alpha |
| Internal padding | `Spacing.standard` (16 pt) |
| Layout | HStack: leading icon → Spacer(16) → content VStack → optional trailing |
| Vertical alignment | `.center` |

### 6.3 DeviceListItem

**Android source:** `DeviceListItem` — icon + device name + type/location + battery age
trailing.

**SwiftUI spec:**

```
┌─────────────────────────────────────────────────────────┐
│  ┌──────┐                                    ┌───────┐  │
│  │ Icon │  Device Name              (title)  │ 🔋    │  │
│  │ Box  │  Type • Location       (subtitle)  │ 42d   │  │
│  └──────┘                                    └───────┘  │
└─────────────────────────────────────────────────────────┘
```

| Element | Spec |
|---------|------|
| Leading | `ButlerIconBox` with SF Symbol from icon mapper |
| Title | Device name — `.headline` weight `.semibold`, 1 line, truncated |
| Subtitle | "TypeName • Location" — `.subheadline`, `onSurfaceVariant` color, 1 line |
| Trailing | VStack (width ~60 pt): battery SF Symbol + "Nd" text |
| Battery icon color | Age-based (see §7) |
| Battery text color | Age-based (see §7) |
| Battery text weight | `.bold` if 365+ days, default otherwise |
| Row padding | `Spacing.standard` (16 pt) |

**Key difference from current iOS:** DeviceRow shows raw `device.typeId` as subtitle,
has 4pt vertical padding, uses `.blue` icon and `.green` battery. New spec shows resolved
type name + location, proper spacing, themed colors, and age-based battery coloring.

### 6.4 DeviceTypeListItem

**Android source:** `DeviceTypeListItem` — icon + type name + battery count/type.

**SwiftUI spec:**

```
┌─────────────────────────────────────────────────┐
│  ┌──────┐                                       │
│  │ Icon │  Smoke Detector                       │
│  │ Box  │  4 × CR123A                           │
│  └──────┘                                       │
└─────────────────────────────────────────────────┘
```

| Element | Spec |
|---------|------|
| Leading | `ButlerIconBox` with SF Symbol for the device type icon |
| Title | Type name — `.headline` weight `.semibold`, 1 line |
| Subtitle | "N × BatteryType" — `.subheadline`, `onSurfaceVariant` |

### 6.5 HistoryListItem

**Android source:** `HistoryListItem` — calendar badge + device info + days-ago trailing.

**SwiftUI spec:**

```
┌──────────────────────────────────────────────────────────┐
│  ┌──────┐                                     ┌───────┐  │
│  │ JAN  │  Living Room Smoke Detector  (name) │ 🔋    │  │
│  │  15  │  Smoke Detector • Living Rm  (info) │ 42d   │  │
│  └──────┘                                     └───────┘  │
└──────────────────────────────────────────────────────────┘
```

**Calendar badge (leading):**

| Property | Value |
|----------|-------|
| Frame | 50 pt × 50 pt |
| Background | `surfaceVariant` |
| Corner radius | 10 pt |
| Month text | Abbreviated uppercase (e.g., "JAN") — `.caption`, `.bold`, `onSurfaceVariant` |
| Day text | Numeric (e.g., "15") — `.headline`, `.bold`, `onSurface` |
| Alignment | Center |

**Trailing (days ago):**

| Property | Value |
|----------|-------|
| Width | ~60 pt |
| Icon | `battery.100` SF Symbol, `onSurfaceVariant` tint |
| Text | "Nd ago" — `.caption`, `onSurfaceVariant` |
| Alignment | Center |

**Key difference from current iOS:** History rows currently show raw ISO timestamps
like `2024-01-01T00:00:00Z`. The calendar badge provides at-a-glance date scanning.

### 6.6 ChatBubble

**AI chat message rows in the AI overlay.**

| Sender | Background | Text Color | Alignment |
|--------|-----------|------------|-----------|
| User | `primaryContainer` (sage green) | `onPrimaryContainer` | Trailing |
| Model | `surfaceVariant` (system gray) | `onSurfaceVariant` | Leading |

| Property | Value |
|----------|-------|
| Corner radius | `CornerRadius.large` (16 pt) |
| Internal padding | `Spacing.medium` (12 pt) |
| Max width | ~80% of container |

**Key difference from current iOS:** User messages should use sage green, not system blue.

---

## 7. Battery Age Coloring

Three-tier system matching `DeviceListComponents.kt` `batteryAgeColor()`.

### Logic

```swift
func batteryAgeColor(days: Int?) -> Color {
    guard let days = days else {
        return .butlerOnSurfaceVariant  // unknown → default gray
    }
    switch days {
    case 365...:
        return .butlerError             // urgent red
    case 180..<365:
        return .butlerBatteryWarning    // amber warning
    default:
        return .butlerOnSurfaceVariant  // normal gray
    }
}

func batteryAgeFontWeight(days: Int?) -> Font.Weight {
    guard let days = days, days >= 365 else { return .regular }
    return .bold
}
```

### Color Values

| Tier | Days | Light Color | Dark Color | Font Weight |
|------|------|-------------|------------|-------------|
| Normal | 0–179 | `onSurfaceVariant` | `onSurfaceVariant` | Regular |
| Warning | 180–364 | `#956D00` Dark Amber | `#E5A100` Bright Amber | Regular |
| Urgent | 365+ | `#BA1A1A` Error Red | `#FFB4AB` Error Pink | **Bold** |
| Unknown | nil | `onSurfaceVariant` | `onSurfaceVariant` | Regular |

All values are WCAG AA compliant on their respective backgrounds.

---

## 8. SF Symbol Mapping

Maps all 37 Android `DeviceIconMapper` icon names to SF Symbol equivalents. Source:
`presentation-core/.../components/DeviceIconMapper.kt`.

**All icons use sage green (Primary) tinting — no per-category colors.**

### Shapes

| Icon Name | Android (Material) | SF Symbol |
|-----------|--------------------|-----------|
| `star` | `Icons.Default.Star` | `star.fill` |
| `circle` | `Icons.Default.Circle` | `circle.fill` |
| `square` | `Icons.Default.Square` | `square.fill` |
| `favorite` | `Icons.Default.Favorite` | `heart.fill` |
| `diamond` | `Icons.Default.Diamond` | `diamond.fill` |
| `hexagon` | `Icons.Default.Hexagon` | `hexagon.fill` |

### Electronics

| Icon Name | Android (Material) | SF Symbol |
|-----------|--------------------|-----------|
| `smartphone` | `Icons.Default.Smartphone` | `iphone` |
| `tablet` | `Icons.Default.TabletMac` | `ipad` |
| `laptop` | `Icons.Default.Laptop` | `laptopcomputer` |
| `watch` | `Icons.Default.Watch` | `applewatch` |
| `headphones` | `Icons.Default.Headphones` | `headphones` |
| `camera` | `Icons.Default.CameraAlt` | `camera.fill` |
| `speaker` | `Icons.Default.Speaker` | `hifispeaker.fill` |
| `videogame_asset` | `Icons.Default.VideogameAsset` | `gamecontroller.fill` |
| `game_controller` | `Icons.Default.SportsEsports` | `gamecontroller.fill` |
| `tv` | `Icons.Default.Tv` | `tv` |
| `router` | `Icons.Default.Router` | `wifi.router` |
| `power` | `Icons.Default.Power` | `bolt.fill` |
| `smart_button` | `Icons.Default.SmartButton` | `button.programmable` |
| `settings_remote` | `Icons.Default.SettingsRemote` | `av.remote.fill` |
| `mouse` | `Icons.Default.Mouse` | `computermouse.fill` |
| `keyboard` | `Icons.Default.Keyboard` | `keyboard` |

### Home / Smart Home

| Icon Name | Android (Material) | SF Symbol |
|-----------|--------------------|-----------|
| `lightbulb` | `Icons.Default.Lightbulb` | `lightbulb.fill` |
| `detector_smoke` | `Icons.Default.Propane` | `sensor.fill` |
| `thermostat` | `Icons.Default.Thermostat` | `thermometer.medium` |
| `sensors` | `Icons.Default.Sensors` | `sensor.fill` |
| `lock` | `Icons.Default.Lock` | `lock.fill` |
| `garage_home` | `Icons.Default.Garage` | `door.garage.closed` |

### Tools / Utility

| Icon Name | Android (Material) | SF Symbol |
|-----------|--------------------|-----------|
| `flashlight_on` | `Icons.Default.Highlight` | `flashlight.on.fill` |
| `drill` | `Icons.Default.Build` | `wrench.and.screwdriver.fill` |
| `brush` | `Icons.Default.Brush` | `paintbrush.fill` |
| `scale` | `Icons.Default.Scale` | `scalemass.fill` |
| `straighten` | `Icons.Default.Straighten` | `ruler.fill` |
| `water_drop` | `Icons.Default.WaterDrop` | `drop.fill` |

### Other

| Icon Name | Android (Material) | SF Symbol |
|-----------|--------------------|-----------|
| `car` | `Icons.Default.DirectionsCar` | `car.fill` |
| `bike` | `Icons.Default.PedalBike` | `bicycle` |
| `schedule` | `Icons.Default.Schedule` | `clock.fill` |
| `location_on` | `Icons.Default.LocationOn` | `location.fill` |
| `account_balance_wallet` | `Icons.Default.AccountBalanceWallet` | `creditcard.fill` |
| `toys` | `Icons.Default.Toys` | `teddybear.fill` |
| *(default/unknown)* | `Icons.Default.DevicesOther` | `desktopcomputer` |

---

## 9. Typography Mapping

Maps Android Material 3 text styles to SwiftUI equivalents.

| Android (Material 3) | SwiftUI | Usage |
|----------------------|---------|-------|
| `titleLarge` | `.title` | Screen titles |
| `headlineMedium` | `.title2` | Section headers |
| `titleMedium` + `SemiBold` | `.headline` | Card titles, device names |
| `bodyMedium` | `.subheadline` | Subtitles, descriptions, secondary text |
| `bodySmall` | `.footnote` | Tertiary information |
| `labelSmall` | `.caption` | Metadata, dates, badge text |

### Notes

- Use `.fontWeight(.semibold)` on `.headline` for device name emphasis (matches Android
  `titleMedium` + `FontWeight.SemiBold`).
- Use `.fontWeight(.bold)` for battery age text at 365+ days.
- Stick to system Dynamic Type sizes — do not hardcode point sizes for text.

---

## 10. Screen Layout Patterns

### Form Screens (Settings, Edit screens)

Use SwiftUI `Form` with `Section` headers:

```swift
Form {
    Section("Device Information") {
        // rows
    }
    Section("Battery") {
        // rows
    }
}
.tint(.butlerPrimary)  // Sage green for all interactive elements
```

### List Screens (Devices, Device Types, History)

Use standard `List` with enriched row content:

```swift
List {
    ForEach(devices) { device in
        DeviceListItem(device: device, deviceType: types[device.typeId])
    }
}
.listStyle(.plain)
```

### Detail / Scroll Screens

Use `ScrollView` + `VStack` with themed components:

```swift
ScrollView {
    VStack(spacing: ButlerSpacing.standard) {
        // ButlerListItemCard sections
    }
    .padding(.horizontal, ButlerSpacing.standard)
}
.background(Color.butlerBackground)
```

### CTA Buttons

Replace flat blue buttons with themed cards:

```swift
// Instead of: Button("Replaced Battery") { ... }
// Use a prominent card-style CTA:
ButlerListItemCard(onClick: action) {
    ButlerIconBox(systemName: "battery.100.circle.fill")
} content: {
    Text("Record Replacement").font(.headline).fontWeight(.semibold)
    Text("Mark battery as replaced today").font(.subheadline)
        .foregroundStyle(.butlerOnSurfaceVariant)
}
```

---

## 11. Anti-Patterns

Explicit "do not do" list for iOS implementation.

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| `.foregroundColor(.blue)` on icons | `.foregroundStyle(.butlerPrimary)` or container colors |
| `.foregroundColor(.green)` on battery | Age-based coloring via `batteryAgeColor(days:)` |
| `.accentColor` for tint | `.tint(.butlerPrimary)` |
| `padding(.vertical, 4)` on rows | `ButlerSpacing.standard` (16 pt) for comfortable touch targets |
| Raw ISO timestamps (`2024-01-01T00:00:00Z`) | Calendar badge (month/day) or formatted date strings |
| Bare `Image(systemName:)` for list icons | Wrap in `ButlerIconBox` with themed container |
| `device.typeId` shown raw to user | Resolve to type name via `deviceType.name` |
| Hardcoded `Color(hex: 0x537A66)` in views | Reference `Color.butlerPrimary` constant |
| `Color.white` / `Color.black` for backgrounds | `Color.butlerBackground` / `Color.butlerSurface` |
| `12` as a magic number for corner radius | `ButlerCornerRadius.medium` |

---

## 12. Implementation Roadmap

Suggested PR sequence for adopting this design language. Each step is an independent PR.

### Phase 1: Foundation

**PR 1 — Design tokens**
- `Color` extensions with all palette colors (light/dark)
- `ButlerSpacing`, `ButlerCornerRadius`, `ButlerIconSize` enums
- No visual changes yet — just constants

### Phase 2: Components

**PR 2 — Core components**
- `ButlerIconBox` SwiftUI view
- `ButlerListItemCard` SwiftUI view
- Unit tests for component rendering

**PR 3 — SF Symbol mapper**
- `SFSymbolMapper` mapping icon name strings → SF Symbol names
- Fallback to `desktopcomputer`
- Unit test covering all 37 entries

### Phase 3: Screen Migrations (one PR per screen)

**PR 4 — DevicesScreen**
- Replace `DeviceRow` with themed `DeviceListItem`
- Add battery age coloring
- Remove `.blue` and `.green` hardcoded colors

**PR 5 — DeviceTypesScreen**
- Replace type rows with themed `DeviceTypeListItem`
- Add icon containers

**PR 6 — HistoryScreen**
- Add calendar badge to history rows
- Replace raw timestamps with formatted dates
- Add days-ago trailing metadata

**PR 7–10 — Remaining screens**
- DeviceDetailScreen, EditBatteryEventScreen, AddDeviceScreen,
  NewDeviceTypeScreen, EditDeviceTypeScreen
- Apply themed colors, spacing, and components

### Phase 4: Polish

**PR 11 — Chat bubbles**
- Sage green user bubbles (not blue)
- Proper spacing and radius

**PR 12 — CTA buttons**
- Replace flat blue buttons with card-style CTAs
- Sage green tinting throughout

---

## Appendix: Reference Files

| File | Contains |
|------|----------|
| `presentation-core/.../theme/Color.kt` | All color definitions |
| `presentation-core/.../theme/Padding.kt` | Spacing and corner radius scales |
| `presentation-core/.../theme/IconSize.kt` | Icon size scale |
| `presentation-core/.../components/ButlerListItemCard.kt` | Card and IconBox components |
| `presentation-core/.../components/DeviceListComponents.kt` | Device list items, battery age logic |
| `presentation-core/.../components/DeviceIconMapper.kt` | Icon name → Material icon mapping (37 entries) |
| `presentation-core/.../components/HistoryListItem.kt` | Calendar badge and history row |
| `ios-app-swift-ui/Features/Home/DeviceRow.swift` | Current iOS implementation (anti-pattern reference) |
| `docs/design/000_Home_Devices.md` – `070_Edit_Device_Type.md` | Original design mockups |
