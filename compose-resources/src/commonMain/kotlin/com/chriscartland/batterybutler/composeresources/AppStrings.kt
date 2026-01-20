package com.chriscartland.batterybutler.composeresources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.StringResource

/**
 * Abstraction for string resource resolution.
 *
 * This interface decouples the application code from the underlying string loading mechanism.
 * It allows for different implementations to be swapped in at runtime via [LocalAppStrings], which
 * is particularly useful for:
 * 1. Screenshot tests (loading strings from XML without a full Compose environment).
 * 2. Previews (using placeholder strings if resources aren't available).
 * 3. Production (delegating to the standard Compose Multiplatform resource system).
 */
interface AppStrings {
    @Composable
    fun resolve(resource: StringResource): String

    @Composable
    fun resolve(
        resource: StringResource,
        vararg args: Any,
    ): String
}

/**
 * CompositionLocal to provide the [AppStrings] implementation to the composition tree.
 * Defaults to [PlaceholderAppStrings] which returns mock values.
 */
val LocalAppStrings = staticCompositionLocalOf<AppStrings> {
    PlaceholderAppStrings()
}

/**
 * Global helper function to resolve a string resource using the current [LocalAppStrings] provider.
 * This is the primary way to load strings in Composable functions, replacing direct calls to
 * `org.jetbrains.compose.resources.stringResource`.
 */
@Composable
fun composeStringResource(resource: StringResource): String = LocalAppStrings.current.resolve(resource)

@Composable
fun composeStringResource(
    resource: StringResource,
    vararg args: Any,
): String = LocalAppStrings.current.resolve(resource, *args)
