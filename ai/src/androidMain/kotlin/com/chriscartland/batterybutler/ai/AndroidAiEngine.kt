package com.chriscartland.batterybutler.ai

import com.chriscartland.batterybutler.domain.model.DeviceIcons
import com.chriscartland.batterybutler.domain.model.ai.AiConstants
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlin.coroutines.cancellation.CancellationException

/**
 * Android implementation of [AiEngine] using the Gemini API via **Firebase AI Logic**
 * (`com.google.firebase:firebase-ai`, Gemini Developer API backend).
 *
 * Migrated from the deprecated standalone `com.google.ai.client.generativeai` SDK, which pinned
 * Ktor 2.x. Once the app also used Ktor 3.x (the REST sync client in `:data-network`), the 2.x
 * Logging plugin gen-ai installs referenced `io.ktor.client.plugins.HttpTimeout` — a class Ktor 3.x
 * removed — and the app crashed on-device with `NoClassDefFoundError`. firebase-ai uses Ktor 3.x, so
 * the conflict is gone at the source.
 *
 * The Gemini API key is configured in the Firebase project (`google-services.json`), not in code.
 * Model construction is **lazy and guarded**: [isAvailable] reflects whether a default [FirebaseApp]
 * is initialized, and the model is only built on first use, so the app starts cleanly even when
 * Firebase is not yet configured (e.g. the placeholder `google-services.json`).
 */
class AndroidAiEngine : AiEngine {
    // The default FirebaseApp auto-initializes from google-services.json via the google-services
    // plugin; FirebaseApp.getInstance() throws if it is absent. Reading it here is cheap and does
    // not construct the model, so app startup never fails on a missing/placeholder Firebase config.
    private val _isAvailable = MutableStateFlow(runCatching { FirebaseApp.getInstance() }.isSuccess)
    override val isAvailable: Flow<Boolean> = _isAvailable.asStateFlow()
    override val compatibility: Flow<Boolean> = flow { emit(true) }

    private val addDeviceTool = FunctionDeclaration(
        name = AiToolNames.ADD_DEVICE,
        description = "Add a new device to the inventory",
        parameters = mapOf(
            AiToolParams.NAME to Schema.string("Name of the device"),
            AiToolParams.LOCATION to Schema.string("Location of the device (optional)"),
            AiToolParams.TYPE to Schema.string("Type of the device (optional)"),
        ),
        optionalParameters = listOf(AiToolParams.LOCATION, AiToolParams.TYPE),
    )

    private val addDeviceTypeTool = FunctionDeclaration(
        name = AiToolNames.ADD_DEVICE_TYPE,
        description = "Add a new device type category",
        parameters = mapOf(
            AiToolParams.NAME to Schema.string("Name of the device type"),
            AiToolParams.BATTERY_TYPE to Schema.string("Type of battery used (e.g. AA, CR2032)"),
            AiToolParams.BATTERY_QUANTITY to Schema.integer("Number of batteries required"),
            AiToolParams.ICON to Schema.string(
                "Icon for the device type. Available icons: " +
                    DeviceIcons.AvailableIcons.joinToString(", "),
            ),
        ),
    )

    private val recordBatteryReplacementTool = FunctionDeclaration(
        name = AiToolNames.RECORD_BATTERY_REPLACEMENT,
        description = "Record a battery replacement event for a device. If the device does not exist, it will be created.",
        parameters = mapOf(
            AiToolParams.DEVICE_NAME to Schema.string("Name of the device"),
            AiToolParams.DATE to Schema.string("Date of replacement (YYYY-MM-DD)"),
            AiToolParams.DEVICE_TYPE to Schema.string("Type of the device (optional, used if creating new device)"),
            AiToolParams.BATTERY_TYPE to Schema.string("Type of battery used (optional)"),
            AiToolParams.LOCATION to Schema.string("Location of the device (optional)"),
        ),
        optionalParameters = listOf(AiToolParams.DEVICE_TYPE, AiToolParams.BATTERY_TYPE, AiToolParams.LOCATION),
    )

    private val updateDeviceTool = FunctionDeclaration(
        name = AiToolNames.UPDATE_DEVICE,
        description = "Update an existing device's properties",
        parameters = mapOf(
            AiToolParams.ID to Schema.string("Device ID from the inventory context"),
            AiToolParams.NEW_NAME to Schema.string("New name for the device (optional)"),
            AiToolParams.NEW_LOCATION to Schema.string("New location (optional)"),
            AiToolParams.NEW_TYPE to Schema.string("New device type name (optional)"),
        ),
        optionalParameters = listOf(AiToolParams.NEW_NAME, AiToolParams.NEW_LOCATION, AiToolParams.NEW_TYPE),
    )

    private val deleteDeviceTool = FunctionDeclaration(
        name = AiToolNames.DELETE_DEVICE,
        description = "Delete a device and all its battery events",
        parameters = mapOf(
            AiToolParams.ID to Schema.string("Device ID from the inventory context"),
        ),
    )

