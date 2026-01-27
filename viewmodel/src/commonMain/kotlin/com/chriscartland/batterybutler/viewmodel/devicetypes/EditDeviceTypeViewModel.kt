package com.chriscartland.batterybutler.viewmodel.devicetypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.presentationmodel.devicetypes.EditDeviceTypeUiState
import com.chriscartland.batterybutler.usecase.DeleteDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceTypeUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val uiState: StateFlow<EditDeviceTypeUiState> = getDeviceTypesUseCase()
        .map { types ->
            val type = types.find { it.id == typeId }
            if (type == null) {
                EditDeviceTypeUiState.NotFound
            } else {
                val usedIcons = types.mapNotNull { it.defaultIcon }.distinct()
                EditDeviceTypeUiState.Success(type, usedIcons)
            }
        }.stateIn(
            scope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = EditDeviceTypeUiState.Loading,
        )

    fun updateDeviceType(input: DeviceTypeInput) {
        val currentState = uiState.value
        if (currentState is EditDeviceTypeUiState.Success) {
            viewModelScope.launch {
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
        viewModelScope.launch {
            deleteDeviceTypeUseCase(typeId)
        }
    }
}
