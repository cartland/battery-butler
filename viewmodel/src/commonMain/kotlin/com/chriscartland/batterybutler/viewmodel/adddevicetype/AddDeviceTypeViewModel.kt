package com.chriscartland.batterybutler.viewmodel.adddevicetype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import com.chriscartland.batterybutler.presentationmodel.adddevicetype.AddDeviceTypeUiState
import com.chriscartland.batterybutler.usecase.AddDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.BatchAddDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.SuggestDeviceIconUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import me.tatarka.inject.annotations.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

    private val suggestIconMutex = Mutex()
    private val suggestedIconFlow = MutableStateFlow<String?>(null)
    private val isSuggestingIconFlow = MutableStateFlow(false)
    private val aiMessagesFlow = MutableStateFlow<List<BatchOperationResult>>(emptyList())

    private val usedIconsFlow = getDeviceTypesUseCase()
        .map { types -> types.mapNotNull { it.defaultIcon }.distinct() }

    val uiState: StateFlow<AddDeviceTypeUiState> = combine(
        isAiBatchImportEnabledFlow,
        aiMessagesFlow,
        suggestedIconFlow,
        usedIconsFlow,
        isSuggestingIconFlow,
    ) { isAiEnabled, aiMessages, suggestedIcon, usedIcons, isSuggestingIcon ->
        AddDeviceTypeUiState(
            isAiBatchImportEnabled = isAiEnabled,
            aiMessages = aiMessages,
            suggestedIcon = suggestedIcon,
            usedIcons = usedIcons,
            isSuggestingIcon = isSuggestingIcon,
        )
    }.stateIn(
        scope = viewModelScope,
        started = defaultWhileSubscribed(),
        initialValue = AddDeviceTypeUiState(),
    )

    fun suggestIcon(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            // Use tryLock to atomically check-and-acquire, preventing race conditions
            // If already suggesting (mutex held), skip this request
            if (!suggestIconMutex.tryLock()) return@launch
            try {
                isSuggestingIconFlow.value = true
                val icon = suggestDeviceIconUseCase(name)
                if (icon != null && icon != "default") {
                    suggestedIconFlow.value = icon
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

    fun batchAddDeviceTypes(input: String) {
        viewModelScope.launch {
            batchAddDeviceTypesUseCase(input).collect { message ->
                aiMessagesFlow.update { it + message }
            }
        }
    }

    fun clearAiMessages() {
        aiMessagesFlow.value = emptyList()
    }
}
