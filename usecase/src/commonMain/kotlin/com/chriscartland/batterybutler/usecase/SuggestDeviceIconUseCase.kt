package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.DeviceIcons
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@Inject
class SuggestDeviceIconUseCase(
    private val aiEngine: AiEngine,
) {
    suspend operator fun invoke(typeName: String): String? {
        val availableIcons = DeviceIcons.AvailableIcons.joinToString(", ")
        val prompt = "Select the best matching icon for a device type named '$typeName' from the following list: [$availableIcons]. " +
            "Return ONLY the icon name from the list. If no good match is found, return 'default'."

        return try {
            aiEngine
                .generateResponse(prompt, null)
                .map { it.text }
                .firstOrNull { it?.isNotBlank() == true }
                ?.trim()
                ?.filter { !it.isWhitespace() } // Simple cleanup
        } catch (e: Exception) {
            null
        }
    }
}
