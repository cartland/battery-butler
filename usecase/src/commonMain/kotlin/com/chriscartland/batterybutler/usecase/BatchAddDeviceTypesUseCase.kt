package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

@Inject
class BatchAddDeviceTypesUseCase(
    private val aiEngine: AiEngine,
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(input: String): Flow<AiMessage> =
        channelFlow {
            val lines = input.lines().filter { it.isNotBlank() }

            for (line in lines) {
                val modelMsgId = uuid4().toString()

                val toolHandler = ToolHandler { name, args ->
                    when (name) {
                        "addDeviceType" -> {
                            val name = args["name"] as? String ?: return@ToolHandler "Error: Missing name"
                            val iconName = args["icon"] as? String ?: "default"

                            try {
                                // Smart deduplication
                                val existingTypes: List<DeviceType> = deviceRepository.getAllDeviceTypes().first()
                                val existingType = existingTypes.find { type -> type.name == name }

                                if (existingType != null) {
                                    "Success: Device type '$name' already exists."
                                } else {
                                    deviceRepository.addDeviceType(
                                        DeviceType(
                                            id = uuid4().toString(),
                                            name = name,
                                            defaultIcon = iconName,
                                        ),
                                    )
                                    "Success: Added device type '$name'"
                                }
                            } catch (e: Exception) {
                                "Error adding device type: ${e.message}"
                            }
                        }
                        else -> "Error: This tool is not supported in this context. Use 'addDeviceType' only."
                    }
                }

                try {
                    aiEngine.generateResponse(line, toolHandler).collect { tokenMsg ->
                        send(AiMessage(modelMsgId, AiRole.MODEL, tokenMsg.text))
                    }
                } catch (e: Exception) {
                    send(AiMessage(uuid4().toString(), AiRole.SYSTEM, "Error processing '$line': ${e.message}"))
                }
            }
        }
}
