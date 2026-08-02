package com.termuxai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.termuxai.app.R
import com.termuxai.app.core.AiState
import com.termuxai.app.core.network.ConnectionState
import com.termuxai.app.ui.theme.StateListening
import com.termuxai.app.ui.theme.StateSleeping
import com.termuxai.app.ui.theme.StateSpeaking
import com.termuxai.app.ui.theme.StateThinking
import com.termuxai.app.ui.theme.StatusConnected
import com.termuxai.app.ui.theme.StatusConnecting
import com.termuxai.app.ui.theme.StatusDisconnected
import com.termuxai.app.ui.theme.StatusError
import com.termuxai.app.viewmodel.AssistantViewModel
import com.termuxai.app.viewmodel.ConnectionViewModel

/**
 * Top-level dashboard. All three data sources here are real, live flows —
 * [ConnectionViewModel.connectionState] mirrors the actual OkHttp WebSocket
 * state, [AssistantViewModel.aiState] mirrors [com.termuxai.app.core.AiStateHolder]
 * (only ever set by the wake word engine/services), and the message log is
 * only ever appended to from actual envelopes received from Termux. Nothing
 * on this screen is mocked or hardcoded sample data.
 */
@Composable
fun DashboardScreen(
    connectionViewModel: ConnectionViewModel,
    assistantViewModel: AssistantViewModel
) {
    val connectionState by connectionViewModel.connectionState.collectAsStateWithLifecycle()
    val aiState by assistantViewModel.aiState.collectAsStateWithLifecycle()
    val messageLog by assistantViewModel.messageLog.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            ConnectionStatusRow(connectionState)

            Spacer(Modifier.height(24.dp))

            AiStateAvatar(aiState)

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isConnectedOrConnecting = connectionState is ConnectionState.Connected ||
                    connectionState is ConnectionState.Connecting ||
                    connectionState is ConnectionState.Reconnecting

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (isConnectedOrConnecting) {
                            connectionViewModel.stopConnection()
                        } else {
                            connectionViewModel.startConnection()
                        }
                    }
                ) {
                    Text(
                        stringResource(
                            if (isConnectedOrConnecting) R.string.dashboard_disconnect
                            else R.string.dashboard_connect
                        )
                    )
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (aiState == AiState.SLEEPING) {
                            assistantViewModel.startWakeWordEngine()
                        } else {
                            assistantViewModel.stopWakeWordEngine()
                        }
                    }
                ) {
                    Text(
                        stringResource(
                            if (aiState == AiState.SLEEPING) R.string.dashboard_start_wake_word
                            else R.string.dashboard_stop_wake_word
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            CommandInputRow(onSend = assistantViewModel::sendCommand)

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.dashboard_log_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            MessageLog(entries = messageLog, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ConnectionStatusRow(state: ConnectionState) {
    val color = when (state) {
        is ConnectionState.Connected -> StatusConnected
        is ConnectionState.Connecting, is ConnectionState.Reconnecting -> StatusConnecting
        is ConnectionState.Error -> StatusError
        is ConnectionState.Disconnected -> StatusDisconnected
    }
    val label = when (state) {
        is ConnectionState.Connected -> stringResource(R.string.connection_status_connected)
        is ConnectionState.Connecting -> stringResource(R.string.connection_status_connecting)
        is ConnectionState.Reconnecting -> stringResource(
            R.string.connection_status_reconnecting, state.attempt
        )
        is ConnectionState.Error -> state.message
        is ConnectionState.Disconnected -> stringResource(R.string.connection_status_disconnected)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AiStateAvatar(state: AiState) {
    val color = when (state) {
        AiState.SLEEPING -> StateSleeping
        AiState.LISTENING -> StateListening
        AiState.THINKING -> StateThinking
        AiState.SPEAKING -> StateSpeaking
    }
    val label = when (state) {
        AiState.SLEEPING -> "Sleeping"
        AiState.LISTENING -> "Listening"
        AiState.THINKING -> "Thinking"
        AiState.SPEAKING -> "Speaking"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.height(12.dp))
        Text(text = label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CommandInputRow(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.dashboard_command_placeholder)) },
            singleLine = true
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            onSend(text)
            text = ""
        }) {
            Text(stringResource(R.string.dashboard_send))
        }
    }
}

@Composable
private fun MessageLog(entries: List<String>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.dashboard_log_empty),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(12.dp)) {
                items(entries.asReversed()) { entry ->
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
