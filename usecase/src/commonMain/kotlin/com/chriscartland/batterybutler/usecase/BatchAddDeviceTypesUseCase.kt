package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
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

    operator fun invoke(input: String): Flow<BatchOperationResult> =
        channelFlow {
            val toolHandler = ToolHandler { name, args ->
                when (name) {
                    AiToolNames.ADD_DEVICE_TYPE -> {
                        val name = args[AiToolParams.NAME] as? String ?: return@ToolHandler "Error: Missing name"
                        val iconName = args[AiToolParams.ICON] as? String ?: "default"

                        // Smart deduplication
                        val existingTypes: List<DeviceType> = deviceRepository.getAllDeviceTypes().first()
                        val existingType = existingTypes.find { type -> type.name == name }

                        if (existingType != null) {
                            "Success: Device type '$name' already exists."
                        } else {
                            when (
                                val result = deviceRepository.addDeviceType(
                                    DeviceType(
                                        id = uuid4().toString(),
                                        name = name,
                                        defaultIcon = iconName,
                                        batteryType = args[AiToolParams.BATTERY_TYPE] as? String ?: "AA",
                                        batteryQuantity = when (val qty = args[AiToolParams.BATTERY_QUANTITY]) {
                                            is String -> qty.toIntOrNull() ?: 1
                                            is Number -> qty.toInt()
                                            else -> 1
                                        },
                                    ),
                                )
                            ) {
                                is Result.Success -> "Success: Added device type '$name'"
                                is Result.Error -> "Error adding device type: ${result.error.message}"
                            }
                        }
                    }

                    else -> {
                        "Error: This tool is not supported in this context. Use '${AiToolNames.ADD_DEVICE_TYPE}' only."
                    }
                }
            }

            val prompt =
                """
                *** SYSTEM INSTRUCTIONS ***
                $systemInstructions

                *** USER INPUT DATA ***
                $input
                """.trimIndent()

            val result = generateAiResponse(prompt, toolHandler)
            when (result) {
                is Result.Success -> {
                    result.data.forEach { msg ->
                        send(BatchOperationResult.Progress(msg))
                    }
                    send(BatchOperationResult.Success("Batch operation completed."))
                }

                is Result.Error -> {
                    send(BatchOperationResult.Error(result.error))
                }
            }
        }

    private suspend fun generateAiResponse(
        prompt: String,
        toolHandler: ToolHandler,
    ): Result<List<String>, DataError.Ai> =
        try {
            val messages = mutableListOf<String>()
            aiEngine.generateResponse(prompt, toolHandler).collect { tokenMsg ->
                messages.add(tokenMsg.text)
            }
            Result.Success(messages)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(
                DataError.Ai.ApiError(
                    message = "Error processing input: ${e.message}",
                    cause = e.toString(),
                ),
            )
        }
}
