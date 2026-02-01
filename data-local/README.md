# Data Local Module

The data-local module provides local persistence using Room database with SQLite.

## Overview

- **Room Multiplatform** - SQLite on Android, iOS, and Desktop
- **Reactive Flows** - All queries return Flow for real-time updates
- **Dual Database** - Production and development databases
- **Schema Migrations** - Version-tracked database migrations

## Components

### AppDatabase

Room database with 3 tables: `devices`, `device_types`, `battery_events`.

```kotlin
@Database(
    entities = [DeviceEntity::class, DeviceTypeEntity::class, BatteryEventEntity::class],
    version = 5
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
}
```

### DeviceDao

Single DAO with operations for all entities:
- Flow-based queries for reactive updates
- Suspend functions for mutations
- Batch insert/update support

### DynamicDatabaseProvider

Manages dual databases based on network mode:
- `battery-butler.db` - Production data
- `battery-butler-dev.db` - Development/mock data

### RoomLocalDataSource

Implements `LocalDataSource` interface, wrapping DAO with entity mapping.

## Schema

### devices
| Column | Type | Description |
|--------|------|-------------|
| id | TEXT PK | Unique identifier |
| name | TEXT | Device name |
| typeId | TEXT | Foreign key to device_types |
| batteryLastReplaced | INTEGER | Epoch milliseconds |
| lastUpdated | INTEGER | Epoch milliseconds |
| location | TEXT? | Optional location |
| imagePath | TEXT? | Optional image path |

### device_types
| Column | Type | Description |
|--------|------|-------------|
| id | TEXT PK | Unique identifier |
| name | TEXT | Type name |
| defaultIcon | TEXT? | Material icon name |
| batteryType | TEXT | e.g., "AA", "9V" |
| batteryQuantity | INTEGER | Number of batteries |

### battery_events
| Column | Type | Description |
|--------|------|-------------|
| id | TEXT PK | Unique identifier |
| deviceId | TEXT | Foreign key to devices |
| date | INTEGER | Epoch milliseconds |
| batteryType | TEXT? | Optional battery type |
| notes | TEXT? | Optional notes |

## Platform Implementations

### Android
Uses `Context.getDatabasePath()` for app database directory.

### iOS
Uses `NSFileManager` Documents directory.

### Desktop
Uses system temp directory (`java.io.tmpdir`).

## Testing

```bash
./gradlew :data-local:allTests
```
