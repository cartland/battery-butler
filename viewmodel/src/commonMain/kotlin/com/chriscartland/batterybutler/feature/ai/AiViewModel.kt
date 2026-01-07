package com.chriscartland.batterybutler.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject

@Inject
class AiViewModel(
    private val aiEngine: AiEngine,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {
    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    val isAiAvailable = aiEngine.isAvailable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun sendMessage(text: String) {
        val userMsgId = uuid4().toString()
        val userMsg = AiMessage(userMsgId, AiRole.USER, text)
        _messages.value += userMsg

        viewModelScope.launch {
            try {
                // Split input by newlines to handle batch requests
                val lines = text.lines().filter { it.isNotBlank() }
                
                for (line in lines) {
                    // Accumulate tokens for streaming effect
                    val modelMsgId = uuid4().toString()
                    var accumulatedText = ""

                    val toolHandler = ToolHandler { name, args ->
                        when (name) {
                            "addDevice" -> {
                                val name = args["name"] as? String ?: return@ToolHandler "Error: Missing name"
                                // Scheme provides 'type' (name of type), not typeId
                                val typeName = args["type"] as? String
                                
                                try {
                                    val typeId = if (!typeName.isNullOrBlank()) {
                                        // Create a new type on the fly or find it (simplification: always create new or use predictable ID?)
                                        // We'll create a new one for now to ensure constraint satisfaction
                                        val newTypeId = uuid4().toString()
                                        deviceRepository.addDeviceType(
                                            DeviceType(
                                                id = newTypeId,
                                                name = typeName,
                                                defaultIcon = "default",
                                            ),
                                        )
                                        newTypeId
                                    } else {
                                        "default_type" // Sentinel or error if FK constraint exists
                                    }

                                    deviceRepository.addDevice(
                                        Device(
                                            id = uuid4().toString(),
                                            name = name,
                                            typeId = typeId,
                                            batteryLastReplaced = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
                                            lastUpdated = Clock.System.now(),
                                        ),
                                    )
                                    "Success: Added device '$name' (Type: ${typeName ?: "Default"})"
                                } catch (e: Exception) {
                                    "Error adding device: ${e.message}"
                                }
                            }
                            "addDeviceType" -> {
                                val name = args["name"] as? String ?: return@ToolHandler "Error: Missing name"
                                val iconName = args["icon"] as? String ?: "default"
                                // Validate icon?
                                try {
                                    deviceRepository.addDeviceType(
                                        DeviceType(
                                            id = uuid4().toString(),
                                            name = name,
                                            defaultIcon = iconName, // simplified
                                        ),
                                    )
                                    "Success: Added device type '$name'"
                                } catch (e: Exception) {
                                    "Error adding device type: ${e.message}"
                                }
                            }
                            else -> "Error: Unknown tool '$name'"
                        }
                    }

                    // Assuming generateResponse emits partial updates of text (tokens)
                    aiEngine.generateResponse(line, toolHandler).collect { tokenMsg ->
                        // We assume the engine returns the FULL text so far OR just tokens.
                        // Let's assume it returns chunks/tokens for now, so we append.
                        // Wait, if AiEngine returns AiMessage, it implies structure.
                        // Let's decide: AiEngine emits accumulated AiMessage.
                        // Then the VM just updates state.
                        updateOrAddMessage(modelMsgId, AiRole.MODEL, tokenMsg.text)
                    }
                }
            } catch (e: Exception) {
                val errorId = uuid4().toString()
                updateOrAddMessage(errorId, AiRole.SYSTEM, "Error: ${e.message}")
            }
        }
    }

    private fun updateOrAddMessage(
        id: String,
        role: AiRole,
        text: String,
    ) {
        val current = _messages.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(text = text)
        } else {
            current.add(AiMessage(id, role, text))
        }
        _messages.value = current
    }
}
