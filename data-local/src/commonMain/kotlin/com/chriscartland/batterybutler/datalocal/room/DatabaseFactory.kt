package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.datalocal.room.AppDatabase

expect class DatabaseFactory {
    fun createDatabase(name: String = DatabaseConstants.PRODUCTION_DATABASE_NAME): AppDatabase
}
