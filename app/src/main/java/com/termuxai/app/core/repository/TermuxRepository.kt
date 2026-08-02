package com.termuxai.app.core.repository

import com.termuxai.app.core.network.ConnectionState
import com.termuxai.app.core.network.TermuxEnvelope
import com.termuxai.app.core.network.TermuxWebSocketClient
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single access point ViewModels use to talk to Termux. This is the
 * repository layer in the app's Clean Architecture split:
 *
 *   Compose UI  ->  ViewModel  ->  TermuxRepository  ->  TermuxWebSocketClient
 *
 * ViewModels never touch OkHttp, WebSocketListener, or the Service directly —
 * only this interface. That keeps the transport swappable (e.g. if a later
 * version adds a fallback transport) without touching UI code, and makes the
 * ViewModel layer trivially fakeable in unit tests without needing a real
 * socket.
 */
interface TermuxRepository {
    val connectionState: StateFlow<ConnectionState>
    val incomingMessages: SharedFlow<TermuxEnvelope>
    val serverUrl: String

    fun connect(url: String = serverUrl)
    fun disconnect()

    /**
     * @return true if the envelope was handed to the socket. false means
     * there is currently no live connection — callers (e.g. the command
     * queue) decide whether to hold the message for retry or surface an
     * error to the user.
     */
    fun send(envelope: TermuxEnvelope): Boolean
}

class DefaultTermuxRepository(
    private val client: TermuxWebSocketClient
) : TermuxRepository {
    override val connectionState: StateFlow<ConnectionState> get() = client.connectionState
    override val incomingMessages: SharedFlow<TermuxEnvelope> get() = client.incomingMessages
    override val serverUrl: String get() = client.serverUrl

    override fun connect(url: String) = client.connect(url)
    override fun disconnect() = client.disconnect()
    override fun send(envelope: TermuxEnvelope): Boolean = client.send(envelope)
}
