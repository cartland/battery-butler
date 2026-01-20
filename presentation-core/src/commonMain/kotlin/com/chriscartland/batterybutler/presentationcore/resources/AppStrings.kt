package com.chriscartland.batterybutler.presentationcore.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.StringResource

interface AppStrings {
    @Composable
    fun resolve(resource: StringResource): String

    @Composable
    fun resolve(
        resource: StringResource,
        vararg args: Any,
    ): String
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> {
    PlaceholderAppStrings()
}

@Composable
fun composeStringResource(resource: StringResource): String = LocalAppStrings.current.resolve(resource)

@Composable
fun composeStringResource(
    resource: StringResource,
    vararg args: Any,
): String = LocalAppStrings.current.resolve(resource, *args)
