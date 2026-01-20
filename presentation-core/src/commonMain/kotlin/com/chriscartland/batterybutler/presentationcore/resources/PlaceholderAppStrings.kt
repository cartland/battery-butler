package com.chriscartland.batterybutler.presentationcore.resources

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource

class PlaceholderAppStrings : AppStrings {
    @Composable
    override fun resolve(resource: StringResource): String = "Mock: ${resource.key}"

    @Composable
    override fun resolve(
        resource: StringResource,
        vararg args: Any,
    ): String = "Mock: ${resource.key} args=${args.joinToString()}"
}
