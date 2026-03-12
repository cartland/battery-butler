package com.chriscartland.batterybutler.experimental.composeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.chriscartland.batterybutler.experimental.composeapp.di.ExperimentalAppComponent
import com.chriscartland.batterybutler.experimental.composeapp.feature.ExperimentalHomeScreen
import com.chriscartland.batterybutler.experimental.composeapp.navigation.ExperimentalScreen
import com.chriscartland.batterybutler.experimental.composeapp.util.ExperimentalScreenListSaver
import com.chriscartland.batterybutler.experimental.presentationcore.CounterContent
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme

@Composable
fun ExperimentalApp(component: ExperimentalAppComponent) {
    BatteryButlerTheme {
        val backStack = rememberSaveable(saver = ExperimentalScreenListSaver) {
            mutableStateListOf<ExperimentalScreen>(ExperimentalScreen.Home)
        }
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<ExperimentalScreen.Home> {
                    ExperimentalHomeScreen(
                        onNavigate = { backStack.add(it) },
                    )
                }
                entry<ExperimentalScreen.Counter> {
                    val vm = viewModel { component.counterViewModel }
                    val observeState by vm.observeState.collectAsStateWithLifecycle()
                    val getState by vm.getState.collectAsStateWithLifecycle()
                    CounterContent(
                        observeState = observeState,
                        getState = getState,
                        onStart = vm::start,
                        onStop = vm::stop,
                        onGet = vm::get,
                    )
                }
            },
        )
    }
}
