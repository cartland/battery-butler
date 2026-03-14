package com.chriscartland.batterybutler.experimental.presentationcore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chriscartland.batterybutler.experimental.domain.model.CounterState
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding

@Composable
fun CounterContent(
    counterRunning: Boolean,
    appCounterRunning: Boolean,
    observeState: CounterState,
    getState: CounterState,
    onStartCounter: () -> Unit,
    onStopCounter: () -> Unit,
    onStartAppCounter: () -> Unit,
    onStopAppCounter: () -> Unit,
    onStartObserving: () -> Unit,
    onStopObserving: () -> Unit,
    onGetOnce: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Padding.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Experimental Counter",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(Padding.large))
            Text(
                text = "VM Counter",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(Padding.small))
            Text(
                text = if (counterRunning) "Running" else "Stopped",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(Padding.small))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Padding.standard, Alignment.CenterHorizontally),
            ) {
                Button(onClick = onStartCounter) {
                    Text("Start")
                }
                OutlinedButton(onClick = onStopCounter) {
                    Text("Stop")
                }
            }
            Spacer(modifier = Modifier.height(Padding.large))
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(Padding.large))
            Text(
                text = "App Counter",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(Padding.small))
            Text(
                text = if (appCounterRunning) "Running" else "Stopped",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(Padding.small))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Padding.standard, Alignment.CenterHorizontally),
            ) {
                Button(onClick = onStartAppCounter) {
                    Text("Start")
                }
                OutlinedButton(onClick = onStopAppCounter) {
                    Text("Stop")
                }
            }
            Spacer(modifier = Modifier.height(Padding.large))
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(Padding.large))
            Text(
                text = "Observe",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(Padding.small))
            CounterValueDisplay(observeState)
            Spacer(modifier = Modifier.height(Padding.small))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Padding.standard, Alignment.CenterHorizontally),
            ) {
                Button(onClick = onStartObserving) {
                    Text("Start")
                }
                OutlinedButton(onClick = onStopObserving) {
                    Text("Stop")
                }
            }
            Spacer(modifier = Modifier.height(Padding.large))
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(Padding.large))
            Text(
                text = "Get",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(Padding.small))
            CounterValueDisplay(getState)
            Spacer(modifier = Modifier.height(Padding.small))
            Button(onClick = onGetOnce) {
                Text("Get Once")
            }
        }
    }
}

@Composable
private fun CounterValueDisplay(state: CounterState) {
    when (state) {
        is CounterState.Idle -> {
            Text(
                text = "—",
                style = MaterialTheme.typography.displayLarge,
            )
        }
        is CounterState.Loading -> {
            CircularProgressIndicator()
        }
        is CounterState.Active -> {
            Text(
                text = "${state.value}",
                style = MaterialTheme.typography.displayLarge,
            )
        }
        is CounterState.Error -> {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentIdlePreview() {
    BatteryButlerTheme {
        CounterContent(
            counterRunning = false,
            appCounterRunning = false,
            observeState = CounterState.Idle,
            getState = CounterState.Idle,
            onStartCounter = {},
            onStopCounter = {},
            onStartAppCounter = {},
            onStopAppCounter = {},
            onStartObserving = {},
            onStopObserving = {},
            onGetOnce = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentLoadingPreview() {
    BatteryButlerTheme {
        CounterContent(
            counterRunning = true,
            appCounterRunning = true,
            observeState = CounterState.Loading,
            getState = CounterState.Loading,
            onStartCounter = {},
            onStopCounter = {},
            onStartAppCounter = {},
            onStopAppCounter = {},
            onStartObserving = {},
            onStopObserving = {},
            onGetOnce = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentActivePreview() {
    BatteryButlerTheme {
        CounterContent(
            counterRunning = true,
            appCounterRunning = true,
            observeState = CounterState.Active(value = 42),
            getState = CounterState.Active(value = 7),
            onStartCounter = {},
            onStopCounter = {},
            onStartAppCounter = {},
            onStopAppCounter = {},
            onStartObserving = {},
            onStopObserving = {},
            onGetOnce = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentErrorPreview() {
    BatteryButlerTheme {
        CounterContent(
            counterRunning = false,
            appCounterRunning = false,
            observeState = CounterState.Error(message = "Failed to observe counter"),
            getState = CounterState.Error(message = "Failed to read counter"),
            onStartCounter = {},
            onStopCounter = {},
            onStartAppCounter = {},
            onStopAppCounter = {},
            onStartObserving = {},
            onStopObserving = {},
            onGetOnce = {},
        )
    }
}
