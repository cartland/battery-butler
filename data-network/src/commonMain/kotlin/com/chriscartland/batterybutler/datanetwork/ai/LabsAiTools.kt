package com.chriscartland.batterybutler.datanetwork.ai

import com.chriscartland.batterybutler.domain.model.DeviceIcons
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The cloud assistant's system prompt and tool declarations, sent with every relay request.
 *
 * They live in the app, not the backend, because they change together with the `ToolHandler`
 * code that executes the tools. Parameters are plain JSON Schema (the backend passes them to
 * Gemini as `parametersJsonSchema`).
 */
internal object LabsAiTools {
    val SYSTEM_INSTRUCTION =
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
        """.trimIndent()

    private class Param(
        val name: String,
        val type: String,
        val description: String,
        val optional: Boolean = false,
    )

    private fun str(
        name: String,
        description: String,
        optional: Boolean = false,
    ) = Param(name, "string", description, optional)

    private fun int(
        name: String,
        description: String,
        optional: Boolean = false,
    ) = Param(name, "integer", description, optional)

    private fun tool(
        name: String,
        description: String,
        vararg params: Param,
    ): JsonObject =
        buildJsonObject {
            put("name", name)
            put("description", description)
            put(
                "parameters",
                buildJsonObject {
                    put("type", "object")
                    put(
                        "properties",
                        buildJsonObject {
                            for (p in params) {
                                put(
                                    p.name,
                                    buildJsonObject {
                                        put("type", p.type)
                                        put("description", p.description)
                                    },
                                )
                            }
                        },
                    )
                    put("required", JsonArray(params.filterNot { it.optional }.map { JsonPrimitive(it.name) }))
                },
            )
        }

    val declarations: JsonArray =
        buildJsonArray {
            add(
                tool(
                    AiToolNames.ADD_DEVICE,
                    "Add a new device to the inventory",
                    str(AiToolParams.NAME, "Name of the device"),
                    str(AiToolParams.LOCATION, "Location of the device (optional)", optional = true),
                    str(AiToolParams.TYPE, "Type of the device (optional)", optional = true),
                ),
            )
            add(
                tool(
                    AiToolNames.ADD_DEVICE_TYPE,
                    "Add a new device type category",
                    str(AiToolParams.NAME, "Name of the device type"),
                    str(AiToolParams.BATTERY_TYPE, "Type of battery used (e.g. AA, CR2032)"),
                    int(AiToolParams.BATTERY_QUANTITY, "Number of batteries required"),
                    str(
                        AiToolParams.ICON,
                        "Icon for the device type. Available icons: " + DeviceIcons.AvailableIcons.joinToString(", "),
                    ),
                ),
            )
            add(
                tool(
                    AiToolNames.RECORD_BATTERY_REPLACEMENT,
                    "Record a battery replacement event for a device. If the device does not exist, it will be created.",
                    str(AiToolParams.DEVICE_NAME, "Name of the device"),
                    str(AiToolParams.DATE, "Date of replacement (YYYY-MM-DD)"),
                    str(AiToolParams.DEVICE_TYPE, "Type of the device (optional, used if creating new device)", optional = true),
                    str(AiToolParams.BATTERY_TYPE, "Type of battery used (optional)", optional = true),
                    str(AiToolParams.LOCATION, "Location of the device (optional)", optional = true),
                ),
            )
            add(
                tool(
                    AiToolNames.UPDATE_DEVICE,
                    "Update an existing device's properties",
                    str(AiToolParams.ID, "Device ID from the inventory context"),
                    str(AiToolParams.NEW_NAME, "New name for the device (optional)", optional = true),
                    str(AiToolParams.NEW_LOCATION, "New location (optional)", optional = true),
                    str(AiToolParams.NEW_TYPE, "New device type name (optional)", optional = true),
                ),
            )
            add(
                tool(
                    AiToolNames.DELETE_DEVICE,
                    "Delete a device and all its battery events",
                    str(AiToolParams.ID, "Device ID from the inventory context"),
                ),
            )
            add(
                tool(
                    AiToolNames.UPDATE_DEVICE_TYPE,
                    "Update an existing device type's properties",
                    str(AiToolParams.ID, "Device type ID from the inventory context"),
                    str(AiToolParams.NEW_NAME, "New name for the device type (optional)", optional = true),
                    str(AiToolParams.NEW_BATTERY_TYPE, "New battery type (optional)", optional = true),
                    int(AiToolParams.NEW_BATTERY_QUANTITY, "New battery quantity (optional)", optional = true),
                    str(AiToolParams.NEW_ICON, "New icon (optional)", optional = true),
                ),
            )
            add(
                tool(
                    AiToolNames.DELETE_DEVICE_TYPE,
                    "Delete a device type. Fails if devices still use it.",
                    str(AiToolParams.ID, "Device type ID from the inventory context"),
                ),
            )
            add(
                tool(
                    AiToolNames.UPDATE_BATTERY_EVENT,
                    "Update a battery replacement event",
                    str(AiToolParams.ID, "Battery event ID from the inventory context"),
                    str(AiToolParams.NEW_DATE, "New date (YYYY-MM-DD) (optional)", optional = true),
                    str(AiToolParams.NOTES, "Notes for the event (optional)", optional = true),
                ),
            )
            add(
                tool(
                    AiToolNames.DELETE_BATTERY_EVENT,
                    "Delete a battery replacement event",
                    str(AiToolParams.ID, "Battery event ID from the inventory context"),
                ),
            )
        }
}
