package com.chriscartland.batterybutler.feature.ai

import android.content.Context
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.ai.ToolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.io.File

class AndroidAiEngine(
    private val context: Context,
) : AiEngine {
    // Simple availability check based on model file existence for now
    // In real implementation, check GPU support etc.
    // Assuming model is at /data/local/tmp/llm.bin (standard for MediaPipe demos)
    // Or users push it there.
    private val modelPath = "/data/local/tmp/llm.bin" // User didn't specify, standard manual path.

    // MediaPipe GenAI and Local Agents integration
    private val _isAvailable = kotlinx.coroutines.flow.MutableStateFlow(false)
    // private var chatSession: com.google.ai.edge.localagents.ChatSession? = null

    override val isAvailable: Flow<Boolean> = _isAvailable.asStateFlow()
    override val compatibility: Flow<Boolean> = flow { emit(true) }

    init {
        // Initial check
        checkAvailability()
    }

    private fun checkAvailability() {
        _isAvailable.value = File(modelPath).exists()
    }

    /*
    private suspend fun ensureChatSession(): com.google.ai.edge.localagents.ChatSession {
        // ... implementation commented out ...
    }
     */

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> =
        flow {
            checkAvailability()
            if (!_isAvailable.value) {
                emit(AiMessage("error", AiRole.MODEL, "Model not found. Please push llm.bin to $modelPath", false))
                return@flow
            }

            try {
                // Stub response for now
                emit(AiMessage("temp_id", AiRole.MODEL, "Model found! (Function calling logic pending SDK fix)", false))
                // Note: API integration commented out due to unresolved references in 0.1.0 SDK.
                // Needs verification of ChatSession.sendMessage signature and Part accessor methods.

            /*
            var currentResponse = chat.sendMessage(prompt)

            // Loop while response has function call
            while (true) {
                // Check candidates
                val candidates = currentResponse.candidatesList
                if (candidates.isEmpty()) break

                val content = candidates[0].content
                val part = if (content.partsCount > 0) content.partsList[0] else null

                if (part != null && part.hasFunctionCall()) {
                     val call = part.functionCall
                     val functionName = call.name
                     val argsJava = call.args.fieldsMap

                     // Convert Java Map to Kotlin Map<String, Any?>
                     val argsKotlin = argsJava.entries.associate { (k, v) ->
                         k to when {
                             v.hasStringValue() -> v.stringValue
                             v.hasNumberValue() -> v.numberValue
                             v.hasBoolValue() -> v.boolValue
                             else -> v.toString()
                         }
                     }

                     // Execute tool
                     val resultJson = toolHandler?.execute(functionName, argsKotlin) ?: "{ \"status\": \"error\", \"message\": \"No tool handler\" }"

                     // Build FunctionResponse
                     val responseStructBuilder = com.google.protobuf.Struct.newBuilder()
                     responseStructBuilder.putFields("result", com.google.protobuf.Value.newBuilder().setStringValue(resultJson).build())

                     val functionResponse = com.google.ai.edge.localagents.FunctionResponse.newBuilder()
                         .setName(functionName)
                         .setResponse(responseStructBuilder.build())
                         .build()

                     // Send function response back to model
                     val followUpContent = com.google.ai.edge.localagents.Content.newBuilder()
                        .setRole("function")
                        .addParts(com.google.ai.edge.localagents.Part.newBuilder().setFunctionResponse(functionResponse).build())
                        .build()

                     currentResponse = chat.sendMessage(followUpContent)
                } else {
                    // Text response
                    val text = part?.text ?: "..."
                    emit(AiMessage("resp_${System.currentTimeMillis()}", AiRole.MODEL, text, false))
                    break
                }
            }
             */
                emit(AiMessage("temp_id", AiRole.MODEL, "Model found! (Function calling logic pending API fix)", false))
            } catch (e: Exception) {
                e.printStackTrace()
                emit(AiMessage("error", AiRole.MODEL, "Error: ${e.message}", false))
            }
        }
}
