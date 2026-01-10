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
    private val systemInstructions =
        """
        Select the best matching icon for a device type from the provided list.
        Return ONLY the icon name.
        If no good match is found, return 'default'.
        """.trimIndent()

    suspend operator fun invoke(typeName: String): String? {
        val availableIcons = DeviceIcons.AvailableIcons.joinToString(", ")
        val prompt =
            """
            *** SYSTEM INSTRUCTIONS ***
            $systemInstructions
            Available Icons: [$availableIcons]

            *** USER INPUT DATA ***
            Device Type: $typeName
            """.trimIndent()

        return try {
            aiEngine
                .generateResponse(prompt, null)
                .map { it.text }
                .firstOrNull { it?.isNotBlank() == true }
                ?.trim()
                ?.filter { !it.isWhitespace() }
        } catch (e: Exception) {
            null
        }
    }
}
