package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The assistant's read-only tools. They run without asking, because this class has no way to
 * change anything: it holds the repositories but only ever calls their reads. A model that's weak
 * at arithmetic or at reading a long inventory can ask for the answer already worked out.
 */
@Inject
class DeviceReadTools(
    private val deviceRepository: DeviceRepository,
    private val needsBatteryRepository: NeedsBatteryRepository,
) {
    suspend fun run(
        name: String,
        args: Map<String, Any?>,
        now: Instant = Clock.System.now(),
    ): ReadResult =
        when (name) {
            AiToolNames.LIST_DEVICES -> listDevices(args, now)
            AiToolNames.GET_DEVICE -> getDevice(args, now)
            AiToolNames.GET_DEVICE_HISTORY -> getDeviceHistory(args)
            AiToolNames.LIST_BATTERY_CHANGES -> listBatteryChanges(args)
            AiToolNames.GET_BATTERIES_TO_BUY -> getBatteriesToBuy(now)
            else -> ReadResult("Error: Unknown tool '$name'")
        }

    private suspend fun listDevices(
        args: Map<String, Any?>,
        now: Instant,
    ): ReadResult {
        val wantedStatus = str(args, AiToolParams.STATUS)?.lowercase()
        val search = str(args, AiToolParams.SEARCH)?.lowercase()
        val marks = marks()
        val types = deviceRepository.getAllDeviceTypes().first().associateBy { it.id }
        val found = deviceRepository.getAllDevices().first().sortedBy { it.name.lowercase() }.filter { d ->
            val status = status(d, now)
            val matchesStatus = when (wantedStatus) {
                null -> true
                "marked" -> d.id in marks
                else -> status == wantedStatus
            }
            val type = types[d.typeId]
            val matchesSearch = search == null ||
                listOfNotNull(d.name, d.location, type?.name, type?.batteryType).any { it.lowercase().contains(search) }
            matchesStatus && matchesSearch
        }
        val lines = found.joinToString("\n") { d ->
            val type = types[d.typeId]
            "- id:${d.id} | ${d.name} | ${d.location ?: "-"} | ${type?.let { "${it.batteryQuantity}×${it.batteryType}" } ?: "?"} | ${status(d, now)}"
        }
        return ReadResult("${found.size} device(s):\n" + lines.ifEmpty { "(none)" }, "Listed devices")
    }

    private suspend fun getDevice(
        args: Map<String, Any?>,
        now: Instant,
    ): ReadResult {
        val id = str(args, AiToolParams.ID) ?: return ReadResult("Error: Missing id")
        val device = deviceRepository.getDeviceById(id).first() ?: return ReadResult("Error: Device not found with id '$id'")
        val type = deviceRepository.getDeviceTypeById(device.typeId).first()
        val events = deviceRepository.getEventsForDevice(id).first()
        val text = buildString {
            appendLine("id:${device.id}")
            appendLine("name: ${device.name}")
            appendLine("location: ${device.location ?: "-"}")
            appendLine("type: ${type?.name ?: "none"}")
            appendLine("batteries: ${type?.let { "${it.batteryQuantity}×${it.batteryType}" } ?: "unknown"}")
            appendLine("last changed: ${if (hasDate(device)) dateOf(device.batteryLastReplaced) else "never"}")
            appendLine("status: ${status(device, now)}")
            appendLine("marked as needing a battery: ${device.id in marks()}")
            append("changes recorded: ${events.size}")
        }
        return ReadResult(text, "Looked up ${device.name}")
    }

    private suspend fun getDeviceHistory(args: Map<String, Any?>): ReadResult {
        val id = str(args, AiToolParams.ID) ?: return ReadResult("Error: Missing id")
        val device = deviceRepository.getDeviceById(id).first() ?: return ReadResult("Error: Device not found with id '$id'")
        val events = deviceRepository.getEventsForDevice(id).first().sortedByDescending { it.date }
        return ReadResult(
            "History of '${device.name}' (${events.size} battery change(s), newest first):\n" + eventLines(events).ifEmpty { "(none)" },
            "Looked up the history of ${device.name}",
        )
    }

    private suspend fun listBatteryChanges(args: Map<String, Any?>): ReadResult {
        val from = str(args, AiToolParams.FROM)?.let { day(it) ?: return ReadResult("Error: from must be YYYY-MM-DD") }
        val to = str(args, AiToolParams.TO)?.let { day(it) ?: return ReadResult("Error: to must be YYYY-MM-DD") }
        val limit = (args[AiToolParams.LIMIT] as? Number)?.toInt() ?: (args[AiToolParams.LIMIT] as? String)?.toIntOrNull() ?: DEFAULT_LIMIT
        val names = deviceRepository.getAllDevices().first().associate { it.id to it.name }
        val found = deviceRepository
            .getAllEvents()
            .first()
            .filter { (from == null || dateOf(it.date) >= from) && (to == null || dateOf(it.date) <= to) }
            .sortedByDescending { it.date }
        val shown = found.take(limit.coerceIn(1, MAX_LIMIT))
        val lines = shown.joinToString("\n") { e ->
            "- id:${e.id} | ${names[e.deviceId] ?: "deleted device"} | ${dateOf(e.date)}" + (e.notes?.let { " | $it" } ?: "")
        }
        return ReadResult("${shown.size} of ${found.size} battery change(s), newest first:\n" + lines.ifEmpty { "(none)" }, "Looked through battery changes")
    }

    private suspend fun getBatteriesToBuy(now: Instant): ReadResult {
        val types = deviceRepository.getAllDeviceTypes().first().associateBy { it.id }
        val marks = marks()
        val due = deviceRepository.getAllDevices().first().filter { status(it, now) in DUE || it.id in marks }
        val totals = mutableMapOf<String, Int>()
        due.forEach { d -> types[d.typeId]?.let { totals[it.batteryType] = (totals[it.batteryType] ?: 0) + it.batteryQuantity } }
        val lines = totals.entries.sortedByDescending { it.value }.joinToString("\n") { "- ${it.value} × ${it.key}" }
        return ReadResult("To change the ${due.size} device(s) that are due, overdue or marked:\n" + lines.ifEmpty { "(nothing)" }, "Worked out what to buy")
    }

    private fun eventLines(events: List<BatteryEvent>): String =
        events.joinToString("\n") { e ->
            "- id:${e.id} | ${dateOf(e.date)}" + (e.batteryType?.let { " | $it" } ?: "") + (e.notes?.let { " | $it" } ?: "")
        }

    private suspend fun marks(): Set<String> = needsBatteryRepository.getNeedsBatteryMarks().first().keys

    private fun hasDate(device: Device) = device.batteryLastReplaced.toEpochMilliseconds() > 0

    private fun status(
        device: Device,
        now: Instant,
    ): String {
        if (!hasDate(device)) return "unknown"
        val days = (now - device.batteryLastReplaced).inWholeDays
        return when {
            days >= BuildAiContextUseCase.OVERDUE_DAYS -> "overdue"
            days >= BuildAiContextUseCase.DUE_DAYS -> "due"
            else -> "ok"
        }
    }

    private fun str(
        args: Map<String, Any?>,
        key: String,
    ): String? = (args[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }

    /** The same day back as text, if it parses. ISO dates compare correctly as strings. */
    private fun day(text: String): String? = runCatching { LocalDate.parse(text.trim()) }.getOrNull()?.toString()

    private fun dateOf(instant: Instant): String =
        kotlinx.datetime.Instant
            .fromEpochMilliseconds(instant.toEpochMilliseconds())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()

    private companion object {
        val DUE = setOf("due", "overdue")
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 200
    }
}
