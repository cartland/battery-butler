package com.chriscartland.batterybutler.viewmodel.adddevicetype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.usecase.AddDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.BatchAddDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.SuggestDeviceIconUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Inject
class AddDeviceTypeViewModel(
    private val addDeviceTypeUseCase: AddDeviceTypeUseCase,
    private val batchAddDeviceTypesUseCase: BatchAddDeviceTypesUseCase,
    private val suggestDeviceIconUseCase: SuggestDeviceIconUseCase,
    private val getDeviceTypesUseCase: com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase,
) : ViewModel() {
    private val _suggestedIcon = MutableStateFlow<String?>(null)
    val suggestedIcon = _suggestedIcon.asStateFlow()

    val usedIcons: StateFlow<List<String>> = getDeviceTypesUseCase()
        .map { types -> types.mapNotNull { it.defaultIcon }.distinct() }
        .stateIn(
            scope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = emptyList(),
        )

    fun suggestIcon(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val icon = suggestDeviceIconUseCase(name)
            if (icon != null && icon != "default") {
                _suggestedIcon.value = icon
            }
        }
    }

    fun consumeSuggestedIcon() {
        _suggestedIcon.value = null
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addDeviceType(input: DeviceTypeInput) {
        viewModelScope.launch {
            val newType = DeviceType(
                id = Uuid.random().toString(),
                name = input.name,
                defaultIcon = input.defaultIcon,
                batteryType = input.batteryType,
                batteryQuantity = input.batteryQuantity,
            )
            addDeviceTypeUseCase(newType)
        }
    }

    private val _aiMessages = kotlinx.coroutines.flow
        .MutableStateFlow<List<com.chriscartland.batterybutler.domain.model.BatchOperationResult>>(
            emptyList(),
        )
    val aiMessages: StateFlow<List<com.chriscartland.batterybutler.domain.model.BatchOperationResult>> = _aiMessages

    fun batchAddDeviceTypes(input: String) {
        viewModelScope.launch {
            batchAddDeviceTypesUseCase(input).collect { message ->
                _aiMessages.update { it + message }
            }
        }
    }

    fun clearAiMessages() {
        _aiMessages.value = emptyList()
    }
}
