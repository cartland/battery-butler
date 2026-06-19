package com.chriscartland.batterybutler.viewmodel.adddevice

import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import com.chriscartland.batterybutler.usecase.AddDeviceUseCase
import com.chriscartland.batterybutler.usecase.BatchAddDevicesUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Inject
class AddDeviceViewModel(
    private val addDeviceUseCase: AddDeviceUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val batchAddDevicesUseCase: BatchAddDevicesUseCase,
    private val featureFlagProvider: FeatureFlagProvider,
) : ViewModel() {
    val isAiBatchImportEnabled: StateFlow<Boolean> =
        featureFlagProvider
            .observeEnabled(FeatureFlag.AI_BATCH_IMPORT)
            .safeStateIn(
                viewModelScope = viewModelScope,
                started = defaultWhileSubscribed(),
                initialValue = featureFlagProvider.isEnabled(FeatureFlag.AI_BATCH_IMPORT),
            )

    val deviceTypes: StateFlow<List<DeviceType>> = getDeviceTypesUseCase()
        .safeStateIn(
            viewModelScope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = emptyList(),
            onError = { e ->
                _actionError.value = e.message ?: "Failed to load device types"
                emptyList()
            },
        )

    private val _isLoading = MutableStateFlow(viewModelScope, false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _actionError = MutableStateFlow<String?>(viewModelScope, null)
    val actionError: StateFlow<String?> = _actionError

    fun dismissActionError() {
        _actionError.value = null
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addDevice(input: DeviceInput) {
        viewModelScope.coroutineScope.launch {
            _isLoading.value = true
            val newDevice = Device(
                id = Uuid.random().toString(),
                name = input.name,
                location = input.location,
                typeId = input.typeId,
                imagePath = input.imagePath,
                batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                lastUpdated = Clock.System.now(),
            )
            when (val result = addDeviceUseCase(newDevice)) {
                is Result.Success -> { /* success */ }

                is Result.Error -> {
                    _actionError.value = result.error.message
                }
            }
            _isLoading.value = false
        }
    }

    private val _aiMessages = MutableStateFlow<List<BatchOperationResult>>(viewModelScope, emptyList())
    val aiMessages: StateFlow<List<BatchOperationResult>> = _aiMessages

    fun batchAddDevices(input: String) {
        viewModelScope.coroutineScope.launch {
            batchAddDevicesUseCase(input).collect { message ->
                _aiMessages.update { it + message }
            }
        }
    }

    fun clearAiMessages() {
        _aiMessages.value = emptyList()
    }
}
