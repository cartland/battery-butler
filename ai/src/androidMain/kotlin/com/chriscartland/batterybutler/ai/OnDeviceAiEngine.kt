package com.chriscartland.batterybutler.ai

import android.content.Context
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerationConfig
import com.google.mlkit.genai.prompt.TextPart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.tatarka.inject.annotations.Inject
import java.util.UUID

@Inject
class OnDeviceAiEngine(
    private val context: Context,
) : AiEngine {
    // Real availability is unknown until checkStatus() runs (see generateResponse below), so
    // default to false rather than optimistically assuming the on-device model is ready.
    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: Flow<Boolean> = _isAvailable.asStateFlow()
    override val compatibility: Flow<Boolean> = flow { emit(true) }

    private val generativeModel by lazy {
        val config = GenerationConfig.builder().build()
        Generation.getClient(config)
    }

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> =
        flow {
            try {
                // AICore/Gemini Nano is only supported on specific hardware and requires the
                // model to be downloaded before use. Attempting generateContent() without
                // checking status first surfaces as an opaque "INFERENCE_ERROR/UNKNOWN" error
                // on unsupported or not-yet-downloaded devices, so check reality first.
                val status = generativeModel.checkStatus()
                _isAvailable.value = status == FeatureStatus.AVAILABLE

                when (status) {
                    FeatureStatus.UNAVAILABLE -> {
                        emit(
                            AiMessage(
                                id = UUID.randomUUID().toString(),
                                role = AiRole.MODEL,
                                text = "On-device AI isn't supported on this device. " +
                                    "Try Cloud AI in Settings instead.",
                                isPartial = false,
                            ),
                        )
                        return@flow
                    }

                    FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                        emit(
                            AiMessage(
                                id = UUID.randomUUID().toString(),
                                role = AiRole.MODEL,
                                text = "The on-device AI model needs to download first (large " +
                                    "file, requires Wi-Fi) — try again in a bit.",
                                isPartial = false,
                            ),
                        )
                        return@flow
                    }

                    else -> {
                        Unit
                    } // AVAILABLE (or an unrecognized future status): proceed below.
                }

                // System Prompt for Function Calling
                val systemPrompt =
                    """
                    You are a helpful assistant for Battery Butler.
                    Available tools:
                    - addDevice(name: String, type: String, location: String)
                    - addDeviceType(name: String, icon: String, batteryType: String)
                    - recordBatteryReplacement(deviceName: String, date: String, deviceType: String, batteryType: String, location: String)
                    - updateDevice(id: String, newName: String, newLocation: String, newType: String)
                    - deleteDevice(id: String)
                    - updateDeviceType(id: String, newName: String, newBatteryType: String, newBatteryQuantity: String, newIcon: String)
                    - deleteDeviceType(id: String)
                    - updateBatteryEvent(id: String, newDate: String, notes: String)
                    - deleteBatteryEvent(id: String)

                    To call a tool, ONLY output JSON: { "tool": "toolName", "args": { ... } }
                    If no tool is needed, respond normally.
                    BEFORE deleting anything, tell the user what you will delete and ask for confirmation.
                    """.trimIndent()
                val fullPrompt = "$systemPrompt\n\nUser: $prompt"

                val request = GenerateContentRequest.builder(TextPart(fullPrompt)).build()

                val response = generativeModel.generateContent(request)
                val text = response.candidates.firstOrNull()?.text ?: "No response text"

                // Simple manually check if response looks like JSON tool call
                val toolCallResult = tryParseToolCall(text)

                if (toolCallResult != null && toolHandler != null) {
                    val (functionName, args) = toolCallResult
                    val resultJson = toolHandler.execute(functionName, args)

                    emit(
                        AiMessage(
                            id = UUID.randomUUID().toString(),
                            role = AiRole.SYSTEM,
                            text = "Executed $functionName: $resultJson",
                            isPartial = false,
                        ),
                    )
                } else {
                    emit(
                        AiMessage(
                            id = UUID.randomUUID().toString(),
                            role = AiRole.MODEL,
                            text = text,
                            isPartial = false,
                        ),
                    )
                }
            } catch (e: Exception) {
                emit(
                    AiMessage(
                        id = UUID.randomUUID().toString(),
                        role = AiRole.MODEL,
                        text = "On-Device AI Error: ${e.message ?: "Unknown error"}",
                        isPartial = false,
                    ),
                )
            }
        }

    private fun tryParseToolCall(text: String): Pair<String, Map<String, String>>? {
        // Strip markdown code blocks if present
        val cleanText = text
            .trim()
            .replace(Regex("^```(?:json)?"), "")
            .replace(Regex("```$"), "")
            .trim()

        return try {
            val json = Json { ignoreUnknownKeys = true }
            val element = json.parseToJsonElement(cleanText).jsonObject
            if (element.containsKey("tool") && element.containsKey("args")) {
                val toolName = element.getValue("tool").jsonPrimitive.content
                val argsElement = element.getValue("args").jsonObject
                val argsMap = argsElement.entries.associate { (k, v) -> k to v.jsonPrimitive.content }
                toolName to argsMap
            } else {
                null
            }
        } catch (
            @Suppress("SwallowedException") _: Exception,
        ) {
            null
        }
    }
}
