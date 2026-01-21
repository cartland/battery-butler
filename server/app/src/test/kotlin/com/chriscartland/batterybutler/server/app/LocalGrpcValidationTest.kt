package com.chriscartland.batterybutler.server.app

import com.chriscartland.batterybutler.proto.SyncServiceGrpcKt
import com.chriscartland.batterybutler.proto.SyncUpdate
import io.grpc.Server
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class LocalGrpcValidationTest {
    private lateinit var server: Server
    private lateinit var channel: io.grpc.ManagedChannel
    private lateinit var client: SyncServiceGrpcKt.SyncServiceCoroutineStub

    @Before
    fun setUp() {
        // Use InProcessServer for robust, non-networked testing
        val serverName = io.grpc.inprocess.InProcessServerBuilder
            .generateName()
        val repository = com.chriscartland.batterybutler.server.data.repository
            .InMemoryDeviceRepository()

        server = io.grpc.inprocess.InProcessServerBuilder
            .forName(serverName)
            .directExecutor()
            .addService(
                com.chriscartland.batterybutler.server.app
                    .BatteryService(),
            ).addService(
                com.chriscartland.batterybutler.server.app
                    .SyncService(repository),
            ).build()
            .start()

        channel = io.grpc.inprocess.InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build()

        client = SyncServiceGrpcKt.SyncServiceCoroutineStub(channel)
    }

    @After
    fun tearDown() {
        channel.shutdown()
        server.shutdown()
    }

    @Test
    fun `test initial snapshot parity`() =
        runBlocking {
            // MockNetwork behavior: Subscribe returns initial snapshot immediately

            val updates = client.subscribe(
                com.google.protobuf.Empty
                    .getDefaultInstance(),
            )

            // Take 1 to verify snapshot
            val firstUpdate = updates.take(1).toList().first()

            assertTrue(firstUpdate.isFullSnapshot, "Should be full snapshot")
            assertTrue(firstUpdate.deviceTypesList.isNotEmpty(), "Should have device types")
            assertTrue(firstUpdate.devicesList.isNotEmpty(), "Should have devices")
        }

    @Test
    fun `test push update and receive`() =
        runBlocking {
            val pushResult = client.pushUpdate(
                SyncUpdate
                    .newBuilder()
                    .setIsFullSnapshot(true)
                    .build(),
            )

            assertTrue(pushResult.success)
        }
}
