package com.yourorg.systemcore.domain.model

/**
 * Real network state read from [android.net.ConnectivityManager] via a live
 * NetworkCallback. Nothing here is polled or estimated - it reflects the platform's
 * own view of connectivity at the moment it changed.
 */
data class NetworkStatus(
    val isConnected: Boolean,
    val transport: NetworkTransport,
    val isMetered: Boolean,
    val isValidated: Boolean,       // has verified internet access, not just link-layer connectivity
    val downstreamKbps: Int,        // link-reported downstream bandwidth estimate
    val upstreamKbps: Int
) {
    companion object {
        fun disconnected() = NetworkStatus(
            isConnected = false,
            transport = NetworkTransport.NONE,
            isMetered = false,
            isValidated = false,
            downstreamKbps = 0,
            upstreamKbps = 0
        )
    }
}

enum class NetworkTransport { WIFI, CELLULAR, ETHERNET, VPN, BLUETOOTH, NONE, UNKNOWN }
