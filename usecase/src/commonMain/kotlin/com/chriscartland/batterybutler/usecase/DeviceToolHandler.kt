package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.datetime.atStartOfDayIn
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

/**
 * A [ToolHandler] that handles all Battery Butler AI tools:
 * - [AiToolNames.ADD_DEVICE]: Add a new device to the inventory
 * - [AiToolNames.ADD_DEVICE_TYPE]: Add a new device type category
 * - [AiToolNames.RECORD_BATTERY_REPLACEMENT]: Record a battery replacement event
 */
@Inject
class DeviceToolHandler(
    private val deviceRepository: DeviceRepository,
    private val findOrCreateDeviceTypeUseCase: FindOrCreateDeviceTypeUseCase,
    private val findOrCreateDeviceUseCase: FindOrCreateDeviceUseCase,
) : ToolHandler {
    override suspend fun execute(
        name: String,
        args: Map<String, Any?>,
    ): String =
        try {
            when (name) {
                AiToolNames.ADD_DEVICE -> addDevice(args)
                AiToolNames.ADD_DEVICE_TYPE -> addDeviceType(args)
                AiToolNames.RECORD_BATTERY_REPLACEMENT -> recordBatteryReplacement(args)
                else -> "Error: Unknown tool '$name'"
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            "Error executing $name: ${e.message}"
        }

    private suspend fun addDevice(args: Map<String, Any?>): String {
        val name = args[AiToolParams.NAME] as? String ?: return "Error: Missing name"
        val typeName = args[AiToolParams.TYPE] as? String

        val typeId = findOrCreateDeviceTypeUseCase(typeName)

        deviceRepository.addDevice(
            Device(
                id = uuid4().toString(),
                name = name,
                typeId = typeId,
                batteryLastReplaced = kotlin.time.Instant.fromEpochMilliseconds(0),
                lastUpdated = Clock.System.now(),
            ),
        )
        return "Success: Added device '$name' (Type: ${typeName ?: "Default"})"
    }

    private suspend fun addDeviceType(args: Map<String, Any?>): String {
        val name = args[AiToolParams.NAME] as? String ?: return "Error: Missing name"
        val batteryType = args[AiToolParams.BATTERY_TYPE] as? String ?: "Unknown"
        val icon = args[AiToolParams.ICON] as? String ?: "default"

        deviceRepository.addDeviceType(
            DeviceType(id = uuid4().toString(), name = name, defaultIcon = icon),
        )
        return "Success: Added device type '$name' (Battery: $batteryType, Icon: $icon)"
    }

    private suspend fun recordBatteryReplacement(args: Map<String, Any?>): String {
        val deviceName = args[AiToolParams.DEVICE_NAME] as? String ?: return "Error: Missing deviceName"
        val dateStr = args[AiToolParams.DATE] as? String ?: return "Error: Missing date"
        val deviceTypeName = args[AiToolParams.DEVICE_TYPE] as? String

        val targetDevice = findOrCreateDeviceUseCase(deviceName, deviceTypeName)

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
        deviceRepository.addEvent(event)

        // Update device if newer
        if (instant > targetDevice.batteryLastReplaced) {
            deviceRepository.updateDevice(targetDevice.copy(batteryLastReplaced = instant))
        }

        return "Success: Recorded battery replacement for '$deviceName' on $dateStr"
    }
}
