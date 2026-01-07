package com.chriscartland.batterybutler.data.di

import com.chriscartland.batterybutler.data.room.AppDatabase

expect class DatabaseFactory {
    fun createDatabase(): AppDatabase
}
