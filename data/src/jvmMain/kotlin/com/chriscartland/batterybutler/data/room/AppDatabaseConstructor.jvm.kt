package com.chriscartland.batterybutler.data.room

import androidx.room.RoomDatabaseConstructor

actual typealias AppDatabaseConstructor = AppDatabaseConstructorImpl

object AppDatabaseConstructorImpl : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase = AppDatabase_Impl()
}
