package com.termuxai.app.core.network

/**
 * Represents the real, observable state of the connection to the Termux AI server.
 * This is the single source of truth the UI (status bar, dashboard card,
 * developer window) renders from. No state here is faked — each value is only
 * ever set from an actual OkHttp WebSocket callback or an actual reconnect
 * scheduling decision.
 */
sealed interface ConnectionState {

    /** No socket exists and no reconnect attempt is scheduled. */
    data object Disconnected : ConnectionState

    /** connect() was called and we are waiting for onOpen/onFailure. */
    data object Connecting : ConnectionState

    /** onOpen fired. The socket is live and send() will attempt delivery. */
    data object Connected : ConnectionState

    /**
     * The socket dropped (onFailure / unexpected onClosed) and we are waiting
     * [nextRetryInMs] before attempt number [attempt].
     */
    data class Reconnecting(val attempt: Int, val nextRetryInMs: Long) : ConnectionState

    /**
     * A terminal-for-now failure was reported by OkHttp. [willRetry] tells the
     * UI whether a reconnect is still scheduled or whether the max attempt
     * count was reached and the user needs to intervene (e.g. re-check that
     * the Termux server is running).
     */
    data class Error(val message: String, val willRetry: Boolean) : ConnectionState
}
