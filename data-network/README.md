# Data Network Module

The data-network module provides remote synchronization using gRPC.

## Overview

- **gRPC with Wire** - Protocol buffers for efficient serialization
- **Bidirectional Streaming** - Real-time sync updates
- **Multi-mode** - Mock, local gRPC, and AWS gRPC
- **Kotlin Multiplatform** - Android, iOS, Desktop

## Components

### RemoteDataSource Interface

```kotlin
interface RemoteDataSource {
    val state: StateFlow<RemoteDataSourceState>
    fun subscribe(): Flow<RemoteUpdate>
    suspend fun push(update: RemoteUpdate): Boolean
}
```

States: `NotStarted`, `InvalidConfiguration`, `Subscribed`

### GrpcSyncDataSource

Production gRPC implementation:
- Bidirectional streaming for subscribe
- Unary RPC for push
- Wire-generated client

### MockRemoteDataSource

Development implementation:
- In-memory state with demo data
- Full snapshot on subscribe
- Simulates remote sync

### DelegatingRemoteDataSource

Routes to appropriate data source based on `NetworkMode`:
- `Mock` → MockRemoteDataSource
- `GrpcLocal` / `GrpcAws` → GrpcSyncDataSource

### SyncMapper

Bidirectional mapping between protobuf and domain models.

## Platform Implementations

### Android/Desktop
Uses OkHttp3 with HTTP/2.

### iOS
Custom Ktor-based gRPC implementation with Darwin engine.

## gRPC Service

```protobuf
service SyncService {
  rpc Subscribe(google.protobuf.Empty) returns (stream SyncUpdate);
  rpc PushUpdate(SyncUpdate) returns (PushResponse);
}

message SyncUpdate {
  bool is_full_snapshot = 1;
  repeated ProtoDeviceType device_types = 2;
  repeated ProtoDevice devices = 3;
  repeated ProtoBatteryEvent events = 4;
}
```

## Testing

```bash
./gradlew :data-network:allTests
```
