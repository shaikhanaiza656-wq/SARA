package com.yourorg.systemcore.presentation.systemwindow

import com.yourorg.systemcore.domain.model.BatteryStatus
import com.yourorg.systemcore.domain.model.NetworkStatus

data class SystemWindowUiState(
    val battery: BatteryStatus? = null,      // null until the first real reading arrives
    val network: NetworkStatus? = null,      // null until the first real reading arrives
    val consoleLog: List<String> = emptyList(),
    val batteryPanelExpanded: Boolean = true,
    val networkPanelExpanded: Boolean = true
)
