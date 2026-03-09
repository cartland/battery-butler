package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.getOrElse
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.atStartOfDayIn
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock

/**
 * A [ToolHandler] that handles all Battery Butler AI tools:
 * - [AiToolNames.ADD_DEVICE]: Add a new device to the inventory
 * - [AiToolNames.ADD_DEVICE_TYPE]: Add a new device type category
 * - [AiToolNames.RECORD_BATTERY_REPLACEMENT]: Record a battery replacement event
 * - [AiToolNames.UPDATE_DEVICE]: Update an existing device
 * - [AiToolNames.DELETE_DEVICE]: Delete a device and its events
 * - [AiToolNames.UPDATE_DEVICE_TYPE]: Update an existing device type
 * - [AiToolNames.DELETE_DEVICE_TYPE]: Delete a device type (if unused)
 * - [AiToolNames.UPDATE_BATTERY_EVENT]: Update a battery event
 * - [AiToolNames.DELETE_BATTERY_EVENT]: Delete a battery event
 */
@Inject
class DeviceToolHandler(
    private val deviceRepository: DeviceRepository,
    private val findOrCreateDeviceTypeUseCase: FindOrCreateDeviceTypeUseCase,
    private val findOrCreateDeviceUseCase: FindOrCreateDeviceUseCase,
    private val updateDeviceLastReplacedUseCase: UpdateDeviceLastReplacedUseCase,
) : ToolHandler {
    override suspend fun execute(
        name: String,
        args: Map<String, Any?>,
    ): String =
        when (name) {
            AiToolNames.ADD_DEVICE -> addDevice(args)
            AiToolNames.ADD_DEVICE_TYPE -> addDeviceType(args)
            AiToolNames.RECORD_BATTERY_REPLACEMENT -> recordBatteryReplacement(args)
            AiToolNames.UPDATE_DEVICE -> updateDevice(args)
            AiToolNames.DELETE_DEVICE -> deleteDevice(args)
            AiToolNames.UPDATE_DEVICE_TYPE -> updateDeviceType(args)
            AiToolNames.DELETE_DEVICE_TYPE -> deleteDeviceType(args)
            AiToolNames.UPDATE_BATTERY_EVENT -> updateBatteryEvent(args)
            AiToolNames.DELETE_BATTERY_EVENT -> deleteBatteryEvent(args)
            else -> "Error: Unknown tool '$name'"
        }

    private suspend fun addDevice(args: Map<String, Any?>): String {
        val name = args[AiToolParams.NAME] as? String ?: return "Error: Missing name"
        val typeName = args[AiToolParams.TYPE] as? String

        val typeId = findOrCreateDeviceTypeUseCase(typeName)
            .getOrElse { return "Error: ${it.message}" }

        return when (
            val result = deviceRepository.addDevice(
                Device(
                    id = uuid4().toString(),
                    name = name,
                    typeId = typeId,
                    batteryLastReplaced = kotlin.time.Instant.fromEpochMilliseconds(0),
                    lastUpdated = Clock.System.now(),
                ),
            )
        ) {
            is Result.Success -> "Success: Added device '$name' (Type: ${typeName ?: "Default"})"
            is Result.Error -> "Error: ${result.error.message}"
        }
    }

    private suspend fun addDeviceType(args: Map<String, Any?>): String {
        val name = args[AiToolParams.NAME] as? String ?: return "Error: Missing name"
        val batteryType = args[AiToolParams.BATTERY_TYPE] as? String ?: "Unknown"
        val icon = args[AiToolParams.ICON] as? String ?: "default"

        return when (
            val result = deviceRepository.addDeviceType(
                DeviceType(id = uuid4().toString(), name = name, defaultIcon = icon),
            )
        ) {
            is Result.Success -> "Success: Added device type '$name' (Battery: $batteryType, Icon: $icon)"
            is Result.Error -> "Error: ${result.error.message}"
        }
    }

    private suspend fun recordBatteryReplacement(args: Map<String, Any?>): String {
        val deviceName = args[AiToolParams.DEVICE_NAME] as? String ?: return "Error: Missing deviceName"
        val dateStr = args[AiToolParams.DATE] as? String ?: return "Error: Missing date"
        val deviceTypeName = args[AiToolParams.DEVICE_TYPE] as? String

        val targetDevice = findOrCreateDeviceUseCase(deviceName, deviceTypeName)
            .getOrElse { return "Error: ${it.message}" }

        // Parse date
        val date = kotlinx.datetime.LocalDate.parse(dateStr)
        val kxInstant = date.atStartOfDayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        val instant = kotlin.time.Instant.fromEpochMilliseconds(kxInstant.toEpochMilliseconds())

        // Add battery event
        val event = BatteryEvent(
            id = uuid4().toString(),
            batteryType = "AA",
            deviceId = targetDevice.id,
            date = instant,
            notes = "Added via AI Chat",
        )
        deviceRepository
            .addEvent(event)
            .getOrElse { return "Error: ${it.message}" }

        // Update device if newer
        if (instant > targetDevice.batteryLastReplaced) {
            deviceRepository
                .updateDevice(targetDevice.copy(batteryLastReplaced = instant))
                .getOrElse { return "Error: ${it.message}" }
        }

        return "Success: Recorded battery replacement for '$deviceName' on $dateStr"
    }

    private suspend fun updateDevice(args: Map<String, Any?>): String {
        val id = args[AiToolParams.ID] as? String ?: return "Error: Missing id"
        val device = deviceRepository.getDeviceById(id).first()
            ?: return "Error: Device not found with id '$id'"

        val newName = args[AiToolParams.NEW_NAME] as? String
        val newLocation = args[AiToolParams.NEW_LOCATION] as? String
        val newType = args[AiToolParams.NEW_TYPE] as? String

        val newTypeId = if (newType != null) {
            findOrCreateDeviceTypeUseCase(newType)
                .getOrElse { return "Error: ${it.message}" }
        } else {
            device.typeId
        }

        val updated = device.copy(
            name = newName ?: device.name,
            location = newLocation ?: device.location,
            typeId = newTypeId,
            lastUpdated = Clock.System.now(),
        )
        deviceRepository
            .updateDevice(updated)
            .getOrElse { return "Error: ${it.message}" }

        val changes = buildList {
            if (newName != null) add("name → '$newName'")
            if (newLocation != null) add("location → '$newLocation'")
            if (newType != null) add("type → '$newType'")
        }
        return "Success: Updated device '${updated.name}' (${changes.joinToString(", ")})"
    }

    private suspend fun deleteDevice(args: Map<String, Any?>): String {
        val id = args[AiToolParams.ID] as? String ?: return "Error: Missing id"
        val device = deviceRepository.getDeviceById(id).first()
            ?: return "Error: Device not found with id '$id'"

        // Delete all events for this device
        val events = deviceRepository.getEventsForDevice(id).first()
        for (event in events) {
            deviceRepository
                .deleteEvent(event.id)
                .getOrElse { return "Error: ${it.message}" }
        }

        deviceRepository
            .deleteDevice(id)
            .getOrElse { return "Error: ${it.message}" }
        return "Success: Deleted device '${device.name}' and ${events.size} associated event(s)"
    }

    private suspend fun updateDeviceType(args: Map<String, Any?>): String {
        val id = args[AiToolParams.ID] as? String ?: return "Error: Missing id"
        val deviceType = deviceRepository.getDeviceTypeById(id).first()
            ?: return "Error: Device type not found with id '$id'"

        val newName = args[AiToolParams.NEW_NAME] as? String
        val newBatteryType = args[AiToolParams.NEW_BATTERY_TYPE] as? String
        val newBatteryQuantity = parseIntParam(args[AiToolParams.NEW_BATTERY_QUANTITY])
        val newIcon = args[AiToolParams.NEW_ICON] as? String

        val updated = deviceType.copy(
            name = newName ?: deviceType.name,
            batteryType = newBatteryType ?: deviceType.batteryType,
            batteryQuantity = newBatteryQuantity ?: deviceType.batteryQuantity,
            defaultIcon = newIcon ?: deviceType.defaultIcon,
        )
        deviceRepository
            .updateDeviceType(updated)
            .getOrElse { return "Error: ${it.message}" }

        val changes = buildList {
            if (newName != null) add("name → '$newName'")
            if (newBatteryType != null) add("batteryType → '$newBatteryType'")
            if (newBatteryQuantity != null) add("batteryQuantity → $newBatteryQuantity")
            if (newIcon != null) add("icon → '$newIcon'")
        }
        return "Success: Updated device type '${updated.name}' (${changes.joinToString(", ")})"
    }

    private suspend fun deleteDeviceType(args: Map<String, Any?>): String {
        val id = args[AiToolParams.ID] as? String ?: return "Error: Missing id"
        val deviceType = deviceRepository.getDeviceTypeById(id).first()
            ?: return "Error: Device type not found with id '$id'"

        val devicesUsingType = deviceRepository.getAllDevices().first().filter { it.typeId == id }
        if (devicesUsingType.isNotEmpty()) {
            return "Error: Cannot delete type '${deviceType.name}' — " +
                "${devicesUsingType.size} device(s) still use it. Reassign them first."
        }

        return when (val result = deviceRepository.deleteDeviceType(id)) {
            is Result.Success -> "Success: Deleted device type '${deviceType.name}'"
            is Result.Error -> "Error: ${result.error.message}"
        }
    }

    private suspend fun updateBatteryEvent(args: Map<String, Any?>): String {
        val id = args[AiToolParams.ID] as? String ?: return "Error: Missing id"
        val event = deviceRepository.getEventById(id).first()
            ?: return "Error: Battery event not found with id '$id'"

        val newDateStr = args[AiToolParams.NEW_DATE] as? String
        val notes = args[AiToolParams.NOTES] as? String

        val newDate = if (newDateStr != null) {
            val date = kotlinx.datetime.LocalDate.parse(newDateStr)
            val kxInstant = date.atStartOfDayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
            kotlin.time.Instant.fromEpochMilliseconds(kxInstant.toEpochMilliseconds())
        } else {
            event.date
        }

        val updated = event.copy(
            date = newDate,
            notes = notes ?: event.notes,
        )
        deviceRepository
            .updateEvent(updated)
            .getOrElse { return "Error: ${it.message}" }

        // Recalculate device's lastReplaced if date changed
        if (newDateStr != null) {
            updateDeviceLastReplacedUseCase(event.deviceId)
                .getOrElse { return "Error: ${it.message}" }
        }

        val changes = buildList {
            if (newDateStr != null) add("date → '$newDateStr'")
            if (notes != null) add("notes → '$notes'")
        }
        return "Success: Updated battery event (${changes.joinToString(", ")})"
    }

    private suspend fun deleteBatteryEvent(args: Map<String, Any?>): String {
        val id = args[AiToolParams.ID] as? String ?: return "Error: Missing id"
        val event = deviceRepository.getEventById(id).first()
            ?: return "Error: Battery event not found with id '$id'"

        val deviceId = event.deviceId
        deviceRepository
            .deleteEvent(id)
            .getOrElse { return "Error: ${it.message}" }

        // Recalculate device's lastReplaced
        updateDeviceLastReplacedUseCase(deviceId)
            .getOrElse { return "Error: ${it.message}" }

        val deviceName = deviceRepository.getDeviceById(deviceId).first()?.name ?: "Unknown"
        return "Success: Deleted battery event for '$deviceName'"
    }

    private fun parseIntParam(value: Any?): Int? =
        when (value) {
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
}
