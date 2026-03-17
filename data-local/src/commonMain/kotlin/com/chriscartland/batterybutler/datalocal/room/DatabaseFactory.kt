package com.chriscartland.batterybutler.datalocal.room

expect class DatabaseFactory {
    fun createDatabase(option: DatabaseOption = DatabaseOption.Production): AppDatabase

    fun evict(option: DatabaseOption)
}
