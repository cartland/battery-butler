package com.chriscartland.batterybutler.ai

import com.chriscartland.batterybutler.ai.BuildConfig
import com.chriscartland.batterybutler.domain.ai.AiConstants
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.ai.AiToolNames
import com.chriscartland.batterybutler.domain.ai.AiToolParams
import com.chriscartland.batterybutler.domain.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.DeviceIcons
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.defineFunction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

class AndroidAiEngine : AiEngine {
    // For Gemini API (Cloud), availability implies we are ready to send requests.
    // If the API key is empty, we might want to show unavailable, but user can add it later.
    private val _isAvailable = MutableStateFlow(true)
    override val isAvailable: Flow<Boolean> = _isAvailable.asStateFlow()
    override val compatibility: Flow<Boolean> = flow { emit(true) }

    // Use API Key from local.properties via BuildConfig
    // Note: ensure BuildConfig is generated for :data module and includes GEMINI_API_KEY if needed.
    // If BuildConfig is only in :composeApp, we might need to pass it in or configure :data to have it.

    private val apiKey = BuildConfig.GEMINI_API_KEY

    // Define Tools without execution blocks (manual execution)
    private val addDeviceTool = defineFunction(
        name = AiToolNames.ADD_DEVICE,
        description = "Add a new device to the inventory",
        parameters = listOf(
            Schema.str(AiToolParams.NAME, "Name of the device"),
            Schema.str(AiToolParams.LOCATION, "Location of the device (optional)"),
            // notes removed as per request
            Schema.str(AiToolParams.TYPE, "Type of the device (optional)"),
        ),
    )

    private val addDeviceTypeTool = defineFunction(
        name = AiToolNames.ADD_DEVICE_TYPE,
        description = "Add a new device type category",
        parameters = listOf(
            Schema.str(AiToolParams.NAME, "Name of the device type"),
            Schema.str(AiToolParams.BATTERY_TYPE, "Type of battery used (e.g. AA, CR2032)"),
            Schema.int(AiToolParams.BATTERY_QUANTITY, "Number of batteries required"),
            Schema.str(
                AiToolParams.ICON,
                "Icon for the device type. Available icons: " +
                    DeviceIcons.AvailableIcons
                        .joinToString(", "),
            ),
        ),
    )

    private val recordBatteryReplacementTool = defineFunction(
        name = AiToolNames.RECORD_BATTERY_REPLACEMENT,
        description = "Record a battery replacement event for a device. If the device does not exist, it will be created.",
        parameters = listOf(
            Schema.str(AiToolParams.DEVICE_NAME, "Name of the device"),
            Schema.str(AiToolParams.DATE, "Date of replacement (YYYY-MM-DD)"),
            Schema.str(AiToolParams.DEVICE_TYPE, "Type of the device (optional, used if creating new device)"),
            Schema.str(AiToolParams.BATTERY_TYPE, "Type of battery used (optional)"),
            Schema.str(AiToolParams.LOCATION, "Location of the device (optional)"),
        ),
    )

    private val generativeModel = GenerativeModel(
        modelName = AiConstants.GEMINI_MODEL_NAME,
        apiKey = apiKey,
        tools = listOf(Tool(listOf(addDeviceTool, addDeviceTypeTool, recordBatteryReplacementTool))),
        systemInstruction = content {
            text(
                "You are Battery Butler, a helpful home inventory assistant. You can add devices and device types. If the user provides a date for a device, use recordBatteryReplacement to log it. When processing bulk data (like tables or CSVs), ignore header rows and only process lines containing valid data.",
            )
        },
    )

    private val chat by lazy { generativeModel.startChat() }

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> =
        flow {
            if (apiKey.contains("TODO")) {
                emit(AiMessage("error", AiRole.MODEL, "Missing API Key. Please update AndroidAiEngine.kt.", false))
                return@flow
            }

            try {
                var response = chat.sendMessage(prompt)

                // Loop while there are function calls
                while (response.functionCalls.isNotEmpty()) {
                    val functionCalls = response.functionCalls
                    val functionResponses = mutableListOf<FunctionResponsePart>()

                    for (call in functionCalls) {
                        val name = call.name
                        val args = call.args

                        // Convert args to Map<String, Any?> for ToolHandler
                        val argsMap = args.entries.associate { (k, v) -> k to v }

                        val resultJson = toolHandler?.execute(name, argsMap)
                            ?: "{ \"status\": \"error\", \"message\": \"No tool handler\" }"

                        // Create response part with JSONObject
                        val resultSdk = JSONObject(mapOf("result" to resultJson))
                        functionResponses.add(FunctionResponsePart(name, resultSdk))
                    }

                    // Send function responses back
                    response = chat.sendMessage(
                        content {
                            for (fr in functionResponses) {
                                part(fr)
                            }
                        },
                    )
                }

                val text = response.text
                emit(AiMessage("resp_${System.currentTimeMillis()}", AiRole.MODEL, text ?: "No text response", false))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
                emit(AiMessage("error", AiRole.MODEL, "Error: ${e.message}", false))
            }
        }
}
