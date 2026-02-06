package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_batch_import_ai
import com.chriscartland.batterybutler.composeresources.generated.resources.action_process_ai
import com.chriscartland.batterybutler.composeresources.generated.resources.label_ai_output
import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import org.jetbrains.compose.resources.StringResource

/**
 * Reusable AI batch import section for adding items via natural language.
 *
 * Provides a text input field with AI processing button and displays
 * operation results in a scrollable list.
 *
 * @param aiMessages List of batch operation results to display
 * @param placeholderRes String resource for the input placeholder text
 * @param onBatchAdd Callback when user submits text for AI processing
 * @param modifier Modifier for the root container
 */
@Composable
fun AiBatchImportSection(
    aiMessages: List<BatchOperationResult>,
    placeholderRes: StringResource,
    onBatchAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            composeStringResource(Res.string.action_batch_import_ai),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        var aiInput by rememberSaveable { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = aiInput,
                onValueChange = { aiInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(composeStringResource(placeholderRes)) },
                maxLines = 3,
            )
            IconButton(
                onClick = {
                    if (aiInput.isNotBlank()) {
                        onBatchAdd(aiInput)
                        aiInput = ""
                    }
                },
                enabled = aiInput.isNotBlank(),
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = composeStringResource(Res.string.action_process_ai),
                )
            }
        }

        if (aiMessages.isNotEmpty()) {
            Text(
                composeStringResource(Res.string.label_ai_output),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(8.dp),
            ) {
                items(aiMessages) { result ->
                    BatchOperationResultItem(result = result)
                }
            }
        }
    }
}
