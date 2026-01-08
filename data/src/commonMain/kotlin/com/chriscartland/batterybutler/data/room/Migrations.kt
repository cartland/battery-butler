package com.chriscartland.batterybutler.data.room

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
