package com.chriscartland.batterybutler.datanetwork.ai

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datanetwork.LabsAuthGateway
import com.chriscartland.batterybutler.datanetwork.rest.createSyncHttpClient
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * The cloud AI assistant, relayed through the Labs backend (`POST /v1/battery-butler/ai/generate`).
 *
 * The app used to call Gemini directly — first with a key compiled into the app, then through
 * `firebase-ai`, which needed a real `google-services.json` the repo never had. Now the Labs
 * backend holds the key and picks the model, so a model retirement is a backend deploy rather than
 * a store release. It follows the app's Labs environment: staging builds talk to staging, prod to
 * prod, with the same sign-in the sync uses.
 *
 * The backend makes exactly one Gemini call per request and keeps no state. This engine owns the
 * conversation and runs the tool loop: when the model calls a tool, the [ToolHandler] executes it
 * against the local database and the results go back as the next request.
 *
 * **Model turns are stored and resent verbatim** ([JsonObject], never rebuilt): Gemini 3 attaches
 * a `thoughtSignature` to function-call parts and rejects a request that does not return it.
 *
 * Like the Firebase engine before it, one conversation is shared by every caller (chat, batch
 * import, icon suggestion), trimmed from the oldest end to stay inside the backend's limits.
 */
@Inject
class LabsCloudAiEngine(
    private val dataModeRepository: DataModeRepository,
    private val labsAuthGateway: LabsAuthGateway,
    private val dispatcherProvider: DispatcherProvider,
) : AiEngine {
    internal var httpClientFactory: () -> HttpClient = { createSyncHttpClient() }
    private val httpClient by lazy { httpClientFactory() }

    private val history = mutableListOf<JsonObject>()
    private val mutex = Mutex()

    override val isAvailable: Flow<Boolean> = dataModeRepository.dataMode.map { it.labsUrl() != null }
    override val compatibility: Flow<Boolean> = flow { emit(true) }

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> =
        flow {
            val baseUrl = dataModeRepository.dataMode.first().labsUrl()
            if (baseUrl == null) {
                emit(error(NOT_LABS_MESSAGE))
                return@flow
            }
            emit(mutex.withLock { converse(baseUrl, prompt, toolHandler) })
        }.flowOn(dispatcherProvider.io)

    /** Runs one user message to a final answer. On failure the conversation is rolled back. */
    private suspend fun converse(
        baseUrl: String,
        prompt: String,
        toolHandler: ToolHandler?,
    ): AiMessage {
        val checkpoint = history.size
        history.add(userText(prompt))
        try {
            repeat(MAX_TOOL_ROUNDS) {
                val outcome = send(baseUrl)
                if (outcome is Outcome.Failed) {
                    rollBack(checkpoint)
                    return error(outcome.message)
                }
                val content = (outcome as Outcome.Ok).content
                val parts = content["parts"] as? JsonArray ?: JsonArray(emptyList())
                if (parts.isEmpty()) {
                    // Gemini returned no candidate (e.g. blocked). Nothing to append — an empty
                    // model turn is not a valid turn to send back.
                    rollBack(checkpoint)
                    return AiMessage(newId(), AiRole.MODEL, NO_RESPONSE_MESSAGE)
                }
                history.add(content)
                val calls = parts.mapNotNull { (it as? JsonObject)?.get("functionCall") as? JsonObject }
                if (calls.isEmpty()) return AiMessage(newId(), AiRole.MODEL, textOf(parts).ifBlank { NO_RESPONSE_MESSAGE })
                history.add(functionResponses(calls, toolHandler))
            }
            Logger.w(TAG) { "tool loop stopped after $MAX_TOOL_ROUNDS rounds" }
            rollBack(checkpoint)
            return error("The assistant took too many steps. Please try a simpler request.")
        } catch (e: CancellationException) {
            rollBack(checkpoint)
            throw e
        }
    }

    private suspend fun send(baseUrl: String): Outcome =
        try {
            val token = labsAuthGateway.getLabsIdToken() ?: return Outcome.Failed(SIGN_IN_MESSAGE)
            val response = httpClient.post("${baseUrl.trimEnd('/')}$GENERATE_PATH") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(requestBody())
            }
            when {
                response.status.isSuccess() -> Outcome.Ok(response.body<GenerateResponseWire>().content)
                response.status == HttpStatusCode.Unauthorized -> Outcome.Failed(SIGN_IN_MESSAGE)
                response.status == HttpStatusCode.Forbidden -> Outcome.Failed(FORBIDDEN_MESSAGE)
                response.status == HttpStatusCode.PayloadTooLarge -> Outcome.Failed(TOO_LARGE_MESSAGE)
                response.status.value >= 500 -> Outcome.Failed(UNAVAILABLE_MESSAGE)
                else -> Outcome.Failed("Error: HTTP ${response.status.value}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, e) { "AI request failed" }
            Outcome.Failed(NETWORK_MESSAGE)
        }

    private fun requestBody(): JsonObject {
        trimHistory()
        return buildJsonObject {
            put("systemInstruction", LabsAiTools.SYSTEM_INSTRUCTION)
            put("tools", LabsAiTools.declarations)
            put("contents", JsonArray(history.toList()))
        }
    }

    /**
     * Drop the oldest exchanges until the conversation fits the backend's limits. A cut only ever
     * lands on a user *message* (not a function-response turn), so a tool call is never separated
     * from its answer — and the newest message is always kept.
     */
    internal fun trimHistory() {
        while (history.size > MAX_CONTENTS || jsonSize() > MAX_REQUEST_CHARS) {
            val nextStart = (1 until history.size).firstOrNull { isUserMessage(history[it]) } ?: return
            repeat(nextStart) { history.removeAt(0) }
        }
    }

    private fun jsonSize(): Int = history.sumOf { it.toString().length }

    private fun rollBack(checkpoint: Int) {
        while (history.size > checkpoint) history.removeAt(history.size - 1)
    }

    private suspend fun functionResponses(
        calls: List<JsonObject>,
        toolHandler: ToolHandler?,
    ): JsonObject {
        val parts = buildJsonArray {
            for (call in calls) {
                val name = call["name"]?.jsonPrimitive?.contentOrNull ?: continue
                // ToolHandler takes flat values; each argument is a primitive in every declared tool.
                val args: Map<String, Any?> = (call["args"] as? JsonObject).orEmpty().mapValues { (_, v) ->
                    (v as? JsonPrimitive)?.contentOrNull
                }
                val result = try {
                    toolHandler?.execute(name, args) ?: NO_TOOL_HANDLER_RESULT
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e(TAG, e) { "tool $name failed" }
                    "{ \"status\": \"error\", \"message\": \"${e.message.orEmpty().replace("\"", "'")}\" }"
                }
                add(
                    buildJsonObject {
                        put(
                            "functionResponse",
                            buildJsonObject {
                                call["id"]?.jsonPrimitive?.contentOrNull?.let { put("id", it) }
                                put("name", name)
                                put("response", buildJsonObject { put("result", result) })
                            },
                        )
                    },
                )
            }
        }
        return buildJsonObject {
            put("role", "user")
            put("parts", parts)
        }
    }

    private sealed interface Outcome {
        data class Ok(
            val content: JsonObject,
        ) : Outcome

        data class Failed(
            val message: String,
        ) : Outcome
    }

    internal companion object {
        const val TAG = "LabsCloudAiEngine"
        const val GENERATE_PATH = "/v1/battery-butler/ai/generate"

        /** Matches the backend's cap (aiSchema.ts `MAX_CONTENTS`), with room for one more round. */
        const val MAX_CONTENTS = 180

        /** The backend accepts 1 MB; stay well inside it. */
        const val MAX_REQUEST_CHARS = 512_000
        const val MAX_TOOL_ROUNDS = 10

        const val NOT_LABS_MESSAGE =
            "Cloud AI needs the Labs backend. Choose Labs (staging or prod) in Settings and sign in."
        const val SIGN_IN_MESSAGE = "Sign in to Labs to use the cloud assistant."
        const val FORBIDDEN_MESSAGE = "Your Labs account can't use the cloud assistant. Ask the owner for edit access."
        const val TOO_LARGE_MESSAGE = "That message is too large for the assistant."
        const val UNAVAILABLE_MESSAGE = "The assistant is temporarily unavailable. Please try again shortly."
        const val NETWORK_MESSAGE = "Couldn't reach the assistant. Check your connection and try again."
        const val NO_RESPONSE_MESSAGE = "No text response"
        const val NO_TOOL_HANDLER_RESULT = "{ \"status\": \"error\", \"message\": \"No tool handler\" }"

        private var counter = 0L

        fun newId(): String = "resp_${++counter}"

        fun error(message: String) = AiMessage("error", AiRole.MODEL, message, false)

        fun userText(text: String): JsonObject =
            buildJsonObject {
                put("role", "user")
                put("parts", buildJsonArray { add(buildJsonObject { put("text", text) }) })
            }

        fun isUserMessage(content: JsonObject): Boolean =
            content["role"]?.jsonPrimitive?.contentOrNull == "user" &&
                (content["parts"] as? JsonArray).orEmpty().any { (it as? JsonObject)?.containsKey("text") == true }

        /** The visible text of a model turn: text parts, minus thought summaries. */
        fun textOf(parts: JsonArray): String =
            parts
                .mapNotNull { it as? JsonObject }
                .filter { it["thought"]?.jsonPrimitive?.booleanOrNull != true }
                .mapNotNull { it["text"]?.jsonPrimitive?.contentOrNull }
                .joinToString("")
    }
}

private fun JsonArray?.orEmpty(): List<kotlinx.serialization.json.JsonElement> = this ?: emptyList()

/** The Labs backend URL for this mode, or null when the mode is not Labs or its URL is unset. */
private fun DataMode.labsUrl(): String? =
    when (this) {
        is DataMode.LabsStaging -> url
        is DataMode.LabsProd -> url
        else -> null
    }?.takeIf { it.isNotBlank() }

@kotlinx.serialization.Serializable
internal data class GenerateResponseWire(
    val content: JsonObject,
    val modelVersion: String = "",
)
