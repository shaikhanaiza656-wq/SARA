package com.yourorg.systemcore.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.yourorg.systemcore.domain.model.NetworkStatus
import com.yourorg.systemcore.domain.model.NetworkTransport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads real network state from the platform via ConnectivityManager.registerDefaultNetworkCallback,
 * which reports changes to whichever network the system currently treats as the default -
 * the same signal the OS itself uses. No polling, no simulated network events.
 *
 * Requires the ACCESS_NETWORK_STATE permission (declared in the manifest) - this is a
 * normal, non-dangerous permission and does not require a runtime prompt.
 */
@Singleton
class NetworkDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observeNetworkStatus(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                trySend(capabilities.toNetworkStatus())
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.disconnected())
            }

            override fun onUnavailable() {
                trySend(NetworkStatus.disconnected())
            }
        }

        // Seed the flow with the current state immediately, rather than waiting for the
        // first callback - registerDefaultNetworkCallback does not deliver one synchronously.
        val activeNetwork = connectivityManager.activeNetwork
        val activeCapabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        trySend(activeCapabilities?.toNetworkStatus() ?: NetworkStatus.disconnected())

        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    private fun NetworkCapabilities.toNetworkStatus(): NetworkStatus {
        val transport = when {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.CELLULAR
            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransport.ETHERNET
            hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkTransport.VPN
            hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkTransport.BLUETOOTH
            else -> NetworkTransport.UNKNOWN
        }

        return NetworkStatus(
            isConnected = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            transport = transport,
            isMetered = !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            isValidated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            downstreamKbps = linkDownstreamBandwidthKbps,
            upstreamKbps = linkUpstreamBandwidthKbps
        )
    }
}
