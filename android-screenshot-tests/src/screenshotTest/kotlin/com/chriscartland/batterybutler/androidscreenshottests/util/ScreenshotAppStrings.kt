package com.chriscartland.batterybutler.androidscreenshottests.util

import androidx.compose.runtime.Composable
import com.chriscartland.batterybutler.composeresources.AppStrings
import org.jetbrains.compose.resources.StringResource

/**
 * Implementation of [AppStrings] for screenshot tests.
 *
 * This class delegates string resolution to a map composed of key-value pairs loaded from `strings.xml`.
 * It uses a [provider] lambda to lazily load the strings only when first requested, avoiding unnecessary
 * I/O during test class initialization.
 *
 * @param provider A lambda that returns the map of string resources. Defaults to loading from the
 * standard relative path: `../compose-resources/src/commonMain/composeResources/values/strings.xml`.
 */
class ScreenshotAppStrings(
    provider: () -> Map<String, String> = {
        XmlStringLoader.loadStrings("../compose-resources/src/commonMain/composeResources/values/strings.xml")
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
