package com.termuxai.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.termuxai.app.TermuxApplication
import com.termuxai.app.core.network.ConnectionState
import com.termuxai.app.core.network.TermuxEnvelope
import com.termuxai.app.core.repository.TermuxRepository
import com.termuxai.app.core.service.TermuxConnectionService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/**
 * Exposed to the top status bar, the dashboard "Connection Status" card, and
 * the Developer window's "Reconnect WebSocket" action. Holds no connection
 * logic itself -- it only forwards to [TermuxRepository] and starts/stops the
 * foreground service that keeps that repository's socket alive when the app
 * is backgrounded.
 */
class ConnectionViewModel(
    private val appContext: Context,
    private val repository: TermuxRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    /** Called once, e.g. from the launcher Activity's onCreate. */
    fun startConnection() {
        TermuxConnectionService.start(appContext)
    }

    fun stopConnection() {
        TermuxConnectionService.stop(appContext)
    }

    /** Developer window "Reconnect WebSocket" action. */
    fun forceReconnect() {
        repository.disconnect()
        repository.connect()
    }

    /**
     * Sends a raw command string. The reply arrives asynchronously via
     * repository.incomingMessages -- this function does not block waiting
     * for it; the UI layer collects incomingMessages separately to render
     * results.
     */
    fun sendCommand(text: String) {
        viewModelScope.launch {
            val sent = repository.send(
                TermuxEnvelope(
                    type = "command.execute",
                    payload = JsonPrimitive(text)
                )
            )
            if (!sent) {
                // No live socket right now. A higher-level CommandQueue
                // (next module) is responsible for persisting/retrying this;
                // this ViewModel only reports transport-level success.
            }
        }
    }

    companion object {
        fun factory(appContext: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = appContext.applicationContext as TermuxApplication
                return ConnectionViewModel(appContext, app.container.termuxRepository) as T
            }
        }
    }
}
