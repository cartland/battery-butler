package com.chriscartland.batterybutler.data.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chriscartland.batterybutler.data.room.AppDatabase
import com.chriscartland.batterybutler.data.room.MIGRATION_3_4

actual class DatabaseFactory(
    private val context: Context,
) {
    actual fun createDatabase(): AppDatabase {
        val dbFile = context.getDatabasePath("blanket.db")
        return Room
            .databaseBuilder<AppDatabase>(
                context = context,
                name = dbFile.absolutePath,
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4)
            .build()
    }
}
