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
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock

@Inject
class BatchAddDevicesUseCase(
    private val aiEngine: AiEngine,
    private val deviceRepository: DeviceRepository,
) {
    private val systemInstructions =
        """
        Analyze the data below and call the ${AiToolNames.ADD_DEVICE} tool for each device found.
        - Ignore header rows (e.g. "Device Name", "Location").
        - If 'Device Type' is missing, imply it from the name if possible.
        """.trimIndent()

    operator fun invoke(input: String): Flow<AiMessage> =
        channelFlow {
            val modelMsgId = uuid4().toString()

            val toolHandler = ToolHandler { name, args ->
                when (name) {
                    AiToolNames.ADD_DEVICE -> {
                        val name = args[AiToolParams.NAME] as? String ?: return@ToolHandler "Error: Missing name"
                        val typeName = args[AiToolParams.TYPE] as? String

                        try {
                            val typeId = if (!typeName.isNullOrBlank()) {
                                // Smart deduplication
                                val existingTypes: List<DeviceType> = deviceRepository.getAllDeviceTypes().first()
                                val existingType = existingTypes.find { type -> type.name == typeName }

                                if (existingType != null) {
                                    existingType.id
                                } else {
                                    val newTypeId = uuid4().toString()
                                    deviceRepository.addDeviceType(
                                        DeviceType(
                                            id = newTypeId,
                                            name = typeName,
                                            defaultIcon = "default",
                                        ),
                                    )
                                    newTypeId
                                }
                            } else {
                                "default_type"
                            }

                            deviceRepository.addDevice(
                                Device(
                                    id = uuid4().toString(),
                                    name = name,
                                    typeId = typeId,
                                    batteryLastReplaced = kotlin.time.Instant.fromEpochMilliseconds(0),
                                    lastUpdated = Clock.System.now(),
                                ),
                            )
                            "Success: Added device '$name' (Type: ${typeName ?: "Default"})"
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            "Error adding device: ${e.message}"
                        }
                    }
                    else -> "Error: This tool is not supported in this context. Use '${AiToolNames.ADD_DEVICE}' only."
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
                    send(AiMessage(modelMsgId, AiRole.MODEL, tokenMsg.text))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                send(AiMessage(uuid4().toString(), AiRole.SYSTEM, "Error processing input: ${e.message}"))
            }
        }
}
