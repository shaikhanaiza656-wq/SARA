package com.yourorg.systemcore.presentation.systemwindow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourorg.systemcore.data.battery.BatteryRepository
import com.yourorg.systemcore.data.network.NetworkRepository
import com.yourorg.systemcore.presentation.console.ConsoleCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SystemWindowViewModel @Inject constructor(
    private val batteryRepository: BatteryRepository,
    private val networkRepository: NetworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemWindowUiState())
    val uiState: StateFlow<SystemWindowUiState> = _uiState.asStateFlow()

    init {
        observeBattery()
        observeNetwork()
    }

    private fun observeBattery() {
        viewModelScope.launch {
            batteryRepository.observeStatus().collect { status ->
                _uiState.update { it.copy(battery = status) }
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkRepository.observeStatus().collect { status ->
                _uiState.update { it.copy(network = status) }
            }
        }
    }

    fun toggleBatteryPanel() {
        _uiState.update { it.copy(batteryPanelExpanded = !it.batteryPanelExpanded) }
    }

    fun toggleNetworkPanel() {
        _uiState.update { it.copy(networkPanelExpanded = !it.networkPanelExpanded) }
    }

    fun submitCommand(rawInput: String) {
        if (rawInput.isBlank()) return
        val command = ConsoleCommand.parse(rawInput)
        val response = when (command) {
            is ConsoleCommand.BatteryStatus -> {
                val battery = _uiState.value.battery
                if (battery == null) {
                    "battery: no reading yet"
                } else {
                    "battery: ${battery.percentage}% · " +
                        "${if (battery.isCharging) "charging" else "not charging"} · " +
                        "${battery.chargePlug} · ${battery.temperatureCelsius}°C"
                }
            }
            is ConsoleCommand.NetworkStatus -> {
                val network = _uiState.value.network
                if (network == null) {
                    "network: no reading yet"
                } else {
                    "network: ${if (network.isConnected) "connected" else "disconnected"} · " +
                        "${network.transport} · " +
                        "${if (network.isMetered) "metered" else "unmetered"} · " +
                        "${network.downstreamKbps}kbps down / ${network.upstreamKbps}kbps up"
                }
            }
            is ConsoleCommand.Help -> "commands: battery, network, help"
            is ConsoleCommand.Unknown -> "unrecognized command: '${command.raw}'"
        }

        _uiState.update {
            it.copy(consoleLog = it.consoleLog + "> $rawInput" + response)
        }
    }
}
