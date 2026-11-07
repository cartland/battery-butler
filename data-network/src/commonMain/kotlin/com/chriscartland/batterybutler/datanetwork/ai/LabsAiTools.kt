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
        You are Battery Butler's assistant, inside the Battery Butler app: a home inventory of battery-powered devices, what batteries each takes, and when each was last changed.

        HOW YOU'RE BEING USED: You're chatting with the owner of this inventory, on their phone. They see your reply, plus a row for each change the app made on your behalf, written by the app from what it stored, and a card whenever a deletion is waiting for their answer. They do not see tool calls or results, so say what you found or did in your reply.

        WHAT YOU KNOW: The "Right now" section below is the whole inventory as it is at this moment, including the effects of anything you just did. Every item has an id; tools take ids. Match what the person says ("the hall alarm", "the big TV remote") to the right item here, and never invent an id. The recent changes shown are the newest; getDeviceHistory gives one device's full history.

        WHAT YOU CAN DO: Add devices and types; record a battery change for a device (today, or on the date given), creating the device if it's new; edit any field; mark a device as needing a battery, or clear that mark; look up history; and delete devices, types and battery changes.

        DELETIONS: You never delete anything yourself. A delete tool hands the decision to the person: the app shows them a card, and the tool returns a note saying so. When that happens, tell them in one sentence what the card will do and stop. Don't call the tool again, and don't say it was deleted.

        OUTCOMES YOU CAN'T SEE: The person can confirm or decline a card and can undo any change from the app. When they do, the next message starts with a bracketed note like [The user undid this: …]. Trust those notes over your own memory: a change is only real while nothing says otherwise.

        WHEN TO ACT: If a request is clearly an instruction ("I changed the hall alarm today"), do it. If it could be a question or an instruction ("the hall alarm?"), answer the question and offer the action in one short sentence. If several items could match, ask which. Only change the fields the person asked to change. When they paste a list or table, skip headers and blank lines and process each real line.

        ANSWERING: Reason over the inventory yourself: count, compare dates, add up batteries by type, and say which devices are due or overdue and why. Dates in tools are YYYY-MM-DD; today's date is in the "Right now" section. Reply in plain text, short, with no markdown; use "- " for a list. Reply in the person's language.
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

    private fun bool(
        name: String,
        description: String,
        optional: Boolean = false,
    ) = Param(name, "boolean", description, optional)

    val declarations: JsonArray =
        buildJsonArray {
            add(
                tool(
                    AiToolNames.ADD_DEVICE,
                    "Add a new device to the inventory. Fails if a device with that name exists.",
                    str(AiToolParams.NAME, "Name of the device"),
                    str(AiToolParams.TYPE, "Type of the device, created if new (optional)", optional = true),
                    str(AiToolParams.LOCATION, "Where the device is (optional)", optional = true),
                ),
            )
            add(
                tool(
                    AiToolNames.ADD_DEVICE_TYPE,
                    "Add a kind of device, with the batteries it takes. Fails if a type with that name exists.",
                    str(AiToolParams.NAME, "Name of the device type"),
                    str(AiToolParams.BATTERY_TYPE, "Battery it takes (e.g. AA, 9V, CR2032)"),
                    int(AiToolParams.BATTERY_QUANTITY, "How many batteries it takes"),
                    str(
                        AiToolParams.ICON,
                        "Icon (optional). One of: " + DeviceIcons.AvailableIcons.joinToString(", "),
                        optional = true,
                    ),
                ),
            )
            add(
                tool(
                    AiToolNames.RECORD_BATTERY_REPLACEMENT,
                    "Record that a device's batteries were changed. Creates the device if there is none with that name. " +
                        "Clears the device's needs-a-battery mark.",
                    str(AiToolParams.DEVICE_NAME, "Name of the device"),
                    str(AiToolParams.DATE, "Date of the change, YYYY-MM-DD (optional; today if omitted)", optional = true),
                    str(AiToolParams.NOTES, "A note about the change (optional)", optional = true),
                    str(AiToolParams.BATTERY_TYPE, "Battery used, if not the type's usual one (optional)", optional = true),
                    str(AiToolParams.DEVICE_TYPE, "Type of the device, used only if creating it (optional)", optional = true),
                    str(AiToolParams.LOCATION, "Where the device is, used only if creating it (optional)", optional = true),
                ),
            )
            add(
                tool(
                    AiToolNames.UPDATE_DEVICE,
                    "Change a device's name, location or type. Only the fields given change.",
                    str(AiToolParams.ID, "Device id from the inventory"),
                    str(AiToolParams.NEW_NAME, "New name (optional)", optional = true),
                    str(AiToolParams.NEW_LOCATION, "New location (optional)", optional = true),
                    str(AiToolParams.NEW_TYPE, "New type by name, created if new (optional)", optional = true),
                ),
            )
            add(
                tool(
                    AiToolNames.DELETE_DEVICE,
                    "Ask the user to confirm deleting a device and all its battery changes. The app asks; you don't.",
                    str(AiToolParams.ID, "Device id from the inventory"),
                ),
            )
            add(
                tool(
                    AiToolNames.UPDATE_DEVICE_TYPE,
                    "Change a device type. Only the fields given change.",
                    str(AiToolParams.ID, "Device type id from the inventory"),
                    str(AiToolParams.NEW_NAME, "New name (optional)", optional = true),
                    str(AiToolParams.NEW_BATTERY_TYPE, "New battery (optional)", optional = true),
                    int(AiToolParams.NEW_BATTERY_QUANTITY, "New battery quantity (optional)", optional = true),
                    str(AiToolParams.NEW_ICON, "New icon (optional)", optional = true),
                ),
            )
            add(
                tool(
                    AiToolNames.DELETE_DEVICE_TYPE,
                    "Ask the user to confirm deleting a device type. Fails if devices still use it.",
                    str(AiToolParams.ID, "Device type id from the inventory"),
                ),
            )
            add(
                tool(
                    AiToolNames.UPDATE_BATTERY_EVENT,
                    "Change the date or note of a recorded battery change.",
                    str(AiToolParams.ID, "Battery change id from the inventory or getDeviceHistory"),
                    str(AiToolParams.NEW_DATE, "New date, YYYY-MM-DD (optional)", optional = true),
                    str(AiToolParams.NOTES, "New note (optional)", optional = true),
                ),
            )
            add(
                tool(
                    AiToolNames.DELETE_BATTERY_EVENT,
                    "Ask the user to confirm deleting a recorded battery change.",
                    str(AiToolParams.ID, "Battery change id from the inventory or getDeviceHistory"),
                ),
            )
            add(
                tool(
                    AiToolNames.MARK_NEEDS_BATTERY,
                    "Mark a device as needing a new battery (a to-do, cleared when a change is recorded), or clear the mark.",
                    str(AiToolParams.ID, "Device id from the inventory"),
                    bool(AiToolParams.NEEDS_BATTERY, "true to mark, false to clear (optional; true if omitted)", optional = true),
                ),
            )
            add(
                tool(
                    AiToolNames.GET_DEVICE_HISTORY,
                    "Every recorded battery change for one device, newest first, with ids.",
                    str(AiToolParams.ID, "Device id from the inventory"),
                ),
            )
        }
}
