package com.chriscartland.batterybutler.usecase.di

import com.chriscartland.batterybutler.usecase.AddBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.AddDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.AddDeviceUseCase
import com.chriscartland.batterybutler.usecase.BatchAddBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.BatchAddDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.BatchAddDevicesUseCase
import com.chriscartland.batterybutler.usecase.DeleteBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.DeleteDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.DeleteDeviceUseCase
import com.chriscartland.batterybutler.usecase.DismissSyncStatusUseCase
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetAppVersionUseCase
import com.chriscartland.batterybutler.usecase.GetBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.usecase.GetEventDetailUseCase
import com.chriscartland.batterybutler.usecase.GetSyncStatusUseCase
import com.chriscartland.batterybutler.usecase.SetNetworkModeUseCase
import com.chriscartland.batterybutler.usecase.SuggestDeviceIconUseCase
import com.chriscartland.batterybutler.usecase.UpdateBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceUseCase

/**
 * Mixin component that declares all use case dependencies.
 *
 * This is not a root component itself, but provides use case bindings
 * to be mixed into AppComponent for proper DI graph construction.
 */
abstract class UseCaseComponent {
    // Device operations
    abstract val addDeviceUseCase: AddDeviceUseCase
    abstract val updateDeviceUseCase: UpdateDeviceUseCase
    abstract val deleteDeviceUseCase: DeleteDeviceUseCase
    abstract val getDevicesUseCase: GetDevicesUseCase
    abstract val getDeviceDetailUseCase: GetDeviceDetailUseCase

    // Device type operations
    abstract val addDeviceTypeUseCase: AddDeviceTypeUseCase
    abstract val updateDeviceTypeUseCase: UpdateDeviceTypeUseCase
    abstract val deleteDeviceTypeUseCase: DeleteDeviceTypeUseCase
    abstract val getDeviceTypesUseCase: GetDeviceTypesUseCase

    // Battery event operations
    abstract val addBatteryEventUseCase: AddBatteryEventUseCase
    abstract val updateBatteryEventUseCase: UpdateBatteryEventUseCase
    abstract val deleteBatteryEventUseCase: DeleteBatteryEventUseCase
    abstract val getBatteryEventsUseCase: GetBatteryEventsUseCase
    abstract val getEventDetailUseCase: GetEventDetailUseCase

    // Batch operations (AI-driven)
    abstract val batchAddDevicesUseCase: BatchAddDevicesUseCase
    abstract val batchAddDeviceTypesUseCase: BatchAddDeviceTypesUseCase
    abstract val batchAddBatteryEventsUseCase: BatchAddBatteryEventsUseCase

    // Sync and network operations
    abstract val getSyncStatusUseCase: GetSyncStatusUseCase
    abstract val dismissSyncStatusUseCase: DismissSyncStatusUseCase
    abstract val setNetworkModeUseCase: SetNetworkModeUseCase

    // App info and utilities
    abstract val getAppVersionUseCase: GetAppVersionUseCase
    abstract val exportDataUseCase: ExportDataUseCase
    abstract val suggestDeviceIconUseCase: SuggestDeviceIconUseCase
}
