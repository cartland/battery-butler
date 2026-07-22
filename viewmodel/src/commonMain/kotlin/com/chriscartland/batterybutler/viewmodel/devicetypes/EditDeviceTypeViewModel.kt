package com.chriscartland.batterybutler.viewmodel.devicetypes

import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.presentationmodel.devicetypes.EditDeviceTypeScreenState
import com.chriscartland.batterybutler.usecase.DeleteDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceTypeUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class EditDeviceTypeViewModelFactory(
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val updateDeviceTypeUseCase: UpdateDeviceTypeUseCase,
    private val deleteDeviceTypeUseCase: DeleteDeviceTypeUseCase,
) {
    fun create(typeId: String): EditDeviceTypeViewModel =
        EditDeviceTypeViewModel(
            typeId,
            getDeviceTypesUseCase,
            updateDeviceTypeUseCase,
            deleteDeviceTypeUseCase,
        )
}

class EditDeviceTypeViewModel(
    private val typeId: String,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val updateDeviceTypeUseCase: UpdateDeviceTypeUseCase,
    private val deleteDeviceTypeUseCase: DeleteDeviceTypeUseCase,
) : ViewModel() {
    val uiState: StateFlow<EditDeviceTypeScreenState> = getDeviceTypesUseCase()
        .map { types ->
            val type = types.find { it.id == typeId }
            if (type == null) {
                EditDeviceTypeScreenState.NotFound
            } else {
                val usedIcons = types.mapNotNull { it.defaultIcon }.distinct()
                EditDeviceTypeScreenState.Success(type, usedIcons)
            }
        }.safeStateIn(
            viewModelScope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = EditDeviceTypeScreenState.Loading,
            // See DeviceTypeDetailViewModel: transient DB failure -> NotFound (logged), not a wedge.
            onError = { EditDeviceTypeScreenState.NotFound },
        )

    fun updateDeviceType(input: DeviceTypeInput) {
        val currentState = uiState.value
        if (currentState is EditDeviceTypeScreenState.Success) {
            viewModelScope.coroutineScope.launch {
                val updatedType = currentState.deviceType.copy(
                    name = input.name,
                    batteryType = input.batteryType,
                    batteryQuantity = input.batteryQuantity,
                    defaultIcon = input.defaultIcon,
                )
                updateDeviceTypeUseCase(updatedType)
            }
        }
    }

    fun deleteDeviceType() {
        viewModelScope.coroutineScope.launch {
            deleteDeviceTypeUseCase(typeId)
        }
    }
}
