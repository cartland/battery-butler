package com.chriscartland.batterybutler.datalocal.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chriscartland.batterybutler.datalocal.room.entity.BatteryEventEntity
import com.chriscartland.batterybutler.datalocal.room.entity.DeviceEntity
import com.chriscartland.batterybutler.datalocal.room.entity.DeviceTypeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices WHERE isDeleted = 0")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :id AND isDeleted = 0")
    fun getDeviceById(id: String): Flow<DeviceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Query("UPDATE devices SET isDeleted = 1, isSynced = 0, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteDevice(
        id: String,
        timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    )

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun hardDeleteDevice(id: String)

    @Query("UPDATE devices SET isSynced = :isSynced WHERE id = :id")
    suspend fun markDeviceSynced(
        id: String,
        isSynced: Boolean,
    )

    // Device Types
    @Query("SELECT * FROM device_types WHERE isDeleted = 0")
    fun getAllDeviceTypes(): Flow<List<DeviceTypeEntity>>

    @Query("SELECT * FROM device_types WHERE id = :id AND isDeleted = 0")
    fun getDeviceTypeById(id: String): Flow<DeviceTypeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceType(type: DeviceTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceTypes(types: List<DeviceTypeEntity>)

    @Update
    suspend fun updateDeviceType(type: DeviceTypeEntity)

    @Query("UPDATE device_types SET isDeleted = 1, isSynced = 0, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteDeviceType(
        id: String,
        timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    )

    @Query("DELETE FROM device_types WHERE id = :id")
    suspend fun hardDeleteDeviceType(id: String)

    @Query("UPDATE device_types SET isSynced = :isSynced WHERE id = :id")
    suspend fun markDeviceTypeSynced(
        id: String,
        isSynced: Boolean,
    )

    @Query("SELECT COUNT(*) FROM device_types WHERE isDeleted = 0")
    suspend fun getDeviceTypeCount(): Int

    // Battery Events
    @Query("SELECT * FROM battery_events WHERE deviceId = :deviceId AND isDeleted = 0 ORDER BY date DESC")
    fun getEventsForDevice(deviceId: String): Flow<List<BatteryEventEntity>>

    @Query("SELECT * FROM battery_events WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllEvents(): Flow<List<BatteryEventEntity>>

    @Query("SELECT * FROM battery_events WHERE id = :id AND isDeleted = 0")
    fun getEventById(id: String): Flow<BatteryEventEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: BatteryEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<BatteryEventEntity>)

    @Update
    suspend fun updateEvent(event: BatteryEventEntity)

    @Query("UPDATE battery_events SET isDeleted = 1, isSynced = 0, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteEvent(
        id: String,
        timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    )

    @Query("DELETE FROM battery_events WHERE id = :id")
    suspend fun hardDeleteEvent(id: String)

    @Query("UPDATE battery_events SET isSynced = :isSynced WHERE id = :id")
    suspend fun markEventSynced(
        id: String,
        isSynced: Boolean,
    )
}
