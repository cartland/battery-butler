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
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.usecase.GetEventDetailUseCase
import com.chriscartland.batterybutler.usecase.SuggestDeviceIconUseCase
import com.chriscartland.batterybutler.usecase.UpdateBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceUseCase

// Not a root component itself, but a mix-in for AppComponent
abstract class UseCaseComponent {
    abstract val addBatteryEventUseCase: AddBatteryEventUseCase
    abstract val addDeviceTypeUseCase: AddDeviceTypeUseCase
    abstract val addDeviceUseCase: AddDeviceUseCase
    abstract val batchAddBatteryEventsUseCase: BatchAddBatteryEventsUseCase
    abstract val batchAddDeviceTypesUseCase: BatchAddDeviceTypesUseCase
    abstract val batchAddDevicesUseCase: BatchAddDevicesUseCase
    abstract val deleteDeviceTypeUseCase: DeleteDeviceTypeUseCase
    abstract val deleteDeviceUseCase: DeleteDeviceUseCase

    abstract val exportDataUseCase: ExportDataUseCase
    abstract val getBatteryEventsUseCase: GetBatteryEventsUseCase
    abstract val getDeviceDetailUseCase: GetDeviceDetailUseCase
    abstract val getDeviceTypesUseCase: GetDeviceTypesUseCase
    abstract val getDevicesUseCase: GetDevicesUseCase
    abstract val getEventDetailUseCase: GetEventDetailUseCase
    abstract val suggestDeviceIconUseCase: SuggestDeviceIconUseCase
    abstract val updateBatteryEventUseCase: UpdateBatteryEventUseCase
    abstract val updateDeviceTypeUseCase: UpdateDeviceTypeUseCase
    abstract val updateDeviceUseCase: UpdateDeviceUseCase
    abstract val deleteBatteryEventUseCase: DeleteBatteryEventUseCase
}
