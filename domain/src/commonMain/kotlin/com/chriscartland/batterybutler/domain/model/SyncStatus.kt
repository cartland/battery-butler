package com.chriscartland.batterybutler.domain.model

sealed interface SyncStatus {
    data object Idle : SyncStatus

    data object Syncing : SyncStatus

    data object Success : SyncStatus

    data class Failed(
        val message: String,
    ) : SyncStatus
}
