package com.chriscartland.batterybutler.datalocal.room

expect class DatabaseFactory {
    fun createDatabase(option: DatabaseOption = DatabaseOption.None): AppDatabase

    fun evict(option: DatabaseOption)
}
