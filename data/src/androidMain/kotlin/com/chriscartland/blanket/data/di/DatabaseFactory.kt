package com.chriscartland.blanket.data.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chriscartland.blanket.data.room.AppDatabase

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
            .build()
    }
}
