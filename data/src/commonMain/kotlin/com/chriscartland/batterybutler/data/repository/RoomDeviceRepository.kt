package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.data.room.DeviceDao
import com.chriscartland.batterybutler.data.room.toDomain
import com.chriscartland.batterybutler.data.room.toEntity
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@Inject
class RoomDeviceRepository(
    private val dao: DeviceDao,
) : DeviceRepository {
    override fun getAllDevices(): Flow<List<Device>> =
        dao.getAllDevices().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getDeviceById(id: String): Flow<Device?> = dao.getDeviceById(id).map { it?.toDomain() }

    override suspend fun addDevice(device: Device) {
        dao.insertDevice(device.toEntity())
    }

    override suspend fun updateDevice(device: Device) {
        dao.updateDevice(device.toEntity())
    }

    override suspend fun deleteDevice(id: String) {
        dao.deleteDevice(id)
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> =
        dao.getAllDeviceTypes().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = dao.getDeviceTypeById(id).map { it?.toDomain() }

    override suspend fun addDeviceType(type: DeviceType) {
        dao.insertDeviceType(type.toEntity())
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        dao.updateDeviceType(type.toEntity())
    }

    override suspend fun deleteDeviceType(id: String) {
        dao.deleteDeviceType(id)
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> =
        dao.getEventsForDevice(deviceId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getAllEvents(): Flow<List<BatteryEvent>> =
        dao.getAllEvents().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getEventById(id: String): Flow<BatteryEvent?> = dao.getEventById(id).map { it?.toDomain() }

    override suspend fun addEvent(event: BatteryEvent) {
        dao.insertEvent(event.toEntity())
    }

    override suspend fun updateEvent(event: BatteryEvent) {
        dao.updateEvent(event.toEntity())
    }

    override suspend fun deleteEvent(id: String) {
        dao.deleteEvent(id)
    }

    override suspend fun ensureDefaultDeviceTypes() {
        // We will seed or update based on Name to ensure icons are correct
        val existingTypes = dao
            .getAllDeviceTypes()
            .map { entities ->
                entities.map { it.toDomain() }.associateBy { it.name }
            }.firstOrNull() ?: emptyMap()

        val defaultTypes = listOf(
            DeviceType(
                id = existingTypes["Smart Button"]?.id ?: com.benasher44.uuid
                    .uuid4()
                    .toString(),
                name = "Smart Button",
                batteryType = "CR2450", // Assumption based on common smart buttons (e.g. Zigbee)
                batteryQuantity = 1,
                defaultIcon = "smart_button",
            ),
            DeviceType(
                id = existingTypes["Smart Motion Sensor"]?.id ?: com.benasher44.uuid
                    .uuid4()
                    .toString(),
                name = "Smart Motion Sensor",
                batteryType = "CR2477", // Assumption for some sensors, or CR123A
                batteryQuantity = 1,
                defaultIcon = "sensors",
            ),
            DeviceType(
                id = existingTypes["Tile Mate"]?.id ?: com.benasher44.uuid
                    .uuid4()
                    .toString(),
                name = "Tile Mate",
                batteryType = "CR1632",
                batteryQuantity = 1,
                defaultIcon = "location_on",
            ),
            DeviceType(
                id = existingTypes["Tile Pro"]?.id ?: com.benasher44.uuid
                    .uuid4()
                    .toString(),
                name = "Tile Pro",
                batteryType = "CR2032",
                batteryQuantity = 1,
                defaultIcon = "location_on",
            ),
            DeviceType(
                id = existingTypes["Calipers"]?.id ?: com.benasher44.uuid
                    .uuid4()
                    .toString(),
                name = "Calipers",
                batteryType = "LR44",
                batteryQuantity = 1,
                defaultIcon = "straighten",
            ),
            DeviceType(
                id = existingTypes["ULTRALOQ U-Bolt Pro Lock"]?.id ?: com.benasher44.uuid
                    .uuid4()
                    .toString(),
                name = "ULTRALOQ U-Bolt Pro Lock",
                batteryType = "AA",
                batteryQuantity = 4,
                defaultIcon = "lock",
            ),
            DeviceType(
                id = existingTypes["Digital Angle Ruler"]?.id ?: com.benasher44.uuid
                    .uuid4()
                    .toString(),
                name = "Digital Angle Ruler",
                batteryType = "CR2032",
                batteryQuantity = 1,
                defaultIcon = "straighten",
            ),
            DeviceType(
                id = existingTypes["Tile Wallet"]?.id ?: com.benasher44.uuid
                    .uuid4()
                    .toString(),
                name = "Tile Wallet",
                batteryType = "Thin Tile",
                batteryQuantity = 1,
                defaultIcon = "account_balance_wallet",
            ),
            DeviceType(
                id = existingTypes["1-9V Smoke Detector"]?.id ?: com.benasher44.uuid
                    .uuid4()
                    .toString(),
                name = "1-9V Smoke Detector",
                batteryType = "9V",
                batteryQuantity = 1,
                defaultIcon = "detector_smoke",
            ),
            DeviceType(
                id = existingTypes["2-AA Smoke Detector"]?.id ?: com.benasher44.uuid
                    .uuid4()
                    .toString(),
                name = "2-AA Smoke Detector",
                batteryType = "AA",
                batteryQuantity = 2,
                defaultIcon = "detector_smoke",
            ),
            DeviceType(
                id = existingTypes["Orbit 57896 Sprinkler Timer"]?.id ?: com.benasher44.uuid
                    .uuid4()
                    .toString(),
                name = "Orbit 57896 Sprinkler Timer",
                batteryType = "CR2032", // User didn't specify but sticking to previous assumption or generic
                batteryQuantity = 1,
                defaultIcon = "water_drop",
            ),
        )

        defaultTypes.forEach { type ->
            if (existingTypes.containsKey(type.name)) {
                // Update to ensure icon is correct
                updateDeviceType(type)
            } else {
                addDeviceType(type)
            }
        }
    }
}
