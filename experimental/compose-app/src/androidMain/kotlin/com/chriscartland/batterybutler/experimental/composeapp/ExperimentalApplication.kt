package com.chriscartland.batterybutler.experimental.composeapp

import android.app.Application
import com.chriscartland.batterybutler.experimental.composeapp.di.ExperimentalAppComponent
import com.chriscartland.batterybutler.experimental.composeapp.di.create

class ExperimentalApplication : Application() {
    val component: ExperimentalAppComponent by lazy {
        ExperimentalAppComponent::class.create()
    }
}
