package com.chriscartland.batterybutler.datalocal.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.chriscartland.batterybutler.datalocal.room.entity.BatteryEventEntity
import com.chriscartland.batterybutler.datalocal.room.entity.DeviceEntity
import com.chriscartland.batterybutler.datalocal.room.entity.DeviceImageCacheEntity
import com.chriscartland.batterybutler.datalocal.room.entity.DeviceTypeEntity

@Database(
    entities = [DeviceEntity::class, DeviceTypeEntity::class, BatteryEventEntity::class, DeviceImageCacheEntity::class],
    version = 7,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao

    abstract fun deviceImageCacheDao(): DeviceImageCacheDao
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
