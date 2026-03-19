package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.getOrElse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.datetime.atStartOfDayIn
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException

@Inject
class BatchAddBatteryEventsUseCase(
    private val aiEngine: AiEngine,
    private val addBatteryEventUseCase: AddBatteryEventUseCase,
    private val findOrCreateDeviceUseCase: FindOrCreateDeviceUseCase,
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

                        val targetDevice = findOrCreateDeviceUseCase(deviceName, deviceTypeName)
                            .getOrElse { return@ToolHandler "Error: ${it.message}" }

                        // Parse Date
                        val date = kotlinx.datetime.LocalDate.parse(dateStr)
                        val kxInstant = date.atStartOfDayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
                        val instant = kotlin.time.Instant.fromEpochMilliseconds(kxInstant.toEpochMilliseconds())

                        // Add Battery Event (also updates device's batteryLastReplaced)
                        val event = BatteryEvent(
                            id = uuid4().toString(),
                            batteryType = "AA",
                            deviceId = targetDevice.id,
                            date = instant,
                            notes = "Imported via AI",
                        )
                        when (val result = addBatteryEventUseCase(event)) {
                            is Result.Success -> "Success: Recorded battery replacement for '$deviceName' on $dateStr"
                            is Result.Error -> "Error recording battery replacement: ${result.error.message}"
                        }
                    }

                    else -> {
                        "Error: This tool is not supported in this context. Use '${AiToolNames.RECORD_BATTERY_REPLACEMENT}' only."
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
