package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.ai.AiToolNames
import com.chriscartland.batterybutler.domain.ai.AiToolParams
import com.chriscartland.batterybutler.domain.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.atStartOfDayIn
import me.tatarka.inject.annotations.Inject

@Inject
class BatchAddBatteryEventsUseCase(
    private val aiEngine: AiEngine,
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(input: String): Flow<AiMessage> =
        channelFlow {
            val lines = input.lines().filter { it.isNotBlank() }

            for (line in lines) {
                val modelMsgId = uuid4().toString()

                val toolHandler = ToolHandler { name, args ->
                    when (name) {
                        AiToolNames.RECORD_BATTERY_REPLACEMENT -> {
                            val deviceName = args[AiToolParams.DEVICE_NAME] as? String ?: return@ToolHandler "Error: Missing deviceName"
                            val dateStr = args[AiToolParams.DATE] as? String ?: return@ToolHandler "Error: Missing date"
                            val deviceTypeName = args[AiToolParams.DEVICE_TYPE] as? String

                            try {
                                // 1. Find or Create Device
                                val existingDevices: List<Device> = deviceRepository.getAllDevices().first()
                                var device: Device? = existingDevices.find { d -> d.name == deviceName }

                                if (device == null) {
                                    // Find or Create Device Type
                                    val typeId = if (!deviceTypeName.isNullOrBlank()) {
                                        val existingTypes: List<DeviceType> = deviceRepository.getAllDeviceTypes().first()
                                        val existingType = existingTypes.find { t -> t.name == deviceTypeName }

                                        if (existingType != null) {
                                            existingType.id
                                        } else {
                                            val newTypeId = uuid4().toString()
                                            deviceRepository.addDeviceType(DeviceType(newTypeId, deviceTypeName, "default"))
                                            newTypeId
                                        }
                                    } else {
                                        "default_type"
                                    }

                                    val newDevice = Device(
                                        id = uuid4().toString(),
                                        name = deviceName,
                                        typeId = typeId,
                                        batteryLastReplaced = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
                                        lastUpdated = Clock.System.now(),
                                    )
                                    deviceRepository.addDevice(newDevice)
                                    device = newDevice
                                }

                                // Ensure non-null for updates
                                val targetDevice: Device = device!!

                                // 2. Parse Date
                                val date = kotlinx.datetime.LocalDate.parse(dateStr)
                                val instant = date.atStartOfDayIn(kotlinx.datetime.TimeZone.currentSystemDefault())

                                // 3. Add Battery Event
                                val event = com.chriscartland.batterybutler.domain.model.BatteryEvent(
                                    id = uuid4().toString(),
                                    deviceId = targetDevice.id,
                                    date = instant,
                                    batteryType = args[AiToolParams.BATTERY_TYPE] as? String ?: "Unknown",
                                    notes = "Imported via AI",
                                )
                                deviceRepository.addEvent(event)

                                // 4. Update Device if newer
                                if (instant > targetDevice.batteryLastReplaced) {
                                    deviceRepository.updateDevice(targetDevice.copy(batteryLastReplaced = instant))
                                }

                                "Success: Recorded battery replacement for '$deviceName' on $dateStr"
                            } catch (e: Exception) {
                                "Error recording battery replacement: ${e.message}"
                            }
                        }
                        else -> "Error: This tool is not supported in this context. Use '${AiToolNames.RECORD_BATTERY_REPLACEMENT}' only."
                    }
                }

                try {
                    aiEngine.generateResponse(line, toolHandler).collect { tokenMsg ->
                        send(AiMessage(modelMsgId, AiRole.MODEL, tokenMsg.text))
                    }
                } catch (e: Exception) {
                    send(AiMessage(uuid4().toString(), AiRole.SYSTEM, "Error processing '$line': ${e.message}"))
                }
            }
        }
}
