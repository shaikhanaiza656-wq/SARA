package com.yourorg.systemcore.presentation.systemwindow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourorg.systemcore.core.ui.theme.CyanDim
import com.yourorg.systemcore.core.ui.theme.NavyBase
import com.yourorg.systemcore.core.ui.theme.NavyVoid
import com.yourorg.systemcore.presentation.console.ConsolePanel
import com.yourorg.systemcore.presentation.panels.BatteryPanel
import com.yourorg.systemcore.presentation.panels.NetworkPanel

@Composable
fun SystemWindowScreen(
    viewModel: SystemWindowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyVoid, NavyBase)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SystemHeader() }

            item {
                BatteryPanel(
                    status = uiState.battery,
                    expanded = uiState.batteryPanelExpanded,
                    onToggle = viewModel::toggleBatteryPanel
                )
            }

            item {
                NetworkPanel(
                    status = uiState.network,
                    expanded = uiState.networkPanelExpanded,
                    onToggle = viewModel::toggleNetworkPanel
                )
            }

            item {
                ConsolePanel(
                    log = uiState.consoleLog,
                    onSubmit = viewModel::submitCommand
                )
            }
        }
    }
}

@Composable
private fun SystemHeader() {
    Column {
        Text(
            text = "SYSTEM CORE",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "device monitoring · online",
            style = MaterialTheme.typography.bodyMedium,
            color = CyanDim
        )
    }
}
