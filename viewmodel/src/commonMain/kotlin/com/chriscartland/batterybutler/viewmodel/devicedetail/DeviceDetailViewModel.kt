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
import com.chriscartland.batterybutler.viewmodel.retryableStateIn
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
    private val retryTrigger = MutableStateFlow(0)

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    // retryableStateIn (not safeStateIn without onError): the upstream is a Room-backed chain
    // that can throw transiently (detail queries race the sync loop's full-snapshot upsert
    // every poll). Without an error state the catch terminated the flow and the screen wedged
    // at Loading forever with zero network requests -- seen in production on android/57.
    val uiState: StateFlow<DeviceDetailScreenState> = retryableStateIn(
        viewModelScope = viewModelScope,
        retryTrigger = retryTrigger,
        started = defaultWhileSubscribed(),
        initialValue = DeviceDetailScreenState.Loading,
        onError = { DeviceDetailScreenState.Error(it.message ?: "Failed to load device details") },
        source = {
            combine(
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
                }
        },
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
