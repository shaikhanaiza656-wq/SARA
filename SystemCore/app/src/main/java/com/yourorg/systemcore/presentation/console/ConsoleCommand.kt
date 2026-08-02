package com.yourorg.systemcore.presentation.console

/**
 * Commands the console currently understands. Unrecognized input resolves to [Unknown]
 * with the raw text preserved, rather than being silently swallowed or faked.
 */
sealed class ConsoleCommand {
    data object BatteryStatus : ConsoleCommand()
    data object NetworkStatus : ConsoleCommand()
    data object Help : ConsoleCommand()
    data class Unknown(val raw: String) : ConsoleCommand()

    companion object {
        fun parse(input: String): ConsoleCommand {
            return when (input.trim().lowercase()) {
                "battery", "battery status", "battery.status" -> BatteryStatus
                "network", "network status", "network.status" -> NetworkStatus
                "help", "?" -> Help
                else -> Unknown(input)
            }
        }
    }
}
