package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Shared DTOs defining the Battery Butler backup file format.
 *
 * These classes are used by both [ExportDataUseCase] and [ImportDataUseCase]
 * to ensure the backup format contract is consistent.
 *
 * ## Versioning
 * - [BackupContainer.formatVersion] identifies the schema version.
 * - Import parses the envelope first, then dispatches to a version-specific parser.
 * - `ignoreUnknownKeys = true` ensures forward compatibility: a v1 importer
 *   can safely skip fields added in v2+.
 */

internal const val BACKUP_TYPE = "battery_butler_backup"
internal const val BACKUP_FORMAT_VERSION = 1

@Serializable
internal data class BackupContainer(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val type: String = BACKUP_TYPE,
    val data: BackupData,
)

@Serializable
internal data class BackupData(
    val devices: List<DeviceDto>,
    val deviceTypes: List<DeviceTypeDto>,
    val events: List<BatteryEventDto>,
)

@Serializable
internal data class DeviceDto(
    val id: String,
    val name: String,
    val typeId: String,
    val batteryLastReplaced: String?,
    val location: String?,
)

@Serializable
internal data class DeviceTypeDto(
    val id: String,
    val name: String,
    val batteryType: String?,
    val batteryQuantity: Int,
    val defaultIcon: String?,
)

@Serializable
internal data class BatteryEventDto(
    val id: String,
    val deviceId: String,
    val date: String,
    val batteryType: String?,
    val notes: String?,
)

// region Domain -> DTO mappers

internal fun Device.toDto() =
    DeviceDto(
        id = id,
        name = name,
        typeId = typeId,
        batteryLastReplaced = batteryLastReplaced.toString(),
        location = location,
    )

internal fun DeviceType.toDto() =
    DeviceTypeDto(
        id = id,
        name = name,
        batteryType = batteryType,
        batteryQuantity = batteryQuantity,
        defaultIcon = defaultIcon,
    )

internal fun BatteryEvent.toDto() =
    BatteryEventDto(
        id = id,
        deviceId = deviceId,
        date = date.toString(),
        batteryType = batteryType,
        notes = notes,
    )

// endregion

// region DTO -> Domain mappers

@OptIn(ExperimentalTime::class)
internal fun DeviceDto.toDomain(): Device =
    Device(
        id = id,
        name = name,
        typeId = typeId,
        batteryLastReplaced = batteryLastReplaced?.let { Instant.parse(it) } ?: Instant.DISTANT_PAST,
        lastUpdated = Clock.System.now(),
        location = location,
    )

internal fun DeviceTypeDto.toDomain(): DeviceType =
    DeviceType(
        id = id,
        name = name,
        batteryType = batteryType ?: "AA",
        batteryQuantity = batteryQuantity,
        defaultIcon = defaultIcon,
    )

@OptIn(ExperimentalTime::class)
internal fun BatteryEventDto.toDomain(): BatteryEvent =
    BatteryEvent(
        id = id,
        deviceId = deviceId,
        date = Instant.parse(date),
        batteryType = batteryType,
        notes = notes,
    )

// endregion
