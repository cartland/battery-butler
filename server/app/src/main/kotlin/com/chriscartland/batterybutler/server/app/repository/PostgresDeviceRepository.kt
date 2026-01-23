package com.chriscartland.batterybutler.server.app.repository

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.server.domain.repository.ServerDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

private val dbUpdateSignal = MutableSharedFlow<Unit>(replay = 1)

object Devices : Table() {
    val id = varchar("id", 128)
    val name = varchar("name", 256)
    val typeId = varchar("type_id", 128)
    val batteryLastReplaced = timestamp("battery_last_replaced")
    val lastUpdated = timestamp("last_updated")
    val location = varchar("location", 256).nullable()
    val imagePath = varchar("image_path", 512).nullable()

    override val primaryKey = PrimaryKey(id)
}

object DeviceTypes : Table() {
    val id = varchar("id", 128)
    val name = varchar("name", 256)
    val batteryType = varchar("battery_type", 64).default("AA")
    val batteryQuantity = integer("battery_quantity").default(1)
    val defaultIcon = varchar("default_icon", 128).nullable()

    override val primaryKey = PrimaryKey(id)
}

object BatteryEvents : Table() {
    val id = varchar("id", 128)
    val deviceId = varchar("device_id", 128)
    val timestamp = timestamp("timestamp")
    val notes = text("notes").nullable()
    val batteryType = varchar("battery_type", 64).nullable()

    override val primaryKey = PrimaryKey(id)
}

@kotlin.OptIn(kotlin.time.ExperimentalTime::class)
class PostgresDeviceRepository : ServerDeviceRepository {
    private fun <T> reactiveQuery(query: suspend () -> T): Flow<T> =
        flow {
            emit(query())
            dbUpdateSignal.collect {
                emit(query())
            }
        }

    override fun getAllDevices(): Flow<List<Device>> =
        reactiveQuery {
            newSuspendedTransaction {
                Devices.selectAll().map {
                    Device(
                        id = it[Devices.id],
                        name = it[Devices.name],
                        typeId = it[Devices.typeId],
                        batteryLastReplaced = it[Devices.batteryLastReplaced].toKotlinInstant(),
                        lastUpdated = it[Devices.lastUpdated].toKotlinInstant(),
                        location = it[Devices.location],
                        imagePath = it[Devices.imagePath],
                    )
                }
            }
        }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> =
        reactiveQuery {
            newSuspendedTransaction {
                DeviceTypes.selectAll().map {
                    DeviceType(
                        id = it[DeviceTypes.id],
                        name = it[DeviceTypes.name],
                        batteryType = it[DeviceTypes.batteryType],
                        batteryQuantity = it[DeviceTypes.batteryQuantity],
                        defaultIcon = it[DeviceTypes.defaultIcon],
                    )
                }
            }
        }

    override fun getAllEvents(): Flow<List<BatteryEvent>> =
        reactiveQuery {
            newSuspendedTransaction {
                BatteryEvents.selectAll().map {
                    BatteryEvent(
                        id = it[BatteryEvents.id],
                        deviceId = it[BatteryEvents.deviceId],
                        date = it[BatteryEvents.timestamp].toKotlinInstant(),
                        notes = it[BatteryEvents.notes] ?: "",
                        batteryType = it[BatteryEvents.batteryType],
                    )
                }
            }
        }

    override suspend fun addDevice(device: Device) {
        newSuspendedTransaction {
            Devices.insert {
                it[id] = device.id
                it[name] = device.name
                it[typeId] = device.typeId
                it[batteryLastReplaced] = device.batteryLastReplaced.toJavaInstant()
                it[lastUpdated] = device.lastUpdated.toJavaInstant()
                it[location] = device.location
                it[imagePath] = device.imagePath
            }
        }
        dbUpdateSignal.emit(Unit)
    }

    override suspend fun updateDevice(device: Device) {
        newSuspendedTransaction {
            Devices.update({ Devices.id eq device.id }) {
                it[name] = device.name
                it[typeId] = device.typeId
                it[batteryLastReplaced] = device.batteryLastReplaced.toJavaInstant()
                it[lastUpdated] = device.lastUpdated.toJavaInstant()
                it[location] = device.location
                it[imagePath] = device.imagePath
            }
        }
        dbUpdateSignal.emit(Unit)
    }

    override suspend fun deleteDevice(id: String) {
        newSuspendedTransaction {
            Devices.deleteWhere { Devices.id eq id }
        }
        dbUpdateSignal.emit(Unit)
    }

    override suspend fun addDeviceType(type: DeviceType) {
        newSuspendedTransaction {
            DeviceTypes.insert {
                it[id] = type.id
                it[name] = type.name
                it[batteryType] = type.batteryType
                it[batteryQuantity] = type.batteryQuantity
                it[defaultIcon] = type.defaultIcon
            }
        }
        dbUpdateSignal.emit(Unit)
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        newSuspendedTransaction {
            DeviceTypes.update({ DeviceTypes.id eq type.id }) {
                it[name] = type.name
                it[batteryType] = type.batteryType
                it[batteryQuantity] = type.batteryQuantity
                it[defaultIcon] = type.defaultIcon
            }
        }
        dbUpdateSignal.emit(Unit)
    }

    override suspend fun deleteDeviceType(id: String) {
        newSuspendedTransaction {
            DeviceTypes.deleteWhere { DeviceTypes.id eq id }
        }
        dbUpdateSignal.emit(Unit)
    }

    override suspend fun addEvent(event: BatteryEvent) {
        newSuspendedTransaction {
            BatteryEvents.insert {
                it[id] = event.id
                it[deviceId] = event.deviceId
                it[timestamp] = event.date.toJavaInstant()
                it[notes] = event.notes
                it[batteryType] = event.batteryType
            }
        }
        dbUpdateSignal.emit(Unit)
    }

    override suspend fun deleteEvent(id: String) {
        newSuspendedTransaction {
            BatteryEvents.deleteWhere { BatteryEvents.id eq id }
        }
        dbUpdateSignal.emit(Unit)
    }

    override fun getUpdates(): Flow<RemoteUpdate> = flow { }
}
