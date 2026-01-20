package com.chriscartland.batterybutler.composeapp

import androidx.compose.runtime.Composable
import com.chriscartland.batterybutler.presentationcore.resources.AppStrings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Production implementation of [AppStrings].
 *
 * This class delegates string resolution to the standard `org.jetbrains.compose.resources.stringResource`
 * function. It is intended to be provided at the root of the application (e.g., in `App.kt`) so that
 * the real string resources are used in the running app.
 */
class ComposeAppStrings : AppStrings {
    @Composable
    override fun resolve(resource: StringResource): String = stringResource(resource)

    @Composable
    override fun resolve(
        resource: StringResource,
        vararg args: Any,
    ): String = stringResource(resource, *args)
}
