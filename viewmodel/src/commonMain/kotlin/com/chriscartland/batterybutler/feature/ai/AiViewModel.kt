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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.atStartOfDayIn
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
                                        // smart-creation: check if exists
                                        val existingTypes = kotlinx.coroutines.flow.first(deviceRepository.getAllDeviceTypes())
                                        val existingType = existingTypes.find { it.name == typeName }
                                        
                                        if (existingType != null) {
                                            existingType.id
                                        } else {
                                            val newTypeId = uuid4().toString()
                                            deviceRepository.addDeviceType(
                                                DeviceType(
                                                    id = newTypeId,
                                                    name = typeName,
                                                    defaultIcon = "default",
                                                ),
                                            )
                                            newTypeId
                                        }
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
                                val deviceName = args["deviceName"] as? String ?: return@ToolHandler "Error: Missing deviceName"
                                val dateStr = args["date"] as? String ?: return@ToolHandler "Error: Missing date"
                                val deviceTypeName = args["deviceType"] as? String
                                
                                try {
                                    // 1. Find or Create Device
                                    val existingDevices = kotlinx.coroutines.flow.first(deviceRepository.getAllDevices())
                                    var device = existingDevices.find { it.name == deviceName } // Exact match per user request
                                    
                                    if (device == null) {
                                        // Find or Create Device Type
                                        val typeId = if (!deviceTypeName.isNullOrBlank()) {
                                            val existingTypes = kotlinx.coroutines.flow.first(deviceRepository.getAllDeviceTypes())
                                            val existingType = existingTypes.find { it.name == deviceTypeName }
                                            
                                            if (existingType != null) {
                                                existingType.id
                                            } else {
                                                val newTypeId = uuid4().toString()
                                                deviceRepository.addDeviceType(DeviceType(newTypeId, deviceTypeName, "default"))
                                                newTypeId
                                            }
                                        } else {
                                            "default_type"
                                        }

                                        val newDevice = Device(
                                            id = uuid4().toString(),
                                            name = deviceName,
                                            typeId = typeId,
                                            batteryLastReplaced = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
                                            lastUpdated = Clock.System.now(),
                                        )
                                        deviceRepository.addDevice(newDevice)
                                        device = newDevice
                                    }

                                    // Ensure non-null for updates
                                    val targetDevice = device!!

                                    // 2. Parse Date
                                    val date = kotlinx.datetime.LocalDate.parse(dateStr)
                                    val instant = date.atStartOfDayIn(kotlinx.datetime.TimeZone.currentSystemDefault())

                                    // 3. Add Battery Event
                                    val event = com.chriscartland.batterybutler.domain.model.BatteryEvent(
                                        id = uuid4().toString(),
                                        deviceId = targetDevice.id,
                                        date = instant,
                                        batteryType = args["batteryType"] as? String ?: "Unknown", // Assuming generic
                                        notes = "Imported via AI"
                                    )
                                    deviceRepository.addEvent(event)

                                    // 4. Update Device if newer
                                    if (instant > targetDevice.batteryLastReplaced) {
                                        deviceRepository.updateDevice(targetDevice.copy(batteryLastReplaced = instant))
                                    }

                                    "Success: Recorded battery replacement for '$deviceName' on $dateStr"
                                } catch (e: Exception) {
                                    "Error recording battery replacement: ${e.message}"
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
