package com.chriscartland.blanket.data.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chriscartland.blanket.data.room.AppDatabase
import java.io.File

actual class DatabaseFactory {
    actual fun createDatabase(): AppDatabase {
        val dbFile = File(System.getProperty("java.io.tmpdir"), "blanket.db")
        return Room
            .databaseBuilder<AppDatabase>(
                name = dbFile.absolutePath,
            ).setDriver(BundledSQLiteDriver())
            .build()
    }
}
