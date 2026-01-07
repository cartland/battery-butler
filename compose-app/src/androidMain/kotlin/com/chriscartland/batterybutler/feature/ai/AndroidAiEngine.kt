package com.chriscartland.batterybutler.feature.ai

import android.content.Context
import com.chriscartland.batterybutler.BuildConfig
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.ai.ToolHandler
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

class AndroidAiEngine(
    private val context: Context,
) : AiEngine {
    // For Gemini API (Cloud), availability implies we are ready to send requests.
    // If the API key is empty, we might want to show unavailable, but user can add it later.
    private val _isAvailable = MutableStateFlow(true)
    override val isAvailable: Flow<Boolean> = _isAvailable.asStateFlow()
    override val compatibility: Flow<Boolean> = flow { emit(true) }

    // Use API Key from local.properties via BuildConfig
    private val apiKey = BuildConfig.GEMINI_API_KEY

    // Define Tools without execution blocks (manual execution)
    private val addDeviceTool = defineFunction(
        name = "addDevice",
        description = "Add a new device to the inventory",
        parameters = listOf(
            Schema.str("name", "Name of the device"),
            Schema.str("location", "Location of the device (optional)"),
            Schema.str("notes", "Notes about the device (optional)"),
            Schema.str("type", "Type of the device (optional)"),
        ),
    )

    private val addDeviceTypeTool = defineFunction(
        name = "addDeviceType",
        description = "Add a new device type category",
        parameters = listOf(
            Schema.str("name", "Name of the device type"),
        ),
    )

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        tools = listOf(Tool(listOf(addDeviceTool, addDeviceTypeTool))),
        systemInstruction = content { text("You are Battery Butler, a helpful home inventory assistant. You can add devices and device types.") }
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
                e.printStackTrace()
                emit(AiMessage("error", AiRole.MODEL, "Error: ${e.message}", false))
            }
        }
}
