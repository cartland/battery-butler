package com.chriscartland.batterybutler.datalocal.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datalocal.room.AppDatabaseConstructor
import com.chriscartland.batterybutler.datalocal.room.MIGRATION_3_4
import com.chriscartland.batterybutler.datalocal.room.MIGRATION_4_5

actual class DatabaseFactory(
    private val context: Context,
) {
    actual fun createDatabase(name: String): AppDatabase {
        val dbFile = context.getDatabasePath(name)
        return Room
            .databaseBuilder<AppDatabase>(
                context = context,
                name = dbFile.absolutePath,
                factory = { AppDatabaseConstructor.initialize() },
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }
}
