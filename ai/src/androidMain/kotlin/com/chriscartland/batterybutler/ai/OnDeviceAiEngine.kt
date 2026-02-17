package com.chriscartland.batterybutler.ai

import android.content.Context
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import me.tatarka.inject.annotations.Inject
import java.util.UUID
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

// ML Kit Imports
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerationConfig
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.TextPart
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Inject
class OnDeviceAiEngine(
    private val context: Context,
) : AiEngine {

    private val _isAvailable = MutableStateFlow(true) // Assume available, update if checkStatus fails
    override val isAvailable: Flow<Boolean> = _isAvailable.asStateFlow()
    override val compatibility: Flow<Boolean> = flow { emit(true) }

    private val generativeModel by lazy {
        val config = GenerationConfig.builder().build()
        Generation.getClient(config)
    }

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?
    ): Flow<AiMessage> = flow {
        try {
            // System Prompt for Function Calling
            val systemPrompt = """
                You are a helpful assistant for Battery Butler.
                Available tools:
                - addDevice(name: String, type: String, location: String)
                - addDeviceType(name: String, icon: String, batteryType: String)

                To call a tool, ONLY output JSON: { "tool": "toolName", "args": { ... } }
                If no tool is needed, respond normally.
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
                
                emit(AiMessage(
                    id = UUID.randomUUID().toString(),
                    role = AiRole.SYSTEM,
                    text = "Executed $functionName: $resultJson",
                    isPartial = false
                ))
            } else {
                emit(AiMessage(
                    id = UUID.randomUUID().toString(),
                    role = AiRole.MODEL,
                    text = text,
                    isPartial = false
                ))
            }

        } catch (e: Exception) {
            emit(AiMessage(
                id = UUID.randomUUID().toString(),
                role = AiRole.MODEL,
                text = "On-Device AI Error: ${e.message}",
                isPartial = false
            ))
        }
    }

    private fun tryParseToolCall(text: String): Pair<String, Map<String, String>>? {
        // Strip markdown code blocks if present
        val cleanText = text.trim()
            .replace(Regex("^```(?:json)?"), "")
            .replace(Regex("```$"), "")
            .trim()

        return try {
            val json = Json { ignoreUnknownKeys = true }
            val element = json.parseToJsonElement(cleanText).jsonObject
            if (element.containsKey("tool") && element.containsKey("args")) {
                val toolName = element["tool"]?.jsonPrimitive?.content ?: return null
                val argsElement = element["args"]?.jsonObject ?: return null
                val argsMap = argsElement.entries.associate { (k, v) -> k to v.jsonPrimitive.content }
                toolName to argsMap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
