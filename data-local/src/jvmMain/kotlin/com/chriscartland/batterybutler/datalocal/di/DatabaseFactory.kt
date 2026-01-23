package com.chriscartland.batterybutler.datalocal.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datalocal.room.AppDatabaseConstructor
import com.chriscartland.batterybutler.datalocal.room.MIGRATION_3_4
import java.io.File

actual class DatabaseFactory {
    actual fun createDatabase(name: String): AppDatabase {
        val dbFile = File(System.getProperty("java.io.tmpdir"), name)
        return Room
            .databaseBuilder<AppDatabase>(
                name = dbFile.absolutePath,
                factory = { AppDatabaseConstructor.initialize() },
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4)
            .build()
    }
}
