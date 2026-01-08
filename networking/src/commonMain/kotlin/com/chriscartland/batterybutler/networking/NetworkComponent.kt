package com.chriscartland.batterybutler.networking

import com.squareup.wire.GrpcClient

expect class NetworkComponent {
    val grpcClient: GrpcClient
}
