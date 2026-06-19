package com.chriscartland.batterybutler.viewmodel.devicetypes

import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeDetailScreenState
import com.chriscartland.batterybutler.usecase.GetDeviceTypeDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import com.rickclephas.kmp.observableviewmodel.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import me.tatarka.inject.annotations.Inject

@Inject
class DeviceTypeDetailViewModelFactory(
    private val getDeviceTypeDetailUseCase: GetDeviceTypeDetailUseCase,
    private val getDevicesUseCase: GetDevicesUseCase,
) {
    fun create(typeId: String): DeviceTypeDetailViewModel =
        DeviceTypeDetailViewModel(
            typeId,
            getDeviceTypeDetailUseCase,
            getDevicesUseCase,
        )
}

class DeviceTypeDetailViewModel(
    private val typeId: String,
    private val getDeviceTypeDetailUseCase: GetDeviceTypeDetailUseCase,
    private val getDevicesUseCase: GetDevicesUseCase,
) : ViewModel() {
    val uiState: StateFlow<DeviceTypeDetailScreenState> = combine(
        getDeviceTypeDetailUseCase(typeId),
        getDevicesUseCase(),
    ) { deviceType, allDevices ->
        if (deviceType == null) {
            DeviceTypeDetailScreenState.NotFound
        } else {
            val devices = allDevices.filter { it.typeId == typeId }
            DeviceTypeDetailScreenState.Success(
                deviceType = deviceType,
                devices = devices,
            )
        }
    }.safeStateIn(
        viewModelScope = viewModelScope,
        started = defaultWhileSubscribed(),
        initialValue = DeviceTypeDetailScreenState.Loading,
    )
}
