package com.chriscartland.batterybutler.experimental.composeapp

import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.experimental.composeapp.di.ExperimentalAppComponent
import com.chriscartland.batterybutler.experimental.composeapp.di.create

class IosExperimentalHelper {
    fun createComponent(): ExperimentalAppComponent {
        val dataStoreFactory = DataStoreFactory()
        return ExperimentalAppComponent::class.create(dataStoreFactory)
    }
}
