package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceIcons
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiChangeKind
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * The assistant's tools that change data. Nothing here runs on the model's say-so: [preview] works
 * out what a change would do without writing, the person approves it on a card, and only then does
 * `AiChatSession` call [run]. Read-only tools are a separate class ([DeviceReadTools]).
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
) {
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
                else -> ToolOutcome.error("Unknown tool '$name'")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ToolOutcome.error("$name failed: ${e.message}")
        }

    /** What [run] would do with these arguments, right now. Writes nothing. */
    suspend fun preview(
        name: String,
        args: Map<String, Any?>,
    ): ToolPreview =
        try {
            when (name) {
                AiToolNames.ADD_DEVICE -> previewAddDevice(args)
                AiToolNames.ADD_DEVICE_TYPE -> previewAddDeviceType(args)
                AiToolNames.RECORD_BATTERY_REPLACEMENT -> previewRecord(args)
                AiToolNames.UPDATE_DEVICE -> previewUpdateDevice(args)
                AiToolNames.DELETE_DEVICE -> previewDeleteDevice(args)
                AiToolNames.UPDATE_DEVICE_TYPE -> previewUpdateDeviceType(args)
                AiToolNames.DELETE_DEVICE_TYPE -> previewDeleteDeviceType(args)
                AiToolNames.UPDATE_BATTERY_EVENT -> previewUpdateEvent(args)
                AiToolNames.DELETE_BATTERY_EVENT -> previewDeleteEvent(args)
                AiToolNames.MARK_NEEDS_BATTERY -> previewMark(args)
                else -> ToolPreview.Refused("Error: Unknown tool '$name'")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ToolPreview.Refused("Error: $name failed: ${e.message}")
        }

    /** The current state of the row [preview] named as this change's basis, to compare at apply time. */
    suspend fun currentBasis(
        name: String,
        args: Map<String, Any?>,
    ): String? {
        val id = str(args, AiToolParams.ID) ?: return null
        return when (name) {
            AiToolNames.UPDATE_DEVICE, AiToolNames.DELETE_DEVICE, AiToolNames.MARK_NEEDS_BATTERY -> {
                deviceRepository.getDeviceById(id).first()?.basis()
            }

            AiToolNames.UPDATE_DEVICE_TYPE, AiToolNames.DELETE_DEVICE_TYPE -> {
                deviceRepository.getDeviceTypeById(id).first()?.toString()
            }

            AiToolNames.UPDATE_BATTERY_EVENT, AiToolNames.DELETE_BATTERY_EVENT -> {
                deviceRepository.getEventById(id).first()?.toString()
            }

            else -> {
                null
            }
        }
    }

    private fun Device.basis(): String = copy(lastUpdated = Instant.fromEpochMilliseconds(0)).toString()

    // ─── previews ───────────────────────────────────────────────────────────────

    private suspend fun previewAddDevice(args: Map<String, Any?>): ToolPreview {
        val name = str(args, AiToolParams.NAME) ?: return ToolPreview.Refused("Error: Missing name")
        findDevice(name)?.let {
            return ToolPreview.Refused("Error: A device named '${it.name}' already exists (id:${it.id}). Update it instead.")
        }
        val type = str(args, AiToolParams.TYPE)
        val extras = listOfNotNull(type, str(args, AiToolParams.LOCATION))
        return ToolPreview.Ready(
            kind = AiChangeKind.ADD,
            summary = "Add $name" + if (extras.isEmpty()) "" else " (${extras.joinToString(", ")})",
            warning = newTypeWarning(type),
        )
    }

    private suspend fun previewAddDeviceType(args: Map<String, Any?>): ToolPreview {
        val name = str(args, AiToolParams.NAME) ?: return ToolPreview.Refused("Error: Missing name")
        findType(name)?.let {
            return ToolPreview.Refused("Error: A type named '${it.name}' already exists (id:${it.id}). Update it instead.")
        }
        val battery = str(args, AiToolParams.BATTERY_TYPE) ?: "AA"
        val quantity = parseInt(args[AiToolParams.BATTERY_QUANTITY])?.coerceAtLeast(1) ?: 1
        return ToolPreview.Ready(AiChangeKind.ADD, "Add the type $name ($quantity × $battery)")
    }

    private suspend fun previewRecord(args: Map<String, Any?>): ToolPreview {
        val deviceName = str(args, AiToolParams.DEVICE_NAME) ?: return ToolPreview.Refused("Error: Missing deviceName")
        val date = str(args, AiToolParams.DATE)?.let { parseDate(it) ?: return ToolPreview.Refused("Error: date must be YYYY-MM-DD") }
            ?: Clock.System.now()
        if (date > Clock.System.now() + 1.days) return ToolPreview.Refused("Error: date is in the future")
        val device = findDevice(deviceName)
        val note = str(args, AiToolParams.NOTES)?.let { " — $it" }.orEmpty()
        return ToolPreview.Ready(
            kind = AiChangeKind.RECORD,
            summary = "Record a battery change for ${device?.name ?: deviceName} on ${dateOf(date)}$note",
            warning = if (device == null) "There's no device called $deviceName yet, so this also adds one." else null,
        )
    }

    private suspend fun previewUpdateDevice(args: Map<String, Any?>): ToolPreview {
        val id = str(args, AiToolParams.ID) ?: return ToolPreview.Refused("Error: Missing id")
        val device = deviceRepository.getDeviceById(id).first()
            ?: return ToolPreview.Refused("Error: Device not found with id '$id'")
        val newName = str(args, AiToolParams.NEW_NAME)
        val newLocation = str(args, AiToolParams.NEW_LOCATION)
        val newType = str(args, AiToolParams.NEW_TYPE)
        val currentType = deviceRepository.getDeviceTypeById(device.typeId).first()?.name
        val changes = buildList {
            if (newName != null && newName != device.name) add("name ${device.name} → $newName")
            if (newLocation != null && newLocation != device.location) add("location ${device.location ?: "none"} → $newLocation")
            if (newType != null && !newType.equals(currentType, ignoreCase = true)) add("type ${currentType ?: "none"} → $newType")
        }
        if (changes.isEmpty()) return ToolPreview.Refused("Success: Nothing to change on '${device.name}'")
        return ToolPreview.Ready(AiChangeKind.UPDATE, "Update ${device.name}: ${changes.joinToString(", ")}", newTypeWarning(newType), id, device.basis())
    }

    private suspend fun previewDeleteDevice(args: Map<String, Any?>): ToolPreview {
        val id = str(args, AiToolParams.ID) ?: return ToolPreview.Refused("Error: Missing id")
        val device = deviceRepository.getDeviceById(id).first()
            ?: return ToolPreview.Refused("Error: Device not found with id '$id'")
        val events = deviceRepository.getEventsForDevice(id).first()
        return ToolPreview.Ready(
            kind = AiChangeKind.DELETE,
            summary = "Delete ${device.name}${eventsClause(events.size)}",
            warning = if (device.imageEtag != null) "Undo can't bring back its photo." else null,
            target = id,
            basis = device.basis(),
        )
    }

    private suspend fun previewUpdateDeviceType(args: Map<String, Any?>): ToolPreview {
        val id = str(args, AiToolParams.ID) ?: return ToolPreview.Refused("Error: Missing id")
        val type = deviceRepository.getDeviceTypeById(id).first()
            ?: return ToolPreview.Refused("Error: Device type not found with id '$id'")
        val updated = updatedType(type, args)
        val changes = typeChanges(type, updated)
        if (changes.isEmpty()) return ToolPreview.Refused("Success: Nothing to change on type '${type.name}'")
        return ToolPreview.Ready(AiChangeKind.UPDATE, "Update the type ${type.name}: ${changes.joinToString(", ")}", target = id, basis = type.toString())
    }

    private suspend fun previewDeleteDeviceType(args: Map<String, Any?>): ToolPreview {
        val id = str(args, AiToolParams.ID) ?: return ToolPreview.Refused("Error: Missing id")
        val type = deviceRepository.getDeviceTypeById(id).first()
            ?: return ToolPreview.Refused("Error: Device type not found with id '$id'")
        val inUse = deviceRepository.getAllDevices().first().count { it.typeId == id }
        if (inUse > 0) return ToolPreview.Refused("Error: Cannot delete type '${type.name}' — $inUse device(s) still use it. Reassign them first.")
        return ToolPreview.Ready(AiChangeKind.DELETE, "Delete the type ${type.name}", target = id, basis = type.toString())
    }

    private suspend fun previewUpdateEvent(args: Map<String, Any?>): ToolPreview {
        val id = str(args, AiToolParams.ID) ?: return ToolPreview.Refused("Error: Missing id")
        val event = deviceRepository.getEventById(id).first()
            ?: return ToolPreview.Refused("Error: Battery event not found with id '$id'")
        val newDate = str(args, AiToolParams.NEW_DATE)?.let { parseDate(it) ?: return ToolPreview.Refused("Error: newDate must be YYYY-MM-DD") }
        val notes = str(args, AiToolParams.NOTES)
        val changes = buildList {
            if (newDate != null && newDate != event.date) add("date ${dateOf(event.date)} → ${dateOf(newDate)}")
            if (notes != null && notes != event.notes) add("note → $notes")
        }
        if (changes.isEmpty()) return ToolPreview.Refused("Success: Nothing to change on that battery change")
        val name = deviceRepository.getDeviceById(event.deviceId).first()?.name ?: "a device"
        return ToolPreview.Ready(AiChangeKind.UPDATE, "Update the battery change for $name: ${changes.joinToString(", ")}", target = id, basis = event.toString())
    }

    private suspend fun previewDeleteEvent(args: Map<String, Any?>): ToolPreview {
        val id = str(args, AiToolParams.ID) ?: return ToolPreview.Refused("Error: Missing id")
        val event = deviceRepository.getEventById(id).first()
            ?: return ToolPreview.Refused("Error: Battery event not found with id '$id'")
        val name = deviceRepository.getDeviceById(event.deviceId).first()?.name ?: "a device"
        return ToolPreview.Ready(AiChangeKind.DELETE, "Delete the battery change for $name on ${dateOf(event.date)}", target = id, basis = event.toString())
    }

    private suspend fun previewMark(args: Map<String, Any?>): ToolPreview {
        val id = str(args, AiToolParams.ID) ?: return ToolPreview.Refused("Error: Missing id")
        val device = deviceRepository.getDeviceById(id).first()
            ?: return ToolPreview.Refused("Error: Device not found with id '$id'")
        val wanted = parseBool(args[AiToolParams.NEEDS_BATTERY]) ?: true
        if (isMarked(id) == wanted) return ToolPreview.Refused("Success: '${device.name}' is already ${if (wanted) "marked" else "unmarked"}")
        val summary = if (wanted) "Mark ${device.name} as needing a battery" else "Clear the needs-a-battery mark on ${device.name}"
        return ToolPreview.Ready(AiChangeKind.MARK, summary, target = id, basis = device.basis())
    }

    private suspend fun newTypeWarning(type: String?): String? = if (type != null && findType(type) == null) "This also creates the type $type." else null

    private fun updatedType(
        type: DeviceType,
        args: Map<String, Any?>,
    ): DeviceType =
        type.copy(
            name = str(args, AiToolParams.NEW_NAME) ?: type.name,
            batteryType = str(args, AiToolParams.NEW_BATTERY_TYPE) ?: type.batteryType,
            batteryQuantity = parseInt(args[AiToolParams.NEW_BATTERY_QUANTITY])?.coerceAtLeast(1) ?: type.batteryQuantity,
            defaultIcon = str(args, AiToolParams.NEW_ICON)?.let(::icon) ?: type.defaultIcon,
        )

    private fun typeChanges(
        prior: DeviceType,
        updated: DeviceType,
    ): List<String> =
        buildList {
            if (updated.name != prior.name) add("name → ${updated.name}")
            if (updated.batteryType != prior.batteryType) add("battery → ${updated.batteryType}")
            if (updated.batteryQuantity != prior.batteryQuantity) add("quantity → ${updated.batteryQuantity}")
            if (updated.defaultIcon != prior.defaultIcon) add("icon → ${updated.defaultIcon}")
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
        return ToolOutcome(
            modelResult = "Success: Added device '${device.name}' (id:${device.id})" + (createdType?.let { ", created type '${it.name}'" } ?: ""),
            undo = {
                if (deviceRepository.getEventsForDevice(device.id).first().isNotEmpty()) {
                    gone("${device.name} has battery changes recorded since, so adding it can't be undone. Delete it instead.")
                } else {
                    deviceRepository.deleteDevice(device.id).flatMap { removeIfUnused(createdType) }
                }
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
            // Only the fields this change touched: anything edited since must survive the undo.
            undo = {
                val current = deviceRepository.getDeviceById(id).first()
                if (current == null) {
                    gone("${prior.name} no longer exists.")
                } else if ((updated.name != prior.name && current.name != updated.name) ||
                    (updated.location != prior.location && current.location != updated.location) ||
                    (updated.typeId != prior.typeId && current.typeId != updated.typeId)
                ) {
                    gone("${prior.name} was changed again since, so this wasn't undone.")
                } else {
                    deviceRepository
                        .updateDevice(
                            current.copy(
                                name = if (updated.name != prior.name) prior.name else current.name,
                                location = if (updated.location != prior.location) prior.location else current.location,
                                typeId = if (updated.typeId != prior.typeId) prior.typeId else current.typeId,
                                lastUpdated = Clock.System.now(),
                            ),
                        ).flatMap { removeIfUnused(createdType) }
                }
            },
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
            undo = {
                deviceRepository
                    .addDevice(device.copy(imageEtag = null)) // The server removed the photo with the device.
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
            undo = {
                val current = deviceRepository.getDeviceTypeById(id).first()
                if (current == null) {
                    gone("The type ${prior.name} no longer exists.")
                } else if ((updated.name != prior.name && current.name != updated.name) ||
                    (updated.batteryType != prior.batteryType && current.batteryType != updated.batteryType) ||
                    (updated.batteryQuantity != prior.batteryQuantity && current.batteryQuantity != updated.batteryQuantity) ||
                    (updated.defaultIcon != prior.defaultIcon && current.defaultIcon != updated.defaultIcon)
                ) {
                    gone("The type ${prior.name} was changed again since, so this wasn't undone.")
                } else {
                    deviceRepository.updateDeviceType(
                        current.copy(
                            name = if (updated.name != prior.name) prior.name else current.name,
                            batteryType = if (updated.batteryType != prior.batteryType) prior.batteryType else current.batteryType,
                            batteryQuantity = if (updated.batteryQuantity != prior.batteryQuantity) prior.batteryQuantity else current.batteryQuantity,
                            defaultIcon = if (updated.defaultIcon != prior.defaultIcon) prior.defaultIcon else current.defaultIcon,
                        ),
                    )
                }
            },
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
        val device = findDevice(deviceName) ?: run {
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
            undo = {
                deviceRepository
                    .deleteEvent(event.id)
                    .flatMap {
                        val remaining = deviceRepository.getEventsForDevice(device.id).first()
                        when {
                            createdDevice != null && remaining.isEmpty() -> {
                                deviceRepository.deleteDevice(device.id).flatMap { removeIfUnused(createdType) }
                            }

                            else -> {
                                restoreLastReplaced(device.id, remaining, fallback = device.batteryLastReplaced)
                            }
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
            undo = {
                val current = deviceRepository.getEventById(id).first()
                if (current == null) {
                    gone("That battery change no longer exists.")
                } else if ((updated.date != prior.date && current.date != updated.date) || (updated.notes != prior.notes && current.notes != updated.notes)) {
                    gone("That battery change was edited again since, so this wasn't undone.")
                } else {
                    deviceRepository
                        .updateEvent(
                            current.copy(
                                date = if (updated.date != prior.date) prior.date else current.date,
                                notes = if (updated.notes != prior.notes) prior.notes else current.notes,
                            ),
                        ).flatMap { updateDeviceLastReplacedUseCase(prior.deviceId).map { } }
                }
            },
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
            undo = { needsBatteryRepository.setNeedsBattery(id, prior) },
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

    /**
     * Put a device's last-changed date back after an undo: the newest event that's left, or
     * [fallback] with none, so a date that never came from an event isn't wiped to "never".
     * Works from the current row, so a later edit to the device survives.
     */
    private suspend fun restoreLastReplaced(
        deviceId: String,
        remaining: List<BatteryEvent>,
        fallback: Instant,
    ): Result<Unit, DataError> {
        val current = deviceRepository.getDeviceById(deviceId).first() ?: return Result.Success(Unit)
        val date = remaining.maxOfOrNull { it.date } ?: fallback
        return deviceRepository.updateDevice(current.copy(batteryLastReplaced = date, lastUpdated = Clock.System.now()))
    }

    private fun gone(message: String): Result<Unit, DataError> = Result.Error(DataError.Unknown(message, null))

    private suspend fun removeIfUnused(type: DeviceType?): Result<Unit, DataError> {
        if (type == null) return Result.Success(Unit)
        val inUse = deviceRepository.getAllDevices().first().any { it.typeId == type.id }
        return if (inUse) Result.Success(Unit) else deviceRepository.deleteDeviceType(type.id)
    }

    private suspend fun isMarked(deviceId: String): Boolean = deviceId in needsBatteryRepository.getNeedsBatteryMarks().first()

    private fun batteries(type: DeviceType) = "${type.batteryQuantity} × ${type.batteryType}"

    private fun eventsClause(count: Int) =
        when (count) {
            0 -> ""
            1 -> " and its 1 battery change"
            else -> " and its $count battery changes"
        }

    private fun icon(name: String?): String = name?.takeIf { it in DeviceIcons.AvailableIcons } ?: "default"

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
