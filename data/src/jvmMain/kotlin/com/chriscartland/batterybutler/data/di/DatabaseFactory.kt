package com.chriscartland.batterybutler.data.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chriscartland.batterybutler.data.room.AppDatabase
import com.chriscartland.batterybutler.data.room.AppDatabaseConstructor
import com.chriscartland.batterybutler.data.room.MIGRATION_3_4
import java.io.File

actual class DatabaseFactory {
    actual fun createDatabase(): AppDatabase {
        val dbFile = File(System.getProperty("java.io.tmpdir"), "blanket.db")
        return Room
            .databaseBuilder<AppDatabase>(
                name = dbFile.absolutePath,
                factory = AppDatabaseConstructor,
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4)
            .build()
    }
}
