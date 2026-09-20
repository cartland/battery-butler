package com.chriscartland.batterybutler.datalocal.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.chriscartland.batterybutler.datalocal.room.entity.BatteryEventEntity
import com.chriscartland.batterybutler.datalocal.room.entity.DeviceEntity
import com.chriscartland.batterybutler.datalocal.room.entity.DeviceImageCacheEntity
import com.chriscartland.batterybutler.datalocal.room.entity.DeviceTypeEntity
import com.chriscartland.batterybutler.datalocal.room.entity.NeedsBatteryFlagEntity

@Database(
    entities = [
        DeviceEntity::class,
        DeviceTypeEntity::class,
        BatteryEventEntity::class,
        DeviceImageCacheEntity::class,
        NeedsBatteryFlagEntity::class,
    ],
    version = 8,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao

    abstract fun deviceImageCacheDao(): DeviceImageCacheDao

    abstract fun needsBatteryFlagDao(): NeedsBatteryFlagDao
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
