package com.chriscartland.batterybutler.server.app

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.server.app.db.DatabaseFactory
import com.chriscartland.batterybutler.server.app.repository.PostgresDeviceRepository
import io.grpc.Server
import io.grpc.ServerBuilder
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    DatabaseFactory.init()
    val grpcServer = startGrpcServer()
    startHttpServer()

    // Shutdown hook for gRPC
    Runtime.getRuntime().addShutdownHook(
        Thread {
            println("Shutting down gRPC server...")
            grpcServer.shutdown()
            println("gRPC server shut down.")
        },
    )
}

fun startGrpcServer(port: Int = 50051): Server {
    val repository = PostgresDeviceRepository()
    val grpcServer = ServerBuilder
        .forPort(port)
        .addService(BatteryService())
        .addService(SyncService(repository).also { Logger.d("BatteryButlerApp") { "SyncService Registered" } })
        .build()
    grpcServer.start()
    println("gRPC server started on port $port")
    return grpcServer
}

fun startHttpServer() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    routing {
        get("/") {
            call.respondText("Ktor: Battery Butler Server Running")
        }
    }
}
