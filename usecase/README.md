# UseCase Module

The usecase module orchestrates business logic between presentation and data layers. It contains use case classes that coordinate repository operations into coherent user workflows.

## Overview

- **25 use cases** covering all app operations
- **Kotlin Multiplatform** - Android, iOS, JVM
- **kotlin-inject** for dependency injection
- **AI-powered** batch operations for smart data entry

## Use Cases

### Device Operations (5)
| Use Case | Description |
|----------|-------------|
| `AddDeviceUseCase` | Create a new device |
| `UpdateDeviceUseCase` | Modify an existing device |
| `DeleteDeviceUseCase` | Remove a device |
| `GetDevicesUseCase` | Retrieve all devices as Flow |
| `GetDeviceDetailUseCase` | Get specific device by ID |

### Device Type Operations (4)
| Use Case | Description |
|----------|-------------|
| `AddDeviceTypeUseCase` | Create a new device type |
| `UpdateDeviceTypeUseCase` | Modify device type |
| `DeleteDeviceTypeUseCase` | Remove device type |
| `GetDeviceTypesUseCase` | Retrieve all device types |

### Battery Event Operations (5)
| Use Case | Description |
|----------|-------------|
| `AddBatteryEventUseCase` | Record battery replacement (cascading update) |
| `UpdateBatteryEventUseCase` | Modify battery event |
| `DeleteBatteryEventUseCase` | Remove battery event |
| `GetBatteryEventsUseCase` | Retrieve events (all or per device) |
| `GetEventDetailUseCase` | Get specific event by ID |

### AI Batch Operations (3)
| Use Case | Description |
|----------|-------------|
| `BatchAddDevicesUseCase` | Parse text and create devices via AI |
| `BatchAddDeviceTypesUseCase` | Parse text and create device types via AI |
| `BatchAddBatteryEventsUseCase` | Parse text and record events via AI |

### Sync & Network (3)
| Use Case | Description |
|----------|-------------|
| `GetSyncStatusUseCase` | Observe sync status |
| `DismissSyncStatusUseCase` | Clear sync status |
| `SetNetworkModeUseCase` | Configure network mode |

### Utilities (5)
| Use Case | Description |
|----------|-------------|
| `GetAppVersionUseCase` | Get app version info |
| `ExportDataUseCase` | Export data as JSON |
| `SuggestDeviceIconUseCase` | AI icon recommendations |
| `UpdateDeviceLastReplacedUseCase` | Sync device with latest event |

## Pattern

All use cases follow a consistent pattern:

```kotlin
@Inject
class AddDeviceUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(device: Device) =
        deviceRepository.addDevice(device)
}
```

- `@Inject` annotation for DI
- `operator fun invoke()` for clean calling: `useCase(device)`
- Suspend functions for async operations
- Flow returns for reactive data

## Dependencies

```
UseCase Module
├── domain (repository interfaces, models)
├── presentation-model (DTOs)
├── ai (LLM integration for batch ops)
├── kotlinx-coroutines
└── kotlin-inject
```

## Testing

```bash
./gradlew :usecase:allTests
```

Tests use `FakeDeviceRepository` and `TestDevices` helper for test data.
