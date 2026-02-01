# Battery Butler - Feature Inventory

This document catalogs all user-facing features in Battery Butler.

## Overview

Battery Butler helps users track battery-powered devices and their replacement history. The app supports Android, iOS, and Desktop platforms.

## Navigation

- **Bottom Tabs**: Devices | Types | History
- **Settings**: Accessible from top app bar
- **FAB**: Context-aware "Add" button on each tab

---

## Device Management

### Home Screen (Devices Tab)
| Feature | Description |
|---------|-------------|
| Device List | Grouped/sorted list with icons, names, types, locations |
| Sort Options | By name, location, battery age, device type (asc/desc) |
| Group Options | By none, type, or location (asc/desc) |
| Sync Status | Visual indicator showing sync state |
| Empty State | Friendly prompt when no devices exist |

### Add Device
| Feature | Description |
|---------|-------------|
| Manual Entry | Name (required), location, device type dropdown |
| AI Batch Import | Natural language input to create multiple devices |
| Type Quick Link | "Manage Device Types" button |

### Edit Device
| Feature | Description |
|---------|-------------|
| Edit Fields | Name, location, device type |
| Delete | Confirmation dialog before deletion |

### Device Detail
| Feature | Description |
|---------|-------------|
| Profile Header | Large icon, name, type, location |
| Battery Info | Battery type and quantity cards |
| Quick Action | "Record Replacement" button |
| Event History | Recent battery events with "View All" link |

---

## Battery Event Tracking

### History Screen
| Feature | Description |
|---------|-------------|
| Event List | All battery replacements across devices |
| Event Info | Device name, type, location, relative time |
| Click to Edit | Tap event to view/edit details |

### Add Battery Event
| Feature | Description |
|---------|-------------|
| Device Selection | Dropdown with "Add New Device" option |
| Date Selection | Defaults to today, manual override available |
| AI Batch Import | Natural language input for multiple events |

### Event Detail
| Feature | Description |
|---------|-------------|
| Date Editing | Date picker to change event date |
| Delete | Remove event |

---

## Device Type Management

### Types Screen
| Feature | Description |
|---------|-------------|
| Type List | Icon, name, battery specs for each type |
| Sort Options | By name or battery type (asc/desc) |
| Group Options | By none or battery type (asc/desc) |

### Add Device Type
| Feature | Description |
|---------|-------------|
| Icon Selection | Grid of 40+ Material icons |
| Type Details | Name, battery type, quantity (+/- buttons) |
| AI Icon Suggest | Auto-suggest icon based on name |
| AI Batch Import | Natural language input for multiple types |

### Edit Device Type
| Feature | Description |
|---------|-------------|
| Edit Fields | Name, battery type, quantity, icon |
| Delete | Confirmation dialog before deletion |

---

## AI Features

### Batch Operations
| Feature | Input Example | Output |
|---------|---------------|--------|
| Batch Add Devices | "bedroom alarm clock, kitchen smoke detector" | Creates multiple devices with parsed names/locations |
| Batch Add Types | "smoke alarms with 2 AAA batteries" | Creates types with parsed specs |
| Batch Add Events | "Replaced remote batteries today" | Creates events with inferred dates |
| Icon Suggestion | Type name "Smoke Detector" | Suggests appropriate icon |

### AI Output Display
- Progress messages with spinner
- Success messages with checkmarks
- Error messages with details
- Color-coded feedback

---

## Settings

| Feature | Description |
|---------|-------------|
| Network Mode | Mock (offline), gRPC Local, gRPC AWS |
| Export Data | Save all data as timestamped JSON file |
| App Version | Platform-specific version display |

---

## Sync & Network

| State | UI Feedback |
|-------|-------------|
| Idle | No indicator |
| Syncing | Spinner with "Syncing..." text |
| Success | Auto-dismisses after 2 seconds |
| Failed | Error message in snackbar |

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
