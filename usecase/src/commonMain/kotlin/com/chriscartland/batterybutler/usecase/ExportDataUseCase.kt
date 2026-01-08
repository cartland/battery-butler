package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@Inject
class ExportDataUseCase(
    private val deviceRepository: DeviceRepository,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend operator fun invoke(): String {
        val devices = deviceRepository.getAllDevices().first()
        val types = deviceRepository.getAllDeviceTypes().first()
        val events = deviceRepository.getAllEvents().first()

        val exportData = ExportData(
            devices = devices.map { it.toDto() },
            deviceTypes = types.map { it.toDto() },
            events = events.map { it.toDto() },
        )

        return json.encodeToString(exportData)
    }

    @Serializable
    private data class ExportData(
        val devices: List<DeviceDto>,
        val deviceTypes: List<DeviceTypeDto>,
        val events: List<BatteryEventDto>,
    )

    @Serializable
    private data class DeviceDto(
        val id: String,
        val name: String,
        val typeId: String,
        val batteryLastReplaced: Instant?,
        val location: String?,
    )

    @Serializable
    private data class DeviceTypeDto(
        val id: String,
        val name: String,
        val batteryType: String?,
        val batteryQuantity: Int,
        val defaultIcon: String?,
    )

    @Serializable
    private data class BatteryEventDto(
        val id: String,
        val deviceId: String,
        val date: Instant,
        val batteryType: String?,
        val notes: String?,
    )

    private fun Device.toDto() =
        DeviceDto(
            id = id,
            name = name,
            typeId = typeId,
            batteryLastReplaced = batteryLastReplaced,
            location = location,
        )

    private fun DeviceType.toDto() =
        DeviceTypeDto(
            id = id,
            name = name,
            batteryType = batteryType,
            batteryQuantity = batteryQuantity,
            defaultIcon = defaultIcon,
        )

    private fun BatteryEvent.toDto() =
        BatteryEventDto(
            id = id,
            deviceId = deviceId,
            date = date,
            batteryType = batteryType,
            notes = notes,
        )
}
