package com.chriscartland.batterybutler.viewmodel.editdevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.presentationmodel.editdevice.EditDeviceScreenState
import com.chriscartland.batterybutler.usecase.DeleteDeviceUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock

@Inject
class EditDeviceViewModelFactory(
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase,
) {
    fun create(deviceId: String): EditDeviceViewModel =
        EditDeviceViewModel(
            deviceId,
            getDeviceDetailUseCase,
            getDeviceTypesUseCase,
            updateDeviceUseCase,
            deleteDeviceUseCase,
        )
}

class EditDeviceViewModel(
    private val deviceId: String,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase,
) : ViewModel() {
    val uiState: StateFlow<EditDeviceScreenState> = combine(
        getDeviceDetailUseCase(deviceId),
        getDeviceTypesUseCase(),
    ) { device, types ->
        if (device == null) {
            EditDeviceScreenState.NotFound
        } else {
            EditDeviceScreenState.Success(
                device = device,
                deviceTypes = types,
            )
        }
    }.safeStateIn(
        scope = viewModelScope,
        started = defaultWhileSubscribed(),
        initialValue = EditDeviceScreenState.Loading,
    )

    fun updateDevice(input: DeviceInput) {
        val currentState = uiState.value
        if (currentState is EditDeviceScreenState.Success) {
            viewModelScope.launch {
                val updatedDevice = currentState.device.copy(
                    name = input.name,
                    location = input.location,
                    typeId = input.typeId,
                    imagePath = input.imagePath ?: currentState.device.imagePath,
                    lastUpdated = Clock.System.now(),
                )
                updateDeviceUseCase(updatedDevice)
            }
        }
    }

    fun deleteDevice() {
        viewModelScope.launch {
            deleteDeviceUseCase(deviceId)
        }
    }
}
