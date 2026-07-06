package com.chriscartland.batterybutler.datalocal.room

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.RandomAccessFile

actual class DatabaseFactory(
    private val context: Context,
) {
    private val instances = mutableMapOf<DatabaseOption, AppDatabase>()

    @Synchronized
    actual fun createDatabase(option: DatabaseOption): AppDatabase = instances.getOrPut(option) { createNewDatabase(option) }

    @Synchronized
    actual fun evict(option: DatabaseOption) {
        instances.remove(option)
    }

    actual fun databaseFileExists(fileName: String): Boolean = context.getDatabasePath(fileName).exists()

    actual fun copyDatabaseFile(
        sourceFileName: String,
        destFileName: String,
    ) {
        val source = context.getDatabasePath(sourceFileName)
        val dest = context.getDatabasePath(destFileName)
        source.copyTo(dest, overwrite = true)
    }

    actual fun renameDatabaseFile(
        sourceFileName: String,
        destFileName: String,
    ) {
        val source = context.getDatabasePath(sourceFileName)
        val dest = context.getDatabasePath(destFileName)
        if (dest.exists()) dest.delete()
        source.renameTo(dest)
        // Move WAL / SHM sidecars too if present
        listOf("-wal", "-shm").forEach { suffix ->
            val src = context.getDatabasePath(sourceFileName + suffix)
            val dst = context.getDatabasePath(destFileName + suffix)
            if (src.exists()) {
                if (dst.exists()) dst.delete()
                src.renameTo(dst)
            }
        }
    }

    actual fun deleteDatabaseFile(fileName: String) {
        listOf("", "-wal", "-shm").forEach { suffix ->
            val f = context.getDatabasePath(fileName + suffix)
            if (f.exists()) f.delete()
        }
    }

    actual fun readUserVersion(fileName: String): Int {
        val file = context.getDatabasePath(fileName)
        if (!file.exists()) return -1
        return try {
            // SQLite header: bytes 60..63 (big-endian) = user version
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(60)
                raf.readInt()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            -1
        }
    }

    private fun createNewDatabase(option: DatabaseOption): AppDatabase {
        promoteBareFileIfNeeded(option)
        val dbFile = context.getDatabasePath(option.fileName)
        return Room
            .databaseBuilder<AppDatabase>(
                context = context,
                name = dbFile.absolutePath,
                factory = { AppDatabaseConstructor.initialize() },
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            // If a legacy restore lands a file at a schema version we have no migration
            // path from (e.g. raw SQLite with user_version=0, or pre-v3 Room), don't
            // hang or throw — fall back to creating a fresh v5 schema. The user-facing
            // RestoreResult will signal DestructiveFallback so the UI can warn.
            .fallbackToDestructiveMigrationFrom(false, 0, 1, 2)
            .build()
    }
}
