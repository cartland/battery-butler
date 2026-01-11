package com.chriscartland.batterybutler.data.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chriscartland.batterybutler.data.room.AppDatabase
import com.chriscartland.batterybutler.data.room.AppDatabaseConstructor
import com.chriscartland.batterybutler.data.room.MIGRATION_3_4
import com.chriscartland.batterybutler.data.room.MIGRATION_4_5

actual class DatabaseFactory(
    private val context: Context,
) {
    actual fun createDatabase(): AppDatabase {
        val dbFile = context.getDatabasePath("blanket.db")
        return Room
            .databaseBuilder<AppDatabase>(
                context = context,
                name = dbFile.absolutePath,
                factory = AppDatabaseConstructor,
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }
}
