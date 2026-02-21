package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tatarka.inject.annotations.Inject

/**
 * Builds a context string containing the user's device inventory
 * and battery history for use in AI prompts.
 */
@Inject
class BuildAiContextUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend fun buildContext(): String {
        val devices = deviceRepository.getAllDevices().first()
        val types = deviceRepository.getAllDeviceTypes().first()
        val events = deviceRepository.getAllEvents().first()
        val tz = TimeZone.currentSystemDefault()

        return buildString {
            appendLine("=== User's Inventory ===")
            appendLine("Devices (${devices.size}):")
            devices.forEach { d ->
                val typeName = types.find { it.id == d.typeId }?.name ?: "Unknown"
                append("- ${d.name} ($typeName)")
                d.location?.let { append(", $it") }
                appendLine()
            }
            appendLine()
            appendLine("Device Types (${types.size}):")
            types.forEach { t ->
                appendLine("- ${t.name}: ${t.batteryQuantity}x ${t.batteryType}")
            }
            appendLine()
            appendLine("Recent Battery Replacements:")
            events.take(20).forEach { e ->
                val deviceName = devices.find { it.id == e.deviceId }?.name ?: "Unknown"
                val localDate = e.date.toLocalDateTime(tz).date
                appendLine("- $deviceName on $localDate")
            }
        }
    }
}
