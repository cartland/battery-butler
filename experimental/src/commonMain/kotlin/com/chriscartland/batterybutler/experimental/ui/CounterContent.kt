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
import androidx.compose.material3.MaterialTheme
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
    state: CounterState,
    onStart: () -> Unit,
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
            when (state) {
                is CounterState.Idle -> {
                    Text(
                        text = "Press Start to begin counting",
                        style = MaterialTheme.typography.bodyLarge,
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
            Spacer(modifier = Modifier.height(Padding.large))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Padding.standard, Alignment.CenterHorizontally),
            ) {
                Button(onClick = onStart) {
                    Text("Start")
                }
                Button(onClick = onGet) {
                    Text("Get")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentIdlePreview() {
    BatteryButlerTheme {
        CounterContent(
            state = CounterState.Idle,
            onStart = {},
            onGet = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentLoadingPreview() {
    BatteryButlerTheme {
        CounterContent(
            state = CounterState.Loading,
            onStart = {},
            onGet = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentActivePreview() {
    BatteryButlerTheme {
        CounterContent(
            state = CounterState.Active(value = 42),
            onStart = {},
            onGet = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentErrorPreview() {
    BatteryButlerTheme {
        CounterContent(
            state = CounterState.Error(message = "Failed to read counter"),
            onStart = {},
            onGet = {},
        )
    }
}
