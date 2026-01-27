package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

class AddDeviceUseCaseTest {
    @Test
    fun `invoke should add device to repository`() =
        runTest {
            val fakeRepository = AddDeviceUseCaseFakeRepository()
            val useCase = AddDeviceUseCase(fakeRepository)
            val device = Device(
                id = "1",
                name = "Test Device",
                typeId = "type-1",
                batteryLastReplaced = Instant.DISTANT_PAST,
                lastUpdated = Instant.DISTANT_PAST,
            )

            useCase(device)

            assertTrue(fakeRepository.addedDevices.contains(device))
        }
}

class AddDeviceUseCaseFakeRepository : DeviceRepository {
    val addedDevices = mutableListOf<Device>()

    override val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)

    override fun getAllDevices(): Flow<List<Device>> = emptyFlow()

    override fun getDeviceById(id: String): Flow<Device?> = emptyFlow()

    override suspend fun addDevice(device: Device) {
        addedDevices.add(device)
    }

    override suspend fun updateDevice(device: Device) {}

    override suspend fun deleteDevice(id: String) {}

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = emptyFlow()

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = emptyFlow()

    override suspend fun addDeviceType(type: DeviceType) {}

    override suspend fun updateDeviceType(type: DeviceType) {}

    override suspend fun deleteDeviceType(id: String) {}

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = emptyFlow()

    override fun getAllEvents(): Flow<List<BatteryEvent>> = emptyFlow()

    override fun getEventById(id: String): Flow<BatteryEvent?> = emptyFlow()

    override suspend fun addEvent(event: BatteryEvent) {}

    override suspend fun updateEvent(event: BatteryEvent) {}

    override suspend fun deleteEvent(id: String) {}
}
