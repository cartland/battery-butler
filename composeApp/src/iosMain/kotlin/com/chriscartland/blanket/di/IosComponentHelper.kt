package com.chriscartland.blanket.di

import com.chriscartland.blanket.data.di.DatabaseFactory

expect object IosComponentHelper {
    fun create(databaseFactory: DatabaseFactory): AppComponent
}
