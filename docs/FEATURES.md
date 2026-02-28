# Battery Butler - Feature Inventory

This document catalogs all user-facing features in Battery Butler, cross-referenced with [User Journeys](USER_JOURNEYS.md) and test coverage.

## Overview

Battery Butler helps users track battery-powered devices and their replacement history. The app supports Android, iOS, and Desktop platforms.

## Navigation

- **Bottom Tabs**: Devices | Types | History
- **Settings**: Accessible from top app bar
- **FAB**: Context-aware "Add" button on each tab

---

## Device Management

### Home Screen (Devices Tab)
| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Device List | Grouped/sorted list with icons, names, types, locations | J2 | HomeViewModelTest (9) |
| Sort Options | By name, location, battery age, device type (asc/desc) | J2 | HomeViewModelTest |
| Group Options | By none, type, or location (asc/desc) | J2 | HomeViewModelTest |
| Sync Status | Visual indicator showing sync state | J16 | DefaultDeviceRepositoryTest |
| Empty State | Friendly prompt when no devices exist | J2 | MainTabsScreenshotTest |

### Add Device
| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Manual Entry | Name (required), location, device type dropdown | J3 | AddDeviceViewModelTest (7) |
| AI Batch Import | Natural language input to create multiple devices | J14 | BatchAddDevicesUseCaseTest |
| Type Quick Link | "Manage Device Types" button | J3 | — |

### Edit Device
| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Edit Fields | Name, location, device type | J5 | EditDeviceViewModelTest (5) |
| Delete | Confirmation dialog before deletion | J5 | EditDeviceViewModelTest |

### Device Detail
| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Profile Header | Large icon, name, type, location | J4 | DeviceDetailViewModelTest (8) |
| Battery Info | Battery type and quantity cards | J4 | DeviceDetailScreenshotTest |
| Quick Action | "Record Replacement" button | J4, J6 | — |
| Event History | Recent battery events with "View All" link | J4 | DeviceDetailViewModelTest |

---

## Battery Event Tracking

### History Screen
| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Event List | All battery replacements across devices | J10 | HistoryListViewModelTest (9) |
| Event Info | Device name, type, location, relative time | J10 | HistoryListViewModelTest |
| Click to Edit | Tap event to view/edit details | J11 | — |

### Add Battery Event
| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Device Selection | Dropdown with "Add New Device" option | J6 | AddBatteryEventViewModelTest (5) |
| Date Selection | Defaults to today, manual override available | J6 | AddBatteryEventViewModelTest |
| AI Batch Import | Natural language input for multiple events | J14 | BatchAddBatteryEventsUseCaseTest (3) |

### Event Detail
| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Date Editing | Date picker to change event date | J11 | EventDetailViewModelTest (6) |
| Delete | Remove event | J11 | EventDetailViewModelTest |

---

## Device Type Management

### Types Screen
| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Type List | Icon, name, battery specs for each type | J7 | DeviceTypeListViewModelTest (6) |
| Sort Options | By name or battery type (asc/desc) | J7 | DeviceTypeListViewModelTest |
| Group Options | By none or battery type (asc/desc) | J7 | DeviceTypeListViewModelTest |

### Add Device Type
| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Icon Selection | Grid of 40+ Material icons | J8 | AddDeviceTypeViewModelTest (4) |
| Type Details | Name, battery type, quantity (+/- buttons) | J8 | AddDeviceTypeViewModelTest |
| AI Icon Suggest | Auto-suggest icon based on name | J8 | SuggestDeviceIconUseCaseTest (4) |
| AI Batch Import | Natural language input for multiple types | J14 | BatchAddDeviceTypesUseCaseTest (3) |

### Edit Device Type
| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Edit Fields | Name, battery type, quantity, icon | J9 | EditDeviceTypeViewModelTest (5) |
| Delete | Confirmation dialog before deletion | J9 | EditDeviceTypeViewModelTest |

---

## AI Features

### Batch Operations
| Feature | Input Example | Output | Journey | Tests |
|---------|---------------|--------|---------|-------|
| Batch Add Devices | "bedroom alarm clock, kitchen smoke detector" | Creates multiple devices with parsed names/locations | J14 | BatchAddDevicesUseCaseTest |
| Batch Add Types | "smoke alarms with 2 AAA batteries" | Creates types with parsed specs | J14 | BatchAddDeviceTypesUseCaseTest (3) |
| Batch Add Events | "Replaced remote batteries today" | Creates events with inferred dates | J14 | BatchAddBatteryEventsUseCaseTest (3) |
| Icon Suggestion | Type name "Smoke Detector" | Suggests appropriate icon | J8 | SuggestDeviceIconUseCaseTest (4) |

### AI Chat
| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Chat Overlay | Natural language interaction from any tab | J13 | AiChatViewModelTest (7) |
| Device Context | AI receives full inventory context | J13 | BuildAiContextUseCaseTest (5), SendChatMessageUseCaseTest (4) |
| Tool Execution | AI can add devices, types, events via tools | J13 | DeviceToolHandlerTest (17) |

### AI Output Display
- Progress messages with spinner
- Success messages with checkmarks
- Error messages with details
- Color-coded feedback

---

## Settings

| Feature | Description | Journey | Tests |
|---------|-------------|---------|-------|
| Network Mode | Mock (offline), gRPC Local, gRPC AWS | J12 | SettingsViewModelTest (13) |
| Export Data | Save all data as timestamped JSON file | J15 | ExportDataUseCaseTest (8) |
| App Version | Platform-specific version display | J12 | SettingsViewModelTest |

---

## Sync & Network

| State | UI Feedback | Journey | Tests |
|-------|-------------|---------|-------|
| Idle | No indicator | J16 | DefaultDeviceRepositoryTest |
| Syncing | Spinner with "Syncing..." text | J16 | DefaultDeviceRepositoryTest |
| Success | Auto-dismisses after 2 seconds | J16 | DefaultDeviceRepositoryTest |
| Failed | Error message in snackbar | J16 | DefaultDeviceRepositoryTest |

---

## Data Export

- **Format**: JSON
- **Contents**: All devices, device types, battery events
- **Filename**: `Battery_Butler_Backup_YYYYMMDD_HHMMSS.json`

---

## Platform Support

| Platform | Status |
|----------|--------|
| Android | Full support |
| iOS (Compose) | Full support |
| iOS (SwiftUI) | Full support |
| Desktop | Full support |

---

## Icon Library

40+ icons available including:
- `detector_smoke` - Smoke detectors
- `thermostat` - Thermostats
- `lightbulb` - Smart lights
- `videocam` - Cameras
- `lock` - Smart locks
- `speaker` - Smart speakers
- `settings_remote` - Remote controls
- And many more...

---

## Feature Counts

| Category | Count |
|----------|-------|
| Main Screens | 8 |
| Forms | 5 |
| AI Features | 4 |
| Sort/Group Options | 8 |
| Total Features | 60+ |
