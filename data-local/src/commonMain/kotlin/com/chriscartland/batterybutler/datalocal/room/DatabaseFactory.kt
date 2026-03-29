package com.chriscartland.batterybutler.datalocal.room

expect class DatabaseFactory {
    fun createDatabase(option: DatabaseOption = DatabaseOption.Offline): AppDatabase

    fun evict(option: DatabaseOption)

    fun databaseFileExists(fileName: String): Boolean

    fun copyDatabaseFile(
        sourceFileName: String,
        destFileName: String,
    )
}
