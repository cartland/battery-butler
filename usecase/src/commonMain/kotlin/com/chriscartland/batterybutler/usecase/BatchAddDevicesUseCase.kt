package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject

@Inject
class BatchAddDevicesUseCase(
    private val aiEngine: AiEngine,
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(input: String): Flow<AiMessage> = channelFlow {
        val lines = input.lines().filter { it.isNotBlank() }
        
        for (line in lines) {
            val modelMsgId = uuid4().toString()
            // Send initial empty message or partials?
            // For simplicity, we just stream what AiEngine gives us.
            
            val toolHandler = ToolHandler { name, args ->
                when (name) {
                    "addDevice" -> {
                        val name = args["name"] as? String ?: return@ToolHandler "Error: Missing name"
                        val typeName = args["type"] as? String
                        
                        try {
                            val typeId = if (!typeName.isNullOrBlank()) {
                                // Smart deduplication
                                val existingTypes: List<DeviceType> = deviceRepository.getAllDeviceTypes().first()
                                val existingType = existingTypes.find { type -> type.name == typeName }
                                
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
                                "default_type"
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
                    else -> "Error: This tool is not supported in this context. Use 'addDevice' only." // Restrictive
                }
            }

            try {
                aiEngine.generateResponse(line, toolHandler).collect { tokenMsg ->
                    // Re-emit messages to the caller
                    // We might want to wrap them or just pass through the text
                    send(AiMessage(modelMsgId, AiRole.MODEL, tokenMsg.text))
                }
            } catch (e: Exception) {
                 send(AiMessage(uuid4().toString(), AiRole.SYSTEM, "Error processing '$line': ${e.message}"))
            }
        }
    }
}
