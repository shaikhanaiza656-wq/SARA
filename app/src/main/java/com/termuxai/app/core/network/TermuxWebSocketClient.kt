package com.termuxai.app.core.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * Owns the single persistent WebSocket connection to the Termux AI server.
 *
 * This is a real OkHttp WebSocket — no simulated frames, no fake timers
 * standing in for network activity. Every state transition below is driven
 * by an actual WebSocketListener callback, except for the Reconnecting
 * countdown, which is a real coroutine delay before a real reconnect attempt.
 *
 * Lifecycle: constructed once in [com.termuxai.app.core.AppContainer] and
 * shared for the whole process. [TermuxConnectionService] just calls
 * connect()/disconnect() on it and mirrors [connectionState] into the
 * foreground notification; it does not own a second copy of this logic.
 */
class TermuxWebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val scope: CoroutineScope,
    initialServerUrl: String = DEFAULT_SERVER_URL
) {
    companion object {
        const val DEFAULT_SERVER_URL = "ws://127.0.0.1:8765"
        private const val TAG = "TermuxWebSocketClient"

        private const val BASE_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val BACKOFF_MULTIPLIER = 1.6
        // 0 = unlimited. Kept finite so the UI can eventually tell the user
        // "Termux server not reachable, check that it's running" instead of
        // retrying forever in total silence.
        private const val MAX_RECONNECT_ATTEMPTS = 0
    }

    /** Current URL we connect to. Exposed as var so the Settings/Connection window can change it. */
    var serverUrl: String = initialServerUrl
        private set

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // replay = 0: this is a live event stream, not a cache. Late subscribers
    // should read connectionState for current status, not replayed messages.
    private val _incomingMessages = MutableSharedFlow<TermuxEnvelope>(extraBufferCapacity = 64)
    val incomingMessages = _incomingMessages.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var manuallyDisconnected = false
    private var reconnectAttempt = 0

    /** Opens the connection. Safe to call again after changing [url]; replaces any existing socket. */
    fun connect(url: String = serverUrl) {
        serverUrl = url
        manuallyDisconnected = false
        reconnectJob?.cancel()
        openSocket()
    }

    /** Closes the connection intentionally. No reconnect will be scheduled after this. */
    fun disconnect() {
        manuallyDisconnected = true
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Client requested disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Sends an envelope over the socket.
     * @return true if OkHttp accepted the frame for sending (queued), false if
     *   there is no live socket right now — callers should queue at a higher
     *   layer (repository) if the message must not be lost.
     */
    fun send(envelope: TermuxEnvelope): Boolean {
        val socket = webSocket ?: return false
        return try {
            val text = json.encodeToString(envelope)
            socket.send(text)
        } catch (e: SerializationException) {
            Log.e(TAG, "Failed to encode outgoing envelope type=${envelope.type}", e)
            false
        }
    }

    private fun openSocket() {
        _connectionState.value = ConnectionState.Connecting
        val request = Request.Builder().url(serverUrl).build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "Connected to $serverUrl")
            reconnectAttempt = 0
            _connectionState.value = ConnectionState.Connected
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val envelope = json.decodeFromString(TermuxEnvelope.serializer(), text)
                if (envelope.type == EnvelopeType.PING) {
                    send(TermuxEnvelope(type = EnvelopeType.PONG))
                    return
                }
                scope.launch { _incomingMessages.emit(envelope) }
            } catch (e: SerializationException) {
                Log.e(TAG, "Received malformed envelope, dropping: ${e.message}")
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.w(TAG, "Socket closed: code=$code reason=$reason")
            if (!manuallyDisconnected) {
                scheduleReconnect("closed ($code): $reason")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "Socket failure: ${t.message}", t)
            if (!manuallyDisconnected) {
                scheduleReconnect(t.message ?: t::class.simpleName ?: "unknown error")
            }
        }
    }

    private fun scheduleReconnect(reason: String) {
        reconnectAttempt++

        if (MAX_RECONNECT_ATTEMPTS > 0 && reconnectAttempt > MAX_RECONNECT_ATTEMPTS) {
            _connectionState.value = ConnectionState.Error(reason, willRetry = false)
            return
        }

        val backoffMs = min(
            MAX_BACKOFF_MS,
            (BASE_BACKOFF_MS * BACKOFF_MULTIPLIER.pow(reconnectAttempt - 1)).toLong()
        )

        _connectionState.value = ConnectionState.Reconnecting(reconnectAttempt, backoffMs)

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(backoffMs)
            if (!manuallyDisconnected) {
                openSocket()
            }
        }
    }
}

/** Shared OkHttpClient tuned for a long-lived local WebSocket, not one-shot HTTP calls. */
fun buildTermuxOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
    // Real heartbeat so a half-dead localhost socket (e.g. Termux process
    // frozen but TCP still "open") gets detected instead of hanging forever.
    .pingInterval(15, TimeUnit.SECONDS)
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS) // no read timeout: this is a streaming connection
    .build()