    private val updateDeviceTypeTool = FunctionDeclaration(
        name = AiToolNames.UPDATE_DEVICE_TYPE,
        description = "Update an existing device type's properties",
        parameters = mapOf(
            AiToolParams.ID to Schema.string("Device type ID from the inventory context"),
            AiToolParams.NEW_NAME to Schema.string("New name for the device type (optional)"),
            AiToolParams.NEW_BATTERY_TYPE to Schema.string("New battery type (optional)"),
            AiToolParams.NEW_BATTERY_QUANTITY to Schema.integer("New battery quantity (optional)"),
            AiToolParams.NEW_ICON to Schema.string("New icon (optional)"),
        ),
        optionalParameters = listOf(
            AiToolParams.NEW_NAME,
            AiToolParams.NEW_BATTERY_TYPE,
            AiToolParams.NEW_BATTERY_QUANTITY,
            AiToolParams.NEW_ICON,
        ),
    )

    private val deleteDeviceTypeTool = FunctionDeclaration(
        name = AiToolNames.DELETE_DEVICE_TYPE,
        description = "Delete a device type. Fails if devices still use it.",
        parameters = mapOf(
            AiToolParams.ID to Schema.string("Device type ID from the inventory context"),
        ),
    )

    private val updateBatteryEventTool = FunctionDeclaration(
        name = AiToolNames.UPDATE_BATTERY_EVENT,
        description = "Update a battery replacement event",
        parameters = mapOf(
            AiToolParams.ID to Schema.string("Battery event ID from the inventory context"),
            AiToolParams.NEW_DATE to Schema.string("New date (YYYY-MM-DD) (optional)"),
            AiToolParams.NOTES to Schema.string("Notes for the event (optional)"),
        ),
        optionalParameters = listOf(AiToolParams.NEW_DATE, AiToolParams.NOTES),
    )

    private val deleteBatteryEventTool = FunctionDeclaration(
        name = AiToolNames.DELETE_BATTERY_EVENT,
        description = "Delete a battery replacement event",
        parameters = mapOf(
            AiToolParams.ID to Schema.string("Battery event ID from the inventory context"),
        ),
    )

    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = AiConstants.GEMINI_MODEL_NAME,
            tools = listOf(
                Tool.functionDeclarations(
                    listOf(
                        addDeviceTool,
                        addDeviceTypeTool,
                        recordBatteryReplacementTool,
                        updateDeviceTool,
                        deleteDeviceTool,
                        updateDeviceTypeTool,
                        deleteDeviceTypeTool,
                        updateBatteryEventTool,
                        deleteBatteryEventTool,
                    ),
                ),
            ),
            systemInstruction = content {
                text(
                    """
                    You are Battery Butler, a helpful home inventory assistant. You have access to the user's device inventory and battery history provided in the context of each message.

                    Capabilities:
                    - ADD devices, device types, and battery replacement events.
                    - UPDATE existing devices, device types, and battery events by their ID from the inventory context.
                    - DELETE devices, device types, and battery events by their ID.

                    Rules:
                    - Users refer to items by name or description. Match their description to the correct item ID from the inventory context, then use that ID in tool calls.
                    - BEFORE deleting anything, tell the user exactly what you will delete and ask for their explicit confirmation. Only call the delete tool after the user says yes.
                    - When updating, only change the fields the user asked to change. Preserve all other fields.
                    - If the user provides a date for a device, use recordBatteryReplacement to log it.
                    - When processing bulk data (like tables or CSVs), ignore header rows and only process lines containing valid data.
                    """.trimIndent(),
                )
            },
        )
    }

    private val chat by lazy { generativeModel.startChat() }

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> =
        flow {
            if (!_isAvailable.value) {
                emit(
                    AiMessage(
                        "error",
                        AiRole.MODEL,
                        "AI is unavailable: Firebase is not configured. Add a real google-services.json with the Gemini API enabled.",
                        false,
                    ),
                )
                return@flow
            }

            try {
                var response = chat.sendMessage(prompt)

                // Loop while there are function calls
                while (response.functionCalls.isNotEmpty()) {
                    val functionResponses = mutableListOf<FunctionResponsePart>()

                    for (call in response.functionCalls) {
                        // firebase-ai exposes args as Map<String, JsonElement>; ToolHandler expects
                        // Map<String, Any?>. Flatten each primitive to its string content (null-safe).
                        val argsMap: Map<String, Any?> = call.args.mapValues { (_, element) ->
                            (element as? JsonPrimitive)?.contentOrNull
                        }

                        val resultJson = toolHandler?.execute(call.name, argsMap)
                            ?: "{ \"status\": \"error\", \"message\": \"No tool handler\" }"

                        val responseJson: JsonObject = buildJsonObject { put("result", JsonPrimitive(resultJson)) }
                        functionResponses.add(
                            FunctionResponsePart(name = call.name, response = responseJson, id = call.id),
                        )
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
                val errorMsg = e.message ?: "Unknown error"
                co.touchlab.kermit.Logger
                    .e("AndroidAiEngine") { "Error generating response: $errorMsg" }
                emit(AiMessage("error", AiRole.MODEL, "Error: $errorMsg", false))
            }
        }
}
