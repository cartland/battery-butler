package com.chriscartland.blanket.data.di

import com.chriscartland.blanket.data.room.AppDatabase

expect class DatabaseFactory {
    fun createDatabase(): AppDatabase
}
