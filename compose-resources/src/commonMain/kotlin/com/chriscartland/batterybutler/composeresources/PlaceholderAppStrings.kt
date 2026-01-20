package com.chriscartland.batterybutler.composeresources

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource

/**
 * Default implementation of [AppStrings] used when no other provider is supplied.
 *
 * This class returns mock strings (e.g., "Mock: string_key") rather than looking up actual values.
 * It ensures that Previews and tests do not crash if [LocalAppStrings] is not explicitly provided,
 * while still giving a visual indication of which string resource is being requested.
 */
class PlaceholderAppStrings : AppStrings {
    @Composable
    override fun resolve(resource: StringResource): String = "Mock: ${resource.key}"

    @Composable
    override fun resolve(
        resource: StringResource,
        vararg args: Any,
    ): String = "Mock: ${resource.key} args=${args.joinToString()}"
}
