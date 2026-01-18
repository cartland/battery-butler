package com.chriscartland.batterybutler.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore

class KmpViewModelStore {
    private val store = ViewModelStore()

    fun put(
        key: String,
        viewModel: ViewModel,
    ) {
        store.put(key, viewModel)
    }

    fun clear() {
        store.clear()
    }
}
