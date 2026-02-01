# Data Module

The data module implements repository interfaces from the domain layer, coordinating between local storage and remote sync.

## Overview

- **Local-first architecture** - All reads from local, writes trigger async sync
- **Bidirectional sync** - Listens for remote updates, pushes local changes
- **Kotlin Multiplatform** - Android, iOS, JVM

## Components

### DefaultDeviceRepository

Main repository implementation coordinating local and remote data sources.

```kotlin
@Inject
class DefaultDeviceRepository(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
) : DeviceRepository {
    // Local-first reads
    override fun getAllDevices() = localDataSource.getAllDevices()

    // Write + sync
    override suspend fun addDevice(device: Device) {
        localDataSource.addDevice(device)
        push() // Async remote sync
    }
}
```

Features:
- `StateFlow<SyncStatus>` for UI sync indicators
- Batch operations for efficient remote updates
- Error handling with graceful fallback

### InMemoryNetworkModeRepository

Manages network mode switching between Mock, GrpcLocal, and GrpcAws.

### StaticAppInfoRepository

Provides app version information.

## Architecture

```
DefaultDeviceRepository
  ├── LocalDataSource (from data-local)
  │   └── Room database
  └── RemoteDataSource (from data-network)
      └── gRPC or Mock
```

## Dependencies

```
data
├── domain (interfaces, models)
├── data-local (Room persistence)
├── data-network (gRPC sync)
└── kotlin-inject
```

## Testing

```bash
./gradlew :data:allTests
```

Uses `FakeLocalDataSource` and `FakeRemoteDataSource` for isolation.
