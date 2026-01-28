package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class UpdateDeviceLastReplacedUseCaseTest {
    @Test
    fun `invoke updates device when event is newer than current timestamp`() =
        runTest {
            val repo = TestRepository()
            val oldDate = Instant.parse("2023-01-01T00:00:00Z")
            val newDate = Instant.parse("2024-06-15T10:30:00Z")
            val device = TestDevices.createDevice(id = "d1", batteryLastReplaced = oldDate)
            val event = BatteryEvent(id = "e1", deviceId = "d1", date = newDate)
            repo.devices.add(device)
            repo.events.add(event)

            val useCase = UpdateDeviceLastReplacedUseCase(repo)
            val result = useCase("d1")

            assertTrue(result)
            assertEquals(newDate, repo.devices.first().batteryLastReplaced)
        }

    @Test
    fun `invoke does not update device when no events are newer`() =
        runTest {
            val repo = TestRepository()
            val currentDate = Instant.parse("2024-06-15T10:30:00Z")
            val oldDate = Instant.parse("2023-01-01T00:00:00Z")
            val device = TestDevices.createDevice(id = "d1", batteryLastReplaced = currentDate)
            val event = BatteryEvent(id = "e1", deviceId = "d1", date = oldDate)
            repo.devices.add(device)
            repo.events.add(event)

            val useCase = UpdateDeviceLastReplacedUseCase(repo)
            val result = useCase("d1")

            assertFalse(result)
            assertEquals(currentDate, repo.devices.first().batteryLastReplaced)
        }

    @Test
    fun `invoke returns false when device not found`() =
        runTest {
            val repo = TestRepository()
            val useCase = UpdateDeviceLastReplacedUseCase(repo)

            val result = useCase("nonexistent")

            assertFalse(result)
        }

    @Test
    fun `invoke returns false when no events exist for device`() =
        runTest {
            val repo = TestRepository()
            val device = TestDevices.createDevice(id = "d1")
            repo.devices.add(device)

            val useCase = UpdateDeviceLastReplacedUseCase(repo)
            val result = useCase("d1")

            assertFalse(result)
        }

    @Test
    fun `invoke finds the latest event when multiple events exist`() =
        runTest {
            val repo = TestRepository()
            val device = TestDevices.createDevice(id = "d1", batteryLastReplaced = Instant.DISTANT_PAST)
            val event1 = BatteryEvent(id = "e1", deviceId = "d1", date = Instant.parse("2023-01-01T00:00:00Z"))
            val event2 = BatteryEvent(id = "e2", deviceId = "d1", date = Instant.parse("2024-06-15T00:00:00Z"))
            val event3 = BatteryEvent(id = "e3", deviceId = "d1", date = Instant.parse("2024-03-01T00:00:00Z"))
            repo.devices.add(device)
            repo.events.addAll(listOf(event1, event2, event3))

            val useCase = UpdateDeviceLastReplacedUseCase(repo)
            val result = useCase("d1")

            assertTrue(result)
            assertEquals(Instant.parse("2024-06-15T00:00:00Z"), repo.devices.first().batteryLastReplaced)
        }

    @Test
    fun `ifNewer updates device when timestamp is newer`() =
        runTest {
            val repo = TestRepository()
            val oldDate = Instant.parse("2023-01-01T00:00:00Z")
            val newDate = Instant.parse("2024-06-15T10:30:00Z")
            val device = TestDevices.createDevice(id = "d1", batteryLastReplaced = oldDate)
            repo.devices.add(device)

            val useCase = UpdateDeviceLastReplacedUseCase(repo)
            val result = useCase.ifNewer("d1", newDate)

            assertTrue(result)
            assertEquals(newDate, repo.devices.first().batteryLastReplaced)
        }

    @Test
    fun `ifNewer does not update device when timestamp is older`() =
        runTest {
            val repo = TestRepository()
            val currentDate = Instant.parse("2024-06-15T10:30:00Z")
            val oldDate = Instant.parse("2023-01-01T00:00:00Z")
            val device = TestDevices.createDevice(id = "d1", batteryLastReplaced = currentDate)
            repo.devices.add(device)

            val useCase = UpdateDeviceLastReplacedUseCase(repo)
            val result = useCase.ifNewer("d1", oldDate)

            assertFalse(result)
            assertEquals(currentDate, repo.devices.first().batteryLastReplaced)
        }

    @Test
    fun `ifNewer returns false when device not found`() =
        runTest {
            val repo = TestRepository()
            val useCase = UpdateDeviceLastReplacedUseCase(repo)

            val result = useCase.ifNewer("nonexistent", Instant.parse("2024-01-01T00:00:00Z"))

            assertFalse(result)
        }

    private class TestRepository : DeviceRepository {
        val devices = mutableListOf<Device>()
        val events = mutableListOf<BatteryEvent>()
        override val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)

        override fun dismissSyncStatus() {}

        override fun getAllDevices(): Flow<List<Device>> = flowOf(devices)

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

        override fun getAllDeviceTypes(): Flow<List<DeviceType>> = flowOf(emptyList())

        override fun getDeviceTypeById(id: String): Flow<DeviceType?> = flowOf(null)

        override suspend fun addDeviceType(type: DeviceType) {}

        override suspend fun updateDeviceType(type: DeviceType) {}

        override suspend fun deleteDeviceType(id: String) {}

        override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = flowOf(events.filter { it.deviceId == deviceId })

        override fun getAllEvents(): Flow<List<BatteryEvent>> = flowOf(events)

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
}
