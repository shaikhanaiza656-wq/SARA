package com.yourorg.systemcore.presentation.console

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourorg.systemcore.core.ui.theme.CyanCore
import com.yourorg.systemcore.core.ui.theme.TextSecondary
import com.yourorg.systemcore.core.ui.theme.glassPanel

@Composable
fun ConsolePanel(
    log: List<String>,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel()
            .padding(18.dp)
    ) {
        Text(
            text = "CONSOLE",
            style = MaterialTheme.typography.titleMedium,
            color = CyanCore
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(vertical = 10.dp)
        ) {
            items(log) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (line.startsWith(">")) CyanCore else TextSecondary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("enter command…", color = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = CyanCore,
                    unfocusedBorderColor = TextSecondary,
                    cursorColor = CyanCore
                )
            )
            IconButton(onClick = {
                onSubmit(input)
                input = ""
            }) {
                Icon(imageVector = Icons.Filled.Send, contentDescription = "Submit command", tint = CyanCore)
            }
        }
    }
}
