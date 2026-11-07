package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The person's data as the assistant sees it: every device with its type, batteries, location,
 * when it was last changed and whether that's due, the types, and the most recent changes.
 *
 * Re-read before every model call (see `AiEngine.generateResponse` with a situation), so a change
 * the assistant just made is already in it, and never written into the conversation, so it never
 * goes stale there. Ids are included because the tools take ids; the model matches a name the
 * person used to an id here.
 *
 * Kept well inside the relay's 20,000-character system instruction: with many devices the recent
 * changes are cut first, and `getDeviceHistory` fetches one device's full history on demand.
 */
@Inject
class BuildAiContextUseCase(
    private val deviceRepository: DeviceRepository,
    private val needsBatteryRepository: NeedsBatteryRepository,
) {
    suspend operator fun invoke(now: Instant = Clock.System.now()): String {
        val devices = deviceRepository.getAllDevices().first().sortedBy { it.name.lowercase() }
        val types = deviceRepository.getAllDeviceTypes().first()
        val events = deviceRepository.getAllEvents().first().sortedByDescending { it.date }
        val marks = needsBatteryRepository.getNeedsBatteryMarks().first().keys
        val typeById = types.associateBy { it.id }
        val deviceById = devices.associateBy { it.id }
        val eventCounts = events.groupingBy { it.deviceId }.eachCount()

        val full = render(now, devices, types, events, marks, typeById, deviceById, eventCounts, RECENT_EVENTS)
        if (full.length <= MAX_CHARS) return full
        return render(now, devices, types, events, marks, typeById, deviceById, eventCounts, FEW_EVENTS)
    }

    private fun render(
        now: Instant,
        devices: List<Device>,
        types: List<DeviceType>,
        events: List<BatteryEvent>,
        marks: Set<String>,
        typeById: Map<String, DeviceType>,
        deviceById: Map<String, Device>,
        eventCounts: Map<String, Int>,
        recent: Int,
    ): String =
        buildString {
            val tz = TimeZone.currentSystemDefault()
            appendLine("Today is ${dateOf(now)} ($tz).")
            appendLine()
            appendLine("Devices (${devices.size}) — id | name | type | batteries | location | last changed | status | changes recorded:")
            devices.forEach { d ->
                val type = typeById[d.typeId]
                val days = ageDays(d, now)
                append("- id:${d.id} | ${d.name} | ${type?.name ?: "no type"} | ${type?.let { "${it.batteryQuantity}×${it.batteryType}" } ?: "?"}")
                append(" | ${d.location ?: "-"} | ")
                append(if (days == null) "never" else "${dateOf(d.batteryLastReplaced)} ($days days ago)")
                append(" | ${status(days)}")
                if (d.id in marks) append(", marked as needing a battery")
                appendLine(" | ${eventCounts[d.id] ?: 0}")
            }
            appendLine("Status: ok = changed under 6 months ago; due = 6–12 months; overdue = a year or more; unknown = no change recorded.")
            appendLine()
            appendLine("Types (${types.size}) — id | name | batteries | icon | devices:")
            types.sortedBy { it.name.lowercase() }.forEach { t ->
                appendLine("- id:${t.id} | ${t.name} | ${t.batteryQuantity}×${t.batteryType} | ${t.defaultIcon ?: "default"} | ${devices.count { it.typeId == t.id }}")
            }
            appendLine()
            val shown = events.take(recent)
            append("Recent battery changes (newest first; ${shown.size} of ${events.size}")
            if (shown.size < events.size) append(" — call getDeviceHistory for one device's full list")
            appendLine(") — id | device | date | battery | notes:")
            shown.forEach { e ->
                appendLine("- id:${e.id} | ${deviceById[e.deviceId]?.name ?: "deleted device"} | ${dateOf(e.date)} | ${e.batteryType ?: "-"} | ${e.notes ?: "-"}")
            }
        }

    private fun ageDays(
        device: Device,
        now: Instant,
    ): Long? {
        if (device.batteryLastReplaced.toEpochMilliseconds() <= 0) return null
        return ((now - device.batteryLastReplaced).inWholeDays).coerceAtLeast(0)
    }

    private fun status(days: Long?): String =
        when {
            days == null -> "unknown"
            days >= OVERDUE_DAYS -> "overdue"
            days >= DUE_DAYS -> "due"
            else -> "ok"
        }

    private fun dateOf(instant: Instant): String =
        kotlinx.datetime.Instant
            .fromEpochMilliseconds(instant.toEpochMilliseconds())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()

    internal companion object {
        const val RECENT_EVENTS = 30
        const val FEW_EVENTS = 8
        const val MAX_CHARS = 16_000

        /** The same thresholds the device list colours by. */
        const val DUE_DAYS = 180
        const val OVERDUE_DAYS = 365
    }
}
