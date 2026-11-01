package com.chriscartland.batterybutler.datalocal.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chriscartland.batterybutler.datalocal.room.entity.NeedsBatteryFlagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NeedsBatteryFlagDao {
    @Query("SELECT * FROM needs_battery_flags")
    fun observeAll(): Flow<List<NeedsBatteryFlagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(flag: NeedsBatteryFlagEntity)

    @Query("DELETE FROM needs_battery_flags WHERE deviceId = :deviceId")
    suspend fun clear(deviceId: String)

    @Query("DELETE FROM needs_battery_flags")
    suspend fun clearAll()
}
