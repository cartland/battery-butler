package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class ExportDataUseCaseTest {
    @Test
    fun `export empty repository returns valid JSON structure`() =
        runTest {
            val repository = TestExportRepository()
            val useCase = ExportDataUseCase(repository)

            val result = useCase()

            // Verify JSON contains required fields
            // Note: formatVersion and type are default values and may not be encoded
            // depending on kotlinx.serialization encodeDefaults setting
            assertTrue(result.isNotEmpty(), "Result should not be empty")
            assertTrue(result.contains("data"), "Missing data key")
            assertTrue(result.contains("devices"), "Missing devices key")
            assertTrue(result.contains("deviceTypes"), "Missing deviceTypes key")
            assertTrue(result.contains("events"), "Missing events key")
        }

    @Test
    fun `export includes device data`() =
        runTest {
            val repository = TestExportRepository()
            repository.devices.add(
                Device(
                    id = "device-1",
                    name = "Smoke Detector",
                    typeId = "type-1",
                    batteryLastReplaced = Instant.parse("2024-01-15T10:30:00Z"),
                    lastUpdated = Instant.parse("2024-01-15T10:30:00Z"),
                    location = "Kitchen",
                ),
            )
            val useCase = ExportDataUseCase(repository)

            val result = useCase()

            assertTrue(result.contains("\"device-1\""))
            assertTrue(result.contains("\"Smoke Detector\""))
            assertTrue(result.contains("\"type-1\""))
            assertTrue(result.contains("\"Kitchen\""))
        }

    @Test
    fun `export includes device type data`() =
        runTest {
            val repository = TestExportRepository()
            repository.deviceTypes.add(
                DeviceType(
                    id = "type-1",
                    name = "Smoke Detector",
                    defaultIcon = "detector_smoke",
                    batteryType = "9V",
                    batteryQuantity = 1,
                ),
            )
            val useCase = ExportDataUseCase(repository)

            val result = useCase()

            assertTrue(result.contains("\"type-1\""))
            assertTrue(result.contains("\"Smoke Detector\""))
            assertTrue(result.contains("\"9V\""))
            assertTrue(result.contains("\"batteryQuantity\""))
            assertTrue(result.contains("\"detector_smoke\""))
        }

    @Test
    fun `export includes battery event data`() =
        runTest {
            val repository = TestExportRepository()
            repository.events.add(
                BatteryEvent(
                    id = "event-1",
                    deviceId = "device-1",
                    date = Instant.parse("2024-01-20T14:00:00Z"),
                    batteryType = "AA",
                    notes = "Replaced during maintenance",
                ),
            )
            val useCase = ExportDataUseCase(repository)

            val result = useCase()

            assertTrue(result.contains("\"event-1\""))
            assertTrue(result.contains("\"device-1\""))
            assertTrue(result.contains("\"AA\""))
            assertTrue(result.contains("\"Replaced during maintenance\""))
        }

    @Test
    fun `export handles null optional fields`() =
        runTest {
            val repository = TestExportRepository()
            repository.devices.add(
                Device(
                    id = "device-1",
                    name = "Test Device",
                    typeId = "type-1",
                    batteryLastReplaced = Instant.DISTANT_PAST,
                    lastUpdated = Instant.DISTANT_PAST,
                    location = null,
                    imagePath = null,
                ),
            )
            repository.deviceTypes.add(
                DeviceType(
                    id = "type-1",
                    name = "Generic",
                    defaultIcon = null,
                ),
            )
            repository.events.add(
                BatteryEvent(
                    id = "event-1",
                    deviceId = "device-1",
                    date = Instant.DISTANT_PAST,
                    batteryType = null,
                    notes = null,
                ),
            )
            val useCase = ExportDataUseCase(repository)

            val result = useCase()

            // Should contain null values for optional fields (check for null keyword)
            assertTrue(result.contains("\"location\""))
            assertTrue(result.contains("\"defaultIcon\""))
            assertTrue(result.contains("null"))
        }

    @Test
    fun `export handles multiple items`() =
        runTest {
            val repository = TestExportRepository()
            repository.devices.add(
                Device(
                    id = "device-1",
                    name = "Device 1",
                    typeId = "type-1",
                    batteryLastReplaced = Instant.DISTANT_PAST,
                    lastUpdated = Instant.DISTANT_PAST,
                ),
            )
            repository.devices.add(
                Device(
                    id = "device-2",
                    name = "Device 2",
                    typeId = "type-1",
                    batteryLastReplaced = Instant.DISTANT_PAST,
                    lastUpdated = Instant.DISTANT_PAST,
                ),
            )
            repository.deviceTypes.add(DeviceType(id = "type-1", name = "Type 1"))
            repository.deviceTypes.add(DeviceType(id = "type-2", name = "Type 2"))
            val useCase = ExportDataUseCase(repository)

            val result = useCase()

            assertTrue(result.contains("\"device-1\""))
            assertTrue(result.contains("\"device-2\""))
            assertTrue(result.contains("\"type-1\""))
            assertTrue(result.contains("\"type-2\""))
        }

    @Test
    fun `export produces valid JSON format`() =
        runTest {
            val repository = TestExportRepository()
            repository.devices.add(
                Device(
                    id = "d1",
                    name = "Test",
                    typeId = "t1",
                    batteryLastReplaced = Instant.DISTANT_PAST,
                    lastUpdated = Instant.DISTANT_PAST,
                ),
            )
            val useCase = ExportDataUseCase(repository)

            val result = useCase()

            // Verify it starts and ends like valid JSON
            assertTrue(result.trimStart().startsWith("{"))
            assertTrue(result.trimEnd().endsWith("}"))
            // Verify it has proper JSON structure markers
            assertTrue(result.contains("\"data\""))
        }

    @Test
    fun `export uses pretty print format`() =
        runTest {
            val repository = TestExportRepository()
            repository.devices.add(
                Device(
                    id = "d1",
                    name = "Test",
                    typeId = "t1",
                    batteryLastReplaced = Instant.DISTANT_PAST,
                    lastUpdated = Instant.DISTANT_PAST,
                ),
            )
            val useCase = ExportDataUseCase(repository)

            val result = useCase()

            // Pretty print should include newlines and indentation
            assertTrue(result.contains("\n"))
            assertTrue(result.contains("    ")) // 4-space indentation
        }
}

