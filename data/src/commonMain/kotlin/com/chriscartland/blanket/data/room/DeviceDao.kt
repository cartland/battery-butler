package com.chriscartland.blanket.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :id")
    fun getDeviceById(id: String): Flow<DeviceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDevice(id: String)

    // Device Types
    @Query("SELECT * FROM device_types")
    fun getAllDeviceTypes(): Flow<List<DeviceTypeEntity>>

    @Query("SELECT * FROM device_types WHERE id = :id")
    fun getDeviceTypeById(id: String): Flow<DeviceTypeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceType(type: DeviceTypeEntity)

    @Update
    suspend fun updateDeviceType(type: DeviceTypeEntity)

    @Query("DELETE FROM device_types WHERE id = :id")
    suspend fun deleteDeviceType(id: String)

    // Battery Events
    @Query("SELECT * FROM battery_events WHERE deviceId = :deviceId ORDER BY date DESC")
    fun getEventsForDevice(deviceId: String): Flow<List<BatteryEventEntity>>

    @Query("SELECT * FROM battery_events ORDER BY date DESC")
    fun getAllEvents(): Flow<List<BatteryEventEntity>>

    @Query("SELECT * FROM battery_events WHERE id = :id")
    fun getEventById(id: String): Flow<BatteryEventEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: BatteryEventEntity)

    @Update
    suspend fun updateEvent(event: BatteryEventEntity)

    @Query("DELETE FROM battery_events WHERE id = :id")
    suspend fun deleteEvent(id: String)
}
