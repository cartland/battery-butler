package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.datalocal.room.AppDatabase

expect class DatabaseFactory {
    fun createDatabase(name: String = "battery-butler.db"): AppDatabase
}
