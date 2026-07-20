package com.chriscartland.batterybutler.viewmodel.devicedetail

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.presentationmodel.devicedetail.DeviceDetailScreenState
import com.chriscartland.batterybutler.usecase.AddBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.GetBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetCachedDeviceImageUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock

@Inject
class DeviceDetailViewModelFactory(
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val getBatteryEventsUseCase: GetBatteryEventsUseCase,
    private val addBatteryEventUseCase: AddBatteryEventUseCase,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
    private val getCachedDeviceImageUseCase: GetCachedDeviceImageUseCase,
) {
    fun create(deviceId: String): DeviceDetailViewModel =
        DeviceDetailViewModel(
            deviceId,
            getDeviceDetailUseCase,
            getDeviceTypesUseCase,
            getBatteryEventsUseCase,
            addBatteryEventUseCase,
            updateDeviceUseCase,
            getCachedDeviceImageUseCase,
        )
}

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceDetailViewModel(
    private val deviceId: String,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val getBatteryEventsUseCase: GetBatteryEventsUseCase,
    private val addBatteryEventUseCase: AddBatteryEventUseCase,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
    private val getCachedDeviceImageUseCase: GetCachedDeviceImageUseCase,
) : ViewModel() {
    val uiState: StateFlow<DeviceDetailScreenState> = combine(
        getDeviceDetailUseCase(deviceId),
        getDeviceTypesUseCase(),
        getBatteryEventsUseCase.forDevice(deviceId),
    ) { device, types, events -> Triple(device, types, events) }
        .flatMapLatest { (device, types, events) ->
            if (device == null) {
                flowOf(DeviceDetailScreenState.NotFound)
            } else {
                val deviceType = types.find { it.id == device.typeId }
                val imageEtag = device.imageEtag
                val imageBytesFlow = if (imageEtag != null) getCachedDeviceImageUseCase(imageEtag) else flowOf(null)
                imageBytesFlow.map { imageBytes ->
                    DeviceDetailScreenState.Success(
                        device = device,
                        deviceType = deviceType,
                        events = events,
                        imageBytes = imageBytes,
                    )
                }
            }
        }.safeStateIn(
            viewModelScope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = DeviceDetailScreenState.Loading,
        )

    fun recordReplacement() {
        viewModelScope.coroutineScope.launch {
            val event = BatteryEvent(
                id = uuid4().toString(),
                deviceId = deviceId,
                date = Clock.System.now(),
            )
            addBatteryEventUseCase(event)
            // Also update the device's last replaced date for quick access
            val currentDevice = (uiState.value as? DeviceDetailScreenState.Success)?.device
            if (currentDevice != null) {
                updateDeviceUseCase(currentDevice.copy(batteryLastReplaced = event.date))
            }
        }
    }
}
