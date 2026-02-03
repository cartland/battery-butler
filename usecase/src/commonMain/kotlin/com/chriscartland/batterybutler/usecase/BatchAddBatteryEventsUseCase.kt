package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.ai.AiEngine
import com.chriscartland.batterybutler.ai.AiToolNames
import com.chriscartland.batterybutler.ai.AiToolParams
import com.chriscartland.batterybutler.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.atStartOfDayIn
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

@Inject
class BatchAddBatteryEventsUseCase(
    private val aiEngine: AiEngine,
    private val deviceRepository: DeviceRepository,
) {
    private val systemInstructions =
        """
        Analyze the data below and call the ${AiToolNames.RECORD_BATTERY_REPLACEMENT} tool for each battery replacement event found.
        - Ignore header rows (e.g. "Device", "Last Replaced").
        - Date format expected: YYYY-MM-DD. If dates are in other formats, convert them.
        - If the Device Type is implied or listed, include it.
        """.trimIndent()

    operator fun invoke(input: String): Flow<BatchOperationResult> =
        channelFlow {
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
                                    batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                                    lastUpdated = Clock.System.now(),
                                )
                                deviceRepository.addDevice(newDevice)
                                device = newDevice
                            }

                            // Ensure non-null for updates
                            val targetDevice: Device = device!!

                            // 2. Parse Date
                            val date = kotlinx.datetime.LocalDate.parse(dateStr)
                            val kxInstant = date.atStartOfDayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
                            val instant = kotlin.time.Instant.fromEpochMilliseconds(kxInstant.toEpochMilliseconds())

                            // 3. Add Battery Event
                            val event = BatteryEvent(
                                id = uuid4().toString(),
                                batteryType = "AA", // Placeholder or from args if available
                                deviceId = targetDevice.id,
                                date = instant,
                                notes = "Imported via AI",
                            )
                            deviceRepository.addEvent(event)

                            // 4. Update Device if newer
                            if (instant > targetDevice.batteryLastReplaced) {
                                deviceRepository.updateDevice(targetDevice.copy(batteryLastReplaced = instant))
                            }

                            "Success: Recorded battery replacement for '$deviceName' on $dateStr"
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            "Error recording battery replacement: ${e.message}"
                        }
                    }
                    else -> "Error: This tool is not supported in this context. Use '${AiToolNames.RECORD_BATTERY_REPLACEMENT}' only."
                }
            }

            try {
                val prompt =
                    """
                    *** SYSTEM INSTRUCTIONS ***
                    $systemInstructions

                    *** USER INPUT DATA ***
                    $input
                    """.trimIndent()

                aiEngine.generateResponse(prompt, toolHandler).collect { tokenMsg ->
                    send(BatchOperationResult.Progress(tokenMsg.text))
                }
                send(BatchOperationResult.Success("Batch operation completed."))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                send(
                    BatchOperationResult.Error(
                        DataError.Ai.ApiError("Error processing input: ${e.message}", e.toString()),
                    ),
                )
            }
        }
}
