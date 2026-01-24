package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.ai.AiEngine
import com.chriscartland.batterybutler.ai.AiMessage
import com.chriscartland.batterybutler.ai.AiRole
import com.chriscartland.batterybutler.ai.AiToolNames
import com.chriscartland.batterybutler.ai.AiToolParams
import com.chriscartland.batterybutler.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException

@Inject
class BatchAddDeviceTypesUseCase(
    private val aiEngine: AiEngine,
    private val deviceRepository: DeviceRepository,
) {
    private val systemInstructions =
        """
        Analyze the data below and call the ${AiToolNames.ADD_DEVICE_TYPE} tool for each item found.
        - Ignore header rows (e.g. "Device Name", "Battery Type").
        - If 'Battery Quantity' is missing or empty, default to 1.
        - If 'Battery Type' is missing, use "AA" as a placeholder or infer from context if possible.
        """.trimIndent()

    operator fun invoke(input: String): Flow<AiMessage> =
        channelFlow {
            val modelMsgId = uuid4().toString()

            val toolHandler = ToolHandler { name, args ->
                when (name) {
                    AiToolNames.ADD_DEVICE_TYPE -> {
                        val name = args[AiToolParams.NAME] as? String ?: return@ToolHandler "Error: Missing name"
                        val iconName = args[AiToolParams.ICON] as? String ?: "default"

                        try {
                            // Smart deduplication
                            val existingTypes: List<DeviceType> = deviceRepository.getAllDeviceTypes().first()
                            val existingType = existingTypes.find { type -> type.name == name }

                            if (existingType != null) {
                                "Success: Device type '$name' already exists."
                            } else {
                                deviceRepository.addDeviceType(
                                    DeviceType(
                                        id = uuid4().toString(),
                                        name = name,
                                        defaultIcon = iconName,
                                        batteryType = args[AiToolParams.BATTERY_TYPE] as? String ?: "AA",
                                        batteryQuantity = (args[AiToolParams.BATTERY_QUANTITY] as? String)?.toIntOrNull()
                                            ?: (args[AiToolParams.BATTERY_QUANTITY] as? Number)?.toInt()
                                            ?: 1,
                                    ),
                                )
                                "Success: Added device type '$name'"
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            "Error adding device type: ${e.message}"
                        }
                    }
                    else -> "Error: This tool is not supported in this context. Use '${AiToolNames.ADD_DEVICE_TYPE}' only."
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
