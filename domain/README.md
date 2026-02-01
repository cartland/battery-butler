# Domain Module

The domain module defines the core business entities, models, and repository contracts for Battery Butler. It serves as the central abstraction layer between presentation, use cases, and data layers.

## Overview

- **Pure Kotlin** - Platform-agnostic domain model
- **Kotlin Multiplatform** - JVM, iOS (arm64, x64, simulatorArm64)
- **Zero external runtime dependencies** - Only Kotlin stdlib and core libraries
- **Reactive first** - All data access uses Flow for reactive streams

## Models and Entities

### Primary Entities

| Entity | Description |
|--------|-------------|
| `Device` | Battery-powered device to track (id, name, typeId, batteryLastReplaced, location) |
| `DeviceType` | Category of devices with default battery config (Smoke Alarm, Thermostat, etc.) |
| `BatteryEvent` | History record for battery replacements |

### Configuration Models

| Model | Description |
|-------|-------------|
| `NetworkMode` | Sealed interface: Mock, GrpcLocal, GrpcAws |
| `SyncStatus` | Sealed interface: Idle, Syncing, Success, Failed |
| `AppVersion` | Platform-specific version info (Android, iOS, Desktop) |

### Input DTOs

| DTO | Description |
|-----|-------------|
| `DeviceInput` | Data transfer object for creating/updating devices |
| `DeviceTypeInput` | Data transfer object for device type operations |

## Repository Interfaces

### DeviceRepository

Primary data access abstraction for devices, device types, and battery events.

```kotlin
interface DeviceRepository {
    val syncStatus: StateFlow<SyncStatus>

    // Devices
    fun getAllDevices(): Flow<List<Device>>
    fun getDeviceById(id: String): Flow<Device?>
    suspend fun addDevice(device: Device)
    suspend fun updateDevice(device: Device)
    suspend fun deleteDevice(id: String)

    // Device Types
    fun getAllDeviceTypes(): Flow<List<DeviceType>>
    suspend fun addDeviceType(type: DeviceType)
    // ... more operations

    // Battery Events
    fun getAllEvents(): Flow<List<BatteryEvent>>
    suspend fun addEvent(event: BatteryEvent)
    // ... more operations
}
```

### NetworkModeRepository

Manages network connectivity configuration.

### AppInfoRepository

Provides platform-specific app version information.

## Architecture

```
┌─────────────────────────────────────────────┐
│              Presentation Layer             │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│               UseCase Layer                 │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         Domain Layer (this module)          │
│   Models │ Repository Interfaces │ Types    │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│               Data Layer                    │
│   data │ data-local │ data-network          │
└─────────────────────────────────────────────┘
```

## Dependencies

This module is **depended upon** by:
- `data` - Implements repository interfaces
- `usecase` - Uses models and repository contracts
- `presentation-*` - Observes flows for UI updates
- `fixtures` - Provides test data

## Testing

Run domain tests:
```bash
./gradlew :domain:allTests
```
