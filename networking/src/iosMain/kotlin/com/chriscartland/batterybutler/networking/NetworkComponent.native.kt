package com.chriscartland.batterybutler.networking

import com.squareup.wire.GrpcClient

actual class NetworkComponent {
    actual val grpcClient: GrpcClient by lazy {
        throw NotImplementedError("Native gRPC client not yet implemented. Use JVM/Android for now.")
    }
}

