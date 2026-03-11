package com.chriscartland.batterybutler.experimental.ui

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
import com.chriscartland.batterybutler.experimental.model.CounterState
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding

@Composable
fun CounterContent(
    observeState: CounterState,
    getState: CounterState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onGet: () -> Unit,
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
                text = "Observe",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(Padding.small))
            CounterValueDisplay(observeState)
            Spacer(modifier = Modifier.height(Padding.small))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Padding.standard, Alignment.CenterHorizontally),
            ) {
                Button(onClick = onStart) {
                    Text("Start")
                }
                OutlinedButton(onClick = onStop) {
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
            Button(onClick = onGet) {
                Text("Get")
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
            observeState = CounterState.Idle,
            getState = CounterState.Idle,
            onStart = {},
            onStop = {},
            onGet = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentLoadingPreview() {
    BatteryButlerTheme {
        CounterContent(
            observeState = CounterState.Loading,
            getState = CounterState.Loading,
            onStart = {},
            onStop = {},
            onGet = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentActivePreview() {
    BatteryButlerTheme {
        CounterContent(
            observeState = CounterState.Active(value = 42),
            getState = CounterState.Active(value = 7),
            onStart = {},
            onStop = {},
            onGet = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentErrorPreview() {
    BatteryButlerTheme {
        CounterContent(
            observeState = CounterState.Error(message = "Failed to observe counter"),
            getState = CounterState.Error(message = "Failed to read counter"),
            onStart = {},
            onStop = {},
            onGet = {},
        )
    }
}
