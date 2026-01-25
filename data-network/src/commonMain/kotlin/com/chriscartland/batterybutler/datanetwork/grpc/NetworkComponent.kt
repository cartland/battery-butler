package com.chriscartland.batterybutler.datanetwork.grpc

import com.squareup.wire.GrpcClient

expect class NetworkComponent {
    fun createGrpcClient(): GrpcClient
}
