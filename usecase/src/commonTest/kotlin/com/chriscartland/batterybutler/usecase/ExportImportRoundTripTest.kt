package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.SyncOutcome
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class ExportImportRoundTripTest {
    private val testDispatcherProvider = object : DispatcherProvider {
        private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val default: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    @Test
    fun `export then import produces identical data`() =
        runTest {
            // Set up source repository with diverse data
            val sourceRepo = RoundTripTestRepository()
            sourceRepo.deviceTypes.add(
                DeviceType(
                    id = "type-smoke",
                    name = "Smoke Detector",
                    batteryType = "9V",
                    batteryQuantity = 1,
                    defaultIcon = "detector_smoke",
                ),
            )
            sourceRepo.deviceTypes.add(
                DeviceType(
                    id = "type-remote",
                    name = "Remote Control",
                    batteryType = "AAA",
                    batteryQuantity = 2,
                    defaultIcon = null,
                ),
            )
            sourceRepo.devices.add(
                Device(
                    id = "device-kitchen",
                    name = "Kitchen Smoke Alarm",
                    typeId = "type-smoke",
                    batteryLastReplaced = Instant.parse("2024-03-15T10:30:00Z"),
                    lastUpdated = Instant.parse("2024-03-15T10:30:00Z"),
                    location = "Kitchen",
                ),
            )
            sourceRepo.devices.add(
                Device(
                    id = "device-tv",
                    name = "TV Remote",
                    typeId = "type-remote",
                    batteryLastReplaced = Instant.parse("2024-01-01T00:00:00Z"),
                    lastUpdated = Instant.parse("2024-01-01T00:00:00Z"),
                    location = null,
                ),
            )
            sourceRepo.events.add(
                BatteryEvent(
                    id = "event-1",
                    deviceId = "device-kitchen",
                    date = Instant.parse("2024-03-15T10:30:00Z"),
                    batteryType = "9V",
                    notes = "Replaced after low battery warning",
                ),
            )
            sourceRepo.events.add(
                BatteryEvent(
                    id = "event-2",
                    deviceId = "device-tv",
                    date = Instant.parse("2024-01-01T00:00:00Z"),
                    batteryType = null,
                    notes = null,
                ),
            )

            // Export
            val exportUseCase = ExportDataUseCase(sourceRepo, testDispatcherProvider)
            val jsonString = exportUseCase()

            // Import into a fresh repository
            val targetRepo = RoundTripTestRepository()
            val importUseCase = ImportDataUseCase(targetRepo, testDispatcherProvider)
            val result = importUseCase(jsonString)

            // Verify import succeeded
            assertTrue(result is Result.Success)
            val importResult = (result as Result.Success).data
            assertEquals(2, importResult.deviceTypesImported)
            assertEquals(2, importResult.devicesImported)
            assertEquals(2, importResult.eventsImported)
            assertTrue(importResult.errors.isEmpty())

            // Verify device types match
            val sourceTypes = sourceRepo.deviceTypes.sortedBy { it.id }
            val targetTypes = targetRepo.deviceTypes.sortedBy { it.id }
            assertEquals(sourceTypes.size, targetTypes.size)
            for (i in sourceTypes.indices) {
                assertEquals(sourceTypes[i].id, targetTypes[i].id)
                assertEquals(sourceTypes[i].name, targetTypes[i].name)
                assertEquals(sourceTypes[i].batteryType, targetTypes[i].batteryType)
                assertEquals(sourceTypes[i].batteryQuantity, targetTypes[i].batteryQuantity)
                assertEquals(sourceTypes[i].defaultIcon, targetTypes[i].defaultIcon)
            }

            // Verify devices match (excluding lastUpdated which is set to now() on import)
            val sourceDevices = sourceRepo.devices.sortedBy { it.id }
            val targetDevices = targetRepo.devices.sortedBy { it.id }
            assertEquals(sourceDevices.size, targetDevices.size)
            for (i in sourceDevices.indices) {
                assertEquals(sourceDevices[i].id, targetDevices[i].id)
                assertEquals(sourceDevices[i].name, targetDevices[i].name)
                assertEquals(sourceDevices[i].typeId, targetDevices[i].typeId)
                assertEquals(sourceDevices[i].batteryLastReplaced, targetDevices[i].batteryLastReplaced)
                assertEquals(sourceDevices[i].location, targetDevices[i].location)
            }

            // Verify events match
            val sourceEvents = sourceRepo.events.sortedBy { it.id }
            val targetEvents = targetRepo.events.sortedBy { it.id }
            assertEquals(sourceEvents.size, targetEvents.size)
            for (i in sourceEvents.indices) {
                assertEquals(sourceEvents[i].id, targetEvents[i].id)
                assertEquals(sourceEvents[i].deviceId, targetEvents[i].deviceId)
                assertEquals(sourceEvents[i].date, targetEvents[i].date)
                assertEquals(sourceEvents[i].batteryType, targetEvents[i].batteryType)
                assertEquals(sourceEvents[i].notes, targetEvents[i].notes)
            }
        }
}

@OptIn(ExperimentalTime::class)
private class RoundTripTestRepository : DeviceRepository {
    val devices = mutableListOf<Device>()
    val deviceTypes = mutableListOf<DeviceType>()
    val events = mutableListOf<BatteryEvent>()

    override val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)

    override fun dismissSyncStatus() {}

    override suspend fun resync(timeout: Duration): SyncOutcome = SyncOutcome.Success

    override suspend fun clearAllLocalData() {}

    override fun getAllDevices(): Flow<List<Device>> = flowOf(devices.toList())

    override fun getDeviceById(id: String): Flow<Device?> = flowOf(devices.find { it.id == id })

    override suspend fun addDevice(device: Device): Result<Unit, DataError> {
        devices.removeAll { it.id == device.id }
        devices.add(device)
        return Result.Success(Unit)
    }

    override suspend fun updateDevice(device: Device): Result<Unit, DataError> = addDevice(device)

    override suspend fun deleteDevice(id: String): Result<Unit, DataError> {
        devices.removeAll { it.id == id }
        return Result.Success(Unit)
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = flowOf(deviceTypes.toList())

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = flowOf(deviceTypes.find { it.id == id })

    override suspend fun addDeviceType(type: DeviceType): Result<Unit, DataError> {
        deviceTypes.removeAll { it.id == type.id }
        deviceTypes.add(type)
        return Result.Success(Unit)
    }

    override suspend fun updateDeviceType(type: DeviceType): Result<Unit, DataError> = addDeviceType(type)

    override suspend fun deleteDeviceType(id: String): Result<Unit, DataError> {
        deviceTypes.removeAll { it.id == id }
        return Result.Success(Unit)
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = flowOf(events.filter { it.deviceId == deviceId })

    override fun getAllEvents(): Flow<List<BatteryEvent>> = flowOf(events.toList())

    override fun getEventById(id: String): Flow<BatteryEvent?> = flowOf(events.find { it.id == id })

    override suspend fun addEvent(event: BatteryEvent): Result<Unit, DataError> {
        events.removeAll { it.id == event.id }
        events.add(event)
        return Result.Success(Unit)
    }

    override suspend fun updateEvent(event: BatteryEvent): Result<Unit, DataError> = addEvent(event)

    override suspend fun deleteEvent(id: String): Result<Unit, DataError> {
        events.removeAll { it.id == id }
        return Result.Success(Unit)
    }
}