// Test-specific repository implementation for export tests
@OptIn(ExperimentalTime::class)
private class TestExportRepository : DeviceRepository {
    val devices = mutableListOf<Device>()
    val deviceTypes = mutableListOf<DeviceType>()
    val events = mutableListOf<BatteryEvent>()

    override val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)

    override fun dismissSyncStatus() {}

    override fun getAllDevices(): Flow<List<Device>> = flowOf(devices.toList())

    override fun getDeviceById(id: String): Flow<Device?> = flowOf(devices.find { it.id == id })

    override suspend fun addDevice(device: Device) {
        devices.add(device)
    }

    override suspend fun updateDevice(device: Device) {
        devices.removeAll { it.id == device.id }
        devices.add(device)
    }

    override suspend fun deleteDevice(id: String) {
        devices.removeAll { it.id == id }
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = flowOf(deviceTypes.toList())

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = flowOf(deviceTypes.find { it.id == id })

    override suspend fun addDeviceType(type: DeviceType) {
        deviceTypes.add(type)
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        deviceTypes.removeAll { it.id == type.id }
        deviceTypes.add(type)
    }

    override suspend fun deleteDeviceType(id: String) {
        deviceTypes.removeAll { it.id == id }
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = flowOf(events.filter { it.deviceId == deviceId })

    override fun getAllEvents(): Flow<List<BatteryEvent>> = flowOf(events.toList())

    override fun getEventById(id: String): Flow<BatteryEvent?> = flowOf(events.find { it.id == id })

    override suspend fun addEvent(event: BatteryEvent) {
        events.add(event)
    }

    override suspend fun updateEvent(event: BatteryEvent) {
        events.removeAll { it.id == event.id }
        events.add(event)
    }

    override suspend fun deleteEvent(id: String) {
        events.removeAll { it.id == id }
    }
}
