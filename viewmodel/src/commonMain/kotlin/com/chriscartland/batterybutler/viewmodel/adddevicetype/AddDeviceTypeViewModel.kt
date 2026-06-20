package com.chriscartland.batterybutler.viewmodel.adddevicetype

import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import com.chriscartland.batterybutler.presentationmodel.adddevicetype.AddDeviceTypeScreenState
import com.chriscartland.batterybutler.usecase.AddDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.BatchAddDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.SuggestDeviceIconUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import me.tatarka.inject.annotations.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow as ObservableMutableStateFlow

@Inject
class AddDeviceTypeViewModel(
    private val addDeviceTypeUseCase: AddDeviceTypeUseCase,
    private val batchAddDeviceTypesUseCase: BatchAddDeviceTypesUseCase,
    private val suggestDeviceIconUseCase: SuggestDeviceIconUseCase,
    private val getDeviceTypesUseCase: com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase,
    private val featureFlagProvider: FeatureFlagProvider,
) : ViewModel() {
    private val isAiBatchImportEnabledFlow =
        featureFlagProvider.observeEnabled(FeatureFlag.AI_BATCH_IMPORT)

    // Exposed to SwiftUI: must use the observable factory so @StateViewModel re-renders.
    // (Private funnel flows below stay plain — they combine into the observable uiState.)
    private val _actionError = ObservableMutableStateFlow<String?>(viewModelScope, null)
    val actionError: StateFlow<String?> = _actionError

    fun dismissActionError() {
        _actionError.value = null
    }

    private val suggestIconMutex = Mutex()
    private val suggestedIconFlow = MutableStateFlow<String?>(null)
    private val isSuggestingIconFlow = MutableStateFlow(false)
    private val aiMessagesFlow = MutableStateFlow<List<BatchOperationResult>>(emptyList())

    private val usedIconsFlow = getDeviceTypesUseCase()
        .map { types -> types.mapNotNull { it.defaultIcon }.distinct() }

    val uiState: StateFlow<AddDeviceTypeScreenState> = combine(
        isAiBatchImportEnabledFlow,
        aiMessagesFlow,
        suggestedIconFlow,
        usedIconsFlow,
        isSuggestingIconFlow,
    ) { isAiEnabled, aiMessages, suggestedIcon, usedIcons, isSuggestingIcon ->
        AddDeviceTypeScreenState(
            isAiBatchImportEnabled = isAiEnabled,
            aiMessages = aiMessages,
            suggestedIcon = suggestedIcon,
            usedIcons = usedIcons,
            isSuggestingIcon = isSuggestingIcon,
        )
    }.safeStateIn(
        viewModelScope = viewModelScope,
        started = defaultWhileSubscribed(),
        initialValue = AddDeviceTypeScreenState(),
        onError = { e ->
            _actionError.value = e.message ?: "Failed to load device type data"
            AddDeviceTypeScreenState()
        },
    )

    fun suggestIcon(name: String) {
        if (name.isBlank()) return
        viewModelScope.coroutineScope.launch {
            // Use tryLock to atomically check-and-acquire, preventing race conditions
            // If already suggesting (mutex held), skip this request
            if (!suggestIconMutex.tryLock()) return@launch
            try {
                isSuggestingIconFlow.value = true
                when (val result = suggestDeviceIconUseCase(name)) {
                    is Result.Success -> {
                        val icon = result.data
                        if (icon != "default") {
                            suggestedIconFlow.value = icon
                        }
                    }

                    is Result.Error -> {
                        _actionError.value = result.error.message
                    }
                }
            } finally {
                isSuggestingIconFlow.value = false
                suggestIconMutex.unlock()
            }
        }
    }

    fun consumeSuggestedIcon() {
        suggestedIconFlow.value = null
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addDeviceType(input: DeviceTypeInput) {
        viewModelScope.coroutineScope.launch {
            val newType = DeviceType(
                id = Uuid.random().toString(),
                name = input.name,
                defaultIcon = input.defaultIcon,
                batteryType = input.batteryType,
                batteryQuantity = input.batteryQuantity,
            )
            when (val result = addDeviceTypeUseCase(newType)) {
                is Result.Success -> { /* success */ }

                is Result.Error -> {
                    _actionError.value = result.error.message
                }
            }
        }
    }

    fun batchAddDeviceTypes(input: String) {
        viewModelScope.coroutineScope.launch {
            batchAddDeviceTypesUseCase(input).collect { message ->
                aiMessagesFlow.update { it + message }
            }
        }
    }

    fun clearAiMessages() {
        aiMessagesFlow.value = emptyList()
    }
}
