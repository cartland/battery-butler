package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceIcons
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiAction
import com.chriscartland.batterybutler.domain.model.ai.AiActionKind
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.flatMap
import com.chriscartland.batterybutler.domain.model.getOrElse
import com.chriscartland.batterybutler.domain.model.map
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The assistant's tools, run against the local database.
 *
 * Every tool answers with a [ToolOutcome]: a result for the model, and for every write an
 * [AiAction] the person sees plus the undo that takes it back. The summaries are written here,
 * from what was actually stored, never from the model's own account of it.
 *
 * Deletions are the one kind of change the app does not make on the model's say-so. The chat
 * session asks the person first, using [describe]; [run] is only called once they've agreed.
 * The three delete tools therefore have no guard of their own.
 *
 * Rules the rest of the app keeps are kept here too: a recorded replacement moves the device's
 * last-changed date forward and clears its "needs a battery" mark (`AddBatteryEventUseCase`);
 * editing or deleting an event recomputes that date (`UpdateDeviceLastReplacedUseCase`); a type
 * in use can't be deleted.
 */
@Inject
class DeviceToolHandler(
    private val deviceRepository: DeviceRepository,
    private val needsBatteryRepository: NeedsBatteryRepository,
    private val updateDeviceLastReplacedUseCase: UpdateDeviceLastReplacedUseCase,
) : ToolHandler {
    override suspend fun execute(
        name: String,
        args: Map<String, Any?>,
    ): String = run(name, args).modelResult

    suspend fun run(
        name: String,
        args: Map<String, Any?>,
    ): ToolOutcome =
        try {
            when (name) {
                AiToolNames.ADD_DEVICE -> addDevice(args)
                AiToolNames.ADD_DEVICE_TYPE -> addDeviceType(args)
                AiToolNames.RECORD_BATTERY_REPLACEMENT -> recordBatteryReplacement(args)
                AiToolNames.UPDATE_DEVICE -> updateDevice(args)
                AiToolNames.DELETE_DEVICE -> deleteDevice(args)
                AiToolNames.UPDATE_DEVICE_TYPE -> updateDeviceType(args)
                AiToolNames.DELETE_DEVICE_TYPE -> deleteDeviceType(args)
                AiToolNames.UPDATE_BATTERY_EVENT -> updateBatteryEvent(args)
                AiToolNames.DELETE_BATTERY_EVENT -> deleteBatteryEvent(args)
                AiToolNames.MARK_NEEDS_BATTERY -> markNeedsBattery(args)
                AiToolNames.GET_DEVICE_HISTORY -> getDeviceHistory(args)
                else -> ToolOutcome.error("Unknown tool '$name'")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ToolOutcome.error("$name failed: ${e.message}")
        }

    /**
     * What a deletion would do, for the confirmation card. Null when the tool isn't a deletion
     * or its target doesn't exist, in which case [run] answers the model with the usual error.
     */
    suspend fun describe(
        name: String,
        args: Map<String, Any?>,
    ): DeletionPreview? {
        val id = str(args, AiToolParams.ID) ?: return null
        return when (name) {
            AiToolNames.DELETE_DEVICE -> {
                val device = deviceRepository.getDeviceById(id).first() ?: return null
                val events = deviceRepository.getEventsForDevice(id).first()
                DeletionPreview(
                    summary = "Delete ${device.name}${eventsClause(events.size)}",
                    consequence = if (device.imageEtag != null || device.imagePath != null) "Its photo is removed for good." else null,
                )
            }

            AiToolNames.DELETE_DEVICE_TYPE -> {
                val type = deviceRepository.getDeviceTypeById(id).first() ?: return null
                DeletionPreview("Delete the type ${type.name}", null)
            }

            AiToolNames.DELETE_BATTERY_EVENT -> {
                val event = deviceRepository.getEventById(id).first() ?: return null
                val name = deviceRepository.getDeviceById(event.deviceId).first()?.name ?: "a device"
                DeletionPreview("Delete the battery change for $name on ${dateOf(event.date)}", null)
            }

            else -> {
                null
            }
        }
    }

    // ─── devices ───────────────────────────────────────────────────────────────

    private suspend fun addDevice(args: Map<String, Any?>): ToolOutcome {
        val name = str(args, AiToolParams.NAME) ?: return ToolOutcome.error("Missing name")
        val existing = findDevice(name)
        if (existing != null) {
            return ToolOutcome.error("A device named '${existing.name}' already exists (id:${existing.id}). Update it instead.")
        }
        val (typeId, createdType) = resolveType(str(args, AiToolParams.TYPE))
        val device = Device(
            id = uuid4().toString(),
            name = name,
            typeId = typeId,
            batteryLastReplaced = Instant.fromEpochMilliseconds(0),
            lastUpdated = Clock.System.now(),
            location = str(args, AiToolParams.LOCATION),
        )
        deviceRepository.addDevice(device).getOrElse { return ToolOutcome.error(it.message) }
        val described = describeDevice(device)
        return ToolOutcome(
            modelResult = "Success: Added device '${device.name}' (id:${device.id})" + (createdType?.let { ", created type '${it.name}'" } ?: ""),
            action = action(AiActionKind.ADDED, "Added $described"),
            undo = {
                deviceRepository.deleteDevice(device.id).flatMap { removeIfUnused(createdType) }
            },
        )
    }

    private suspend fun updateDevice(args: Map<String, Any?>): ToolOutcome {
        val id = str(args, AiToolParams.ID) ?: return ToolOutcome.error("Missing id")
        val prior = deviceRepository.getDeviceById(id).first()
            ?: return ToolOutcome.error("Device not found with id '$id'")
        val newName = str(args, AiToolParams.NEW_NAME)
        val newLocation = str(args, AiToolParams.NEW_LOCATION)
        val newType = str(args, AiToolParams.NEW_TYPE)
        val (typeId, createdType) = if (newType != null) resolveType(newType) else prior.typeId to null

        val changes = buildList {
            if (newName != null && newName != prior.name) add("name → $newName")
            if (newLocation != null && newLocation != prior.location) add("location → $newLocation")
            if (typeId != prior.typeId) add("type → $newType")
        }
        if (changes.isEmpty()) return ToolOutcome("Success: Nothing to change on '${prior.name}'")

        val updated = prior.copy(
            name = newName ?: prior.name,
            location = newLocation ?: prior.location,
            typeId = typeId,
            lastUpdated = Clock.System.now(),
        )
        deviceRepository.updateDevice(updated).getOrElse { return ToolOutcome.error(it.message) }
        return ToolOutcome(
            modelResult = "Success: Updated device '${updated.name}' (${changes.joinToString(", ")})",
            action = action(AiActionKind.UPDATED, "Updated ${prior.name}: ${changes.joinToString(", ")}"),
            undo = { deviceRepository.updateDevice(prior).flatMap { removeIfUnused(createdType) } },
        )
    }

    private suspend fun deleteDevice(args: Map<String, Any?>): ToolOutcome {
        val id = str(args, AiToolParams.ID) ?: return ToolOutcome.error("Missing id")
        val device = deviceRepository.getDeviceById(id).first()
            ?: return ToolOutcome.error("Device not found with id '$id'")
        val events = deviceRepository.getEventsForDevice(id).first()
        val wasMarked = isMarked(id)

        for (event in events) {
            deviceRepository.deleteEvent(event.id).getOrElse { return ToolOutcome.error(it.message) }
        }
        deviceRepository.deleteDevice(id).getOrElse { return ToolOutcome.error(it.message) }
        // Same reason as DeleteDeviceUseCase: the mark has no foreign key, so it would outlive the device.
        needsBatteryRepository.setNeedsBattery(id, needsBattery = false)

        return ToolOutcome(
            modelResult = "Success: Deleted device '${device.name}' and ${events.size} battery event(s)",
            action = action(AiActionKind.DELETED, "Deleted ${device.name}${eventsClause(events.size)}"),
            undo = {
                deviceRepository
                    .addDevice(device)
                    .flatMap {
                        var result: Result<Unit, DataError> = Result.Success(Unit)
                        for (event in events) result = result.flatMap { deviceRepository.addEvent(event) }
                        result
                    }.flatMap { if (wasMarked) needsBatteryRepository.setNeedsBattery(id, true) else Result.Success(Unit) }
            },
        )
    }

    // ─── types ─────────────────────────────────────────────────────────────────

    private suspend fun addDeviceType(args: Map<String, Any?>): ToolOutcome {
        val name = str(args, AiToolParams.NAME) ?: return ToolOutcome.error("Missing name")
        findType(name)?.let {
            return ToolOutcome.error("A type named '${it.name}' already exists (id:${it.id}). Update it instead.")
        }
        val type = DeviceType(
            id = uuid4().toString(),
            name = name,
            defaultIcon = icon(str(args, AiToolParams.ICON)),
            batteryType = str(args, AiToolParams.BATTERY_TYPE) ?: "AA",
            batteryQuantity = parseInt(args[AiToolParams.BATTERY_QUANTITY])?.coerceAtLeast(1) ?: 1,
        )
        deviceRepository.addDeviceType(type).getOrElse { return ToolOutcome.error(it.message) }
        return ToolOutcome(
            modelResult = "Success: Added device type '${type.name}' (id:${type.id}, ${batteries(type)}, icon ${type.defaultIcon})",
            action = action(AiActionKind.ADDED, "Added type ${type.name} (${batteries(type)})"),
            undo = { deviceRepository.deleteDeviceType(type.id) },
        )
    }

    private suspend fun updateDeviceType(args: Map<String, Any?>): ToolOutcome {
        val id = str(args, AiToolParams.ID) ?: return ToolOutcome.error("Missing id")
        val prior = deviceRepository.getDeviceTypeById(id).first()
            ?: return ToolOutcome.error("Device type not found with id '$id'")
        val newName = str(args, AiToolParams.NEW_NAME)
        val newBatteryType = str(args, AiToolParams.NEW_BATTERY_TYPE)
        val newQuantity = parseInt(args[AiToolParams.NEW_BATTERY_QUANTITY])?.coerceAtLeast(1)
        val newIcon = str(args, AiToolParams.NEW_ICON)?.let(::icon)

        val updated = prior.copy(
            name = newName ?: prior.name,
            batteryType = newBatteryType ?: prior.batteryType,
            batteryQuantity = newQuantity ?: prior.batteryQuantity,
            defaultIcon = newIcon ?: prior.defaultIcon,
        )
        val changes = buildList {
            if (updated.name != prior.name) add("name → ${updated.name}")
            if (updated.batteryType != prior.batteryType) add("battery → ${updated.batteryType}")
            if (updated.batteryQuantity != prior.batteryQuantity) add("quantity → ${updated.batteryQuantity}")
            if (updated.defaultIcon != prior.defaultIcon) add("icon → ${updated.defaultIcon}")
        }
        if (changes.isEmpty()) return ToolOutcome("Success: Nothing to change on type '${prior.name}'")
        deviceRepository.updateDeviceType(updated).getOrElse { return ToolOutcome.error(it.message) }
        return ToolOutcome(
            modelResult = "Success: Updated device type '${updated.name}' (${changes.joinToString(", ")})",
            action = action(AiActionKind.UPDATED, "Updated type ${prior.name}: ${changes.joinToString(", ")}"),
            undo = { deviceRepository.updateDeviceType(prior) },
        )
    }

    private suspend fun deleteDeviceType(args: Map<String, Any?>): ToolOutcome {
        val id = str(args, AiToolParams.ID) ?: return ToolOutcome.error("Missing id")
        val type = deviceRepository.getDeviceTypeById(id).first()
            ?: return ToolOutcome.error("Device type not found with id '$id'")
        val inUse = deviceRepository.getAllDevices().first().count { it.typeId == id }
        if (inUse > 0) {
            return ToolOutcome.error("Cannot delete type '${type.name}' — $inUse device(s) still use it. Reassign them first.")
        }
        deviceRepository.deleteDeviceType(id).getOrElse { return ToolOutcome.error(it.message) }
        return ToolOutcome(
            modelResult = "Success: Deleted device type '${type.name}'",
            action = action(AiActionKind.DELETED, "Deleted the type ${type.name}"),
            undo = { deviceRepository.addDeviceType(type) },
        )
    }

    // ─── battery changes ───────────────────────────────────────────────────────

    private suspend fun recordBatteryReplacement(args: Map<String, Any?>): ToolOutcome {
        val deviceName = str(args, AiToolParams.DEVICE_NAME) ?: return ToolOutcome.error("Missing deviceName")
        val date = str(args, AiToolParams.DATE)?.let { parseDate(it) ?: return ToolOutcome.error("date must be YYYY-MM-DD") }
            ?: Clock.System.now()

        var createdType: DeviceType? = null
        var createdDevice: Device? = null
        val prior = findDevice(deviceName)
        val device = prior ?: run {
            val (typeId, newType) = resolveType(str(args, AiToolParams.DEVICE_TYPE))
            createdType = newType
            Device(
                id = uuid4().toString(),
                name = deviceName,
                typeId = typeId,
                batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                lastUpdated = Clock.System.now(),
                location = str(args, AiToolParams.LOCATION),
            ).also {
                deviceRepository.addDevice(it).getOrElse { e -> return ToolOutcome.error(e.message) }
                createdDevice = it
            }
        }
        val type = deviceRepository.getDeviceTypeById(device.typeId).first()
        val event = BatteryEvent(
            id = uuid4().toString(),
            deviceId = device.id,
            date = date,
            batteryType = str(args, AiToolParams.BATTERY_TYPE) ?: type?.batteryType,
            notes = str(args, AiToolParams.NOTES),
        )
        deviceRepository.addEvent(event).getOrElse { return ToolOutcome.error(it.message) }
        if (date > device.batteryLastReplaced) {
            deviceRepository
                .updateDevice(device.copy(batteryLastReplaced = date, lastUpdated = Clock.System.now()))
                .getOrElse { return ToolOutcome.error(it.message) }
        }
        val wasMarked = isMarked(device.id)
        needsBatteryRepository.setNeedsBattery(device.id, needsBattery = false)

        val day = dateOf(date)
        return ToolOutcome(
            modelResult = "Success: Recorded battery replacement for '${device.name}' on $day" +
                (createdDevice?.let { " (created the device, id:${it.id})" } ?: ""),
            action = action(
                AiActionKind.RECORDED,
                "Recorded a battery change for ${device.name} on $day" + (if (createdDevice != null) " (new device)" else ""),
            ),
            undo = {
                deviceRepository
                    .deleteEvent(event.id)
                    .flatMap {
                        when {
                            createdDevice != null -> deviceRepository.deleteDevice(device.id).flatMap { removeIfUnused(createdType) }
                            else -> deviceRepository.updateDevice(prior!!)
                        }
                    }.flatMap { if (wasMarked) needsBatteryRepository.setNeedsBattery(device.id, true) else Result.Success(Unit) }
            },
        )
    }

    private suspend fun updateBatteryEvent(args: Map<String, Any?>): ToolOutcome {
        val id = str(args, AiToolParams.ID) ?: return ToolOutcome.error("Missing id")
        val prior = deviceRepository.getEventById(id).first()
            ?: return ToolOutcome.error("Battery event not found with id '$id'")
        val newDate = str(args, AiToolParams.NEW_DATE)?.let { parseDate(it) ?: return ToolOutcome.error("newDate must be YYYY-MM-DD") }
        val notes = str(args, AiToolParams.NOTES)
        val updated = prior.copy(date = newDate ?: prior.date, notes = notes ?: prior.notes)
        val changes = buildList {
            if (updated.date != prior.date) add("date → ${dateOf(updated.date)}")
            if (updated.notes != prior.notes) add("notes → ${updated.notes}")
        }
        if (changes.isEmpty()) return ToolOutcome("Success: Nothing to change on that battery event")
        deviceRepository.updateEvent(updated).getOrElse { return ToolOutcome.error(it.message) }
        updateDeviceLastReplacedUseCase(prior.deviceId).getOrElse { return ToolOutcome.error(it.message) }
        val name = deviceRepository.getDeviceById(prior.deviceId).first()?.name ?: "a device"
        return ToolOutcome(
            modelResult = "Success: Updated battery event (${changes.joinToString(", ")})",
            action = action(AiActionKind.UPDATED, "Updated the battery change for $name: ${changes.joinToString(", ")}"),
            undo = { deviceRepository.updateEvent(prior).flatMap { updateDeviceLastReplacedUseCase(prior.deviceId).map { } } },
        )
    }

    private suspend fun deleteBatteryEvent(args: Map<String, Any?>): ToolOutcome {
        val id = str(args, AiToolParams.ID) ?: return ToolOutcome.error("Missing id")
        val event = deviceRepository.getEventById(id).first()
            ?: return ToolOutcome.error("Battery event not found with id '$id'")
        deviceRepository.deleteEvent(id).getOrElse { return ToolOutcome.error(it.message) }
        updateDeviceLastReplacedUseCase(event.deviceId).getOrElse { return ToolOutcome.error(it.message) }
        val name = deviceRepository.getDeviceById(event.deviceId).first()?.name ?: "a device"
        return ToolOutcome(
            modelResult = "Success: Deleted battery event for '$name'",
            action = action(AiActionKind.DELETED, "Deleted the battery change for $name on ${dateOf(event.date)}"),
            undo = { deviceRepository.addEvent(event).flatMap { updateDeviceLastReplacedUseCase(event.deviceId).map { } } },
        )
    }

    // ─── marks and reads ───────────────────────────────────────────────────────

    private suspend fun markNeedsBattery(args: Map<String, Any?>): ToolOutcome {
        val id = str(args, AiToolParams.ID) ?: return ToolOutcome.error("Missing id")
        val device = deviceRepository.getDeviceById(id).first()
            ?: return ToolOutcome.error("Device not found with id '$id'")
        val wanted = parseBool(args[AiToolParams.NEEDS_BATTERY]) ?: true
        val prior = isMarked(id)
        if (prior == wanted) {
            return ToolOutcome("Success: '${device.name}' is already ${if (wanted) "marked" else "unmarked"}")
        }
        needsBatteryRepository.setNeedsBattery(id, wanted).getOrElse { return ToolOutcome.error(it.message) }
        return ToolOutcome(
            modelResult = "Success: ${if (wanted) "Marked" else "Unmarked"} '${device.name}'",
            action = action(
                AiActionKind.MARKED,
                if (wanted) "Marked ${device.name} as needing a battery" else "Cleared the needs-battery mark on ${device.name}",
            ),
            undo = { needsBatteryRepository.setNeedsBattery(id, prior) },
        )
    }

    private suspend fun getDeviceHistory(args: Map<String, Any?>): ToolOutcome {
        val id = str(args, AiToolParams.ID) ?: return ToolOutcome.error("Missing id")
        val device = deviceRepository.getDeviceById(id).first()
            ?: return ToolOutcome.error("Device not found with id '$id'")
        val events = deviceRepository.getEventsForDevice(id).first().sortedByDescending { it.date }
        val lines = events.joinToString("\n") { e ->
            "- id:${e.id} | ${dateOf(e.date)}" + (e.batteryType?.let { " | $it" } ?: "") + (e.notes?.let { " | $it" } ?: "")
        }
        return ToolOutcome(
            modelResult = "History of '${device.name}' (${events.size} battery change(s), newest first):\n" + lines.ifEmpty { "(none)" },
            action = action(AiActionKind.LOOKED_UP, "Looked up the history of ${device.name}", undoable = false),
        )
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private suspend fun findDevice(name: String): Device? = deviceRepository.getAllDevices().first().find { it.name.equals(name, ignoreCase = true) }

    private suspend fun findType(name: String): DeviceType? = deviceRepository.getAllDeviceTypes().first().find { it.name.equals(name, ignoreCase = true) }

    /**
     * The id of the type called [name], creating it if there isn't one. A blank name means the
     * app's own catch-all type, as the add-device screen does. The second value is the type this
     * call created, if any, so an undo can take it away again.
     */
    private suspend fun resolveType(name: String?): Pair<String, DeviceType?> {
        if (name == null) return DEFAULT_TYPE_ID to null
        findType(name)?.let { return it.id to null }
        val type = DeviceType(id = uuid4().toString(), name = name, defaultIcon = "default")
        deviceRepository.addDeviceType(type).getOrElse { return DEFAULT_TYPE_ID to null }
        return type.id to type
    }

    private suspend fun removeIfUnused(type: DeviceType?): Result<Unit, DataError> {
        if (type == null) return Result.Success(Unit)
        val inUse = deviceRepository.getAllDevices().first().any { it.typeId == type.id }
        return if (inUse) Result.Success(Unit) else deviceRepository.deleteDeviceType(type.id)
    }

    private suspend fun isMarked(deviceId: String): Boolean = deviceId in needsBatteryRepository.getNeedsBatteryMarks().first()

    private suspend fun describeDevice(device: Device): String {
        val type = deviceRepository.getDeviceTypeById(device.typeId).first()
        val extras = listOfNotNull(type?.name?.takeIf { device.typeId != DEFAULT_TYPE_ID }, device.location)
        return device.name + if (extras.isEmpty()) "" else " (${extras.joinToString(", ")})"
    }

    private fun batteries(type: DeviceType) = "${type.batteryQuantity} × ${type.batteryType}"

    private fun eventsClause(count: Int) =
        when (count) {
            0 -> ""
            1 -> " and its 1 battery change"
            else -> " and its $count battery changes"
        }

    private fun icon(name: String?): String = name?.takeIf { it in DeviceIcons.AvailableIcons } ?: "default"

    private fun action(
        kind: AiActionKind,
        summary: String,
        undoable: Boolean = true,
    ) = AiAction(id = "act_${uuid4()}", kind = kind, summary = summary, undoable = undoable)

    private fun str(
        args: Map<String, Any?>,
        key: String,
    ): String? = (args[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }

    private fun parseInt(value: Any?): Int? =
        when (value) {
            is Int -> value
            is Number -> value.toInt()
            is String -> value.trim().toIntOrNull()
            else -> null
        }

    private fun parseBool(value: Any?): Boolean? =
        when (value) {
            is Boolean -> value

            is String -> value.trim().lowercase().let {
                if (it == "true") {
                    true
                } else if (it == "false") {
                    false
                } else {
                    null
                }
            }

            else -> null
        }

    private fun parseDate(text: String): Instant? =
        runCatching { LocalDate.parse(text.trim()) }.getOrNull()?.let {
            Instant.fromEpochMilliseconds(it.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds())
        }

    private fun dateOf(instant: Instant): String =
        kotlinx.datetime.Instant
            .fromEpochMilliseconds(instant.toEpochMilliseconds())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()

    private companion object {
        /** The app's catch-all type, as `FindOrCreateDeviceTypeUseCase` names it. */
        const val DEFAULT_TYPE_ID = "default_type"
    }
}
