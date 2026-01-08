package com.chriscartland.batterybutler.viewmodel.devicetypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.usecase.DeleteDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceTypeUseCase
import kotlinx.coroutines.flow.SharingStarted
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
    fun create(typeId: String): EditDeviceTypeViewModel = EditDeviceTypeViewModel(
        typeId,
        getDeviceTypesUseCase,
        updateDeviceTypeUseCase,
        deleteDeviceTypeUseCase
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
                EditDeviceTypeUiState.Success(type)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
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

sealed interface EditDeviceTypeUiState {
    data object Loading : EditDeviceTypeUiState

    data object NotFound : EditDeviceTypeUiState

    data class Success(
        val deviceType: DeviceType,
    ) : EditDeviceTypeUiState
}
