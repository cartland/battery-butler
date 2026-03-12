package com.chriscartland.batterybutler.experimental.composeapp.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.experimental.composeapp.navigation.ExperimentalScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalHomeScreen(
    onNavigate: (ExperimentalScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Experimental") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Counter",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = {
                        Text("Observe and get a counter value")
                    },
                    modifier = Modifier.clickable {
                        onNavigate(ExperimentalScreen.Counter)
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
