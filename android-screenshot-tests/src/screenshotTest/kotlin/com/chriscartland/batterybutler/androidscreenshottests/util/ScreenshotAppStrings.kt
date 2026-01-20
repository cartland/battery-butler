package com.chriscartland.batterybutler.androidscreenshottests.util

import androidx.compose.runtime.Composable
import com.chriscartland.batterybutler.presentationcore.resources.AppStrings
import org.jetbrains.compose.resources.StringResource

class ScreenshotAppStrings(
    provider: () -> Map<String, String> = {
        XmlStringLoader.loadStrings("../presentation-feature/src/commonMain/composeResources/values/strings.xml")
    },
) : AppStrings {
    private val strings: Map<String, String> by lazy { provider() }

    @Composable
    override fun resolve(resource: StringResource): String = strings[resource.key] ?: "MISSING: ${resource.key}"

    @Composable
    override fun resolve(
        resource: StringResource,
        vararg args: Any,
    ): String {
        val template = strings[resource.key] ?: "MISSING: ${resource.key}"
        return if (args.isNotEmpty()) {
            try {
                template.format(*args)
            } catch (e: Exception) {
                template
            }
        } else {
            template
        }
    }
}
