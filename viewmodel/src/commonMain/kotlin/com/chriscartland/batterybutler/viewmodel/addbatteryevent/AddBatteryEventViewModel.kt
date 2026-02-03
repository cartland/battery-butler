package com.chriscartland.batterybutler.viewmodel.addbatteryevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import com.chriscartland.batterybutler.usecase.AddBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.BatchAddBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.time.Instant

@Inject
class AddBatteryEventViewModel(
    private val addBatteryEventUseCase: AddBatteryEventUseCase,
    private val getDevicesUseCase: GetDevicesUseCase,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
    private val batchAddBatteryEventsUseCase: BatchAddBatteryEventsUseCase,
    private val featureFlagProvider: FeatureFlagProvider,
) : ViewModel() {
    val isAiBatchImportEnabled: StateFlow<Boolean> =
        featureFlagProvider
            .observeEnabled(FeatureFlag.AI_BATCH_IMPORT)
            .stateIn(
                scope = viewModelScope,
                started = defaultWhileSubscribed(),
                initialValue = featureFlagProvider.isEnabled(FeatureFlag.AI_BATCH_IMPORT),
            )

    private val _aiMessages = MutableStateFlow<List<BatchOperationResult>>(emptyList())
    val aiMessages: StateFlow<List<BatchOperationResult>> = _aiMessages

    val devices = getDevicesUseCase().stateIn(
        scope = viewModelScope,
        started = defaultWhileSubscribed(),
        initialValue = emptyList(),
    )

    fun addEvent(
        deviceId: String,
        date: Instant,
        batteryType: String?,
        notes: String?,
    ) {
        viewModelScope.launch {
            addBatteryEventUseCase(
                BatteryEvent(
                    id = uuid4().toString(),
                    deviceId = deviceId,
                    date = date,
                    batteryType = batteryType,
                    notes = notes,
                ),
            )
            // Need to update device batteryLastReplaced?
            // Domain repo or UseCase should ideally handle this logic consistency.
            // But for manually added single event:
            val device = getDeviceDetailUseCase(deviceId).first()
            if (device != null && date > device.batteryLastReplaced) {
                updateDeviceUseCase(device.copy(batteryLastReplaced = date))
            }
        }
    }

    fun batchAddEvents(input: String) {
        viewModelScope.launch {
            batchAddBatteryEventsUseCase(input).collect { message ->
                _aiMessages.update { it + message }
            }
        }
    }

    fun clearAiMessages() {
        _aiMessages.value = emptyList()
    }
}
