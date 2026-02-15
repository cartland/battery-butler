package com.chriscartland.batterybutler.datalocal.room

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE devices ADD COLUMN location TEXT")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE battery_events ADD COLUMN batteryType TEXT")
        connection.execSQL("ALTER TABLE battery_events ADD COLUMN notes TEXT")
    }
}
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // Devices
        connection.execSQL("ALTER TABLE devices ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE devices ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE devices ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")

        // Device Types
        connection.execSQL("ALTER TABLE device_types ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE device_types ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE device_types ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")

        // Battery Events
        connection.execSQL("ALTER TABLE battery_events ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE battery_events ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE battery_events ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
    }
}
