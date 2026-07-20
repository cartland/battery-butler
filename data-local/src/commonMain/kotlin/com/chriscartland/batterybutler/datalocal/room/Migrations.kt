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
        connection.execSQL("ALTER TABLE devices ADD COLUMN imageEtag TEXT")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS device_image_cache (imageEtag TEXT NOT NULL PRIMARY KEY, bytes BLOB NOT NULL, contentType TEXT NOT NULL)",
        )
    }
}
