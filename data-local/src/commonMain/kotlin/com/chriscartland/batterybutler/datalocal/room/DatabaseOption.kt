package com.chriscartland.batterybutler.datalocal.room

enum class DatabaseOption(
    val fileName: String,
) {
    None("battery-butler.db"),
    Mock("battery-butler-mock.db"),
    GrpcLocal("battery-butler-grpc-local.db"),
    GrpcAws("battery-butler-grpc-aws.db"),
    GrpcDev("battery-butler-grpc-dev.db"),
}
