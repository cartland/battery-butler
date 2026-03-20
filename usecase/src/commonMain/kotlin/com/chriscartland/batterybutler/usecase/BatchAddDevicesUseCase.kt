package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.getOrElse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

@Inject
class BatchAddDevicesUseCase(
    private val aiEngine: AiEngine,
    private val addDeviceUseCase: AddDeviceUseCase,
    private val findOrCreateDeviceTypeUseCase: FindOrCreateDeviceTypeUseCase,
) {
    private val systemInstructions =
        """
        Analyze the data below and call the ${AiToolNames.ADD_DEVICE} tool for each device found.
        - Ignore header rows (e.g. "Device Name", "Location").
        - If 'Device Type' is missing, imply it from the name if possible.
        """.trimIndent()

    operator fun invoke(input: String): Flow<BatchOperationResult> =
        channelFlow {
            val toolHandler = ToolHandler { name, args ->
                when (name) {
                    AiToolNames.ADD_DEVICE -> {
                        val name = args[AiToolParams.NAME] as? String ?: return@ToolHandler "Error: Missing name"
                        val typeName = args[AiToolParams.TYPE] as? String

                        val typeId = findOrCreateDeviceTypeUseCase(typeName)
                            .getOrElse { return@ToolHandler "Error: ${it.message}" }

                        when (
                            val result = addDeviceUseCase(
                                Device(
                                    id = uuid4().toString(),
                                    name = name,
                                    typeId = typeId,
                                    batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                                    lastUpdated = Clock.System.now(),
                                ),
                            )
                        ) {
                            is Result.Success -> "Success: Added device '$name' (Type: ${typeName ?: "Default"})"
                            is Result.Error -> "Error adding device: ${result.error.message}"
                        }
                    }

                    else -> {
                        "Error: This tool is not supported in this context. Use '${AiToolNames.ADD_DEVICE}' only."
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
