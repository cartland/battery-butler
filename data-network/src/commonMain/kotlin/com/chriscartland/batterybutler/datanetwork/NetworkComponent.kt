package com.chriscartland.batterybutler.datanetwork

import com.squareup.wire.GrpcClient

expect class NetworkComponent {
    fun createGrpcClient(): GrpcClient
}
