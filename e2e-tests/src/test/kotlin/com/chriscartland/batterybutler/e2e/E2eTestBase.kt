package com.chriscartland.batterybutler.e2e

import com.chriscartland.batterybutler.proto.GrpcBatteryServiceClient
import com.chriscartland.batterybutler.proto.GrpcSyncServiceClient
import com.chriscartland.batterybutler.proto.ProtoDevice
import com.chriscartland.batterybutler.proto.ProtoDeviceType
import com.chriscartland.batterybutler.proto.SyncUpdate
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.junit.After
import org.junit.Before
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Base class for E2E tests. Configures Wire GrpcClient targeting a real server.
 *
 * Server URL is read from system property `e2e.server.url`.
 * Defaults to `http://localhost:50051` (local server).
 *
 * All test data uses IDs prefixed with `e2e-test-{uuid}` for isolation.
 */
abstract class E2eTestBase {
    protected lateinit var batteryClient: GrpcBatteryServiceClient
    protected lateinit var syncClient: GrpcSyncServiceClient

    private val testRunId = UUID.randomUUID().toString().take(8)
    private val createdIds = mutableListOf<TestDataIds>()

    private data class TestDataIds(
        val deviceTypeIds: List<String>,
        val deviceIds: List<String>,
        val eventIds: List<String>,
    )

    @Before
    fun setUpClients() {
        val serverUrl = System.getProperty("e2e.server.url") ?: "http://localhost:50051"

        val okHttpClient = OkHttpClient
            .Builder()
            .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()

        val grpcClient = GrpcClient
            .Builder()
            .client(okHttpClient)
            .baseUrl(serverUrl)
            .build()

        batteryClient = GrpcBatteryServiceClient(grpcClient)
        syncClient = GrpcSyncServiceClient(grpcClient)
    }

    @After
    fun cleanUpTestData() =
        runBlocking {
            // Push deletes for all test data created during this test
            for (ids in createdIds) {
                try {
                    val deleteUpdate = SyncUpdate(
                        is_full_snapshot = false,
                        deleted_device_type_ids = ids.deviceTypeIds,
                        deleted_device_ids = ids.deviceIds,
                        deleted_event_ids = ids.eventIds,
                    )
                    syncClient.PushUpdate().execute(deleteUpdate)
                } catch (_: Exception) {
                    // Best-effort cleanup
                }
            }
        }

    /** Generate a unique test ID for isolation. */
    protected fun testId(suffix: String): String = "e2e-test-$testRunId-$suffix"

    /** Create a test ProtoDeviceType. ID is tracked for cleanup. */
    protected fun testDeviceType(
        id: String = testId("type"),
        name: String = "E2E Test Device Type",
        batteryType: String = "AA",
        batteryQuantity: Int = 2,
    ): ProtoDeviceType {
        trackIds(deviceTypeIds = listOf(id))
        return ProtoDeviceType(
            id = id,
            name = name,
            default_icon = "test",
            battery_type = batteryType,
            battery_quantity = batteryQuantity,
        )
    }

    /** Create a test ProtoDevice. ID is tracked for cleanup. */
    protected fun testDevice(
        id: String = testId("device"),
        typeId: String = testId("type"),
        name: String = "E2E Test Device",
    ): ProtoDevice {
        trackIds(deviceIds = listOf(id))
        return ProtoDevice(
            id = id,
            name = name,
            type_id = typeId,
            location = "E2E Test",
            battery_last_replaced_timestamp_ms = System.currentTimeMillis(),
            last_updated_timestamp_ms = System.currentTimeMillis(),
            image_path = "",
        )
    }

    private fun trackIds(
        deviceTypeIds: List<String> = emptyList(),
        deviceIds: List<String> = emptyList(),
        eventIds: List<String> = emptyList(),
    ) {
        createdIds.add(TestDataIds(deviceTypeIds, deviceIds, eventIds))
    }
}
